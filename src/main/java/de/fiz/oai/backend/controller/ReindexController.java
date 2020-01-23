package de.fiz.oai.backend.controller;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
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

}
