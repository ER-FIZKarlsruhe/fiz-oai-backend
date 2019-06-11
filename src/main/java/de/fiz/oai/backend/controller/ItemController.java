package de.fiz.oai.backend.controller;

import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.dao.impl.CassandraDAOItem;
import de.fiz.oai.backend.models.Item;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/items")
public class ItemController extends AbstractController{

    @Context
    ServletContext servletContext;

    DAOItem daoItem = new CassandraDAOItem();

    @GET
    @Path("/{identifier}")
    @Produces(MediaType.APPLICATION_XML)
    public String getItem(@PathParam("identifier") String identifier)
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
    public void deleteItem(@PathParam("identifier") String identifier)
            throws Exception {
        checkApplicationReady();

        daoItem.delete(identifier);
    }


    @POST
    @Consumes("multipart/form-data")
    public void createItem(@Multipart(value = "content", type = "text/xml") String content) {
        checkApplicationReady();

        final Item item;

    }

}
