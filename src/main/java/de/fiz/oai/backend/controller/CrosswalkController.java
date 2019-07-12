package de.fiz.oai.backend.controller;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import de.fiz.oai.backend.models.Crosswalk;
import de.fiz.oai.backend.service.CrosswalkService;

@Path("/crosswalk")
public class CrosswalkController extends AbstractController {

  @Inject
  CrosswalkService crosswalkService;

  private Logger LOGGER = LoggerFactory.getLogger(CrosswalkController.class);

  @GET
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  public Crosswalk getFormat(@PathParam("name") String name, @Context HttpServletRequest request,
      @Context HttpServletResponse response) {

    if (StringUtils.isBlank(name)) {
      throw new BadRequestException("name path parameter cannot be empty!");
    }

    Crosswalk crosswalk;
    try {
      crosswalk = crosswalkService.read(name);
    } catch (IOException e) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }

    if (crosswalk == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    return crosswalk;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<Crosswalk> getAllCrosswalks(@Context HttpServletRequest request, @Context HttpServletResponse response) {
    List<Crosswalk> crosswalks;
    try {
      crosswalks = crosswalkService.readAll();
    } catch (IOException e) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }

    return crosswalks;
  }

  @DELETE
  @Path("/{name}")
  public void deleteCrosswalk(@PathParam("name") String name, @Context HttpServletRequest request,
      @Context HttpServletResponse response) {

    if (StringUtils.isBlank(name)) {
      throw new BadRequestException("identifier path parameter cannot be empty!");
    }

    try {
      crosswalkService.delete(name);
    } catch (NotFoundException e) {
      throw new WebApplicationException(Status.NOT_FOUND);
    } catch (IOException ioe) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Crosswalk createCrosswalk(Crosswalk crosswalk, @Context HttpServletRequest request,
      @Context HttpServletResponse response) {

    if (StringUtils.isBlank(crosswalk.getName())) {
      throw new WebApplicationException("Crosswalk identifier cannot be empty!", Status.BAD_REQUEST);
    }

    if (StringUtils.isBlank(crosswalk.getFormatFrom())) {
      throw new WebApplicationException("Crosswalk format cannot be empty!", Status.BAD_REQUEST);
    }

    if (StringUtils.isBlank(crosswalk.getFormatTo())) {
      throw new WebApplicationException("Crosswalk format cannot be empty!", Status.BAD_REQUEST);
    }
    
    if (crosswalk.getXsltStylesheet() == null || crosswalk.getXsltStylesheet().isEmpty()) {
      throw new WebApplicationException("Crosswalk crosswalk cannot be empty!", Status.BAD_REQUEST);
    }

    if (!Pattern.matches("[A-Za-z0-9\\-_\\.!~\\*'\\(\\)]+", crosswalk.getFormatFrom())) {
      throw new WebApplicationException("Crosswalk formatFrom does not match regex!", Status.BAD_REQUEST);
    }
    
    if (!Pattern.matches("[A-Za-z0-9\\-_\\.!~\\*'\\(\\)]+", crosswalk.getFormatTo())) {
      throw new WebApplicationException("Crosswalk formatTo does not match regex!", Status.BAD_REQUEST);
    }
    
    Crosswalk newCrosswalk = null;

    try {
      newCrosswalk = crosswalkService.create(newCrosswalk);
    } catch (IOException e) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }

    return newCrosswalk;
  }

}
