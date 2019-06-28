package de.fiz.oai.backend.controller;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/version")
public class VersionController extends AbstractController{

    private Logger LOGGER = LoggerFactory.getLogger(VersionController.class);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getVersion() throws IOException {

        LOGGER.debug("getVersion called");

        return "0.1.0";
    }

}
