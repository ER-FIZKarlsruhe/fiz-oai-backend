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

import de.fiz.oai.backend.exceptions.AlreadyExistsException;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.service.ItemService;
import de.fiz.oai.backend.utils.Configuration;

@Path("/item")
public class ItemController extends AbstractController {

  @Context
  ServletContext servletContext;

  @Inject
  ItemService itemService;

  private Logger LOGGER = LoggerFactory.getLogger(ItemController.class);

  @GET
  @Path("/{identifier}")
  @Produces(MediaType.APPLICATION_JSON)
  public Item getItem(@PathParam("identifier") String identifier, @QueryParam("format") String format,
      @QueryParam("content") Boolean content, @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws WebApplicationException, IOException {

    if (content == null) {
      content = false;
    }

    final Item item = itemService.read(identifier, format, content);
    LOGGER.info("getItem: {} ", item);

    if (item == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    return item;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public SearchResult<Item> searchItems(@QueryParam("rows") Integer rows,
      @QueryParam("set") String set, @QueryParam("format") String format, @QueryParam("from") String from,
      @QueryParam("until") String until, @QueryParam("content") Boolean content, @QueryParam("lastItemId") String lastItemId, @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws WebApplicationException, IOException {

    LOGGER.info("rows: {}", rows);
    LOGGER.info("set: {}", set);
    LOGGER.info("format: {}", format);
    LOGGER.info("from: {}", from);
    LOGGER.info("until: {}", until);
    LOGGER.info("content: {}", content);
    LOGGER.info("lastItemId: {}", lastItemId);
    
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

    SearchResult<Item> result = itemService.search(rows, set, format, fromDate, untilDate, content, lastItemId);

    return result;
  }

  @DELETE
  @Path("/{identifier}")
  public void deleteItem(@PathParam("identifier") String identifier, @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws WebApplicationException, IOException {

    if (StringUtils.isBlank(identifier)) {
      throw new BadRequestException("identifier to delete cannot be empty!");
    }

    try {
      itemService.delete(identifier);
    } catch (NotFoundException e) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Item createItem(@FormDataParam("content") String content, @FormDataParam("item") Item item,
      @Context HttpServletRequest request, @Context HttpServletResponse response) {
    LOGGER.info("createItem item: {}", item.toString());
    LOGGER.info("content: {}", content);
    
    if (!content.contains(item.getIdentifier())) {
      throw new WebApplicationException("Cannot find the identifier in the content!", Status.BAD_REQUEST);
    }

    Content itemContent = new Content();
    itemContent.setContent(content);
    itemContent.setFormat(item.getIngestFormat());
    itemContent.setIdentifier(item.getIdentifier());

    item.setContent(itemContent);

    Item newItem = null;

    try {
      newItem = itemService.create(item);
    } catch (NotFoundException e) {
      throw new WebApplicationException(Status.NOT_FOUND);
    } catch (AlreadyExistsException e) {
        throw new WebApplicationException(Status.CONFLICT);
    } catch (Exception e) {
      LOGGER.error("An unexpected exception occured", e);
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }

    return newItem;
  }

  @PUT
  @Path("/{identifier}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Item updateItem(@PathParam("identifier") String identifier, @FormDataParam("content") String content,
      @FormDataParam("item") Item item, @Context HttpServletRequest request, @Context HttpServletResponse response) {

    if (!identifier.equals(item.getIdentifier())) {
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
    
    Item updateItem = null;
    try {
      updateItem = itemService.update(item);
    } catch (NotFoundException e) {
      throw new WebApplicationException(Status.NOT_FOUND);
    } catch (IOException e) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }

    return updateItem;
  }

}
