package de.fiz.oai.backend.integration;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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

import de.fiz.oai.backend.controller.CrosswalkController;
import de.fiz.oai.backend.dao.DAOCrosswalk;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Crosswalk;
import de.fiz.oai.backend.models.Format;

public class CrosswalkControllerIT extends JerseyTest {

  private Logger LOGGER = LoggerFactory.getLogger(CrosswalkControllerIT.class);

  @Mock
  private DAOCrosswalk daoCrosswalk;

  @Mock
  HttpServletRequest request;

  @Mock
  HttpServletResponse response;

  @Override
  protected Application configure() {
    MockitoAnnotations.initMocks(this);

    ResourceConfig config = new ResourceConfig(CrosswalkController.class);
    config.register(new AbstractBinder() {

      @Override
      protected void configure() {
        bind(daoCrosswalk).to(DAOCrosswalk.class);
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
  public void testGetCrosswalk() throws Exception {
    Crosswalk crosswalk = new Crosswalk();
    crosswalk.setName("Oai2Marc");
    crosswalk.setFormatFrom("oai_dc");
    crosswalk.setFormatTo("oai_dc");
    crosswalk.setXsltStylesheet("Please use an Xslt stylesheet here!".getBytes());

    when(daoCrosswalk.read(any())).thenReturn(crosswalk);

    Response response = target("/crosswalk/Oai2Marc").request().get();

    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Http Crosswalk-Type should be: ", MediaType.APPLICATION_JSON,
        response.getHeaderString(HttpHeaders.CONTENT_TYPE));
    
    String json = response.readEntity(String.class);
    LOGGER.info("testGetCrosswalk json: " + json);
    
//    crosswalk = response.readEntity(Crosswalk.class);
//    assertEquals("Crosswalk format should be: ", "oai_dc", crosswalk.getFormat());
//    assertEquals("Crosswalk identifier should be: ", "123456", crosswalk.getIdentifier());
  }

  @Test
  public void testGetCrosswalkEmptyName() throws Exception {

    Response response = target("/crosswalk/%20").request().get();

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }


  
  @Test
  public void testGetCrosswalkNotFound() throws Exception {
    when(daoCrosswalk.read(any())).thenReturn(null);

    Response response = target("/crosswalk/wer").request().get();

    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }
  
  
  @Test
  public void testGetAllCrosswalks() throws Exception {
    when(daoCrosswalk.readAll()).thenReturn(getTestCrosswalkList());

    Response response = target("/crosswalk").request().get();

    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
    assertEquals("Http Content-Type should be: ", MediaType.APPLICATION_JSON,
        response.getHeaderString(HttpHeaders.CONTENT_TYPE));

    List<Crosswalk> result = response.readEntity(new GenericType<List<Crosswalk>>() {
    });
    assertEquals("result size should be: ", 100, result.size());
  }


  

  @Test
  public void testDeleteCrosswalk() throws Exception {
    doNothing().when(daoCrosswalk).delete(any());

    Response response = target("/crosswalk/123456").request().delete();

    assertEquals("Http Response should be 204: ", Status.NO_CONTENT.getStatusCode(), response.getStatus());
  }


  @Test
  public void testDeleteCrosswalkEmptyName() throws Exception {

    Response response = target("/crosswalk/%20").request().delete();

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testDeleteCrosswalkNotFound() throws Exception {
    doThrow(NotFoundException.class).when(daoCrosswalk).delete(any());

    Response response = target("/crosswalk/wer").request().delete();

    assertEquals("Http Response should be 404: ", Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }
  


  @Test
  public void testCreateFormat() throws Exception {
    Crosswalk crosswalk = new Crosswalk();
    crosswalk.setName("Oai2Marc");
    crosswalk.setFormatFrom("oai_dc");
    crosswalk.setFormatTo("oai_dc");
    crosswalk.setXsltStylesheet("Please use an Xslt stylesheet here!".getBytes());

    when(daoCrosswalk.create(any())).thenReturn(crosswalk);

    Response response = target("crosswalk").request().post(Entity.json(
        "{\"name\":\"Oai2Marc\",\"formatFrom\":\"oai_dc\",\"formatTo\":\"oai_dc\",\"xsltStylesheet\":\"UGxlYXNlIHVzZSBhbiBYc2x0IHN0eWxlc2hlZXQgaGVyZSE=\"}"));

    assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreateFormatNoName() throws Exception {
   
    Response response = target("crosswalk").request().post(Entity.json(
        "{\"formatFrom\":\"oai_dc\",\"formatTo\":\"oai_dc\",\"xsltStylesheet\":\"UGxlYXNlIHVzZSBhbiBYc2x0IHN0eWxlc2hlZXQgaGVyZSE=\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreateFormatNoFormatFrom() throws Exception {
   
    Response response = target("crosswalk").request().post(Entity.json(
        "{\"name\":\"Oai2Marc\",\"formatTo\":\"oai_dc\",\"xsltStylesheet\":\"UGxlYXNlIHVzZSBhbiBYc2x0IHN0eWxlc2hlZXQgaGVyZSE=\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testCreateFormatNoFormatTo() throws Exception {
   
    Response response = target("crosswalk").request().post(Entity.json(
        "{\"name\":\"Oai2Marc\",\"formatFrom\":\"oai_dc\",\"xsltStylesheet\":\"UGxlYXNlIHVzZSBhbiBYc2x0IHN0eWxlc2hlZXQgaGVyZSE=\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreateFormatNoXslt() throws Exception {
   
    Response response = target("crosswalk").request().post(Entity.json(
        "{\"name\":\"Oai2Marc\",\"formatFrom\":\"oai_dc\",\"formatTo\":\"oai_dc\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  
  @Test
  public void testCreateFormatWrongFormatFrom() throws Exception {
   
    Response response = target("crosswalk").request().post(Entity.json(
        "{\"name\":\"Oai2Marc\",\"formatFrom\":\"oai dc\",\"formatTo\":\"oai_dc\",\"xsltStylesheet\":\"UGxlYXNlIHVzZSBhbiBYc2x0IHN0eWxlc2hlZXQgaGVyZSE=\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
 
  @Test
  public void testCreateFormatWrongFormatTo() throws Exception {
   
    Response response = target("crosswalk").request().post(Entity.json(
        "{\"name\":\"Oai2Marc\",\"formatFrom\":\"oai_dc\",\"formatTo\":\"o a i\",\"xsltStylesheet\":\"UGxlYXNlIHVzZSBhbiBYc2x0IHN0eWxlc2hlZXQgaGVyZSE=\"}"));

    assertEquals("Http Response should be 400: ", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  
  private List<Crosswalk> getTestCrosswalkList() {
    List<Crosswalk> formatList = new ArrayList<Crosswalk>();

    for (int i = 0; i < 100; i++) {
      Crosswalk crosswalk = new Crosswalk();
      crosswalk.setName("Crosswalk" + i);
      crosswalk.setFormatFrom("oai_dc");
      crosswalk.setFormatTo("marc");
      crosswalk.setXsltStylesheet("Please use an Xslt stylesheet here!".getBytes());
      
      formatList.add(crosswalk);
    }
    
    return formatList;
  }

}
