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

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.service.ItemService;
import de.fiz.oai.backend.utils.Configuration;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

@Path("/item")
@Api(value = "/item", tags = {"Item Management"})
@SwaggerDefinition(tags = {
    @Tag(name = "Item Management", description = "Operations related to managing items")
})
public class ItemController extends AbstractController {

  @Context
  ServletContext servletContext;

  @Inject
  ItemService itemService;

  private static Logger LOGGER = LoggerFactory.getLogger(ItemController.class);


  @GET
  @Path("/{identifier}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get Item by Identifier", notes = "Retrieve an item using its identifier", response = Item.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successful retrieval of item", response = Item.class),
      @ApiResponse(code = 404, message = "Item not found")
  })
  public Item getItem(@ApiParam(value = "Identifier of the item", required = true) @PathParam("identifier") String identifier, 
          @ApiParam(value = "Format of the item", required = false) @QueryParam("format") String format,
          @ApiParam(value = "Include content in the response", required = false) @QueryParam("content") Boolean content, 
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
  @ApiOperation(value = "Search Items", notes = "Search for items based on various parameters", response = SearchResult.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successful retrieval of search results", response = SearchResult.class),
      @ApiResponse(code = 400, message = "Invalid search parameters")
  })
  public SearchResult<Item> searchItems(
      @ApiParam(value = "Number of rows to retrieve", required = false) @QueryParam("rows") Integer rows,
      @ApiParam(value = "Set to search within", required = false) @QueryParam("set") String set,
      @ApiParam(value = "Format of the items", required = true) @QueryParam("format") String format,
      @ApiParam(value = "Start date for search", required = false) @QueryParam("from") String from,
      @ApiParam(value = "End date for search", required = false) @QueryParam("until") String until,
      @ApiParam(value = "Include content in the response", required = false) @QueryParam("content") Boolean content,
      @ApiParam(value = "Search mark for pagination", required = false) @QueryParam("searchMark") String searchMark,
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

    return result;
  }

  @DELETE
  @Path("/{identifier}")
  @ApiOperation(value = "Delete Item by Identifier", notes = "Delete an item using its identifier")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Successful deletion of item"),
      @ApiResponse(code = 400, message = "Invalid identifier")
  })
  public void deleteItem(
      @ApiParam(value = "Identifier of the item", required = true) @PathParam("identifier") String identifier,
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
  @ApiOperation(value = "Create Item", notes = "Create a new item", response = Item.class)
  @ApiResponses(value = {
      @ApiResponse(code = 201, message = "Item successfully created", response = Item.class),
      @ApiResponse(code = 400, message = "Invalid item data")
  })
  public Item createItem(
      @ApiParam(value = "Content of the item", required = true) @FormDataParam("content") String content,
      @ApiParam(value = "Item object", required = true) @FormDataParam("item") Item item,
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
  @ApiOperation(value = "Update Item", notes = "Update an existing item", response = Item.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Item successfully updated", response = Item.class),
      @ApiResponse(code = 400, message = "Invalid item data")
  })
  public Item updateItem(
      @ApiParam(value = "Identifier of the item", required = true) @PathParam("identifier") String identifier,
      @ApiParam(value = "Content of the item", required = true) @FormDataParam("content") String content,
      @ApiParam(value = "Item object", required = true) @FormDataParam("item") Item item,
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

}
