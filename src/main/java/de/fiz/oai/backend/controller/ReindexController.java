package de.fiz.oai.backend.controller;


import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import de.fiz.oai.backend.service.SearchService;

@Path("/reindex")
public class ReindexController extends AbstractController {

  @Inject
  SearchService searchService;

  @POST
  @Path("/stop")
  public void stopReindexAll() {

    if (searchService.stopReindexAll(3, 1000)) {
      throw new WebApplicationException(Status.OK);
    }
    
    throw new WebApplicationException("Not able to stop reindex process.", Status.INTERNAL_SERVER_ERROR);

  }
  
  @POST
  @Path("/start")
  public void startReindexAll() {

      if (searchService.reindexAll()) {
        throw new WebApplicationException("Reindex process correctly started.", Status.OK);
      }
      throw new WebApplicationException("Not able to start reindex process, maybe is already started. Please check with /status command.", Status.INTERNAL_SERVER_ERROR);

  }
  
  @GET
  @Path("/status")
  @Produces(MediaType.TEXT_PLAIN)
  public String getStatus(){

    return searchService.getReindexStatusVerbose();
    
  }

}
