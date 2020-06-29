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

import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.service.ContentService;

@Path("/content")
public class ContentController extends AbstractController {

  @Inject
  ContentService contentService;

  @GET
  @Path("/{identifier}/{format}")
  @Produces(MediaType.APPLICATION_JSON)
  public Content getContent(@PathParam("identifier") String identifier, @PathParam("format") String format , @Context HttpServletRequest request,
      @Context HttpServletResponse response) {

    if (StringUtils.isBlank(identifier)) {
      throw new BadRequestException("identifier path parameter cannot be empty!");
    }
    
    if (StringUtils.isBlank(format)) {
      throw new BadRequestException("format path parameter cannot be empty!");
    }

    Content content;
    try {
      content = contentService.read(identifier, format);
    } catch (IOException e) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }

    if (content == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    return content;
  }

  @GET
  @Path("/{identifier}")
  @Produces(MediaType.APPLICATION_JSON)
  public List<Content> getAllContentFormats(@PathParam("identifier") String identifier, @Context HttpServletRequest request,
      @Context HttpServletResponse response) {

    if (StringUtils.isBlank(identifier)) {
      throw new BadRequestException("identifier path parameter cannot be empty!");
    }
    
    List<Content> content;
    try {
      content = contentService.readFormats(identifier);
    } catch (IOException e) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }

    if (content == null || content.isEmpty()) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    return content;
  }
  


  @DELETE
  @Path("/{identifier}/{format}")
  public void deleteContent(@PathParam("identifier") String identifier, @PathParam("format") String format , @Context HttpServletRequest request,
      @Context HttpServletResponse response) {

    if (StringUtils.isBlank(identifier)) {
      throw new BadRequestException("identifier path parameter cannot be empty!");
    }
    
    if (StringUtils.isBlank(format)) {
      throw new BadRequestException("format path parameter cannot be empty!");
    }

    try {
      contentService.delete(identifier, format);
    } catch (NotFoundException e) {
      throw new WebApplicationException(Status.NOT_FOUND);
    } catch (IOException ioe) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Content createContent(Content content, @Context HttpServletRequest request,
      @Context HttpServletResponse response) {

    validate(content);
    
    Content newContent = null;

    try {
      newContent = contentService.create(newContent);
    } catch (IOException e) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }

    return newContent;
  }


  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Content updateContent(Content content, @Context HttpServletRequest request,
      @Context HttpServletResponse response) {

	  validate(content);
	  Content newContent = null;

    try {
      newContent = contentService.update(newContent);
    } catch (IOException e) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }

    return newContent;
  }
  
  
  private void validate(Content content) throws WebApplicationException {
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
