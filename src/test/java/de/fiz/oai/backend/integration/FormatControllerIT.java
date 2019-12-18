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
package de.fiz.oai.backend.integration;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.controller.FormatController;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Format;
import de.fiz.oai.backend.service.FormatService;

public class FormatControllerIT extends JerseyTest {

  private Logger LOGGER = LoggerFactory.getLogger(FormatControllerIT.class);

  @Mock
  private FormatService formatService;

  @Mock
  HttpServletRequest request;

  @Mock
  HttpServletResponse response;

  @Override
  protected Application configure() {
    MockitoAnnotations.initMocks(this);

    ResourceConfig config = new ResourceConfig(FormatController.class);
    config.register(new AbstractBinder() {

      @Override
      protected void configure() {
        bind(formatService).to(FormatService.class);
        bind(request).to(HttpServletRequest.class);
        bind(response).to(HttpServletResponse.class);
      }
    });
    config.register(MultiPartFeature.class);
    return config;
  }

  @Override
  protected void configureClient(ClientConfig clientConfig) {
    clientConfig.register(MultiPartFeature.class);
  }

  @Test
  public void testGetFormat() throws Exception {
    Format format = new Format();
    format.setMetadataPrefix("oai_dc");
    format.setSchemaLocation("http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
    format.setSchemaNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/");
    format.setIdentifierXpath("/identifier");
    

    when(formatService.read("oai_dc")).thenReturn(format);

    Response response = target("/format/oai_dc").request().get();

    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Http Content-Type should be: ", MediaType.APPLICATION_JSON,
        response.getHeaderString(HttpHeaders.CONTENT_TYPE));
    String result = response.readEntity(String.class);
    LOGGER.info("getFormat json:" + result);
    
//    Format result = response.readEntity(Format.class);
//    assertEquals("Format metadataPrefix should be: ", "oai_dc", result.getMetadataPrefix());
  }

