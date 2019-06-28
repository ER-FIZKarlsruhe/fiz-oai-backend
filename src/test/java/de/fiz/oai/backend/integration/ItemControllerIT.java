package de.fiz.oai.backend.integration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
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
import de.fiz.oai.backend.exceptions.NotFoundException;
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
    config.register(MultiPartFeature.class);
        
    return config;
  }
  
  @Override
  protected void configureClient(ClientConfig clientConfig) {
      clientConfig.register(MultiPartFeature.class);
}
  
  @Test
  public void testGetItem() throws Exception {
    LOGGER.info("testGetItem");

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
    LOGGER.info("testGetItemNotFound");

    when(daoItem.read("123Fragerei")).thenReturn(null);
    
    Response response = target("/item/123Fragerei").request().get();
    
    assertEquals("Http Response should be 204: ", Status.NO_CONTENT.getStatusCode(), response.getStatus());
  }
  
  
  @Test
  public void testDeleteItem() throws Exception {
    LOGGER.info("testDeleteItem");

    doNothing().when(daoItem).delete("65465456");
    
    Response response = target("/item/65465456").request().delete();
    
    assertEquals("Http Response should be 204: ", Status.NO_CONTENT.getStatusCode(), response.getStatus());
  }
  
  
  @Test
  public void testDeleteItemEmptyIdentifier() throws Exception {
    LOGGER.info("testDeleteItemEmptyIdentifier");

    doThrow(IOException.class).when(daoItem).delete(" ");
    
    Response response = target("/item/%20").request().delete();
    
    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testDeleteItemNotFound() throws Exception {
    LOGGER.info("testDeleteItemNotFound");

    doThrow(NotFoundException.class).when(daoItem).delete("123Fragerei");
    
    Response response = target("/item/123Fragerei").request().delete();
    
    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }
  
  
  @Test
  public void testCreateItem() throws Exception {
    LOGGER.info("testCreateItem");
    
    Item item = new Item();
    item.setIdentifier("65465456");
    item.setDatestamp("1972-05-20T20:33:18.772Z");
    item.setDeleteFlag(false);
    item.setSets(List.of("foo", "bar", "baz"));
    item.setIngestFormat("radar");
    String json = "{\"identifier\":\"65465456\",\"datestamp\":\"1972-05-20T20:33:18.772Z\",\"deleteFlag\":false,\"sets\":[\"foo\",\"bar\",\"baz\"],\"ingestFormat\":\"radar\"}";
    
    String xml = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" + 
        "    <dc:title>testCreateItem</dc:title>\n" + 
        "    <dc:date>2019-01-01T08:00:00Z</dc:date>\n" + 
        "    <dc:type>text</dc:type>\n" + 
        "    <dc:format>application/pdf</dc:format>\n" + 
        "    <dc:identifier>65465456</dc:identifier>\n" + 
        "    <dc:source>Some exmaple source</dc:source>\n" + 
        "    <dc:publisher>FIZ Karlsruhe</dc:publisher>\n" + 
        "  </oai_dc:dc>";
    
    FormDataMultiPart form = new FormDataMultiPart();
    form.field("item", json, MediaType.APPLICATION_JSON_TYPE);
    FormDataBodyPart fdp = new FormDataBodyPart("content", xml, MediaType.TEXT_XML_TYPE);
    form.bodyPart(fdp);
    
    

    when(daoItem.create(any(Item.class))).thenReturn(item);
    
    Response response = target("/item").request().post(Entity.entity(form, form.getMediaType()));
    
    
    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
  }
  
}
