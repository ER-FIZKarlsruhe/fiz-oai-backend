package de.fiz.oai.backend.testcontainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.testcontainers.containers.output.OutputFrame;

import java.io.IOException;

public class ControllerTestIT extends TestContainerManager {



    @Test
    public void testTomcatIsRunningAndWarDeployed() {
        Assert.assertTrue("Tomcat should be running", tomcatContainer.isRunning());

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/info/version";
        System.out.println("Tomcat is running with deployed WAR at: " + baseUrl);

        Client client = ClientBuilder.newClient();
        Response response = client.target(baseUrl).request().get(); // Replace with your endpoint

        Assertions.assertEquals(200, response.getStatus());
        String responseBody = response.readEntity(String.class);
        Assertions.assertEquals("develop-SNAPSHOT", responseBody); // Example assertion

        client.close();
    }

    @Test
    public void testFormatController() throws IOException {
        Assert.assertTrue("Tomcat should be running", tomcatContainer.isRunning());

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/format";
        System.out.println("Tomcat is running with deployed WAR at: " + baseUrl);


        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("metadataPrefix", "oai_dc");
        node.put("schemaLocation", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
        node.put("schemaNamespace", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        node.put("identifierXpath", "/");
        String json = node.toString();

        EntityBuilder builder = EntityBuilder.create();
        builder.setText(json);
        builder.setContentType(ContentType.APPLICATION_JSON);
        HttpClientContext context = HttpClientContext.create();

        //Check if format already exists at oai-provider
//        HttpGet get = new HttpGet(baseUrl + "/oai_dc");
//        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
//        getResponse.getEntity().consumeContent();
//        getResponse.close();

        CloseableHttpResponse response;
        HttpPost post = new HttpPost(baseUrl);
        post.addHeader("Accept", "application/json");
        response = getHttpResponse(post, builder, context, false);

        Assertions.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    }

    private CloseableHttpResponse getHttpResponse(
            HttpRequestBase requestBase, EntityBuilder builder, HttpClientContext context, boolean useProxy) throws IOException {

        Integer timeout = new Integer(20000);
        RequestConfig defaultRequestConfig =
                RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).setConnectionRequestTimeout(timeout)
                        .build();
        RequestConfig requestConfig =
                RequestConfig.copy(defaultRequestConfig)
                        .setProxy(new HttpHost("proxy", 8888)).build();
        requestBase.setConfig(defaultRequestConfig);
        if (useProxy) {
            requestBase.setConfig(requestConfig);
        }
        HttpEntity httpEntity = builder.build();
        if (requestBase instanceof HttpEntityEnclosingRequestBase) {
            ((HttpEntityEnclosingRequestBase)requestBase).setEntity(httpEntity);
        }
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = client.execute(requestBase, context);

        final String logs = tomcatContainer.getLogs(OutputFrame.OutputType.STDOUT);
        final String errlogs = tomcatContainer.getLogs(OutputFrame.OutputType.STDERR);

        return response;
    }
}
