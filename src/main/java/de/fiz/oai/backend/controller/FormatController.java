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

import de.fiz.oai.backend.models.Format;
import de.fiz.oai.backend.service.FormatService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/format")
@Api(value = "/format", tags = "FormatController", description = "Controller for managing formats")
public class FormatController extends AbstractController {

  @Inject
  FormatService formatService;

  private static Logger LOGGER = LoggerFactory.getLogger(FormatController.class);

  @GET
  @Path("/{metadataPrefix}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Get format by metadataPrefix",
      response = Format.class
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Format retrieved successfully", response = Format.class),
      @ApiResponse(code = 404, message = "Format not found"),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public Format getFormat(
      @ApiParam(value = "Metadata prefix of the format", required = true) @PathParam("metadataPrefix") String metadataPrefix,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    if (StringUtils.isBlank(metadataPrefix)) {
      throw new BadRequestException("name QueryParam cannot be empty!");
    }

    Format format = formatService.read(metadataPrefix);

    if (format == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    return format;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Get all formats",
      response = Format.class,
      responseContainer = "List"
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Formats retrieved successfully", response = Format.class, responseContainer = "List"),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public List<Format> getAllFormats(
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    List<Format> formatList = formatService.readAll();

    return formatList;
  }

  @DELETE
  @Path("/{metadataPrefix}")
  @ApiOperation(
      value = "Delete format by metadataPrefix"
  )
  @ApiResponses({
      @ApiResponse(code = 204, message = "Format deleted successfully"),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public void deleteFormat(
      @ApiParam(value = "Metadata prefix of the format to delete", required = true) @PathParam("metadataPrefix") String metadataPrefix,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    if (StringUtils.isBlank(metadataPrefix)) {
      throw new BadRequestException("name to delete cannot be empty!");
    }

    formatService.delete(metadataPrefix);
  }
  
  

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Create new format",
      response = Format.class
  )
  @ApiResponses({
      @ApiResponse(code = 201, message = "Format created successfully", response = Format.class),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public Format createFormat(
      @ApiParam(value = "Format to create", required = true) Format format,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {
      
    LOGGER.info("createFormat format: {}", format.toString());

    if (StringUtils.isBlank(format.getMetadataPrefix())) {
      throw new WebApplicationException("Format metadataPrefix cannot be empty!", Status.BAD_REQUEST);
    }

    if (StringUtils.isBlank(format.getSchemaLocation())) {
      throw new WebApplicationException("Format schemaLocation cannot be empty!", Status.BAD_REQUEST);
    }
    
    if (StringUtils.isBlank(format.getSchemaNamespace())) {
      throw new WebApplicationException("Format schemaNamespace cannot be empty!", Status.BAD_REQUEST);
    }
    
    if (!Pattern.matches( "[A-Za-z0-9\\-_\\.!~\\*'\\(\\)]+", format.getMetadataPrefix()) ) {
      throw new WebApplicationException("Format metadataPrefix does not match regex!", Status.BAD_REQUEST);
    }
    
    Format newFormat = null;

    newFormat = formatService.create(format);

    LOGGER.info("newFormat: {}", newFormat);
    return newFormat;
  }

  
  
  @PUT
  @Path("/{metadataPrefix}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Update existing format",
      response = Format.class
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Format updated successfully", response = Format.class),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public Format updateFormat(
      @ApiParam(value = "Metadata prefix of the format", required = true) @PathParam("metadataPrefix") String metadataPrefix,
      @ApiParam(value = "Format to update", required = true) Format format,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {
  
	LOGGER.info("createFormat format: {}", format.toString());
    if (StringUtils.isBlank(format.getMetadataPrefix())) {
      throw new WebApplicationException("Format metadataPrefix cannot be empty!", Status.BAD_REQUEST);
    }

    if (StringUtils.isBlank(format.getSchemaLocation())) {
      throw new WebApplicationException("Format schemaLocation cannot be empty!", Status.BAD_REQUEST);
    }
    
    if (StringUtils.isBlank(format.getSchemaNamespace())) {
      throw new WebApplicationException("Format schemaNamespace cannot be empty!", Status.BAD_REQUEST);
    }
    
    
    if (!Pattern.matches( "[A-Za-z0-9\\-_\\.!~\\*'\\(\\)]+", format.getMetadataPrefix()) ) {
      throw new WebApplicationException("Format metadataPrefix does not match regex!", Status.BAD_REQUEST);
    }

    if (!metadataPrefix.equals(format.getMetadataPrefix())) {
      throw new WebApplicationException("The metadataPrefix  in the path and the set json does not match!",
          Status.BAD_REQUEST);
    }

    formatService.update(format);

    return format;
  }

}
