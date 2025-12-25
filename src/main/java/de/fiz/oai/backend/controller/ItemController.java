/*
 * Copyright 2019 FIZ Karlsruhe - Leibniz-Institut fuer Informationsinfrastruktur GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fiz.oai.backend.controller;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.service.ItemService;
import de.fiz.oai.backend.utils.Configuration;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Path("/item")
@Tag(name = "Item Management", description = "Operations related to managing items")
public class ItemController extends AbstractController {

  @Context
  ServletContext servletContext;

  @Inject
  ItemService itemService;

  private static final Logger LOGGER = LoggerFactory.getLogger(ItemController.class);


  @GET
  @Path("/{identifier}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get Item by Identifier", description = "Retrieve an item using its identifier.")
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Successful retrieval of item"),
          @ApiResponse(responseCode = "404", description = "Item not found")
  })
  public Item getItem(
          @Parameter(description = "Identifier of the item", required = true) @PathParam("identifier") String identifier,
          @Parameter(description = "Format of the item", required = false) @QueryParam("format") String format,
          @Parameter(description = "Include content in the response", required = false)@QueryParam("content") Boolean content,
          @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    if (content == null) {
      content = false;
    }

    final Item item = itemService.read(identifier, format, content);
    LOGGER.info("getItem: format: {}, item: {} ", format, item);

    if (item == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    return item;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Search Items", description = "Search for items based on various parameters.")
  @ApiResponses({
          @ApiResponse(responseCode = "204", description = "Successful retrieval of search results"),
          @ApiResponse(responseCode = "400", description = "Invalid search parameters")
  })
  public SearchResult<Item> searchItems(
      @Parameter(description = "Number of rows to retrieve", required = false) @QueryParam("rows") Integer rows,
      @Parameter(description = "Set to search within", required = false) @QueryParam("set") String set,
      @Parameter(description = "Format of the items", required = true) @QueryParam("format") String format,
      @Parameter(description = "Start date for search", required = false) @QueryParam("from") String from,
      @Parameter(description = "End date for search", required = false) @QueryParam("until") String until,
      @Parameter(description = "Include content in the response", required = false) @QueryParam("content") Boolean content,
      @Parameter(description = "Search mark for pagination", required = false) @QueryParam("searchMark") String searchMark,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    LOGGER.info("rows: {}", rows);
    LOGGER.info("set: {}", set);
    LOGGER.info("format: {}", format);
    LOGGER.info("from: {}", from);
    LOGGER.info("until: {}", until);
    LOGGER.info("content: {}", content);
    LOGGER.info("searchMark: {}", searchMark);
    
    Date fromDate = null;
    Date untilDate = null;

    if (StringUtils.isBlank(format)) {
      throw new BadRequestException("format QueryParam cannot be empty!");
    }

    try {
      if (!StringUtils.isBlank(from)) {
        fromDate = Configuration.getDateformat().parse(from);
      }
    } catch (ParseException e) {
      throw new BadRequestException("Invalid from QueryParam!");
    }

    try {
      if (!StringUtils.isBlank(until)) {
        untilDate = Configuration.getDateformat().parse(until);
      }
    } catch (ParseException e) {
      throw new BadRequestException("Invalid until QueryParam!");
    }

    if (content == null) {
      content = false;
    }

    SearchResult<Item> result = itemService.search(rows, set, format, fromDate, untilDate, content, searchMark);
    response.setHeader("Connection", "keep-alive");
    return result;
  }

  @DELETE
  @Path("/{identifier}")
  @Operation(summary = "Delete Item by Identifier", description = "Delete an item using its identifier.")
  @ApiResponses({
          @ApiResponse(responseCode = "204", description = "Successful deletion of item"),
          @ApiResponse(responseCode = "400", description = "Invalid identifier")
  })
  public void deleteItem(
      @Parameter(description = "Identifier of the item", required = true) @PathParam("identifier") String identifier,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    if (StringUtils.isBlank(identifier)) {
      throw new BadRequestException("identifier to delete cannot be empty!");
    }

    itemService.delete(identifier);
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Create Item", description = "Create a new item.")
  @ApiResponses({
          @ApiResponse(responseCode = "201", description = "Item successfully created"),
          @ApiResponse(responseCode = "400", description = "Invalid item data")
  })
  public Item createItem(
      @Parameter(description = "Content object", required = true) @FormDataParam("content") String content,
      @Parameter(description = "Item object", required = true) @FormDataParam("item") Item item,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {
    LOGGER.info("createItem item: {}", item.toString());
    LOGGER.debug("content: {}", content);
    
    Configuration config = Configuration.getInstance();
    boolean checkItemIdentifierInContent = Boolean.valueOf(config.getProperty("checkItemIdentifierInContent", "true"));
    
    if (checkItemIdentifierInContent && !content.contains(item.getIdentifier())) {
      throw new WebApplicationException("Cannot find the identifier in the content!", Status.BAD_REQUEST);
    }

    Content itemContent = new Content();
    itemContent.setContent(content);
    itemContent.setFormat(item.getIngestFormat());
    itemContent.setIdentifier(item.getIdentifier());

    item.setContent(itemContent);

    Item newItem = null;

    newItem = itemService.create(item);
    response.setStatus(HttpServletResponse.SC_CREATED);

    return newItem;
  }

  @PUT
  @Path("/{identifier}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Update Item", description = "Update an existing item.")
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Item successfully updated"),
          @ApiResponse(responseCode = "400", description = "Invalid item data")
  })
  public Item updateItem(
      @Parameter(description = "Identifier of the item", required = true) @PathParam("identifier") String identifier,
      @Parameter(description = "Content of the item", required = true) @FormDataParam("content") String content,
      @Parameter(description = "Item object", required = true) @FormDataParam("item") Item item,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    Configuration config = Configuration.getInstance();
    boolean checkItemIdentifierInContent = Boolean.valueOf(config.getProperty("checkItemIdentifierInContent", "true"));


    if (checkItemIdentifierInContent &&!identifier.equals(item.getIdentifier())) {
      throw new WebApplicationException("The identifier in the path and the item json does not match!",
          Status.BAD_REQUEST);
    }

    if (!content.contains(identifier)) {
      throw new WebApplicationException("Cannot find the identifier in the content!", Status.BAD_REQUEST);
    }

    Content itemContent = new Content();
    itemContent.setContent(content);
    itemContent.setFormat(item.getIngestFormat());
    itemContent.setIdentifier(item.getIdentifier());

    item.setContent(itemContent);
    
    Item updateItem = itemService.update(item);

    return updateItem;
  }

  @PUT
  @Path("/tags/{identifier}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Update Item Tags", description = "Update only Tags of an existing item.")
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Item-Tags successfully updated"),
          @ApiResponse(responseCode = "400", description = "Invalid data")
  })
  public Item updateItemTags(
          @Parameter(description = "Identifier of the item", required = true) @PathParam("identifier") String identifier,
          @Parameter(description = "List of String with Tags", required = true) List<String> tags,
          @Context HttpServletRequest request,
          @Context HttpServletResponse response) throws IOException {
    return itemService.updateTags(identifier, tags);
  }

}
