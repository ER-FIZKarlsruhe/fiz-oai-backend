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
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.service.ContentService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;



@Path("/content")
@Api(value = "/content", tags = "ContentController", description = "Controller for managing content")
public class ContentController extends AbstractController {

  @Inject
  ContentService contentService;

  private static Logger LOGGER = LoggerFactory.getLogger(ContentController.class);
  
  @GET
  @Path("/{identifier}/{format}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Get content by identifier and format",
      response = Content.class
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Content retrieved successfully", response = Content.class),
      @ApiResponse(code = 404, message = "Content not found"),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public Content getContent(
      @ApiParam(value = "Identifier of the content", required = true) @PathParam("identifier") String identifier,
      @ApiParam(value = "Format of the content", required = true) @PathParam("format") String format,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    if (StringUtils.isBlank(identifier)) {
      throw new BadRequestException("identifier path parameter cannot be empty!");
    }
    
    if (StringUtils.isBlank(format)) {
      throw new BadRequestException("format path parameter cannot be empty!");
    }

    Content content = contentService.read(identifier, format);

    if (content == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    return content;
  }

  @GET
  @Path("/{identifier}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Get all content formats by identifier",
      response = Content.class,
      responseContainer = "List"
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Content formats retrieved successfully", response = Content.class, responseContainer = "List"),
      @ApiResponse(code = 404, message = "Content not found"),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public List<Content> getAllContentFormats(
      @ApiParam(value = "Identifier of the content", required = true) @PathParam("identifier") String identifier,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    if (StringUtils.isBlank(identifier)) {
      throw new BadRequestException("identifier path parameter cannot be empty!");
    }
    
    List<Content> content = contentService.readFormats(identifier);

    if (content == null || content.isEmpty()) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    return content;
  }
  


  @DELETE
  @Path("/{identifier}/{format}")
  @ApiOperation(
      value = "Delete content by identifier and format"
  )
  @ApiResponses({
      @ApiResponse(code = 204, message = "Content deleted successfully"),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public void deleteContent(
      @ApiParam(value = "Identifier of the content", required = true) @PathParam("identifier") String identifier,
      @ApiParam(value = "Format of the content", required = true) @PathParam("format") String format,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    if (StringUtils.isBlank(identifier)) {
      throw new BadRequestException("identifier path parameter cannot be empty!");
    }
    
    if (StringUtils.isBlank(format)) {
      throw new BadRequestException("format path parameter cannot be empty!");
    }

    contentService.delete(identifier, format);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Create new content",
      response = Content.class
  )
  @ApiResponses({
      @ApiResponse(code = 201, message = "Content created successfully", response = Content.class),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public Content createContent(
      @ApiParam(value = "Content to create", required = true) Content content,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

	  LOGGER.info("createContent " + content.toString());
    validate(content);
    
    return contentService.create(content);

  }


  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Update existing content",
      response = Content.class
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Content updated successfully", response = Content.class),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public Content updateContent(
      @ApiParam(value = "Content to update", required = true) Content content,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    validate(content);

    return contentService.update(content);
  }
  
  
  private void validate(Content content) {
    if (StringUtils.isBlank(content.getIdentifier())) {
        throw new WebApplicationException("Content identifier cannot be empty!", Status.BAD_REQUEST);
      }

      if (StringUtils.isBlank(content.getFormat())) {
        throw new WebApplicationException("Content format cannot be empty!", Status.BAD_REQUEST);
      }
      
      if (StringUtils.isBlank(content.getContent())) {
        throw new WebApplicationException("Content content cannot be empty!", Status.BAD_REQUEST);
      }
      
      if (!Pattern.matches( "[A-Za-z0-9\\-_\\.!~\\*'\\(\\)]+", content.getFormat()) ) {
        throw new WebApplicationException("Content format does not match regex!", Status.BAD_REQUEST);
      }
  }

}
