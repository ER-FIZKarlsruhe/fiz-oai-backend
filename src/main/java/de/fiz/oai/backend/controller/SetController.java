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
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import de.fiz.oai.backend.models.Set;
import de.fiz.oai.backend.service.SetService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;



@Path("/set")
@Tag(name = "SetController", description = "Controller for managing sets")
public class SetController extends AbstractController {

  @Context
  ServletContext servletContext;

  @Inject
  SetService setService;

  @GET
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get set by name", description = "Retrieve a set based on its name.")
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Set retrieved successfully"),
          @ApiResponse(responseCode = "404", description = "Set not found"),
          @ApiResponse(responseCode = "400", description = "Bad request")
  })
  public Set getSet(
      @Parameter(description = "Name of the set", required = true) @PathParam("name") String name,
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
  @Operation(summary = "Get all sets", description = "Retrieve all sets.")
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Sets retrieved successfully"),
          @ApiResponse(responseCode = "400", description = "Bad request")
  })
  public List<Set> getAllSets(
      @Context HttpServletRequest request,
      @Context HttpServletResponse response) throws IOException {
      
    final List<Set> setList = setService.readAll();
    
    return setList;
  }
  
  
  @DELETE
  @Path("/{name}")
  @Operation(summary = "Delete set by name", description = "Deletes a set based on its name.")
  @ApiResponses({
          @ApiResponse(responseCode = "204", description = "Set deleted successfully"),
          @ApiResponse(responseCode = "400", description = "Bad request")
  })
  public void deleteSet(
      @Parameter(description = "Name of the set to delete", required = true) @PathParam("name") String name,
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
  @Operation(summary = "Create a new set", description = "Creates a new set.")
  @ApiResponses({
          @ApiResponse(responseCode = "201", description = "Set created successfully"),
          @ApiResponse(responseCode = "400", description = "Bad request")
  })
  public Set createSet(
      @Parameter(description = "Set to create", required = true) Set set,
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
  @Operation(summary = "Update existing set", description = "Updates an existing set.")
  @ApiResponses({
          @ApiResponse(responseCode = "200", description = "Set updated successfully"),
          @ApiResponse(responseCode = "400", description = "Bad request")
  })
  public Set updateSet(
      @Parameter(description = "Name of the set", required = true) @PathParam("name") String name,
      @Parameter(description = "Set to update", required = true) Set set,
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
