package de.fiz.oai.backend.integration;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.controller.InfoController;


public class InfoControllerIT extends JerseyTest {

  private Logger LOGGER = LoggerFactory.getLogger(InfoControllerIT.class);

  @Override
  protected Application configure() {
    enable(TestProperties.LOG_TRAFFIC);
    ResourceConfig config = new ResourceConfig(InfoController.class);
    return config;
  }
  
  
  @Test
  public void testVersion() throws Exception {
    LOGGER.info("testVersion");
    Response response = target("/info/version").request().get();
    
    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Http Content-Type should be: ", MediaType.TEXT_PLAIN, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
 
    String content = response.readEntity(String.class);
    assertEquals("Content of response is: ", "0.1.0", content);
  }
  
}