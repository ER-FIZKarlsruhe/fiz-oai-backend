package de.fiz.oai.backend.testcontainer;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public abstract class BaseInstance extends TestContainerManager {

    protected Logger LOGGER = LoggerFactory.getLogger(BaseInstance.class);

    private static final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    private static final CloseableHttpClient httpClient;

    static {
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(100);

        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                // Prevents the client from being closed after one request
                .setConnectionManagerShared(true)
                .build();
    }


    @Test
    public void testTomcatIsRunningAndWarDeployed() {
        Assertions.assertTrue(tomcatContainer.isRunning(), "Tomcat should be running");

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/info/version";
        LOGGER.info("Tomcat is running with deployed WAR at: " + baseUrl);

        Client client = ClientBuilder.newClient();
        Response response = client.target(baseUrl).request().get(); // Replace with your endpoint

        Assertions.assertEquals(200, response.getStatus());
        String responseBody = response.readEntity(String.class);
        Assertions.assertNotNull(responseBody); // Example assertion

        client.close();
    }


    protected void createItemNoVerify(String identifier, String template, String setTag) throws IOException {
        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/item/";

        LOGGER.info("createItem {}", identifier);

        String xml = template.replace("@@doi@@", identifier);

        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("identifier", identifier);
        node.put("ingestFormat", "radar");
        node.putPOJO("tags", List.of( setTag));
        String json = node.toString();

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addPart("item", new StringBody(json, ContentType.APPLICATION_JSON));
        builder.addBinaryBody("content", xml.getBytes(StandardCharsets.UTF_8));
        HttpClientContext context = HttpClientContext.create();

        String identifierUrlEncoded = URLEncoder.encode(identifier, StandardCharsets.UTF_8);
        LOGGER.info("identifierUrlEncoded: {}", identifierUrlEncoded);
        CloseableHttpResponse response;
        HttpPost post = new HttpPost(baseUrl);
        response = getHttpResponse(post, builder, context, false);
        response.getEntity().getContent().readAllBytes();
        response.close();

    }


    protected void createItem(String identifier, String template, String setTag) throws IOException {
        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/item/";

        LOGGER.info("createItem {}", identifier);

        String xml = template.replace("@@doi@@", identifier);

        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("identifier", identifier);
        node.put("ingestFormat", "radar");
        node.putPOJO("tags", List.of( setTag));
        String json = node.toString();

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addPart("item", new StringBody(json, ContentType.APPLICATION_JSON));
        builder.addBinaryBody("content", xml.getBytes(StandardCharsets.UTF_8));
        HttpClientContext context = HttpClientContext.create();

        String identifierUrlEncoded = URLEncoder.encode(identifier, StandardCharsets.UTF_8);
        LOGGER.info("identifierUrlEncoded: {}", identifierUrlEncoded);
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
        String baseUrl = "http://" + tomcatContainer.getHost() + ":" +
                tomcatContainer.getMappedPort(8080) + "/oai-backend/item/";

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        HttpClientContext context = HttpClientContext.create();

        HttpGet get = new HttpGet(baseUrl + id + "?format=" + format + "&content=true");
        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);

        try (InputStream is = getResponse.getEntity().getContent()) {
            String responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            Assertions.assertEquals(HttpStatus.SC_OK,
                    getResponse.getStatusLine().getStatusCode());

            // Parse JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);

            String xmlContent = root
                    .path("content")
                    .path("content")
                    .asText();

            Assertions.assertFalse(xmlContent.isEmpty(), "XML content must not be empty");
            Assertions.assertTrue(xmlContent.contains(contains));

            // Validate XML well-formed
            assertWellFormedXml(xmlContent);
        } finally {
            getResponse.close();
        }
    }


    private void assertWellFormedXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new InputSource(new StringReader(xml)));

        } catch (Exception e) {
            Assertions.fail("XML is not well-formed", e);
        }
    }


    protected String searchItems(String format, String set, boolean content, String searchMark) throws IOException {
        String url = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/item?format=" + format + "&content=" + content;
        if (StringUtils.isNotBlank(searchMark)) {
            url += "&searchMark=" + searchMark;
        }

        if (StringUtils.isNotBlank(searchMark)) {
            url += "&set=" + set;
        }

        LOGGER.info("searchItems " );

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
            LOGGER.error(e.getMessage(), e);
        }
        LOGGER.info("searchItems {}", result);
        Assertions.assertEquals(HttpStatus.SC_OK, getResponse.getStatusLine().getStatusCode());
        getResponse.close();

        return result;
    }



    protected boolean reindexElasticsearch(String expectIndexName, String continueIndexName, int expectedResponseCode) throws IOException {
        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/reindex/";

        LOGGER.info("reindex " );
        EntityBuilder builder = EntityBuilder.create();
        builder.setText("");
        HttpClientContext context = HttpClientContext.create();
        String url = baseUrl + "start";
        if (StringUtils.isNotBlank(continueIndexName)) {
            url += "?indexName=" + continueIndexName;
        }
        HttpPost post = new HttpPost(url);
        CloseableHttpResponse postResponse = getHttpResponse(post, builder, context, false);
        postResponse.getEntity().getContent().readAllBytes();

        int statusCode= postResponse.getStatusLine().getStatusCode();
        Assertions.assertEquals(expectedResponseCode, statusCode);
        postResponse.close();

        if(statusCode != HttpStatus.SC_OK) {
            return false;
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        String esUrl = "http://localhost:" + ElasticsearchTestContainer.container.getMappedPort(9200) +"/";

        String indexNameToCheck = expectIndexName;
        if (StringUtils.isNotBlank(continueIndexName)) {
            indexNameToCheck = continueIndexName;
        }

        HttpGet get = new HttpGet(esUrl + "_cat/indices");
        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
        InputStream is = getResponse.getEntity().getContent();
        String indices = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        LOGGER.info("indices: {}", indices);
        Assertions.assertTrue(indices.contains(indexNameToCheck), "Index name "+ indices + " does not fit " + indexNameToCheck);
        Assertions.assertEquals(HttpStatus.SC_OK, getResponse.getStatusLine().getStatusCode());
        getResponse.close();


        get = new HttpGet(esUrl + "_cat/aliases");
        getResponse = getHttpResponse(get, builder, context, false);
        is = getResponse.getEntity().getContent();
        String aliases = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        LOGGER.info("aliases: {}", aliases);
        Assertions.assertTrue(aliases.contains("items " + indexNameToCheck));
        Assertions.assertEquals(HttpStatus.SC_OK, getResponse.getStatusLine().getStatusCode());
        getResponse.close();

        return true;
    }


    protected boolean startReindex(int expectedResponseCode, String continueIndexName)
            throws IOException {

        String baseUrl = "http://" + tomcatContainer.getHost() + ":"
                + tomcatContainer.getMappedPort(8080)
                + "/oai-backend/reindex/start";

        if (StringUtils.isNotBlank(continueIndexName)) {
            baseUrl += "?indexName=" + continueIndexName;
        }

        HttpPost post = new HttpPost(baseUrl);
        EntityBuilder builder = EntityBuilder.create().setText("");

        CloseableHttpResponse response =
                getHttpResponse(post, builder, HttpClientContext.create(), false);

        response.getEntity().getContent().readAllBytes();
        int statusCode = response.getStatusLine().getStatusCode();
        response.close();

        Assertions.assertEquals(expectedResponseCode, statusCode);

        return statusCode == HttpStatus.SC_OK;
    }

    protected void waitForReindexAndVerifyES(String expectedIndexName)
            throws Exception {

        boolean finished = false;

        for (int i = 0; i < 120; i++) {
            Thread.sleep(500);
            String status = getReindexStatus();
            LOGGER.info(status);
            if (status.contains("FINISHED")) {
                finished = true;
                break;
            }
        }

        Assertions.assertTrue(finished, "Reindex did not finish");

        String esUrl = "http://localhost:"
                + ElasticsearchTestContainer.container.getMappedPort(9200) + "/";

        HttpClientContext context = HttpClientContext.create();
        EntityBuilder builder = EntityBuilder.create();

        // ---- indices ----
        HttpGet get = new HttpGet(esUrl + "_cat/indices");
        CloseableHttpResponse response = getHttpResponse(get, builder, context, false);

        String indices = new String(
                response.getEntity().getContent().readAllBytes(),
                StandardCharsets.UTF_8
        );
        response.close();

        Assertions.assertTrue(indices.contains(expectedIndexName),
                "Expected index not found: " + expectedIndexName);

        // ---- aliases ----
        get = new HttpGet(esUrl + "_cat/aliases");
        response = getHttpResponse(get, builder, context, false);

        String aliases = new String(
                response.getEntity().getContent().readAllBytes(),
                StandardCharsets.UTF_8
        );
        response.close();

        Assertions.assertTrue(aliases.contains("items " + expectedIndexName));
    }

    protected void stopReindex(int expectedResponseCode) throws IOException {
        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/reindex/stop";

        LOGGER.info("stopReindex");
        EntityBuilder builder = EntityBuilder.create();
        builder.setText("");
        HttpClientContext context = HttpClientContext.create();

        HttpPost post = new HttpPost(baseUrl);
        CloseableHttpResponse postResponse = getHttpResponse(post, builder, context, false);
        postResponse.getEntity().getContent().readAllBytes();
        int statusCode = postResponse.getStatusLine().getStatusCode();
        postResponse.close();

        Assertions.assertEquals(expectedResponseCode, statusCode);
    }

    protected void awaitReindexFinished() throws Exception {
        boolean finished = false;

        for (int i = 0; i < 120; i++) {
            Thread.sleep(500);
            String status = getReindexStatus();
            LOGGER.info(status);
            if (status.contains("FINISHED")) {
                finished = true;
                break;
            }
        }

        Assertions.assertTrue(finished, "Reindex did not finish after being stopped");
    }

    protected void awaitReindexHasStarted() throws Exception {
        int maxWaitMs = 10_000;
        int pollIntervalMs = 100;

        for (int waited = 0; waited < maxWaitMs; waited += pollIntervalMs) {
            Thread.sleep(pollIntervalMs);

            String status = getReindexStatus();
            LOGGER.info("Reindex status: {}", status);

            // Reindex has started once indexed count > 0
            if (status.contains("STARTED")) {
                return;
            }
        }

        Assertions.fail("Reindex never started indexing within timeout");
    }



    protected void reindexItem(String itemIdentifier, int expectedResponseCode) throws IOException {
        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/reindex/item/" + URLEncoder.encode(itemIdentifier, StandardCharsets.UTF_8);

        LOGGER.info("reindexItem " );
        EntityBuilder builder = EntityBuilder.create();
        builder.setText("");
        HttpClientContext context = HttpClientContext.create();

        HttpPost post = new HttpPost(baseUrl);
        CloseableHttpResponse postResponse = getHttpResponse(post, builder, context, false);
        Assertions.assertEquals(expectedResponseCode, postResponse.getStatusLine().getStatusCode());
        postResponse.close();
    }


    protected void checkItemsInIndex(int expectedItems) throws IOException {
        String esUrl = "http://localhost:" + ElasticsearchTestContainer.container.getMappedPort(9200) +"/";

        EntityBuilder builder = EntityBuilder.create();
        HttpClientContext context = HttpClientContext.create();

        HttpGet get = new HttpGet(esUrl + "items/_search");
        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
        InputStream is = getResponse.getEntity().getContent();
        String searchResult = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        LOGGER.info("search result: {}", searchResult);
        Assertions.assertTrue(searchResult.contains("total\":{\"value\":"+ expectedItems ));

        Assertions.assertEquals(HttpStatus.SC_OK, getResponse.getStatusLine().getStatusCode());
        getResponse.close();
    }

    /**
     * Retrieves a specific item from the Elasticsearch 'items' index by its identifier.
     *
     * @param itemIdentifier The ID of the item to retrieve.
     * @param expectedResponseCode The expected HTTP response code (e.g., HttpStatus.SC_OK).
     * @return The content of the retrieved item as a String.
     * @throws IOException If an I/O error occurs during the HTTP request.
     */
    protected String retrieveItemFromES(String itemIdentifier, int expectedResponseCode) throws IOException {
        // 1. Construct the base Elasticsearch URL
        String esUrl = "http://localhost:" + ElasticsearchTestContainer.container.getMappedPort(9200) +"/";

        // 2. *** FIX: URL Encode the identifier ***
        String encodedIdentifier = URLEncoder.encode(itemIdentifier, StandardCharsets.UTF_8);

        // 3. Build the specific GET request URL for the document using the encoded identifier
        String documentUrl = esUrl + "items/_doc/" + encodedIdentifier; // Use encodedIdentifier here

        LOGGER.info("Retrieving item from ES with URL: {}", documentUrl);

        EntityBuilder builder = EntityBuilder.create();
        HttpClientContext context = HttpClientContext.create();

        // 3. Create the HTTP GET request
        HttpGet get = new HttpGet(documentUrl);

        CloseableHttpResponse getResponse = null;
        String itemContent = null;

        try {
            // 4. Execute the request (assuming getHttpResponse is your utility method)
            getResponse = getHttpResponse(get, builder, context, false);

            // 5. Assert the expected response status code
            Assertions.assertEquals(expectedResponseCode, getResponse.getStatusLine().getStatusCode());

            // 6. If the response is successful (200 OK), read the content
            if (expectedResponseCode == HttpStatus.SC_OK && getResponse.getEntity() != null) {
                InputStream is = getResponse.getEntity().getContent();
                itemContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                LOGGER.info("Successfully retrieved item content: {}", itemContent);
            } else if (getResponse.getEntity() != null) {
                // Log response body even for non-OK status for debugging
                InputStream is = getResponse.getEntity().getContent();
                LOGGER.warn("ES response body (status: {}): {}", expectedResponseCode, new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }

        } finally {
            // 7. Ensure the response is closed
            if (getResponse != null) {
                getResponse.close();
            }
        }

        return itemContent;
    }


    protected void createFormatIfNotExisting(String prefix, String schemaLocation, String namespace) throws IOException {
        Assertions.assertTrue(tomcatContainer.isRunning(), "Tomcat should be running");

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/format/";
        LOGGER.info("Attempting to create format {} if it does not exist.", prefix);

        // --- 1. CHECK IF FORMAT ALREADY EXISTS ---
        HttpClientContext context = HttpClientContext.create();
        EntityBuilder builder = EntityBuilder.create();
        builder.setText(""); // Empty body for GET request
        builder.setContentType(ContentType.APPLICATION_JSON);

        HttpGet getCheck = new HttpGet(baseUrl + prefix);

        try (CloseableHttpResponse getCheckResponse = getHttpResponse(getCheck, builder, context, false)) {
            int checkStatusCode = getCheckResponse.getStatusLine().getStatusCode();

            if (checkStatusCode == HttpStatus.SC_OK) {
                // Format already exists, no need to create it.
                LOGGER.info("Format {} already exists (status {}). Skipping creation.", prefix, checkStatusCode);
                return;
            } else if (checkStatusCode == HttpStatus.SC_NOT_FOUND) {
                // Format does not exist, proceed to create it.
                LOGGER.info("Format {} does not exist (status {}). Proceeding to create.", prefix, checkStatusCode);
                // Fall through to creation logic
            } else {
                // Unexpected status code on check, we might want to fail or log a warning
                LOGGER.warn("Unexpected status code ({}) when checking for format {}. Proceeding to create.", checkStatusCode, prefix);
            }
        }


        // --- 2. CREATE THE FORMAT (If check failed or returned 404) ---
        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("metadataPrefix", prefix);
        node.put("schemaLocation", schemaLocation);
        node.put("schemaNamespace", namespace);
        node.put("identifierXpath", "/");
        String json = node.toString();

        // Rebuild builder for POST request with JSON payload
        builder = EntityBuilder.create();
        builder.setText(json);
        builder.setContentType(ContentType.APPLICATION_JSON);

        CloseableHttpResponse postResponse;
        HttpPost post = new HttpPost(baseUrl);
        post.addHeader("Accept", "application/json");
        postResponse = getHttpResponse(post, builder, context, false);

        try {
            // Logging for debugging (optional, based on your original method)
            //final String logs = tomcatContainer.getLogs(OutputFrame.OutputType.STDOUT);
            //final String errlogs = tomcatContainer.getLogs(OutputFrame.OutputType.STDERR);

            // Assert that the creation was successful (HTTP 200 OK)
            Assertions.assertEquals(HttpStatus.SC_OK, postResponse.getStatusLine().getStatusCode(), "Failed to create format " + prefix);
            LOGGER.info("Format {} successfully created.", prefix);
        } finally {
            postResponse.close();
        }

        // --- 3. VERIFY CREATION (Optional, based on your original method) ---
        // The original method verified immediately after creation.
        // I am keeping this verification step for consistency.

        HttpGet getVerify = new HttpGet(baseUrl + prefix);

        try (CloseableHttpResponse getVerifyResponse = getHttpResponse(getVerify, builder, context, false)) {
            // Read the content to fully consume the entity, then assert status.
            // Reading all bytes can prevent issues with connection reuse.
            getVerifyResponse.getEntity().getContent().readAllBytes();
            Assertions.assertEquals(HttpStatus.SC_OK, getVerifyResponse.getStatusLine().getStatusCode(), "Verification GET failed for format " + prefix);
            LOGGER.info("Format {} successfully verified.", prefix);
        }
    }

    protected void updateFormat(String prefix, String schemaLocation, String namespace) throws IOException{
        LOGGER.info("updateFormat {}", prefix);
        Assertions.assertTrue(tomcatContainer.isRunning(), "Tomcat should be running");

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
        LOGGER.info("deleteFormat {}", prefix);

        Assertions.assertTrue(tomcatContainer.isRunning(), "Tomcat should be running");

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
        LOGGER.info("deleteCrosswalk {}", prefix);

        Assertions.assertTrue(tomcatContainer.isRunning(), "Tomcat should be running");

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

    protected void deleteSet(String spec) throws IOException{
        LOGGER.info("deleteSet {}", spec);

        Assertions.assertTrue(tomcatContainer.isRunning(), "Tomcat should be running");

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/set/";

        EntityBuilder builder = EntityBuilder.create();
        HttpClientContext context = HttpClientContext.create();

        CloseableHttpResponse putResponse;
        HttpDelete delete = new HttpDelete(baseUrl + spec);
        delete.addHeader("Accept", "application/json");
        putResponse = getHttpResponse(delete, builder, context, false);

        Assertions.assertEquals(204, putResponse.getStatusLine().getStatusCode());
        putResponse.close();

        HttpGet get = new HttpGet(baseUrl + spec);
        CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
        getResponse.getEntity().getContent().readAllBytes();
        Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, getResponse.getStatusLine().getStatusCode());
        getResponse.close();
    }


    protected void createSet(String spec, String name, String description, List<String> tags, int expectedResponse) throws IOException{
        Assertions.assertTrue(tomcatContainer.isRunning(), "Tomcat should be running");

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/set/";
        LOGGER.info("Set {}", spec);


        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("spec", spec);
        node.put("name", name);
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

        //final String logs = tomcatContainer.getLogs(OutputFrame.OutputType.STDOUT);
        //final String errlogs = tomcatContainer.getLogs(OutputFrame.OutputType.STDERR);

        Assertions.assertEquals(expectedResponse, postResponse.getStatusLine().getStatusCode());
        postResponse.close();

        if (postResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            HttpGet get = new HttpGet(baseUrl + spec);
            CloseableHttpResponse getResponse = getHttpResponse(get, builder, context, false);
            getResponse.getEntity().getContent().readAllBytes();
            Assertions.assertEquals(HttpStatus.SC_OK, getResponse.getStatusLine().getStatusCode());
            getResponse.close();
        }
    }

    protected void updateSet(String spec, String name, String description, List<String> tags) throws IOException{
        Assertions.assertTrue(tomcatContainer.isRunning(), "Tomcat should be running");

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/set/";
        LOGGER.info("Set {}", spec);


        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("spec", spec);
        node.put("name", name);
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
        HttpPut put = new HttpPut(baseUrl + spec);
        put.addHeader("Accept", "application/json");
        postResponse = getHttpResponse(put, builder, context, false);

        Assertions.assertEquals(HttpStatus.SC_OK, postResponse.getStatusLine().getStatusCode());
        postResponse.close();
    }



    protected void createCrosswalkIfNotExisting(String name, String formatFrom, String formatTo, String xsltStylesheet) throws IOException {
        Assertions.assertTrue(tomcatContainer.isRunning(), "Tomcat should be running");

        LOGGER.info("createCrosswalkIfNotExisting {}", name);

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" + tomcatContainer.getMappedPort(8080) + "/oai-backend/crosswalk/";
        HttpClientContext context = HttpClientContext.create();

        // 1. Check if the crosswalk already exists
        HttpGet getCheck = new HttpGet(baseUrl + name);
        try (CloseableHttpResponse getCheckResponse = getHttpResponse(getCheck, null, context, false)) {
            int statusCode = getCheckResponse.getStatusLine().getStatusCode();

            // If the crosswalk is found (e.g., HTTP 200 OK), then we don't need to create it.
            if (statusCode == HttpStatus.SC_OK) {
                LOGGER.info("Crosswalk {} already exists. Skipping creation.", name);
                // Ensure the response entity is consumed and the response is closed
                getCheckResponse.getEntity().getContent().readAllBytes();
                return;
            }

            // If the crosswalk is not found (e.g., HTTP 404 Not Found), proceed to creation.
            // We will assert that the status is NOT_FOUND if we expect it to be new.
            Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, statusCode,
                    "Expected status 404 Not Found if crosswalk is new.");

            // Ensure the response entity is consumed
            getCheckResponse.getEntity().getContent().readAllBytes();
        }

        // 2. If it does not exist, proceed with creation (POST)
        LOGGER.info("Crosswalk {} not found. Creating it...", name);

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

        HttpPost post = new HttpPost(baseUrl);
        post.addHeader("Accept", "application/json");

        try (CloseableHttpResponse postResponse = getHttpResponse(post, builder, context, false)) {

            // Assert that the creation POST was successful (HTTP 200 OK)
            postResponse.getEntity().getContent().readAllBytes();
            Assertions.assertEquals(HttpStatus.SC_OK, postResponse.getStatusLine().getStatusCode(),
                    "Expected status 200 OK after successful crosswalk creation.");
        }

        // 3. Final verification (GET) to ensure it was created successfully
        HttpGet getVerify = new HttpGet(baseUrl + name);
        try (CloseableHttpResponse getVerifyResponse = getHttpResponse(getVerify, null, context, false)) {

            // Assert that the created crosswalk is now retrievable (HTTP 200 OK)
            getVerifyResponse.getEntity().getContent().readAllBytes();
            Assertions.assertEquals(HttpStatus.SC_OK, getVerifyResponse.getStatusLine().getStatusCode(),
                    "Expected status 200 OK after verifying created crosswalk.");
        }
    }

    protected void updateCrosswalk(String name, String formatFrom, String formatTo, String xsltStylesheet) throws IOException {
        Assertions.assertTrue(tomcatContainer.isRunning(), "Tomcat should be running");

        LOGGER.info("updateCrosswalk {}", name);

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


    protected boolean processCrosswalk(String crosswalkName, int expectedResponseCode)
            throws IOException {

        String baseUrl =
                "http://" + tomcatContainer.getHost()
                        + ":" + tomcatContainer.getMappedPort(8080)
                        + "/oai-backend/crosswalk/"
                        + crosswalkName
                        + "/process?updateItemTimestamp=true";

        LOGGER.info("process crosswalk {}", crosswalkName);

        EntityBuilder builder = EntityBuilder.create();
        builder.setText("");

        HttpClientContext context = HttpClientContext.create();
        HttpPut put = new HttpPut(baseUrl);

        try (CloseableHttpResponse response =
                     getHttpResponse(put, builder, context, false)) {

            int statusCode = response.getStatusLine().getStatusCode();

            Assertions.assertEquals(
                    expectedResponseCode,
                    statusCode,
                    "Unexpected HTTP response code when starting crosswalk"
            );

            // 200 → started
            // anything else (e.g. 409) → rejected
            return statusCode == HttpStatus.SC_OK;
        }
    }



    protected String getCrosswalkStatus(String crosswalkName) throws IOException {
        String url = "http://" + tomcatContainer.getHost() + ":" +
                tomcatContainer.getMappedPort(8080) +
                "/oai-backend/crosswalk/status";
        LOGGER.info("process crosswalk {} ", crosswalkName );
        EntityBuilder builder = EntityBuilder.create();
        builder.setText("");
        HttpClientContext context = HttpClientContext.create();
        HttpGet get = new HttpGet(url);
        try (CloseableHttpResponse response = getHttpResponse(get, builder, context, false)) {
            Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            return EntityUtils.toString(response.getEntity());
        }
    }

    protected String getReindexStatus() throws IOException {
        String url = "http://" + tomcatContainer.getHost() + ":" +
                tomcatContainer.getMappedPort(8080) +
                "/oai-backend/reindex/status";
        LOGGER.info("process reindex");
        EntityBuilder builder = EntityBuilder.create();
        builder.setText("");
        HttpClientContext context = HttpClientContext.create();
        HttpGet get = new HttpGet(url);
        try (CloseableHttpResponse response = getHttpResponse(get, builder, context, false)) {
            Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            return EntityUtils.toString(response.getEntity());
        }
    }


    protected CloseableHttpResponse getHttpResponse(
            HttpRequestBase requestBase, Object builder, HttpClientContext context, boolean useProxy) throws IOException {

        int timeout = 20000;
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(timeout)
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .build();

        // Apply Proxy if requested
        if (useProxy) {
            requestBase.setConfig(RequestConfig.copy(defaultRequestConfig)
                    .setProxy(new HttpHost("proxy", 8888))
                    .build());
        } else {
            requestBase.setConfig(defaultRequestConfig);
        }

        // 2. Optimized Entity Building
        if (builder != null && requestBase instanceof HttpEntityEnclosingRequestBase) {
            HttpEntity entity = null;
            if (builder instanceof EntityBuilder) {
                entity = ((EntityBuilder) builder).build();
            } else if (builder instanceof MultipartEntityBuilder) {
                entity = ((MultipartEntityBuilder) builder).build();
            }
            ((HttpEntityEnclosingRequestBase) requestBase).setEntity(entity);
        }

        // 3. Execute using the Shared Client
        try {
            return httpClient.execute(requestBase, context);
        } catch (IOException e) {
            LOGGER.error("HTTP Request failed: {}", e.getMessage());
            throw e;
        }
    }


    protected void createItems(int totalItems) throws InterruptedException, IOException {
        String template = new String(Files.readAllBytes(Paths.get("src/test/resources/radar-md-template.xml")));

        // Use a thread pool to send requests in parallel
        int threads = Runtime.getRuntime().availableProcessors() * 4;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
        java.util.concurrent.atomic.AtomicInteger failedRequests = new java.util.concurrent.atomic.AtomicInteger(0);

        LOGGER.info("Starting parallel creation of {} items using {} threads", totalItems, threads);

        //Prepare test items. Create a lot, that reindex will not finish too quickly
        for (int i = 1; i <= totalItems; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // This calls the refactored getHttpResponse using the pooled client
                    createItemNoVerify("10.5072/38238_" + System.nanoTime(), template, "testtag");
                } catch (Exception e) {
                    failedRequests.incrementAndGet();
                    LOGGER.error("Failed to create item {}: {}", index, e.getMessage());
                }
            });
        }

        executor.shutdown();
        // Wait up to 2 minutes for all threads to finish
        if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.MINUTES)) {
            executor.shutdownNow();
        }
    }

}
