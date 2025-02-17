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

import jakarta.inject.Inject;
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
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.models.Format;
import de.fiz.oai.backend.service.FormatService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Path("/format")
@Tag(name = "FormatController", description = "Controller for managing formats")
public class FormatController extends AbstractController {

  @Inject
  FormatService formatService;

  private static Logger LOGGER = LoggerFactory.getLogger(FormatController.class);

  @GET
  @Path("/{metadataPrefix}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get format by metadataPrefix", description = "Retrieves a format based on its metadataPrefix.")
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Format retrieved successfully"),
          @ApiResponse(responseCode = "404", description = "Format not found"),
          @ApiResponse(responseCode = "400", description = "Bad request")
  })
  public Format getFormat(
      @Parameter(description = "Metadata prefix of the format", required = true) @PathParam("metadataPrefix") String metadataPrefix,
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
  @Operation(summary = "Get all formats", description = "Retrieves all available formats.")
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Formats retrieved successfully"),
          @ApiResponse(responseCode = "400", description = "Bad request")
  })
  public List<Format> getAllFormats(
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    List<Format> formatList = formatService.readAll();

    return formatList;
  }

  @DELETE
  @Path("/{metadataPrefix}")
  @Operation(summary = "Delete format by metadataPrefix")
  @ApiResponses({
          @ApiResponse(responseCode = "204", description = "Format deleted successfully"),
          @ApiResponse(responseCode = "400", description = "Bad request")
  })
  public void deleteFormat(
          @Parameter(description = "Metadata prefix of the format to delete", required = true) @PathParam("metadataPrefix") String metadataPrefix,
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
  @Operation(summary = "Create new format", description = "Creates a new format entry.")
  @ApiResponses({
          @ApiResponse(responseCode = "201", description = "Format created successfully"),
          @ApiResponse(responseCode = "400", description = "Bad request")
  })
  public Format createFormat(
          @Parameter(description = "Format to create", required = true) Format format,
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
  @Operation(summary = "Update existing format", description = "Updates an existing format entry.")
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Format updated successfully"),
          @ApiResponse(responseCode = "400", description = "Bad request")
  })
  public Format updateFormat(
          @Parameter(description = "Metadata prefix of the format", required = true) @PathParam("metadataPrefix") String metadataPrefix,
          @Parameter(description = "Format to update", required = true) Format format,
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
