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

import java.util.Arrays;
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

import de.fiz.oai.backend.FizOaiExceptionMapper;
import de.fiz.oai.backend.controller.ContentController;
import de.fiz.oai.backend.exceptions.AlreadyExistsException;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.service.ContentService;

public class ContentControllerIT extends JerseyTest {

  private Logger LOGGER = LoggerFactory.getLogger(ContentControllerIT.class);

  @Mock
  private ContentService contentService;

  @Mock
  HttpServletRequest request;

  @Mock
  HttpServletResponse response;

  @Override
  protected Application configure() {
    MockitoAnnotations.initMocks(this);

    ResourceConfig config = new ResourceConfig(ContentController.class);
    config.register(new AbstractBinder() {

      @Override
      protected void configure() {
        bind(contentService).to(ContentService.class);
        bind(request).to(HttpServletRequest.class);
        bind(response).to(HttpServletResponse.class);
      }
    });
    config.register(MultiPartFeature.class);
    config.register(FizOaiExceptionMapper.class);
    return config;
  }

  @Override
  protected void configureClient(ClientConfig clientConfig) {
    clientConfig.register(MultiPartFeature.class);
  }

  @Test
  public void testGetContent() throws Exception {
    Content content = new Content();
    content.setIdentifier("123456");
    content.setFormat("oai_dc");
    content.setContent("Wann wirds endlich wieder Sommer");

    when(contentService.read(any(), any())).thenReturn(content);

    Response response = target("/content/123456/oai_dc").request().get();

    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Http Content-Type should be: ", MediaType.APPLICATION_JSON,
        response.getHeaderString(HttpHeaders.CONTENT_TYPE));
    
