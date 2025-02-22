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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.models.Crosswalk;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.service.CrosswalkService;
import de.fiz.oai.backend.utils.Configuration;

import de.fiz.oai.backend.utils.Configuration;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Path("/crosswalk")
@Tag(name = "CrosswalkController", description = "Controller for managing crosswalks")
public class CrosswalkController extends AbstractController {

  @Inject
  CrosswalkService crosswalkService;

  private static Logger LOGGER = LoggerFactory.getLogger(CrosswalkController.class);
  
  @GET
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get crosswalk by name", description = "Retrieves a crosswalk based on its name.")
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Crosswalk retrieved successfully"),
          @ApiResponse(responseCode = "404", description = "Crosswalk not found"),
          @ApiResponse(responseCode = "400", description = "Bad request")
  })
  public Crosswalk getFormat(
      @Parameter(description = "Name of the crosswalk", required = true) @PathParam("name") String name,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    if (StringUtils.isBlank(name)) {
      throw new BadRequestException("name path parameter cannot be empty!");
    }

    Crosswalk crosswalk = crosswalkService.read(name);

    if (crosswalk == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    return crosswalk;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get all crosswalks", description = "Retrieves all crosswalks available.")
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Crosswalks retrieved successfully"),
          @ApiResponse(responseCode = "400", description = "Bad request")
  })
  public List<Crosswalk> getAllCrosswalks(
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {
      
    List<Crosswalk> crosswalks = crosswalkService.readAll();

    return crosswalks;
  }

  @DELETE
  @Path("/{name}")
  @Operation(summary = "Delete crosswalk by name")
  @ApiResponses({
          @ApiResponse(responseCode = "204", description = "Crosswalk deleted successfully"),
          @ApiResponse(responseCode = "400", description = "Bad request")
  })
  public void deleteCrosswalk(
      @Parameter(description = "Name of the crosswalk", required = true) @PathParam("name") String name,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    if (StringUtils.isBlank(name)) {
      throw new BadRequestException("identifier path parameter cannot be empty!");
    }

    crosswalkService.delete(name);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Create new crosswalk", description = "Creates a new crosswalk entry.")
  @ApiResponses({
          @ApiResponse(responseCode = "201", description = "Crosswalk created successfully"),
          @ApiResponse(responseCode = "400", description = "Bad request")
  })
  public Crosswalk createCrosswalk(
          @Parameter(description = "Crosswalk to create", required = true) Crosswalk crosswalk,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    validate(crosswalk);

    return crosswalkService.create(crosswalk);
  }

  
  @PUT
  @Path("/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Update existing crosswalk", description = "Updates an existing crosswalk entry.")
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Crosswalk updated successfully"),
          @ApiResponse(responseCode = "400", description = "Bad request")
  })
  public Crosswalk updateCrosswalk(
          @Parameter(description = "Name of the crosswalk", required = true) @PathParam("name") String name,
          @Parameter(description = "Crosswalk to update", required = true) Crosswalk crosswalk,
          @Context HttpServletRequest request,
          @Context HttpServletResponse response) throws IOException {

    validate(crosswalk);

    return crosswalkService.update(crosswalk);
  }
  
  private void validate(Crosswalk crosswalk) {
    if (StringUtils.isBlank(crosswalk.getName())) {
        throw new WebApplicationException("Crosswalk identifier cannot be empty!", Status.BAD_REQUEST);
      }

      if (StringUtils.isBlank(crosswalk.getFormatFrom())) {
        throw new WebApplicationException("Crosswalk format cannot be empty!", Status.BAD_REQUEST);
      }

      if (StringUtils.isBlank(crosswalk.getFormatTo())) {
        throw new WebApplicationException("Crosswalk format cannot be empty!", Status.BAD_REQUEST);
      }
      
      if (crosswalk.getXsltStylesheet() == null || crosswalk.getXsltStylesheet().isEmpty()) {
        throw new WebApplicationException("Crosswalk crosswalk cannot be empty!", Status.BAD_REQUEST);
      }

      if (!Pattern.matches("[A-Za-z0-9\\-_\\.!~\\*'\\(\\)]+", crosswalk.getFormatFrom())) {
        throw new WebApplicationException("Crosswalk formatFrom does not match regex!", Status.BAD_REQUEST);
      }
      
      if (!Pattern.matches("[A-Za-z0-9\\-_\\.!~\\*'\\(\\)]+", crosswalk.getFormatTo())) {
        throw new WebApplicationException("Crosswalk formatTo does not match regex!", Status.BAD_REQUEST);
      }
  }

  
  @PUT
  @Path("/{name}/process")
  @Operation(summary = "Process crosswalk by name", description = "Process crosswalk by name.")
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Crosswalk processed successfully"),
          @ApiResponse(responseCode = "400", description = "Bad request")
  })
  public void process(
      @Parameter(description = "Name of the crosswalk", required = true) @PathParam("name") String name,
      @Parameter(description = "Update item timestamp", required = true) @QueryParam("updateItemTimestamp") String updateItemTimestampParam,
      @Parameter(description = "Start date for processing") @QueryParam("from") String from,
      @Parameter(description = "End date for processing") @QueryParam("until") String until,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    LOGGER.info("name: {}", name);
    LOGGER.info("from: {}", from);
    LOGGER.info("until: {}", until);
  
    
    Date fromDate = null;
    Date untilDate = null;
    Boolean updateItemTimestamp = null;

    if (StringUtils.isBlank(name)) {
      throw new BadRequestException("name PathParam cannot be empty!");
    }
    
    if (StringUtils.isBlank(updateItemTimestampParam)) {
        throw new BadRequestException("updateItemTimestamp QueryParam cannot be empty!");
    } else {
        updateItemTimestamp = Boolean.valueOf(updateItemTimestampParam);
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



    crosswalkService.process(name, updateItemTimestamp, fromDate, untilDate);

    return;
  }
  
}
