package de.fiz.oai.backend.controller;

import de.fiz.oai.backend.FizOAIBackendApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("/version")
public class VersionController extends AbstractController{

    private Logger LOGGER = LoggerFactory.getLogger(VersionController.class);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getVersion() throws IOException {

        LOGGER.info("getVersion called");

        return "0.1.0";
    }

}
