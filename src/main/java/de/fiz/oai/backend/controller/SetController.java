package de.fiz.oai.backend.controller;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Set;
import de.fiz.oai.backend.service.SetService;

@Path("/set")
public class SetController extends AbstractController {

  @Context
  ServletContext servletContext;

  @Inject
  SetService setService;

  private Logger LOGGER = LoggerFactory.getLogger(SetController.class);
  
  
  @GET
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  public Set getSet(@PathParam("name") String name ,  @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws Exception {

    if (name == null || StringUtils.isBlank(name)) {
      throw new BadRequestException("name QueryParam cannot be empty!");
    }
    
    final Set set = setService.read(name);
    
    if (set == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    return set;
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<Set> getAllSets( @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws Exception {
    final List<Set> setList = setService.readAll();
    
    return setList;
  }
  
  @DELETE
  @Path("/{name}")
  public void deleteSet(@PathParam("name") String name, @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws Exception {

    if (StringUtils.isBlank(name)) {
      throw new BadRequestException("name to delete cannot be empty!");
    }

    try {
      setService.delete(name);
    } catch (NotFoundException e) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }
  }
  
  
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Set createSet( Set set, @Context HttpServletRequest request, @Context HttpServletResponse response) {
    
    if (StringUtils.isBlank( set.getName())) {
      throw new WebApplicationException("Set name cannot be empty!", Status.BAD_REQUEST);
    }

    if (StringUtils.isBlank(set.getSpec())) {
      throw new WebApplicationException("Set spec cannot be empty!", Status.BAD_REQUEST);
    }
    
    if (!Pattern.matches( "([A-Za-z0-9\\-_\\.!~\\*'\\(\\)])+(:[A-Za-z0-9\\-_\\.!~\\*'\\(\\)]+)*", set.getSpec() ) ) {
      throw new WebApplicationException("Set spec does not match regex!", Status.BAD_REQUEST);
    }
    
    Set newSet = null;
    
    try {
      newSet = setService.create(set);
    } catch (NotFoundException nfe) {
      throw new  WebApplicationException(Status.NOT_FOUND);
    } catch (IOException e) {
      throw new  WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    } 
    return newSet;
  }
  
  @PUT
  @Path("/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Set updateSet(@PathParam("name") String name, Set set,
      @Context HttpServletRequest request, @Context HttpServletResponse response) {

    if (StringUtils.isBlank( set.getName())) {
      throw new WebApplicationException("Set name cannot be empty!", Status.BAD_REQUEST);
    }

    if (StringUtils.isBlank(set.getSpec())) {
      throw new WebApplicationException("Set spec cannot be empty!", Status.BAD_REQUEST);
    }
    
    if (!Pattern.matches( "([A-Za-z0-9\\-_\\.!~\\*'\\(\\)])+(:[A-Za-z0-9\\-_\\.!~\\*'\\(\\)]+)*", set.getSpec() ) ) {
      throw new WebApplicationException("Set spec does not match regex!", Status.BAD_REQUEST);
    }

    if (!name.equals(set.getName())) {
      throw new WebApplicationException("The name in the path and the set json does not match!", Status.BAD_REQUEST);
    }
    
    Set updateSet = null;
    
    try {
      updateSet = setService.update(set);
    } catch (NotFoundException nfe) {
      throw new  WebApplicationException(Status.NOT_FOUND);
    } catch (IOException e) {
      throw new  WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    } 
    
    return updateSet;
  }
  
}
