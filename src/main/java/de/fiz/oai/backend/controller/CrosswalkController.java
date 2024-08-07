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
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.models.Crosswalk;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.service.CrosswalkService;
import de.fiz.oai.backend.utils.Configuration;

import de.fiz.oai.backend.utils.Configuration;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/crosswalk")
@Api(value = "/crosswalk", tags = "CrosswalkController", description = "Controller for managing crosswalks")
public class CrosswalkController extends AbstractController {

  @Inject
  CrosswalkService crosswalkService;

  private static Logger LOGGER = LoggerFactory.getLogger(CrosswalkController.class);
  
  @GET
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Get crosswalk by name",
      response = Crosswalk.class
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Crosswalk retrieved successfully", response = Crosswalk.class),
      @ApiResponse(code = 404, message = "Crosswalk not found"),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public Crosswalk getFormat(
      @ApiParam(value = "Name of the crosswalk", required = true) @PathParam("name") String name,
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
  @ApiOperation(
      value = "Get all crosswalks",
      response = Crosswalk.class,
      responseContainer = "List"
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Crosswalks retrieved successfully", response = Crosswalk.class, responseContainer = "List"),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public List<Crosswalk> getAllCrosswalks(
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {
      
    List<Crosswalk> crosswalks = crosswalkService.readAll();

    return crosswalks;
  }

  @DELETE
  @Path("/{name}")
  @ApiOperation(
      value = "Delete crosswalk by name"
  )
  @ApiResponses({
      @ApiResponse(code = 204, message = "Crosswalk deleted successfully"),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public void deleteCrosswalk(
      @ApiParam(value = "Name of the crosswalk", required = true) @PathParam("name") String name,
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
  @ApiOperation(
      value = "Create new crosswalk",
      response = Crosswalk.class
  )
  @ApiResponses({
      @ApiResponse(code = 201, message = "Crosswalk created successfully", response = Crosswalk.class),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public Crosswalk createCrosswalk(
      @ApiParam(value = "Crosswalk to create", required = true) Crosswalk crosswalk,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    validate(crosswalk);

    return crosswalkService.create(crosswalk);
  }

  
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Update existing crosswalk",
      response = Crosswalk.class
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Crosswalk updated successfully", response = Crosswalk.class),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public Crosswalk updateCrosswalk(
      @ApiParam(value = "Crosswalk to update", required = true) Crosswalk crosswalk,
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
  @ApiOperation(
      value = "Process crosswalk by name"
  )
  @ApiResponses({
      @ApiResponse(code = 204, message = "Crosswalk processed successfully"),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public void process(
      @ApiParam(value = "Name of the crosswalk", required = true) @PathParam("name") String name,
      @ApiParam(value = "Update item timestamp", required = true) @QueryParam("updateItemTimestamp") String updateItemTimestampParam,
      @ApiParam(value = "Start date for processing") @QueryParam("from") String from,
      @ApiParam(value = "End date for processing") @QueryParam("until") String until,
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
