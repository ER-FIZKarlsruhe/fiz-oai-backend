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

import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Format;
import de.fiz.oai.backend.service.FormatService;

@Path("/format")
public class FormatController extends AbstractController {

  @Inject
  FormatService formatService;

  private Logger LOGGER = LoggerFactory.getLogger(FormatController.class);

  @GET
  @Path("/{metadataPrefix}")
  @Produces(MediaType.APPLICATION_JSON)
  public Format getFormat(@PathParam("metadataPrefix") String metadataPrefix, @Context HttpServletRequest request,
      @Context HttpServletResponse response) {

    if (StringUtils.isBlank(metadataPrefix)) {
      throw new BadRequestException("name QueryParam cannot be empty!");
    }

    Format format;
    try {
      format = formatService.read(metadataPrefix);
    } catch (IOException e) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }

    if (format == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    return format;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<Format> getAllFormats(@Context HttpServletRequest request, @Context HttpServletResponse response) {

    List<Format> formatList;
    try {
      formatList = formatService.readAll();
    } catch (IOException e) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }

    return formatList;
  }

  @DELETE
  @Path("/{metadataPrefix}")
  public void deleteFormat(@PathParam("metadataPrefix") String metadataPrefix, @Context HttpServletRequest request,
      @Context HttpServletResponse response) {

    if (StringUtils.isBlank(metadataPrefix)) {
      throw new BadRequestException("name to delete cannot be empty!");
    }

    try {
      formatService.delete(metadataPrefix);
    } catch (NotFoundException e) {
      throw new WebApplicationException(Status.NOT_FOUND);
    } catch (IOException ioe) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Format createFormat(Format format, @Context HttpServletRequest request,
      @Context HttpServletResponse response) {

    if (StringUtils.isBlank(format.getMetadataPrefix())) {
      throw new WebApplicationException("Format metadataPrefix cannot be empty!", Status.BAD_REQUEST);
    }

    if (StringUtils.isBlank(format.getSchemaLocation())) {
      throw new WebApplicationException("Format schemaLocation cannot be empty!", Status.BAD_REQUEST);
    }
    
    if (StringUtils.isBlank(format.getSchemaNamespace())) {
      throw new WebApplicationException("Format schemaNamespace cannot be empty!", Status.BAD_REQUEST);
    }
    
    if (StringUtils.isBlank(format.getIdentifierXpath())) {
      throw new WebApplicationException("Format identifierXPath cannot be empty!", Status.BAD_REQUEST);
    }
    
    if (!Pattern.matches( "[A-Za-z0-9\\-_\\.!~\\*'\\(\\)]+", format.getMetadataPrefix()) ) {
      throw new WebApplicationException("Format metadataPrefix does not match regex!", Status.BAD_REQUEST);
    }
    
    // TODO add more validations

    Format newFormat = null;

    try {
      newFormat = formatService.create(newFormat);
    } catch (IOException e) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }

    LOGGER.info("newSet: " + newFormat);
    return newFormat;
  }

  @PUT
  @Path("/{metadataPrefix}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Format updateFormat(@PathParam("metadataPrefix") String metadataPrefix, Format format,
      @Context HttpServletRequest request, @Context HttpServletResponse response) {

    if (StringUtils.isBlank(format.getMetadataPrefix())) {
      throw new WebApplicationException("Format metadataPrefix cannot be empty!", Status.BAD_REQUEST);
    }

    if (StringUtils.isBlank(format.getSchemaLocation())) {
      throw new WebApplicationException("Format schemaLocation cannot be empty!", Status.BAD_REQUEST);
    }
    
    if (StringUtils.isBlank(format.getSchemaNamespace())) {
      throw new WebApplicationException("Format schemaNamespace cannot be empty!", Status.BAD_REQUEST);
    }
    
    if (StringUtils.isBlank(format.getIdentifierXpath())) {
      throw new WebApplicationException("Format identifierXPath cannot be empty!", Status.BAD_REQUEST);
    }
    
    if (!Pattern.matches( "[A-Za-z0-9\\-_\\.!~\\*'\\(\\)]+", format.getMetadataPrefix()) ) {
      throw new WebApplicationException("Format metadataPrefix does not match regex!", Status.BAD_REQUEST);
    }

    if (!metadataPrefix.equals(format.getMetadataPrefix())) {
      throw new WebApplicationException("The metadataPrefix  in the path and the set json does not match!",
          Status.BAD_REQUEST);
    }

    try {
      formatService.update(format);

    } catch (NotFoundException nfe) {
      throw new WebApplicationException(Status.NOT_FOUND);
    } catch (IOException e) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }

    return format;
  }

}
