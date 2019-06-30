package de.fiz.oai.backend.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import de.fiz.oai.backend.models.SearchResult;


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
    Item item = new Item();
    item.setIdentifier("65465456");
    item.setDatestamp("1972-05-20T20:33:18.772Z");
    item.setDeleteFlag(false);
    item.setSets(List.of("foo", "bar", "baz"));
    item.setIngestFormat("radar");
    
    when(daoItem.read("65465456")).thenReturn(item);
    
    Response response = target("/item/65465456").queryParam("format", "oai_dc").request().get();
    
    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Http Content-Type should be: ", MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
 
    String content = response.readEntity(String.class);
    assertEquals("Content of response is: ", "{\"identifier\":\"65465456\",\"datestamp\":\"1972-05-20T20:33:18.772Z\",\"deleteFlag\":false,\"sets\":[\"foo\",\"bar\",\"baz\"],\"ingestFormat\":\"radar\"}", content);
  }
  
  @Test
  public void testGetItemMissingFormat() throws Exception {
    
    Response response = target("/item/65465456").request().get();
    
    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testGetItemNotFound() throws Exception {
    when(daoItem.read("123Fragerei")).thenReturn(null);
    
    Response response = target("/item/123Fragerei").queryParam("format", "oai_dc").request().get();
    
    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }
  
  
  @Test
  public void testSearchItems() throws Exception {
    when(daoItem.search(any(), any(), any(), any(), any(), any()))
    .thenReturn(getTestItemList());

    Response response = target("/item").queryParam("offset", 0).queryParam("rows", 20).queryParam("set", "abc")
    .queryParam("format", "oai_dc").queryParam("from", "1970-01-01T00:00:01Z").queryParam("until", "2970-01-01T00:00:01Z").queryParam("content", "").request()
    .get();
    
    SearchResult searchResult = response.readEntity(SearchResult.class);
    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Http Content-Type should be: ", MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
    assertEquals("Total results must be 100", 100, searchResult.getTotal());
    assertEquals("Result size must be 100", 100, searchResult.getSize());
    assertNotNull("result list must not be null", searchResult.getData());
  }
  
  @Test
  public void testSearchItemsMissingFormatParameter() throws Exception {
    Response response = target("/item").queryParam("offset", 0).queryParam("rows", 20).queryParam("set", "abc")
    .queryParam("format", "").queryParam("from", "1970-01-01T00:00:01Z").queryParam("until", "2970-01-01T00:00:01Z").queryParam("content", "").request()
    .get();
    
    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testSearchItemsInvalidFromParameter() throws Exception {
    Response response = target("/item").queryParam("offset", 0).queryParam("rows", 20).queryParam("set", "abc")
    .queryParam("format", "oai_dc").queryParam("from", "ABGS").queryParam("until", "2970-01-01T00:00:01Z").queryParam("content", "").request()
    .get();
    
    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testSearchItemsInvalidUntilParameter() throws Exception {
    Response response = target("/item").queryParam("offset", 0).queryParam("rows", 20).queryParam("set", "abc")
    .queryParam("format", "oai_dc").queryParam("from", "1970-01-01T00:00:01Z").queryParam("until", "1970-01T00:00:01Z").queryParam("content", "").request()
    .get();
    
    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  
  @Test
  public void testDeleteItem() throws Exception {
    doNothing().when(daoItem).delete("65465456");
    
    Response response = target("/item/65465456").request().delete();
    
    assertEquals("Http Response should be 204: ", Status.NO_CONTENT.getStatusCode(), response.getStatus());
  }
  
  
  @Test
  public void testDeleteItemEmptyIdentifier() throws Exception {
    doThrow(IOException.class).when(daoItem).delete(" ");
    
    Response response = target("/item/%20").request().delete();
    
    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testDeleteItemNotFound() throws Exception {
    doThrow(NotFoundException.class).when(daoItem).delete("123Fragerei");
    
    Response response = target("/item/123Fragerei").request().delete();
    
    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }
  
  
  @Test
  public void testCreateItem() throws Exception {
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
  
  @Test
  public void testCreateItemBadIdentifier() throws Exception {
    //The json use an identifier that is not in the xml!
    String json = "{\"identifier\":\"NotInXml\",\"datestamp\":\"1972-05-20T20:33:18.772Z\",\"deleteFlag\":false,\"sets\":[\"foo\",\"bar\",\"baz\"],\"ingestFormat\":\"radar\"}";
    
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

    Response response = target("/item").request().post(Entity.entity(form, form.getMediaType()));
    
    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  
  @Test
  public void testUpdateItem() throws Exception {
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
    when(daoItem.read(any())).thenReturn(item);
    
    Response response = target("/item/65465456").request().put(Entity.entity(form, form.getMediaType()));
    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testUpdateItemIdentifierNotFound() throws Exception {
    //The json use an identifier that is not in the xml!
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

    when(daoItem.read(any())).thenReturn(null);//This will trigger the NotFound!
    
    Response response = target("/item/65465456").request().put(Entity.entity(form, form.getMediaType()));
    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testUpdateItemBadIdentifierInJson() throws Exception {
    //The json use an identifier that is not in the xml!
    String json = "{\"identifier\":\"NotInXml\",\"datestamp\":\"1972-05-20T20:33:18.772Z\",\"deleteFlag\":false,\"sets\":[\"foo\",\"bar\",\"baz\"],\"ingestFormat\":\"radar\"}";
    
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

    Response response = target("/item/65465456").request().put(Entity.entity(form, form.getMediaType()));
    
    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testUpdateItemBadIdentifierInXml() throws Exception {
    //The json use an identifier that is not in the xml!
    String json = "{\"identifier\":\"65465456\",\"datestamp\":\"1972-05-20T20:33:18.772Z\",\"deleteFlag\":false,\"sets\":[\"foo\",\"bar\",\"baz\"],\"ingestFormat\":\"radar\"}";
    
    String xml = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" + 
        "    <dc:title>testCreateItem</dc:title>\n" + 
        "    <dc:date>2019-01-01T08:00:00Z</dc:date>\n" + 
        "    <dc:type>text</dc:type>\n" + 
        "    <dc:format>application/pdf</dc:format>\n" + 
        "    <dc:identifier>BadIdentifier</dc:identifier>\n" + 
        "    <dc:source>Some exmaple source</dc:source>\n" + 
        "    <dc:publisher>FIZ Karlsruhe</dc:publisher>\n" + 
        "  </oai_dc:dc>";
    
    FormDataMultiPart form = new FormDataMultiPart();
    form.field("item", json, MediaType.APPLICATION_JSON_TYPE);
    FormDataBodyPart fdp = new FormDataBodyPart("content", xml, MediaType.TEXT_XML_TYPE);
    form.bodyPart(fdp);

    Response response = target("/item/65465456").request().put(Entity.entity(form, form.getMediaType()));
    
    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testUpdateItemBadIdentifierInPath() throws Exception {
    //The json use an identifier that is not in the xml!
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

    Response response = target("/item/badidentifier").request().put(Entity.entity(form, form.getMediaType()));
    
    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  
  private List<Item> getTestItemList() {
    List<Item> items = new ArrayList<Item>();
    
    for (int i = 0; i < 100; i++) {
      Item item = new Item();
      item.setIdentifier(String.valueOf(i));
      
      items.add(item);
    }
    LOGGER.info("getTestItemList size: " + items.size());
    return items;
  }
  
}
