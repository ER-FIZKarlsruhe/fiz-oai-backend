package de.fiz.oai.backend.controller;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;
import javax.servlet.ServletContext;
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
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.dao.impl.CassandraDAOItem;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Item;

@Path("/item")
public class ItemController extends AbstractController {

  SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-DD'T'hh:mm:ss'Z'");

  @Context
  ServletContext servletContext;

  @Inject
  DAOItem daoItem = new CassandraDAOItem();

  private Logger LOGGER = LoggerFactory.getLogger(ItemController.class);

  @GET
  @Path("/{identifier}")
  @Produces(MediaType.APPLICATION_JSON)
  public Item getItem(@PathParam("identifier") String identifier, @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws Exception {

    final Item item = daoItem.read(identifier);
    LOGGER.info("getItem: " + item);
    if (item != null) {
      return item;
    }

    return null;
  }

  @DELETE
  @Path("/{identifier}")
  public void deleteItem(@PathParam("identifier") String identifier, @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws Exception {

    if (StringUtils.isBlank(identifier)) {
      throw new BadRequestException("identifier to delete cannot be empty!");
    }

    try {
      daoItem.delete(identifier);
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

    Item newItem = null;
    
    //Overwrite datestamp!
    item.setDatestamp(format.format(new Date()));
    //Validate item
    //TODO ingestFormat exists?
    
    //TODO given sets exists?
    
    //TODO IngestFormat: Exists?
    //TODO Xsd Validate the content against the ingestFormat! 
    
    try {
      newItem = daoItem.create(item);
      
      //TODO create content
      
    } catch (NotFoundException e) {
      throw new WebApplicationException(Status.NOT_FOUND);
    } catch (Exception e) {
      LOGGER.error("An unexpected exception occured", e);
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
    
    LOGGER.info("createItem content: " + content);
    
    return newItem;
  }

  public DAOItem getDaoItem() {
    return daoItem;
  }

  public void setDaoItem(DAOItem daoItem) {
    this.daoItem = daoItem;
  }

}
