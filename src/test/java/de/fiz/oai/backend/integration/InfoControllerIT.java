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

import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.FizOaiExceptionMapper;
import de.fiz.oai.backend.controller.InfoController;
import de.fiz.oai.backend.service.TransformerService;


public class InfoControllerIT extends JerseyTest {

  @Mock
  private TransformerService transformerService;
  
  private Logger LOGGER = LoggerFactory.getLogger(InfoControllerIT.class);

  @Override
  protected Application configure() {
    MockitoAnnotations.initMocks(this);
    enable(TestProperties.LOG_TRAFFIC);
    ResourceConfig config = new ResourceConfig(InfoController.class);
    config.register(new AbstractBinder() {

      @Override
      protected void configure() {
        bind(transformerService).to(TransformerService.class);
      }
    });
    config.register(FizOaiExceptionMapper.class);
    return config;
  }
  
  
  @Test
  public void testVersion() throws Exception {
    LOGGER.info("testVersion");
    Response response = target("/info/version").request().get();
    
    //During test this method will return a 204, as the MANIFEST file containing the real version ist not available!
    assertEquals("Http Response should be 204: ", Status.NO_CONTENT.getStatusCode(), response.getStatus());
  }
  
}
