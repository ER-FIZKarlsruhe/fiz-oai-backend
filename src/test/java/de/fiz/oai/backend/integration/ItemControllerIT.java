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
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
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

import de.fiz.oai.backend.FizOaiExceptionMapper;
import de.fiz.oai.backend.controller.ItemController;
import de.fiz.oai.backend.exceptions.AlreadyExistsException;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.service.ItemService;

public class ItemControllerIT extends JerseyTest {

  private Logger LOGGER = LoggerFactory.getLogger(ItemControllerIT.class);

  @Mock
  private ItemService itemService;

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
        bind(itemService).to(ItemService.class);
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
  public void testGetItemNoContent() throws Exception {
    Item item = new Item();
    item.setIdentifier("65465456");
    item.setDatestamp("1972-05-20T20:33:18.772Z");
    item.setDeleteFlag(false);
    item.setTags(List.of("foo", "bar", "baz"));
    item.setFormats(List.of("nlm", "oai_dc"));
    item.setSets(List.of("article", "chapter"));
    item.setIngestFormat("radar");

    when(itemService.read(any(), any(), eq(false))).thenReturn(item);

    Response response = target("/item/65465456").queryParam("format", "oai_dc").request().get();

    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Http Content-Type should be: ", MediaType.APPLICATION_JSON,
        response.getHeaderString(HttpHeaders.CONTENT_TYPE));

