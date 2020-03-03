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

import de.fiz.oai.backend.controller.SetController;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Set;
import de.fiz.oai.backend.service.SetService;

public class SetControllerIT extends JerseyTest {

  private Logger LOGGER = LoggerFactory.getLogger(SetControllerIT.class);

  @Mock
  private SetService setService;

  @Mock
  HttpServletRequest request;

  @Mock
  HttpServletResponse response;

  @Override
  protected Application configure() {
    MockitoAnnotations.initMocks(this);

    ResourceConfig config = new ResourceConfig(SetController.class);
    config.register(new AbstractBinder() {

      @Override
      protected void configure() {
        bind(setService).to(SetService.class);
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
  public void testGetSet() throws Exception {
    Set set = new Set();
    set.setName("iee");
    set.setSpec("fiz:iee");
    set.setDescription("This set contains the organization unit IEE");

    when(setService.read("iee")).thenReturn(set);

    Response response = target("/set/iee").request().get();

    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Http Content-Type should be: ", MediaType.APPLICATION_JSON,
        response.getHeaderString(HttpHeaders.CONTENT_TYPE));

    Set result = response.readEntity(Set.class);
    assertEquals("Set name should be: ", "iee", result.getName());
    assertEquals("Set spec should be: ", "fiz:iee", result.getSpec());
    assertEquals("Set description should be: ", "This set contains the organization unit IEE", result.getDescription());
  }

  @Test
  public void testGetSetEmptyName() throws Exception {

    Response response = target("/set/%20").request().get();

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetSetNotFound() throws Exception {
    when(setService.read("notfound")).thenReturn(null);

    Response response = target("/set/notfound").request().get();

    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetAllSets() throws Exception {
    when(setService.readAll()).thenReturn(getTestSetList());

    Response response = target("/set").request().get();

    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Http Content-Type should be: ", MediaType.APPLICATION_JSON,
        response.getHeaderString(HttpHeaders.CONTENT_TYPE));

    List<Set> result = response.readEntity(new GenericType<List<Set>>() {
    });
    assertEquals("result size should be: ", 100, result.size());
  }

  @Test
  public void testDeleteSet() throws Exception {
    doNothing().when(setService).delete("65465456");

    Response response = target("/set/65465456").request().delete();

    assertEquals("Http Response should be 204: ", Status.NO_CONTENT.getStatusCode(), response.getStatus());
  }

  @Test
  public void testDeleteSetEmptyName() throws Exception {
    doThrow(IOException.class).when(setService).delete("%20");

    Response response = target("/set/%20").request().delete();

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testDeleteSetNotFound() throws Exception {
    doThrow(NotFoundException.class).when(setService).delete("123Fragerei");

    Response response = target("/set/123Fragerei").request().delete();

    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreateSet() throws Exception {
    Set set = new Set();
    set.setName("iee");
    set.setSpec("fiz:iee");
    set.setDescription("This set contains the organization unit IEE");

    when(setService.create(any())).thenReturn(set);

    Response response = target("set").request().post(Entity.json(
        "{\"name\":\"iee\",\"spec\":\"fiz:iee\",\"description\":\"This set contains the organization unit IEE\"}"));

    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreateSetNoName() throws Exception {
    Response response = target("set").request()
        .post(Entity.json("{\"spec\":\"fiz:iee\",\"description\":\"This set contains the organization unit IEE\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreateSetNoSpec() throws Exception {
    Response response = target("set").request().post(Entity.json(
        "{\"name\":\"iee\",\"description\":\"This set contains the organization unit IEE\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testCreateSetInvalidSpec() throws Exception {
    Response response = target("set").request().post(Entity.json(
        "{\"name\":\"iee\",\"spec\":\"fiz iee\",\"description\":\"This set contains the organization unit IEE\"}")); //spec must not have whitespace!

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  
  @Test
  public void testUpdateSet() throws Exception {
    Set set = new Set();
    set.setName("iee");
    set.setSpec("fiz:iee");
    set.setDescription("This set contains the organization unit IEE");

    when(setService.update(any())).thenReturn(set);
    
    Response response = target("set/iee").request().put(Entity.json(
        "{\"name\":\"iee\",\"spec\":\"fiz:iee\",\"description\":\"This set contains the organization unit IEE\"}"));

    String content = response.readEntity(String.class);
    LOGGER.info("testUpdateSet content: " + content);
    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testUpdateSetNoName() throws Exception {
    Response response = target("set/iee").request()
        .put(Entity.json("{\"spec\":\"fiz:iee\",\"description\":\"This set contains the organization unit IEE\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testUpdateSetNoSpec() throws Exception {
    Response response = target("set/iee").request().put(Entity.json(
        "{\"name\":\"iee\",\"description\":\"This set contains the organization unit IEE\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testUpdateSetInvalidSpec() throws Exception {
    Response response = target("set/iee").request().put(Entity.json(
        "{\"name\":\"iee\",\"spec\":\"fiz iee\",\"description\":\"This set contains the organization unit IEE\"}")); //spec must not have whitespace!

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testUpdateSetNameDoesNotMatch() throws Exception {
    Response response = target("set/ieee").request().put(Entity.json(
        "{\"name\":\"iee\",\"spec\":\"fiz:iee\",\"description\":\"This set contains the organization unit IEE\"}")); //spec must not have whitespace!
    String content = response.readEntity(String.class);
    LOGGER.info("testCreateUpdateNameDoesNotMatch content: " + content);
    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testUpdateSetNotFound() throws Exception {
    doThrow(NotFoundException.class).when(setService).update(any());
    
    Response response = target("set/iee").request().put(Entity.json(
        "{\"name\":\"iee\",\"spec\":\"fiz:iee\",\"description\":\"This set contains the organization unit IEE\"}"));

    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  
  
  private List<Set> getTestSetList() {
    List<Set> setList = new ArrayList<Set>();

    for (int i = 0; i < 100; i++) {
      Set set = new Set();
      set.setName("iee" + i);
      set.setSpec("fiz:iee:" + i);
      set.setDescription("This set contains the organization unit IEE " + i);
      setList.add(set);
    }
    LOGGER.info("getTestItemList size: " + setList.size());
    return setList;
  }

}
