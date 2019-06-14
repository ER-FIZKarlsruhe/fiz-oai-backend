package de.fiz.oai.backend.controller;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

public class VersionControllerTest {

    @Test
    public void testVersion() throws Exception {
        HttpGet httpPost = new HttpGet("http://localhost:8999/fiz-oai-backend/version");

        try (CloseableHttpClient client = HttpClientBuilder.create().build();
             CloseableHttpResponse response = client.execute(httpPost)) {
            String bodyAsString = EntityUtils.toString(response.getEntity());
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            Assert.assertNotNull(bodyAsString);
        }
    }
}
