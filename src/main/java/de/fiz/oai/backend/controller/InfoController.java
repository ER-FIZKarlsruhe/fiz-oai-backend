package de.fiz.oai.backend.controller;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.utils.Configuration;

@Path("/info")
public class InfoController extends AbstractController{

    private Logger LOGGER = LoggerFactory.getLogger(InfoController.class);

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public String getVersion() throws IOException {

        LOGGER.debug("getVersion called");
        Configuration.getInstance().getProperty("name");
        return "0.1.0";
    }
    
    @GET
    @Path("/configuration")
    @Produces(MediaType.TEXT_PLAIN)
    public String getConfigInfo() throws IOException {
      StringBuilder builder = new StringBuilder();
      for (Map.Entry<Object, Object> entry : Configuration.getInstance().getProperties().entrySet()) {
          if (entry.getKey().toString().toLowerCase().contains("password")) {
              builder.append(entry.getKey() + " : ***********\n");
          }
          else {
              builder.append(entry.getKey() + " : " + entry.getValue() + "\n");
          }
      }
      
      return builder.toString();
    }
    

}