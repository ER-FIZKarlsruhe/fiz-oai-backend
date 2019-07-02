package de.fiz.oai.backend.controller;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

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
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.service.ItemService;
import de.fiz.oai.backend.service.impl.ItemServiceImpl;

@Path("/item")
public class ItemController extends AbstractController {

  SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-DD'T'hh:mm:ss'Z'");

  @Context
  ServletContext servletContext;

  @Inject
  ItemService itemService = new ItemServiceImpl();
  
  private Logger LOGGER = LoggerFactory.getLogger(ItemController.class);

  @GET
  @Path("/{identifier}")
  @Produces(MediaType.APPLICATION_JSON)
  public Item getItem(@PathParam("identifier") String identifier, @QueryParam("format") String format ,  @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws Exception {

    if (StringUtils.isBlank(format)) {
      throw new BadRequestException("format QueryParam cannot be empty!");
    }
    
    final Item item = itemService.read(identifier);
    LOGGER.info("getItem: " + item);
    
    if (item == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    return item;
  }
  
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public SearchResult<Item> searchItems(
      @QueryParam("offset") Integer offset, 
      @QueryParam("rows") Integer rows, 
      @QueryParam("set") String set, 
      @QueryParam("format") String format, 
      @QueryParam("from") String from , 
      @QueryParam("until") String until , 
      @QueryParam("content") String content , 
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws Exception {

    LOGGER.debug("offset: " + offset);
    LOGGER.debug("rows: " + rows);
    LOGGER.debug("set: " + set);
    LOGGER.debug("format: " + format);
    LOGGER.debug("from: " + from);
    LOGGER.debug("until: " + until);
    LOGGER.debug("content: " + content);
    
    if (StringUtils.isBlank(format)) {
      throw new BadRequestException("format QueryParam cannot be empty!");
    }
    
    try {
      if (!StringUtils.isBlank(from)) {
        dateFormat.parse(from);
      }
    } catch(ParseException e) {
      throw new BadRequestException("Invalid from QueryParam!");
    }
    
    try {
      if (!StringUtils.isBlank(until)) {
        dateFormat.parse(until);
      }
    } catch(ParseException e) {
      throw new BadRequestException("Invalid until QueryParam!");
    }

    SearchResult<Item> result = itemService.search(offset, rows, set, format, from, until);

    return result;
  }
  
  

  @DELETE
  @Path("/{identifier}")
  public void deleteItem(@PathParam("identifier") String identifier, @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws Exception {

    if (StringUtils.isBlank(identifier)) {
      throw new BadRequestException("identifier to delete cannot be empty!");
    }

    try {
      itemService.delete(identifier);
    } catch (NotFoundException e) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Item createItem(@FormDataParam("content") String content, @FormDataParam("item") Item item,
      @Context HttpServletRequest request, @Context HttpServletResponse response) {
    LOGGER.info("createItem item: " + item.toString());

    if (!content.contains(item.getIdentifier())) {
      throw new WebApplicationException("Cannot find the identifier in the content!", Status.BAD_REQUEST);
    }
    Item newItem = null;
    
    try {
      newItem = itemService.create(item);
    } catch (NotFoundException e) {
      throw new WebApplicationException(Status.NOT_FOUND);
    } catch (Exception e) {
      LOGGER.error("An unexpected exception occured", e);
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
    
    LOGGER.info("createItem content: " + content);
    
    return newItem;
  }
  
  @PUT
  @Path("/{identifier}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Item updateItem(@PathParam("identifier") String identifier, @FormDataParam("content") String content, @FormDataParam("item") Item item,
      @Context HttpServletRequest request, @Context HttpServletResponse response) {

    if (!identifier.equals(item.getIdentifier())) {
      throw new WebApplicationException("The identifier in the path and the item json does not match!", Status.BAD_REQUEST);
    }
    
    if (!content.contains(identifier)) {
      throw new WebApplicationException("Cannot find the identifier in the content!", Status.BAD_REQUEST);
    }
    
    Item updateItem = null;
    try {
      updateItem = itemService.update(item);
    } catch (NotFoundException e) {
      throw new  WebApplicationException(Status.NOT_FOUND);
    } catch (IOException e) {
      throw new  WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    } 
    
    LOGGER.info("createItem content: " + content);
    
    return updateItem;
  }

}
