package de.fiz.oai.backend.integration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

import de.fiz.oai.backend.controller.ItemController;
import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.models.Item;


public class ItemControllerIT extends JerseyTest {

  private Logger LOGGER = LoggerFactory.getLogger(ItemControllerIT.class);

  @Mock
  private DAOItem daoItem;
  
  @Mock
  HttpServletRequest request;
  
  @Mock
  HttpServletResponse response;
  
  @Override
  protected Application configure() {
    MockitoAnnotations.initMocks(this);
    enable(TestProperties.LOG_TRAFFIC);
    ResourceConfig config = new ResourceConfig(ItemController.class);
        config.register(new AbstractBinder() {
          
          @Override
          protected void configure() {
              bind(daoItem).to(DAOItem.class);
              bind(request).to(HttpServletRequest.class);
              bind(response).to(HttpServletResponse.class);
          }
        });
        
    return config;
  }
  
  
  @Test
  public void testGetItem() throws Exception {
    LOGGER.info("testVersion");

    Item item = new Item();
    item.setIdentifier("65465456");
    item.setDatestamp("1972-05-20T20:33:18.772Z");
    item.setDeleteFlag(false);
    item.setSets(List.of("foo", "bar", "baz"));
    item.setIngestFormat("radar");
    
    when(daoItem.read("65465456")).thenReturn(item);
    
    Response response = target("/item/65465456").request().get();
    
    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Http Content-Type should be: ", MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
 
    String content = response.readEntity(String.class);
    LOGGER.info("item json: " + content);
    assertEquals("Content of response is: ", "{\"identifier\":\"65465456\",\"datestamp\":\"1972-05-20T20:33:18.772Z\",\"deleteFlag\":false,\"sets\":[\"foo\",\"bar\",\"baz\"],\"ingestFormat\":\"radar\"}", content);
  }
  
  
  @Test
  public void testGetItemNotFound() throws Exception {
    LOGGER.info("testVersion");

    when(daoItem.read("123Fragerei")).thenReturn(null);
    
    Response response = target("/item/123Fragerei").request().get();
    
    assertEquals("Http Response should be 204: ", Status.NO_CONTENT.getStatusCode(), response.getStatus());
  }
  
}