    String content = response.readEntity(String.class);
    LOGGER.debug("content " + content);
    assertEquals("Content of response is: ",
        "{\"identifier\":\"65465456\",\"datestamp\":\"1972-05-20T20:33:18.772Z\",\"deleteFlag\":false,\"sets\":[\"article\",\"chapter\"],\"formats\":[\"nlm\",\"oai_dc\"],\"tags\":[\"foo\",\"bar\",\"baz\"],\"ingestFormat\":\"radar\",\"content\":null}", content);
  }

  @Test
  public void testGetItemWithContent() throws Exception {
    Content content = new Content();
    content.setFormat("oai_dc");
    content.setIdentifier("65465456");
    content.setContent("Das ist ein wenig content");

    Item item = new Item();
    item.setIdentifier("65465456");
    item.setDatestamp("1972-05-20T20:33:18.772Z");
    item.setDeleteFlag(false);
    item.setTags(List.of("foo", "bar", "baz"));
    item.setFormats(List.of("nlm", "oai_dc"));
    item.setSets(List.of("article", "chapter"));
    item.setIngestFormat("radar");
    item.setContent(content);

    when(itemService.read(any(), any(), eq(true))).thenReturn(item);

    Response response = target("/item/65465456").queryParam("format", "oai_dc").queryParam("content", "true").request()
        .get();

    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Http Content-Type should be: ", MediaType.APPLICATION_JSON,
        response.getHeaderString(HttpHeaders.CONTENT_TYPE));

    String responseEntity = response.readEntity(String.class);
    LOGGER.info("responseEntity " + responseEntity);
    assertEquals("Content of response is: ",
        "{\"identifier\":\"65465456\",\"datestamp\":\"1972-05-20T20:33:18.772Z\",\"deleteFlag\":false,\"sets\":[\"article\",\"chapter\"],\"formats\":[\"nlm\",\"oai_dc\"],\"tags\":[\"foo\",\"bar\",\"baz\"],\"ingestFormat\":\"radar\",\"content\":{\"identifier\":\"65465456\",\"format\":\"oai_dc\",\"content\":\"Das ist ein wenig content\"}}",
        responseEntity);
  }

  @Test
  public void testGetItemNotFound() throws Exception {
    when(itemService.read(any(), any(), any())).thenReturn(null);

    Response response = target("/item/123Fragerei").queryParam("format", "oai_dc").request().get();

    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void testSearchItemsNoContent() throws Exception {
    when(itemService.search(any(), any(), any(), any(), any(), eq(false), any())).thenReturn(getTestSearchResult());

    Response response = target("/item").queryParam("rows", 20).queryParam("set", "abc").queryParam("format", "oai_dc")
        .queryParam("from", "1970-01-01T00:00:01Z").queryParam("until", "2970-01-01T00:00:01Z")
        .queryParam("content", "").request().get();

    SearchResult searchResult = response.readEntity(SearchResult.class);
    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Http Content-Type should be: ", MediaType.APPLICATION_JSON,
        response.getHeaderString(HttpHeaders.CONTENT_TYPE));
    assertEquals("Total results must be 100", 100, searchResult.getTotal());
    assertEquals("Result size must be 100", 100, searchResult.getSize());
    assertNotNull("result list must not be null", searchResult.getData());
  }

  @Test
  public void testSearchItemsMissingFormatParameter() throws Exception {
    Response response = target("/item").queryParam("offset", 0).queryParam("rows", 20).queryParam("set", "abc")
        .queryParam("format", "").queryParam("from", "1970-01-01T00:00:01Z").queryParam("until", "2970-01-01T00:00:01Z")
        .queryParam("content", "").request().get();

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testSearchItemsInvalidFromParameter() throws Exception {
    Response response = target("/item").queryParam("offset", 0).queryParam("rows", 20).queryParam("set", "abc")
        .queryParam("format", "oai_dc").queryParam("from", "ABGS").queryParam("until", "2970-01-01T00:00:01Z")
        .queryParam("content", "").request().get();

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testSearchItemsInvalidUntilParameter() throws Exception {
    Response response = target("/item").queryParam("offset", 0).queryParam("rows", 20).queryParam("set", "abc")
        .queryParam("format", "oai_dc").queryParam("from", "1970-01-01T00:00:01Z")
        .queryParam("until", "1970-01T00:00:01Z").queryParam("content", "").request().get();

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testDeleteItem() throws Exception {
    doNothing().when(itemService).delete("65465456");

    Response response = target("/item/65465456").request().delete();

    assertEquals("Http Response should be 204: ", Status.NO_CONTENT.getStatusCode(), response.getStatus());
  }

  @Test
  public void testDeleteItemEmptyIdentifier() throws Exception {
    doThrow(IOException.class).when(itemService).delete(" ");

    Response response = target("/item/%20").request().delete();

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testDeleteItemNotFound() throws Exception {
    doThrow(NotFoundException.class).when(itemService).delete("123Fragerei");

    Response response = target("/item/123Fragerei").request().delete();

    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreateItem() throws Exception {
    Item item = new Item();
    item.setIdentifier("65465456");
    item.setDatestamp("1972-05-20T20:33:18.772Z");
    item.setDeleteFlag(false);
    item.setTags(List.of("foo", "bar", "baz"));
    item.setIngestFormat("radar");
    String json = "{\"identifier\":\"65465456\",\"datestamp\":\"1972-05-20T20:33:18.772Z\",\"deleteFlag\":false,\"tags\":[\"foo\",\"bar\",\"baz\"],\"ingestFormat\":\"radar\"}";

    String xml = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
        + "    <dc:title>testCreateItem</dc:title>\n" + "    <dc:date>2019-01-01T08:00:00Z</dc:date>\n"
        + "    <dc:type>text</dc:type>\n" + "    <dc:format>application/pdf</dc:format>\n"
        + "    <dc:identifier>65465456</dc:identifier>\n" + "    <dc:source>Some exmaple source</dc:source>\n"
        + "    <dc:publisher>FIZ Karlsruhe</dc:publisher>\n" + "  </oai_dc:dc>";

    FormDataMultiPart form = new FormDataMultiPart();
    form.field("item", json, MediaType.APPLICATION_JSON_TYPE);
    FormDataBodyPart fdp = new FormDataBodyPart("content", xml, MediaType.TEXT_XML_TYPE);
    form.bodyPart(fdp);

    when(itemService.create(any(Item.class))).thenReturn(item);

    Response response = target("/item").request().post(Entity.entity(form, form.getMediaType()));

    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testCreateItemAlreadyExist() throws Exception {
    
    Item item = new Item();
    item.setIdentifier("65465456");
    item.setDatestamp("1972-05-20T20:33:18.772Z");
    item.setDeleteFlag(false);
    item.setTags(List.of("foo", "bar", "baz"));
    item.setIngestFormat("radar");
    String json = "{\"identifier\":\"65465456\",\"datestamp\":\"1972-05-20T20:33:18.772Z\",\"deleteFlag\":false,\"tags\":[\"foo\",\"bar\",\"baz\"],\"ingestFormat\":\"radar\"}";

    String xml = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
        + "    <dc:title>testCreateItem</dc:title>\n" + "    <dc:date>2019-01-01T08:00:00Z</dc:date>\n"
        + "    <dc:type>text</dc:type>\n" + "    <dc:format>application/pdf</dc:format>\n"
        + "    <dc:identifier>65465456</dc:identifier>\n" + "    <dc:source>Some exmaple source</dc:source>\n"
        + "    <dc:publisher>FIZ Karlsruhe</dc:publisher>\n" + "  </oai_dc:dc>";

    FormDataMultiPart form = new FormDataMultiPart();
    form.field("item", json, MediaType.APPLICATION_JSON_TYPE);
    FormDataBodyPart fdp = new FormDataBodyPart("content", xml, MediaType.TEXT_XML_TYPE);
    form.bodyPart(fdp);

    doThrow(AlreadyExistsException.class).when(itemService).create(any());

    Response response = target("/item").request().post(Entity.entity(form, form.getMediaType()));

    assertEquals("Http Response should be 409: ", Status.CONFLICT.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreateItemBadIdentifier() throws Exception {
    // The json use an identifier that is not in the xml!
    String json = "{\"identifier\":\"NotInXml\",\"datestamp\":\"1972-05-20T20:33:18.772Z\",\"deleteFlag\":false,\"tags\":[\"foo\",\"bar\",\"baz\"],\"ingestFormat\":\"radar\"}";

    String xml = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
        + "    <dc:title>testCreateItem</dc:title>\n" + "    <dc:date>2019-01-01T08:00:00Z</dc:date>\n"
        + "    <dc:type>text</dc:type>\n" + "    <dc:format>application/pdf</dc:format>\n"
        + "    <dc:identifier>65465456</dc:identifier>\n" + "    <dc:source>Some exmaple source</dc:source>\n"
        + "    <dc:publisher>FIZ Karlsruhe</dc:publisher>\n" + "  </oai_dc:dc>";

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
    item.setTags(List.of("foo", "bar", "baz"));
    item.setIngestFormat("radar");
    String json = "{\"identifier\":\"65465456\",\"datestamp\":\"1972-05-20T20:33:18.772Z\",\"deleteFlag\":false,\"tags\":[\"foo\",\"bar\",\"baz\"],\"ingestFormat\":\"radar\"}";

    String xml = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
        + "    <dc:title>testCreateItem</dc:title>\n" + "    <dc:date>2019-01-01T08:00:00Z</dc:date>\n"
        + "    <dc:type>text</dc:type>\n" + "    <dc:format>application/pdf</dc:format>\n"
        + "    <dc:identifier>65465456</dc:identifier>\n" + "    <dc:source>Some exmaple source</dc:source>\n"
        + "    <dc:publisher>FIZ Karlsruhe</dc:publisher>\n" + "  </oai_dc:dc>";

    FormDataMultiPart form = new FormDataMultiPart();
    form.field("item", json, MediaType.APPLICATION_JSON_TYPE);
    FormDataBodyPart fdp = new FormDataBodyPart("content", xml, MediaType.TEXT_XML_TYPE);
    form.bodyPart(fdp);

    when(itemService.update(any(Item.class))).thenReturn(item);

    Response response = target("/item/65465456").request().put(Entity.entity(form, form.getMediaType()));
    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testUpdateItemIdentifierNotFound() throws Exception {
    // The json use an identifier that is not in the xml!
    String json = "{\"identifier\":\"65465456\",\"datestamp\":\"1972-05-20T20:33:18.772Z\",\"deleteFlag\":false,\"tags\":[\"foo\",\"bar\",\"baz\"],\"ingestFormat\":\"radar\"}";

    String xml = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
        + "    <dc:title>testCreateItem</dc:title>\n" + "    <dc:date>2019-01-01T08:00:00Z</dc:date>\n"
        + "    <dc:type>text</dc:type>\n" + "    <dc:format>application/pdf</dc:format>\n"
        + "    <dc:identifier>65465456</dc:identifier>\n" + "    <dc:source>Some exmaple source</dc:source>\n"
        + "    <dc:publisher>FIZ Karlsruhe</dc:publisher>\n" + "  </oai_dc:dc>";

    FormDataMultiPart form = new FormDataMultiPart();
    form.field("item", json, MediaType.APPLICATION_JSON_TYPE);
    FormDataBodyPart fdp = new FormDataBodyPart("content", xml, MediaType.TEXT_XML_TYPE);
    form.bodyPart(fdp);

    doThrow(NotFoundException.class).when(itemService).update(any());

    Response response = target("/item/65465456").request().put(Entity.entity(form, form.getMediaType()));
    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void testUpdateItemBadIdentifierInJson() throws Exception {
    // The json use an identifier that is not in the xml!
    String json = "{\"identifier\":\"NotInXml\",\"datestamp\":\"1972-05-20T20:33:18.772Z\",\"deleteFlag\":false,\"tags\":[\"foo\",\"bar\",\"baz\"],\"ingestFormat\":\"radar\"}";

    String xml = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
        + "    <dc:title>testCreateItem</dc:title>\n" + "    <dc:date>2019-01-01T08:00:00Z</dc:date>\n"
        + "    <dc:type>text</dc:type>\n" + "    <dc:format>application/pdf</dc:format>\n"
        + "    <dc:identifier>65465456</dc:identifier>\n" + "    <dc:source>Some exmaple source</dc:source>\n"
        + "    <dc:publisher>FIZ Karlsruhe</dc:publisher>\n" + "  </oai_dc:dc>";

    FormDataMultiPart form = new FormDataMultiPart();
    form.field("item", json, MediaType.APPLICATION_JSON_TYPE);
    FormDataBodyPart fdp = new FormDataBodyPart("content", xml, MediaType.TEXT_XML_TYPE);
    form.bodyPart(fdp);

    Response response = target("/item/65465456").request().put(Entity.entity(form, form.getMediaType()));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testUpdateItemBadIdentifierInXml() throws Exception {
    // The json use an identifier that is not in the xml!
    String json = "{\"identifier\":\"65465456\",\"datestamp\":\"1972-05-20T20:33:18.772Z\",\"deleteFlag\":false,\"tags\":[\"foo\",\"bar\",\"baz\"],\"ingestFormat\":\"radar\"}";

    String xml = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
        + "    <dc:title>testCreateItem</dc:title>\n" + "    <dc:date>2019-01-01T08:00:00Z</dc:date>\n"
        + "    <dc:type>text</dc:type>\n" + "    <dc:format>application/pdf</dc:format>\n"
        + "    <dc:identifier>BadIdentifier</dc:identifier>\n" + "    <dc:source>Some exmaple source</dc:source>\n"
        + "    <dc:publisher>FIZ Karlsruhe</dc:publisher>\n" + "  </oai_dc:dc>";

    FormDataMultiPart form = new FormDataMultiPart();
    form.field("item", json, MediaType.APPLICATION_JSON_TYPE);
    FormDataBodyPart fdp = new FormDataBodyPart("content", xml, MediaType.TEXT_XML_TYPE);
    form.bodyPart(fdp);

    Response response = target("/item/65465456").request().put(Entity.entity(form, form.getMediaType()));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testUpdateItemBadIdentifierInPath() throws Exception {
    // The json use an identifier that is not in the xml!
    String json = "{\"identifier\":\"65465456\",\"datestamp\":\"1972-05-20T20:33:18.772Z\",\"deleteFlag\":false,\"tags\":[\"foo\",\"bar\",\"baz\"],\"ingestFormat\":\"radar\"}";

    String xml = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
        + "    <dc:title>testCreateItem</dc:title>\n" + "    <dc:date>2019-01-01T08:00:00Z</dc:date>\n"
        + "    <dc:type>text</dc:type>\n" + "    <dc:format>application/pdf</dc:format>\n"
        + "    <dc:identifier>65465456</dc:identifier>\n" + "    <dc:source>Some exmaple source</dc:source>\n"
        + "    <dc:publisher>FIZ Karlsruhe</dc:publisher>\n" + "  </oai_dc:dc>";

    FormDataMultiPart form = new FormDataMultiPart();
    form.field("item", json, MediaType.APPLICATION_JSON_TYPE);
    FormDataBodyPart fdp = new FormDataBodyPart("content", xml, MediaType.TEXT_XML_TYPE);
    form.bodyPart(fdp);

    Response response = target("/item/badidentifier").request().put(Entity.entity(form, form.getMediaType()));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testUpdateItemTags() throws Exception {
    Item item = new Item();
    item.setIdentifier("65465456");
    item.setDatestamp("1972-05-20T20:33:18.772Z");
    item.setDeleteFlag(false);
    item.setTags(List.of("foo", "bar", "baz"));
    item.setIngestFormat("radar");
    when(itemService.updateTags(any(String.class), any(List.class))).thenReturn(item);
    String json = "[\"mih\"]";
    Response response = target("/item/tags/65465456").request().put(Entity.json(json));
    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testUpdateItemMetadataNotExistingIdentifier() throws Exception {
    when(itemService.updateTags(any(String.class), any(List.class))).thenThrow(new WebApplicationException(Status.NOT_FOUND));
    String json = "[\"mih\"]";
    Response response = target("/item/tags/notexisting").request().put(Entity.json(json));
    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  private SearchResult<Item> getTestSearchResult() {
    List<Item> items = new ArrayList<Item>();

    for (int i = 0; i < 100; i++) {
      Item item = new Item();
      item.setIdentifier(String.valueOf(i));

      items.add(item);
    }

    SearchResult<Item> result = new SearchResult<Item>();
    result.setData(items);
    result.setTotal(items.size());
    result.setSize(items.size());

    LOGGER.info("getTestItemList size: " + items.size());
    return result;
  }

}
