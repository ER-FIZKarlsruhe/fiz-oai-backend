package de.fiz.oai.backend.testcontainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
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
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.testcontainers.containers.output.OutputFrame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public abstract class BaseInstance extends TestContainerManager {


    @Test
    public void testTomcatIsRunningAndWarDeployed() {
        Assert.assertTrue("Tomcat should be running", tomcatContainer.isRunning());

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/info/version";
        System.out.println("Tomcat is running with deployed WAR at: " + baseUrl);

        Client client = ClientBuilder.newClient();
        Response response = client.target(baseUrl).request().get(); // Replace with your endpoint

        Assertions.assertEquals(200, response.getStatus());
        String responseBody = response.readEntity(String.class);
        Assertions.assertNotNull(responseBody); // Example assertion

        client.close();
    }





    protected void createItem(String identifier, String template) throws IOException {
        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/item/";

        System.out.println("createItem " + identifier);


        String xml = template.replace("@@doi@@", identifier);

        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("identifier", identifier);
        node.put("ingestFormat", "radar");
        node.putPOJO("tags", List.of( "testtag"));
        String json = node.toString();

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addPart("item", new StringBody(json, ContentType.APPLICATION_JSON));
        builder.addBinaryBody("content", xml.getBytes(StandardCharsets.UTF_8));
        HttpClientContext context = HttpClientContext.create();

        String identifierUrlEncoded = URLEncoder.encode(identifier, "UTF-8");
        System.out.println("identifierUrlEncoded: " + identifierUrlEncoded);
        CloseableHttpResponse response;
        HttpPost post = new HttpPost(baseUrl);
        response = getHttpResponse(post, builder, context, false);
        response.getEntity().getContent().readAllBytes();
        response.close();

        //Read item radar content
        testFormatContent(identifierUrlEncoded, "radar", "http://radar-service.eu/schemas/descriptive/radar/v09/radar-elements");
        testFormatContent(identifierUrlEncoded, "oai_dc", "http://purl.org/dc/elements/1.1/");
        testFormatContent(identifierUrlEncoded, "datacite", "http://datacite.org/schema/kernel-4");
    }

    private void testFormatContent(String id, String format, String contains) throws IOException {
        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/item/";
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        HttpClientContext context = HttpClientContext.create();

        HttpGet get = new HttpGet(baseUrl + id + "?format=" + format + "&content=true");
        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
        InputStream is = getResponse.getEntity().getContent();
        String radarXml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        Assertions.assertTrue(radarXml.contains(contains));
        Assertions.assertEquals(HttpStatus.SC_OK, getResponse.getStatusLine().getStatusCode());
        getResponse.close();
    }


    protected String searchItems(String format, boolean content, String searchMark) throws IOException {
        String url = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/item?format=" + format + "&content=" + content;
        if (StringUtils.isNotBlank(searchMark)) {
            url += "&searchMark=" + searchMark;
        }

        System.out.println("searchItems " );

        String result = null;
        EntityBuilder builder = EntityBuilder.create();
        HttpClientContext context = HttpClientContext.create();

        HttpGet get = new HttpGet(url );
        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
        InputStream is = getResponse.getEntity().getContent();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder strBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                strBuilder.append(line).append("\n"); // Append each line
            }
            // Now you have the complete content
            result = strBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("searchItems " + result);
        Assertions.assertEquals(HttpStatus.SC_OK, getResponse.getStatusLine().getStatusCode());
        getResponse.close();

        return result;
    }



    protected void reindexElasticsearch(String expectIndexName) throws IOException {
        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/reindex/";

        System.out.println("reindex " );
        CloseableHttpResponse response;
        EntityBuilder builder = EntityBuilder.create();
        //httpEntity = new StringEntity("", StandardCharsets.UTF_8);
        builder.setText("");
        HttpClientContext context = HttpClientContext.create();

        HttpPost post = new HttpPost(baseUrl + "start");
        CloseableHttpResponse postResponse = getHttpResponse(post, builder, context, false);
        postResponse.getEntity().getContent().readAllBytes();
        Assertions.assertEquals(HttpStatus.SC_OK, postResponse.getStatusLine().getStatusCode());
        postResponse.close();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        String esUrl = "http://localhost:" + ElasticsearchTestContainer.container.getMappedPort(9200) +"/";

        HttpGet get = new HttpGet(esUrl + "_cat/indices");
        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
        InputStream is = getResponse.getEntity().getContent();
        String indices = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("indices: " + indices);
        Assertions.assertTrue(indices.contains(expectIndexName));
        Assertions.assertEquals(HttpStatus.SC_OK, getResponse.getStatusLine().getStatusCode());
        getResponse.close();


        get = new HttpGet(esUrl + "_cat/aliases");
        getResponse = getHttpResponse(get, builder, context, false);
        is = getResponse.getEntity().getContent();
        String aliases = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("aliases: " + aliases);
        Assertions.assertTrue(aliases.contains("items " + expectIndexName));
        Assertions.assertEquals(HttpStatus.SC_OK, getResponse.getStatusLine().getStatusCode());
        getResponse.close();

    }

    protected void checkItemsInIndex(int expectedItems) throws IOException {
        String esUrl = "http://localhost:" + ElasticsearchTestContainer.container.getMappedPort(9200) +"/";

        EntityBuilder builder = EntityBuilder.create();
        HttpClientContext context = HttpClientContext.create();

        HttpGet get = new HttpGet(esUrl + "items/_search");
        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
        InputStream is = getResponse.getEntity().getContent();
        String searchResult = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("search result: " + searchResult);
        Assertions.assertTrue(searchResult.contains("total\":{\"value\":"+ expectedItems ));

        Assertions.assertEquals(HttpStatus.SC_OK, getResponse.getStatusLine().getStatusCode());
        getResponse.close();
    }



    protected void createFormat(String prefix, String schemaLocation, String namespace) throws IOException{
        Assert.assertTrue("Tomcat should be running", tomcatContainer.isRunning());

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/format/";
        System.out.println("createFormat " + prefix);


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

        final String logs = tomcatContainer.getLogs(OutputFrame.OutputType.STDOUT);
        final String errlogs = tomcatContainer.getLogs(OutputFrame.OutputType.STDERR);

        Assertions.assertEquals(HttpStatus.SC_OK, postResponse.getStatusLine().getStatusCode());
        postResponse.close();

        HttpGet get = new HttpGet(baseUrl + prefix);
        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
        getResponse.getEntity().getContent().readAllBytes();
        Assertions.assertEquals(HttpStatus.SC_OK, getResponse.getStatusLine().getStatusCode());
        getResponse.close();

    }

    protected void updateFormat(String prefix, String schemaLocation, String namespace) throws IOException{
        System.out.println("updateFormat " + prefix);
        Assert.assertTrue("Tomcat should be running", tomcatContainer.isRunning());

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/format/";

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

        CloseableHttpResponse putResponse;
        HttpPut put = new HttpPut(baseUrl + prefix);
        put.addHeader("Accept", "application/json");
        putResponse = getHttpResponse(put, builder, context, false);

        Assertions.assertEquals(HttpStatus.SC_OK, putResponse.getStatusLine().getStatusCode());
        putResponse.close();

        HttpGet get = new HttpGet(baseUrl + prefix);
        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
        getResponse.getEntity().getContent().readAllBytes();
        Assertions.assertEquals(HttpStatus.SC_OK, getResponse.getStatusLine().getStatusCode());
        getResponse.close();

    }


    protected void deleteFormat(String prefix) throws IOException{
        System.out.println("deleteFormat " + prefix);

        Assert.assertTrue("Tomcat should be running", tomcatContainer.isRunning());

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/format/";

        EntityBuilder builder = EntityBuilder.create();
        HttpClientContext context = HttpClientContext.create();

        CloseableHttpResponse putResponse;
        HttpDelete delete = new HttpDelete(baseUrl + prefix);
        delete.addHeader("Accept", "application/json");
        putResponse = getHttpResponse(delete, builder, context, false);

        Assertions.assertEquals(204, putResponse.getStatusLine().getStatusCode());
        putResponse.close();

        HttpGet get = new HttpGet(baseUrl + prefix);
        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
        getResponse.getEntity().getContent().readAllBytes();
        Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, getResponse.getStatusLine().getStatusCode());
        getResponse.close();
    }


    protected void deleteCrosswalk(String prefix) throws IOException{
        System.out.println("deleteCrosswalk " + prefix);

        Assert.assertTrue("Tomcat should be running", tomcatContainer.isRunning());

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/crosswalk/";

        EntityBuilder builder = EntityBuilder.create();
        HttpClientContext context = HttpClientContext.create();

        CloseableHttpResponse putResponse;
        HttpDelete delete = new HttpDelete(baseUrl + prefix);
        delete.addHeader("Accept", "application/json");
        putResponse = getHttpResponse(delete, builder, context, false);

        Assertions.assertEquals(204, putResponse.getStatusLine().getStatusCode());
        putResponse.close();

        HttpGet get = new HttpGet(baseUrl + prefix);
        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
        getResponse.getEntity().getContent().readAllBytes();
        Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, getResponse.getStatusLine().getStatusCode());
        getResponse.close();
    }

    protected void deleteSet(String setName) throws IOException{
        System.out.println("deleteSet " + setName);

        Assert.assertTrue("Tomcat should be running", tomcatContainer.isRunning());

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/set/";

        EntityBuilder builder = EntityBuilder.create();
        HttpClientContext context = HttpClientContext.create();

        CloseableHttpResponse putResponse;
        HttpDelete delete = new HttpDelete(baseUrl + setName);
        delete.addHeader("Accept", "application/json");
        putResponse = getHttpResponse(delete, builder, context, false);

        Assertions.assertEquals(204, putResponse.getStatusLine().getStatusCode());
        putResponse.close();

        HttpGet get = new HttpGet(baseUrl + setName);
        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
        getResponse.getEntity().getContent().readAllBytes();
        Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, getResponse.getStatusLine().getStatusCode());
        getResponse.close();
    }


    protected void createSet(String name, String spec, String description, List<String> tags) throws IOException{
        Assert.assertTrue("Tomcat should be running", tomcatContainer.isRunning());

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/set/";
        System.out.println("Set " + name);


        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("name", name);
        node.put("spec", spec);
        node.put("description", description);
        if (tags != null && !tags.isEmpty()) {
            node.putPOJO("tags", tags); // More efficient for lists
        } else {
            node.putArray("tags"); // Empty array if no tags
        }

        String json = node.toString();

        EntityBuilder builder = EntityBuilder.create();
        builder.setText(json);
        builder.setContentType(ContentType.APPLICATION_JSON);
        HttpClientContext context = HttpClientContext.create();

        CloseableHttpResponse postResponse;
        HttpPost post = new HttpPost(baseUrl);
        post.addHeader("Accept", "application/json");
        postResponse = getHttpResponse(post, builder, context, false);

        final String logs = tomcatContainer.getLogs(OutputFrame.OutputType.STDOUT);
        final String errlogs = tomcatContainer.getLogs(OutputFrame.OutputType.STDERR);

        Assertions.assertEquals(HttpStatus.SC_OK, postResponse.getStatusLine().getStatusCode());
        postResponse.close();

        HttpGet get = new HttpGet(baseUrl + name);
        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
        getResponse.getEntity().getContent().readAllBytes();
        Assertions.assertEquals(HttpStatus.SC_OK, getResponse.getStatusLine().getStatusCode());
        getResponse.close();
    }

    protected void updateSet(String name, String spec, String description, List<String> tags) throws IOException{
        Assert.assertTrue("Tomcat should be running", tomcatContainer.isRunning());

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/set/";
        System.out.println("Set " + name);


        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("name", name);
        node.put("spec", spec);
        node.put("description", description);
        if (tags != null && !tags.isEmpty()) {
            node.putPOJO("tags", tags); // More efficient for lists
        } else {
            node.putArray("tags"); // Empty array if no tags
        }

        String json = node.toString();

        EntityBuilder builder = EntityBuilder.create();
        builder.setText(json);
        builder.setContentType(ContentType.APPLICATION_JSON);
        HttpClientContext context = HttpClientContext.create();

        CloseableHttpResponse postResponse;
        HttpPut put = new HttpPut(baseUrl + name);
        put.addHeader("Accept", "application/json");
        postResponse = getHttpResponse(put, builder, context, false);

        Assertions.assertEquals(HttpStatus.SC_OK, postResponse.getStatusLine().getStatusCode());
        postResponse.close();
    }



    protected void createCrosswalk(String name, String formatFrom, String formatTo, String xsltStylesheet) throws IOException {
        Assert.assertTrue("Tomcat should be running", tomcatContainer.isRunning());

        System.out.println("createCrosswalk " + name);

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

        postResponse.getEntity().getContent().readAllBytes();
        Assertions.assertEquals(HttpStatus.SC_OK, postResponse.getStatusLine().getStatusCode());
        postResponse.close();

        HttpGet get = new HttpGet(baseUrl + name);
        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
        getResponse.getEntity().getContent().readAllBytes();
        Assertions.assertEquals(HttpStatus.SC_OK, getResponse.getStatusLine().getStatusCode());
        getResponse.close();
    }

    protected void updateCrosswalk(String name, String formatFrom, String formatTo, String xsltStylesheet) throws IOException {
        Assert.assertTrue("Tomcat should be running", tomcatContainer.isRunning());

        System.out.println("updateCrosswalk " + name);

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

        HttpPut put = new HttpPut(baseUrl + name);
        put.addHeader("Accept", "application/json");
        CloseableHttpResponse putResponse = getHttpResponse(put, builder, context, false);

        putResponse.getEntity().getContent().readAllBytes();
        Assertions.assertEquals(HttpStatus.SC_OK, putResponse.getStatusLine().getStatusCode());
        putResponse.close();
    }

    protected CloseableHttpResponse getHttpResponse(
            HttpRequestBase requestBase, Object builder, HttpClientContext context, boolean useProxy) throws IOException {

        // Default timeout values
        int timeout = 20000;

        // Configuring the request with the timeouts
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(timeout)
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .build();

        // Proxy configuration (if needed)
        RequestConfig requestConfig = RequestConfig.copy(defaultRequestConfig)
                .setProxy(new HttpHost("proxy", 8888))
                .build();

        // Apply the request config
        requestBase.setConfig(useProxy ? requestConfig : defaultRequestConfig);

        // Build entity if needed
        HttpEntity httpEntity = null;
        if (builder instanceof EntityBuilder) {
            httpEntity = ((EntityBuilder) builder).build();
        } else if (builder instanceof MultipartEntityBuilder) {
            httpEntity = ((MultipartEntityBuilder) builder).build();
        }

        // If the request is an HTTP entity enclosing request, set the entity
        if (httpEntity != null && requestBase instanceof HttpEntityEnclosingRequestBase) {
            ((HttpEntityEnclosingRequestBase) requestBase).setEntity(httpEntity);
        }

        // Using HttpClient with connection management
        try {
            CloseableHttpClient client = createHttpClient(defaultRequestConfig);
            // Execute the request and return the response
            return client.execute(requestBase, context);
        } catch (SocketException e) {
            // Handle socket exceptions like "Socket closed"
            System.err.println("Socket closed unexpectedly: " + e.getMessage());
            throw new IOException("Error during HTTP request", e);
        } catch (IOException e) {
            // Handle general IO exceptions
            System.err.println("IO Error during HTTP request: " + e.getMessage());
            throw e;
        }
    }

    private CloseableHttpClient createHttpClient(RequestConfig defaultRequestConfig) {
        // Connection manager to manage multiple connections efficiently
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200);  // Set the maximum total connections
        cm.setDefaultMaxPerRoute(20);  // Set the maximum connections per route (per target host)

        return HttpClients.custom()
                .setDefaultRequestConfig(defaultRequestConfig)
                .setConnectionManager(cm)  // Set the connection manager
                .build();
    }
}
