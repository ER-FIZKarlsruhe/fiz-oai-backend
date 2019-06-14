package de.fiz.oai.backend.controller;

import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.dao.impl.CassandraDAOItem;
import de.fiz.oai.backend.models.Item;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/item")
public class ItemController extends AbstractController{

    @Context
    ServletContext servletContext;

    DAOItem daoItem = new CassandraDAOItem();

    @GET
    @Path("/{identifier}")
    @Produces(MediaType.APPLICATION_XML)
    public String getItem(@PathParam("identifier") String identifier, @Context HttpServletRequest request,
                          @Context HttpServletResponse response)
            throws Exception {
        checkApplicationReady();

        final Item item = daoItem.read(identifier);

        if (item != null) {
            return item.getContent();
        }

        return null;
    }

    @DELETE
    @Path("/{identifier}")
    public void deleteItem(@PathParam("identifier") String identifier, @Context HttpServletRequest request,
                           @Context HttpServletResponse response)
            throws Exception {
        checkApplicationReady();

        daoItem.delete(identifier);
    }


    @POST
    @Consumes(MediaType.TEXT_XML)
    public void createItem(String content, @Context HttpServletRequest request,
                           @Context HttpServletResponse response) {
        checkApplicationReady();

        System.out.println(content);
    }

}
