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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import de.fiz.oai.backend.models.Set;
import de.fiz.oai.backend.service.SetService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;



@Path("/set")
@Api(value = "/set", tags = "SetController", description = "Controller for managing sets")
public class SetController extends AbstractController {

  @Context
  ServletContext servletContext;

  @Inject
  SetService setService;

  @GET
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Get set by name",
      response = Set.class
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Set retrieved successfully", response = Set.class),
      @ApiResponse(code = 404, message = "Set not found"),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public Set getSet(
      @ApiParam(value = "Name of the set", required = true) @PathParam("name") String name,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    if (name == null || StringUtils.isBlank(name)) {
      throw new BadRequestException("name QueryParam cannot be empty!");
    }
    
    final Set set = setService.read(name);
    
    if (set == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    return set;
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Get all sets",
      response = Set.class,
      responseContainer = "List"
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Sets retrieved successfully", response = Set.class, responseContainer = "List"),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public List<Set> getAllSets(
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {
      
    final List<Set> setList = setService.readAll();
    
    return setList;
  }
  
  
  @DELETE
  @Path("/{name}")
  @ApiOperation(
      value = "Delete set by name"
  )
  @ApiResponses({
      @ApiResponse(code = 204, message = "Set deleted successfully"),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public void deleteSet(
      @ApiParam(value = "Name of the set to delete", required = true) @PathParam("name") String name,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    if (StringUtils.isBlank(name)) {
      throw new BadRequestException("name to delete cannot be empty!");
    }

    setService.delete(name);
  }
  
  
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Create new set",
      response = Set.class
  )
  @ApiResponses({
      @ApiResponse(code = 201, message = "Set created successfully", response = Set.class),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public Set createSet(
      @ApiParam(value = "Set to create", required = true) Set set,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {
    
    if (StringUtils.isBlank( set.getName())) {
      throw new WebApplicationException("Set name cannot be empty!", Status.BAD_REQUEST);
    }

    if (StringUtils.isBlank(set.getSpec())) {
      throw new WebApplicationException("Set spec cannot be empty!", Status.BAD_REQUEST);
    }
    
    if (!Pattern.matches( "([A-Za-z0-9\\-_\\.!~\\*'\\(\\)])+(:[A-Za-z0-9\\-_\\.!~\\*'\\(\\)]+)*", set.getSpec() ) ) {
      throw new WebApplicationException("Set spec does not match regex!", Status.BAD_REQUEST);
    }
    
    Set newSet = null;
    
    newSet = setService.create(set);
    return newSet;
  }
  
  
  @PUT
  @Path("/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Update existing set",
      response = Set.class
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Set updated successfully", response = Set.class),
      @ApiResponse(code = 400, message = "Bad request")
  })
  public Set updateSet(
      @ApiParam(value = "Name of the set", required = true) @PathParam("name") String name,
      @ApiParam(value = "Set to update", required = true) Set set,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {

    if (StringUtils.isBlank( set.getName())) {
      throw new WebApplicationException("Set name cannot be empty!", Status.BAD_REQUEST);
    }

    if (StringUtils.isBlank(set.getSpec())) {
      throw new WebApplicationException("Set spec cannot be empty!", Status.BAD_REQUEST);
    }
    
    if (!Pattern.matches( "([A-Za-z0-9\\-_\\.!~\\*'\\(\\)])+(:[A-Za-z0-9\\-_\\.!~\\*'\\(\\)]+)*", set.getSpec() ) ) {
      throw new WebApplicationException("Set spec does not match regex!", Status.BAD_REQUEST);
    }

    if (!name.equals(set.getName())) {
      throw new WebApplicationException("The name in the path and the set json does not match!", Status.BAD_REQUEST);
    }
    
    Set updateSet = null;
    
    updateSet = setService.update(set);
    
    return updateSet;
  }
  
}
