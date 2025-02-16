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
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.testcontainers.containers.output.OutputFrame;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

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
    public void testAllEndpoints() throws IOException {
        createFormat("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        createFormat("radar", "https://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/", "http://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/");
        createFormat("datacite", "https://schema.datacite.org/meta/kernel-4.0/metadata.xsd", "http://datacite.org/schema/kernel-4");

        createCrosswalk("Radar2datacite", "radar", "datacite", "src/test/resources/RadarMD-v9.1-to-DataciteMD-v4_4.xslt");
        createCrosswalk("Radar2OAI_DC_v09", "radar", "oai_dc", "src/test/resources/Radar2OAI_DC_v9.1.xsl");

        sendRadarMetadataToOaiBackend();
    }



    private void sendRadarMetadataToOaiBackend() throws IOException {
        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/item/";

        String doi = "10.5072/38238";
        String xml = new String(Files.readAllBytes(Paths.get("src/test/resources/10.5072-38238.xml")));

        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("identifier", doi);
        node.put("ingestFormat", "radar");
        String json = node.toString();

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addPart("item", new StringBody(json, ContentType.APPLICATION_JSON));
        builder.addBinaryBody("content", xml.getBytes(StandardCharsets.UTF_8));
        HttpClientContext context = HttpClientContext.create();

        String identifierUrlEncoded = URLEncoder.encode(doi, "UTF-8");

//        HttpGet get = new HttpGet(oaiUploadUrl + identifierUrlEncoded)
//        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false)
//        log.info("Search doi in backend returns: " + getResponse.getStatusLine().getStatusCode())
//        getResponse.getEntity().consumeContent()
//        getResponse.close()

        CloseableHttpResponse response;

        HttpPost post = new HttpPost(baseUrl);
        response = getHttpResponse(post, builder, context, false);

        response.getEntity().consumeContent();
        final String logs = tomcatContainer.getLogs(OutputFrame.OutputType.STDOUT);
        final String errlogs = tomcatContainer.getLogs(OutputFrame.OutputType.STDERR);
        response.close();

    }


    private void createFormat(String prefix, String schemaLocation, String namespace) throws IOException{
        Assert.assertTrue("Tomcat should be running", tomcatContainer.isRunning());

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/format/";
        System.out.println("Tomcat is running with deployed WAR at: " + baseUrl);


        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("metadataPrefix", prefix);
        node.put("schemaLocation", schemaLocation);
        node.put("schemaNamespace", namespace);
        node.put("identifierXpath", "/");
        String json = node.toString();

        EntityBuilder builder = EntityBuilder.create();
        builder.setText(json);
        builder.setContentType(ContentType.APPLICATION_JSON);
        HttpClientContext context = HttpClientContext.create();

        CloseableHttpResponse postResponse;
        HttpPost post = new HttpPost(baseUrl);
        post.addHeader("Accept", "application/json");
        postResponse = getHttpResponse(post, builder, context, false);
        Assertions.assertEquals(HttpStatus.SC_OK, postResponse.getStatusLine().getStatusCode());
        postResponse.close();

        HttpGet get = new HttpGet(baseUrl + prefix);
        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
        getResponse.getEntity().consumeContent();
        Assertions.assertEquals(HttpStatus.SC_OK, getResponse.getStatusLine().getStatusCode());
        getResponse.close();

    }

    private void createCrosswalk(String name, String formatFrom, String formatTo, String xsltStylesheet) throws IOException {
        Assert.assertTrue("Tomcat should be running", tomcatContainer.isRunning());

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/crosswalk/";
        String dataciteXslt = new String(Files.readAllBytes(Paths.get(xsltStylesheet)));

        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("name", name);
        node.put("formatFrom", formatFrom);
        node.put("formatTo", formatTo);
        node.put("xsltStylesheet", dataciteXslt);

        String json = node.toString();

        EntityBuilder builder = EntityBuilder.create();
        builder.setText(json);
        builder.setContentType(ContentType.APPLICATION_JSON);
        HttpClientContext context = HttpClientContext.create();

        HttpPost post = new HttpPost(baseUrl);
        post.addHeader("Accept", "application/json");
        CloseableHttpResponse postResponse = getHttpResponse(post, builder, context, false);

        postResponse.getEntity().consumeContent();
        Assertions.assertEquals(HttpStatus.SC_OK, postResponse.getStatusLine().getStatusCode());
        postResponse.close();

        HttpGet get = new HttpGet(baseUrl + "Radar2datacite");
        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
        getResponse.getEntity().consumeContent();
        Assertions.assertEquals(HttpStatus.SC_OK, getResponse.getStatusLine().getStatusCode());
        getResponse.close();

    }

    private CloseableHttpResponse getHttpResponse(
            HttpRequestBase requestBase, Object builder, HttpClientContext context, boolean useProxy) throws IOException {

        int timeout = 20000;
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(timeout)
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .build();

        RequestConfig requestConfig = RequestConfig.copy(defaultRequestConfig)
                .setProxy(new HttpHost("proxy", 8888))
                .build();

        requestBase.setConfig(useProxy ? requestConfig : defaultRequestConfig);

        HttpEntity httpEntity = null;
        if (builder instanceof EntityBuilder) {
            httpEntity = ((EntityBuilder) builder).build();
        } else if (builder instanceof MultipartEntityBuilder) {
            httpEntity = ((MultipartEntityBuilder) builder).build();
        }

        if (httpEntity != null && requestBase instanceof HttpEntityEnclosingRequestBase) {
            ((HttpEntityEnclosingRequestBase) requestBase).setEntity(httpEntity);
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            return client.execute(requestBase, context);
        }
    }
}