    String json = response.readEntity(String.class);
    LOGGER.info("testGetContent json: " + json);
    
//    content = response.readEntity(Content.class);
//    assertEquals("Content format should be: ", "oai_dc", content.getFormat());
//    assertEquals("Content identifier should be: ", "123456", content.getIdentifier());
  }

  @Test
  public void testGetContentEmptyIdentifier() throws Exception {

    Response response = target("/content/%20/asd").request().get();

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetContentEmptyFormat() throws Exception {

    Response response = target("/content/wer/%20").request().get();

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testGetContentNotFound() throws Exception {
    when(contentService.read(any(),any())).thenReturn(null);

    Response response = target("/content/wer/oai_dc").request().get();

    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }
  
  
  @Test
  public void testGetContentFormats() throws Exception {
    Content content1 = new Content();
    content1.setIdentifier("123456");
    content1.setFormat("oai_dc");
    content1.setContent("Wann wirds endlich wieder Sommer");

    Content content2 = new Content();
    content1.setIdentifier("123456");
    content1.setFormat("marc");
    content1.setContent("Wann wirds endlich wieder Sommer");

    List<Content> contentList = Arrays.asList(new Content[]{content1, content2});
    
    when(contentService.readFormats(any())).thenReturn(contentList);

    Response response = target("/content/123456").request().get();

    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Http Content-Type should be: ", MediaType.APPLICATION_JSON,
        response.getHeaderString(HttpHeaders.CONTENT_TYPE));

    List<Content> result = response.readEntity(new GenericType<List<Content>>() {
    });
    assertEquals("result size should be: ", 2, result.size());
  }
  

  @Test
  public void testGetContentAllFormatsEmptyIdentifier() throws Exception {

    Response response = target("/content/%20").request().get();

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testGetContentAllFormatsNotFound() throws Exception {
    when(contentService.readFormats(any())).thenReturn(null);

    Response response = target("/content/weroderwas").request().get();

    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }
  

  @Test
  public void testDeleteContent() throws Exception {
    doNothing().when(contentService).delete(any(),any());

    Response response = target("/content/123456/oai_dc").request().delete();

    assertEquals("Http Response should be 204: ", Status.NO_CONTENT.getStatusCode(), response.getStatus());
  }

  @Test
  public void testDeleteContentEmptyIdentifier() throws Exception {

    Response response = target("/content/%20/asd").request().delete();

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testDeleteContentEmptyFormat() throws Exception {

    Response response = target("/content/wer/%20").request().delete();

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testDeleteContentNotFound() throws Exception {
    doThrow(NotFoundException.class).when(contentService).delete(any(),any());

    Response response = target("/content/wer/oai_dc").request().delete();

    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }
  


  @Test
  public void testCreateContent() throws Exception {
    Content content = new Content();
    content.setIdentifier("123456");
    content.setFormat("oai_dc");
    content.setContent("Wann wirds endlich wieder Sommer");

    when(contentService.create(any())).thenReturn(content);

    Response response = target("content").request().post(Entity.json(
        "{\"identifier\":\"123456\",\"format\":\"oai_dc\",\"content\":\"V2FubiB3aXJkcyBlbmRsaWNoIHdpZWRlciBTb21tZXI=\"}"));

    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testCreateContentAlreadyExist() throws Exception {
    doThrow(AlreadyExistsException.class).when(contentService).create(any());

    Response response = target("content").request().post(Entity.json(
            "{\"identifier\":\"123456\",\"format\":\"oai_dc\",\"content\":\"V2FubiB3aXJkcyBlbmRsaWNoIHdpZWRlciBTb21tZXI=\"}"));
    
    assertEquals("Http Response should be 409: ", Status.CONFLICT.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreateContentNoIdentifier() throws Exception {
   
    Response response = target("content").request().post(Entity.json(
        "{\"identifier\":\"\",\"format\":\"oai_dc\",\"content\":\"V2FubiB3aXJkcyBlbmRsaWNoIHdpZWRlciBTb21tZXI=\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreateContentNoFormat() throws Exception {
   
    Response response = target("content").request().post(Entity.json(
        "{\"identifier\":\"123456\",\"format\":\"\",\"content\":\"V2FubiB3aXJkcyBlbmRsaWNoIHdpZWRlciBTb21tZXI=\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testCreateContentWrongFormat() throws Exception {
   
    Response response = target("content").request().post(Entity.json(
        "{\"identifier\":\"123456\",\"format\":\"oai dc\",\"content\":\"V2FubiB3aXJkcyBlbmRsaWNoIHdpZWRlciBTb21tZXI=\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testCreateContentNoContent() throws Exception {
   
    Response response = target("content").request().post(Entity.json(
        "{\"identifier\":\"123456\",\"format\":\"oai_dc\",\"content\":\"\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  
  
  @Test
  public void testUpdateContent() throws Exception {
    Content content = new Content();
    content.setIdentifier("123456");
    content.setFormat("oai_dc");
    content.setContent("Wann wirds endlich wieder Sommer");

    when(contentService.update(any())).thenReturn(content);

    Response response = target("content").request().put(Entity.json(
        "{\"identifier\":\"123456\",\"format\":\"oai_dc\",\"content\":\"V2FubiB3aXJkcyBlbmRsaWNoIHdpZWRlciBTb21tZXI=\"}"));

    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testUpdateContentNoIdentifier() throws Exception {
   
    Response response = target("content").request().put(Entity.json(
        "{\"identifier\":\"\",\"format\":\"oai_dc\",\"content\":\"V2FubiB3aXJkcyBlbmRsaWNoIHdpZWRlciBTb21tZXI=\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testUpdateContentNoFormat() throws Exception {
   
    Response response = target("content").request().put(Entity.json(
        "{\"identifier\":\"123456\",\"format\":\"\",\"content\":\"V2FubiB3aXJkcyBlbmRsaWNoIHdpZWRlciBTb21tZXI=\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testUpdateContentWrongFormat() throws Exception {
   
    Response response = target("content").request().put(Entity.json(
        "{\"identifier\":\"123456\",\"format\":\"oai dc\",\"content\":\"V2FubiB3aXJkcyBlbmRsaWNoIHdpZWRlciBTb21tZXI=\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testUpdateContentNoContent() throws Exception {
   
    Response response = target("content").request().put(Entity.json(
        "{\"identifier\":\"123456\",\"format\":\"oai_dc\",\"content\":\"\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

}
