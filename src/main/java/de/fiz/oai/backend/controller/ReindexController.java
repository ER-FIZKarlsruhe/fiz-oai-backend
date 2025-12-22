package de.fiz.oai.backend.controller;


import java.io.IOException;
import java.util.List;
import java.util.Map;

import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.service.ItemService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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

  @Inject
  ItemService itemService;

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
  public Response startReindexAll(@QueryParam("indexName") String indexName) {
    if (searchService.reindexAll(indexName)) {
      return Response.ok("Reindex process correctly started.").build();
    }
    return Response.status(Status.CONFLICT)
            .entity("Not able to start reindex process, maybe it is already started. Please check with /status command.")
            .build();
  }

  @POST
  @Path("/item/{identifier}")
  @Produces(MediaType.TEXT_PLAIN)
  @Operation(summary = "Updates an item in the search index", description = "Updates an Item in the search index.")
  @ApiResponses({
          @ApiResponse(responseCode = "204", description = "Index document successfully updated"),
          @ApiResponse(responseCode = "404", description = "Not found")
  })
  public void reindexItem(
          @Parameter(description = "Identifier of the item", required = true) @PathParam("identifier") String identifier) throws IOException {
    Item item = itemService.read(identifier,null,false);

    if (item != null) {

      List<Map<String, Object>> itemDocs = searchService.readDocuments(List.of(item));
      if (itemDocs.isEmpty()) {
        searchService.createDocument(item);
      } else {
        searchService.updateDocument(item);
      }
    } else {
      throw new WebApplicationException("Item not found.", Status.NOT_FOUND);
    }

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
