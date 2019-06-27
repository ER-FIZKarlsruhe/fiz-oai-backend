package de.fiz.oai.backend.controller;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.dao.impl.CassandraDAOItem;
import de.fiz.oai.backend.models.Item;

@Path("/item")
public class ItemController extends AbstractController {

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

    daoItem.delete(identifier);
  }

  @POST
  @Consumes(MediaType.TEXT_XML)
  public void createItem(String content, @Context HttpServletRequest request, @Context HttpServletResponse response) {
    System.out.println(content);
  }

  public DAOItem getDaoItem() {
    return daoItem;
  }

  public void setDaoItem(DAOItem daoItem) {
    this.daoItem = daoItem;
  }

}
