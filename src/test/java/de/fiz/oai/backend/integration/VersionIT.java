package de.fiz.oai.backend.integration;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VersionIT {

  public static String TEST_OAI_URL = "http://localhost:8999/fiz-oai-backend/version";
  
  private Logger LOGGER = LoggerFactory.getLogger(VersionIT.class);

  @Test
  public void testVersion() throws Exception {
    LOGGER.info("testVersion");
    HttpGet httpPost = new HttpGet(TEST_OAI_URL);

    try (CloseableHttpClient client = HttpClientBuilder.create().build();
        CloseableHttpResponse response = client.execute(httpPost)) {
      String bodyAsString = EntityUtils.toString(response.getEntity());
      LOGGER.info("response: " + bodyAsString);
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      Assert.assertNotNull(bodyAsString);
      Assert.assertEquals("0.1.0", bodyAsString);
    }
  }
  
}