  @Test
  public void testGetFormatEmptyName() throws Exception {

    Response response = target("/format/%20").request().get();

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetFormatNotFound() throws Exception {
    when(formatService.read("notfound")).thenReturn(null);

    Response response = target("/format/notfound").request().get();

    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetAllFormats() throws Exception {
    when(formatService.readAll()).thenReturn(getTestFormatList());

    Response response = target("/format").request().get();

    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Http Content-Type should be: ", MediaType.APPLICATION_JSON,
        response.getHeaderString(HttpHeaders.CONTENT_TYPE));

    List<Format> result = response.readEntity(new GenericType<List<Format>>() {
    });
    assertEquals("result size should be: ", 100, result.size());
  }

  @Test
  public void testDeleteFormat() throws Exception {
    doNothing().when(formatService).delete("oai_dc");

    Response response = target("/format/oai_dc").request().delete();

    assertEquals("Http Response should be 204: ", Status.NO_CONTENT.getStatusCode(), response.getStatus());
  }

  @Test
  public void testDeleteFormatEmptyName() throws Exception {
    doThrow(IOException.class).when(formatService).delete("%20");

    Response response = target("/format/%20").request().delete();

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testDeleteFormatNotFound() throws Exception {
    doThrow(NotFoundException.class).when(formatService).delete("123Fragerei");

    Response response = target("/format/123Fragerei").request().delete();

    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreateFormat() throws Exception {
    Format format = new Format();
    format.setMetadataPrefix("oai_dc");
    format.setSchemaLocation("http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
    format.setSchemaNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/");
    format.setIdentifierXpath("/identifier");

    when(formatService.create(any())).thenReturn(format);

    Response response = target("format").request().post(Entity.json(
        "{\"metadataPrefix\":\"oai_dc\",\"schemaLocation\":\"http://www.openarchives.org/OAI/2.0/oai_dc.xsd\",\"schemaNamespace\":\"http://www.openarchives.org/OAI/2.0/oai_dc/\",\"identifierXpath\":\"/identifier\"}"));

    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreateFormatNoPrefix() throws Exception {
    Response response = target("format").request().post(Entity.json(
        "{\"schemaLocation\":\"http://www.openarchives.org/OAI/2.0/oai_dc.xsd\",\"schemaNamespace\":\"http://www.openarchives.org/OAI/2.0/oai_dc/\",\"identifierXpath\":\"/identifier\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreateFormatNoLocation() throws Exception {
    Response response = target("format").request().post(Entity.json(
        "{\"metadataPrefix\":\"oai_dc\",\"schemaNamespace\":\"http://www.openarchives.org/OAI/2.0/oai_dc/\",\"identifierXpath\":\"/identifier\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testCreateFormatNoNamespace() throws Exception {
    Response response = target("format").request().post(Entity.json(
        "{\"metadataPrefix\":\"oai_dc\",\"schemaLocation\":\"http://www.openarchives.org/OAI/2.0/oai_dc.xsd\",\"identifierXpath\":\"/identifier\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testCreateFormatNoXPath() throws Exception {
    Response response = target("format").request().post(Entity.json(
        "{\"metadataPrefix\":\"oai_dc\",\"schemaLocation\":\"http://www.openarchives.org/OAI/2.0/oai_dc.xsd\",\"schemaNamespace\":\"http://www.openarchives.org/OAI/2.0/oai_dc/\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testCreateFormatInvamidMetadataPrefix() throws Exception {
    Response response = target("format").request().post(Entity.json(
        "{\"metadataPrefix\":\"oai dc\",\"schemaLocation\":\"http://www.openarchives.org/OAI/2.0/oai_dc.xsd\",\"schemaNamespace\":\"http://www.openarchives.org/OAI/2.0/oai_dc/\",\"identifierXpath\":\"/identifier\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testUpdateFormat() throws Exception {
    Format format = new Format();
    format.setMetadataPrefix("oai_dc");
    format.setSchemaLocation("http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
    format.setSchemaNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/");
    format.setIdentifierXpath("/identifier");

    when(formatService.update(any())).thenReturn(format);
    
    
    Response response = target("format/oai_dc").request().put(Entity.json(
        "{\"metadataPrefix\":\"oai_dc\",\"schemaLocation\":\"http://www.openarchives.org/OAI/2.0/oai_dc.xsd\",\"schemaNamespace\":\"http://www.openarchives.org/OAI/2.0/oai_dc/\",\"identifierXpath\":\"/identifier\"}"));

    String content = response.readEntity(String.class);
    LOGGER.info("testUpdateFormat content: " + content);
    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
  }
  
  
  @Test
  public void testUpdateFormatNoPrefix() throws Exception {
    Response response = target("format/oai_dc").request().put(Entity.json(
        "{\"schemaLocation\":\"http://www.openarchives.org/OAI/2.0/oai_dc.xsd\",\"schemaNamespace\":\"http://www.openarchives.org/OAI/2.0/oai_dc/\",\"identifierXpath\":\"/identifier\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testUpdateFormatNoLocation() throws Exception {
    Response response = target("format/oai_dc").request().put(Entity.json(
        "{\"metadataPrefix\":\"oai_dc\",\"schemaNamespace\":\"http://www.openarchives.org/OAI/2.0/oai_dc/\",\"identifierXpath\":\"/identifier\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testUpdateFormatNoNamespace() throws Exception {
    Response response = target("format/oai_dc").request().put(Entity.json(
        "{\"metadataPrefix\":\"oai_dc\",\"schemaLocation\":\"http://www.openarchives.org/OAI/2.0/oai_dc.xsd\",\"identifierXpath\":\"/identifier\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testUpdateFormatNoXPath() throws Exception {
    Response response = target("format/oai_dc").request().put(Entity.json(
        "{\"metadataPrefix\":\"oai_dc\",\"schemaLocation\":\"http://www.openarchives.org/OAI/2.0/oai_dc.xsd\",\"schemaNamespace\":\"http://www.openarchives.org/OAI/2.0/oai_dc/\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testUpdateFormatInvamidMetadataPrefix() throws Exception {
    Response response = target("format/oai_dc").request().put(Entity.json(
        "{\"metadataPrefix\":\"oai dc\",\"schemaLocation\":\"http://www.openarchives.org/OAI/2.0/oai_dc.xsd\",\"schemaNamespace\":\"http://www.openarchives.org/OAI/2.0/oai_dc/\",\"identifierXpath\":\"/identifier\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testCreateUpdateNameDoesNotMatch() throws Exception {
    Response response = target("format/abc_dc").request().put(Entity.json(
        "{\"metadataPrefix\":\"oai_dc\",\"schemaLocation\":\"http://www.openarchives.org/OAI/2.0/oai_dc.xsd\",\"schemaNamespace\":\"http://www.openarchives.org/OAI/2.0/oai_dc/\",\"identifierXpath\":\"/identifier\"}"));

    String content = response.readEntity(String.class);
    LOGGER.info("testCreateUpdateNameDoesNotMatch content: " + content);
    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testUpdateFormatNotFound() throws Exception {
    doThrow(NotFoundException.class).when(formatService).update(any());
    
    Response response = target("format/oai_dc").request().put(Entity.json(
        "{\"metadataPrefix\":\"oai_dc\",\"schemaLocation\":\"http://www.openarchives.org/OAI/2.0/oai_dc.xsd\",\"schemaNamespace\":\"http://www.openarchives.org/OAI/2.0/oai_dc/\",\"identifierXpath\":\"/identifier\"}"));

    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  
  
  private List<Format> getTestFormatList() {
    List<Format> formatList = new ArrayList<Format>();

    for (int i = 0; i < 100; i++) {
      Format format = new Format();
      format.setMetadataPrefix("format" + i);
      format.setSchemaLocation("http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
      format.setSchemaNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/");
      format.setIdentifierXpath("/identifier");
      
      formatList.add(format);
    }
    
    return formatList;
  }

}
