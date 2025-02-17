package de.fiz.oai.backend.controller;


import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import de.fiz.oai.backend.service.SearchService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Path("/reindex")
@Tag(name = "ReindexController", description = "Controller for managing reindex operations")
public class ReindexController extends AbstractController {

  @Inject
  SearchService searchService;

  @POST
  @Path("/stop")
  @Operation(summary = "Stop reindexing process")
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Reindexing process stopped successfully"),
          @ApiResponse(responseCode = "500", description = "Not able to stop reindex process")
  })
  public void stopReindexAll() {

    if (searchService.stopReindexAll(3, 1000)) {
      throw new WebApplicationException(Status.OK);
    }
    
    throw new WebApplicationException("Not able to stop reindex process.", Status.INTERNAL_SERVER_ERROR);

  }
  
  @POST
  @Path("/start")
  @Operation(summary = "Start reindexing process")
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Reindexing process started successfully"),
          @ApiResponse(responseCode = "500", description = "Not able to start reindex process, maybe it is already started. Please check with /status command.")
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
  @Operation(summary = "Get reindexing status", description = "Retrieves the current reindexing status.")
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Reindexing status retrieved successfully")
  })
  public String getStatus() {

    return searchService.getReindexStatusVerbose();
    
  }

  @GET
  @Path("/commit")
  @Operation(summary = "Commit reindexing changes")
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Reindexing changes committed successfully"),
          @ApiResponse(responseCode = "500", description = "Failed to commit reindexing changes")
  })
  public void commit() throws IOException {
    searchService.commit();
  }

}
