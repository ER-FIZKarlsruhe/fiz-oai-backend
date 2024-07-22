package de.fiz.oai.backend.controller;


import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import de.fiz.oai.backend.service.SearchService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/reindex")
@Api(value = "/reindex", tags = "ReindexController", description = "Controller for managing reindex operations")
public class ReindexController extends AbstractController {

  @Inject
  SearchService searchService;

  @POST
  @Path("/stop")
  @ApiOperation(
      value = "Stop reindexing process"
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Reindexing process stopped successfully"),
      @ApiResponse(code = 500, message = "Not able to stop reindex process")
  })
  public void stopReindexAll() {

    if (searchService.stopReindexAll(3, 1000)) {
      throw new WebApplicationException(Status.OK);
    }
    
    throw new WebApplicationException("Not able to stop reindex process.", Status.INTERNAL_SERVER_ERROR);

  }
  
  @POST
  @Path("/start")
  @ApiOperation(
      value = "Start reindexing process"
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Reindexing process started successfully"),
      @ApiResponse(code = 500, message = "Not able to start reindex process, maybe is already started. Please check with /status command.")
  })
  public void startReindexAll() {

      if (searchService.reindexAll()) {
        throw new WebApplicationException("Reindex process correctly started.", Status.OK);
      }
      throw new WebApplicationException("Not able to start reindex process, maybe is already started. Please check with /status command.", Status.INTERNAL_SERVER_ERROR);

  }
  
  @GET
  @Path("/status")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(
      value = "Get reindexing status",
      response = String.class
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Reindexing status retrieved successfully", response = String.class)
  })
  public String getStatus() {

    return searchService.getReindexStatusVerbose();
    
  }

  @GET
  @Path("/commit")
  @ApiOperation(
      value = "Commit reindexing changes"
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Reindexing changes committed successfully"),
      @ApiResponse(code = 500, message = "Failed to commit reindexing changes")
  })
  public void commit() throws IOException {
    searchService.commit();
  }

}
