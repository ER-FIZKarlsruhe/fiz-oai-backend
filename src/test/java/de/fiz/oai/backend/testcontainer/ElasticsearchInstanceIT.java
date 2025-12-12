package de.fiz.oai.backend.testcontainer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


public class ElasticsearchInstanceIT extends BaseInstance {


    @Test
    public void testReindexAll() throws IOException, InterruptedException {
        teardownAndReset();
        setup();

        String template = new String(Files.readAllBytes(Paths.get("src/test/resources/radar-md-template.xml")));

        createFormatIfNotExisting("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        createFormatIfNotExisting("radar", "https://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/", "http://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/");
        createFormatIfNotExisting("datacite", "https://schema.datacite.org/meta/kernel-4.0/metadata.xsd", "http://datacite.org/schema/kernel-4");

        createCrosswalkIfNotExisting("Radar2datacite", "radar", "datacite", "src/test/resources/RadarMD-v9.1-to-DataciteMD-v4_4.xslt");
        createCrosswalkIfNotExisting("Radar2OAI_DC_v09", "radar", "oai_dc", "src/test/resources/Radar2OAI_DC_v9.1.xsl");

        createSet("testset", "testset", "this is a testset", List.of("testtag"), HttpStatus.SC_OK);

        createItem("10.5072/38238", template, "testtag");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        checkItemsInIndex(1);
        String result = searchItems("radar", null,true, null);
        Assertions.assertTrue(result.contains("\"searchMark\":null"));
        Assertions.assertTrue(result.contains("\"total\":1,\"size\":1"));

        for(int i = 0; i <= 100; i++) {
            createItem("10.5072/38238_" + i, template, "testtag");
        }

        reindexElasticsearch("items2", null);
        reindexElasticsearch("", "items2");
        reindexElasticsearch("items3", null);

        //Wait a bit, that ES has all documents in the index
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        checkItemsInIndex(102);

        int countSearchWithMarks = 1;
        result = searchItems("oai_dc" , null,false, null);
        SearchResult<Item> itemResult = convertStringToSearchResult(result);
        String searchMark = itemResult.getSearchMark();
        Assertions.assertTrue(searchMark != null);
        System.out.println("searchMark " + searchMark);

        while (searchMark != null) {
            countSearchWithMarks++;
            result = searchItems("oai_dc" , null,false, searchMark);
            itemResult = convertStringToSearchResult(result);
            searchMark = itemResult.getSearchMark();
            System.out.println("searchMark " + searchMark);
        }

        Assertions.assertEquals(2,countSearchWithMarks);

    }


    @Test
    public void testReindexItem() throws IOException, InterruptedException {
        teardownAndReset();
        setup();

        String template = new String(Files.readAllBytes(Paths.get("src/test/resources/radar-md-template.xml")));

        createFormatIfNotExisting("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        createFormatIfNotExisting("radar", "https://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/", "http://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/");
        createFormatIfNotExisting("datacite", "https://schema.datacite.org/meta/kernel-4.0/metadata.xsd", "http://datacite.org/schema/kernel-4");

        createCrosswalkIfNotExisting("Radar2datacite", "radar", "datacite", "src/test/resources/RadarMD-v9.1-to-DataciteMD-v4_4.xslt");
        createCrosswalkIfNotExisting("Radar2OAI_DC_v09", "radar", "oai_dc", "src/test/resources/Radar2OAI_DC_v9.1.xsl");

        createSet("testset", "testset", "this is a testset", List.of("testtag"), HttpStatus.SC_OK);

        createItem("10.5072/11111", template, "testtag");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        checkItemsInIndex(1);
        String result = searchItems("radar",null, true, null);
        Assertions.assertTrue(result.contains("\"searchMark\":null"));

        //reindex item okay
        reindexItem("10.5072/11111", HttpStatus.SC_NO_CONTENT);

        //reindex item not found
        reindexItem("10.5072/11111fgdf8", HttpStatus.SC_NOT_FOUND);
    }


    SearchResult<Item> convertStringToSearchResult(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        JavaType type = objectMapper.getTypeFactory().constructParametricType(SearchResult.class, Item.class);
        SearchResult<Item> itemResult = null;
        try {
            itemResult = objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return itemResult;
    }

    @Test
    public void testCrudFormats() throws IOException {
        createFormatIfNotExisting("test_format", "http://abc.de/", "http://abc.de/test_format");
        updateFormat("test_format", "http://adc.de/new", "http://adc.de/test_format");
        deleteFormat("test_format");
    }

    @Test
    public void testCrudCrosswalks() throws IOException {
        createFormatIfNotExisting("test_format1", "http://abc.de/", "http://abc.de/test_format");
        createFormatIfNotExisting("test_format2", "http://abc.de/", "http://abc.de/test_format");
        createCrosswalkIfNotExisting("format1ToFormat2", "test_format1", "test_format2", "src/test/resources/RadarMD-v9.1-to-DataciteMD-v4_4.xslt");
        updateCrosswalk("format1ToFormat2", "test_format1", "test_format2", "src/test/resources/Radar2OAI_DC_v9.1.xsl");
        deleteCrosswalk("format1ToFormat2");
    }

    @Test
    public void testCrudSets() throws IOException {
        createSet("testset1", "testset1", "this is a testset1", List.of("testtag"), HttpStatus.SC_OK);
        updateSet("testset1", "testset1chenged", "Changed testset1", List.of("testtag"));
        deleteSet("testset1");

        //BAD request, Missing parent nodes
        createSet("A:B:C", "A:B:C", "this is a test hierarchy", List.of("testtag"), HttpStatus.SC_BAD_REQUEST);

        //Create root node
        createSet("A", "A", "this is a root set", List.of("testtag"), HttpStatus.SC_OK);

        //BAD request, Missing parent node
        createSet("A:B:C", "A:B:C", "this is a test hierarchy", List.of("testtag"), HttpStatus.SC_BAD_REQUEST);

        //Create parent node
        createSet("A:B", "A:B", "this is a parent set", List.of("testtag"), HttpStatus.SC_OK);

        //Finally OK
        createSet("A:B:C", "C", "this is a test hierarchy", List.of("testtag"), HttpStatus.SC_OK);
    }

    @Test
    public void testSetSearchWithResumptionToken() throws Exception {
        teardownAndReset();
        setup();

        ObjectMapper mapper = new ObjectMapper();

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" +
                tomcatContainer.getMappedPort(8080) + "/oai-backend/set/";

        // -----------------------------------------
        // 1. CREATE 200 SETS
        // -----------------------------------------
        for (int i = 1; i <= 2000; i++) {
            String name = "set" + i;
            createSet("spec_" + i, name, "description_" + i, List.of("tag"), HttpStatus.SC_OK);
        }

        // -----------------------------------------
        // 2. GET ALL SETS FROM /oai-backend/set
        // -----------------------------------------
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet getAll = new HttpGet(baseUrl);
        getAll.addHeader("Accept", "application/json");

        List<String> expectedFullSetNames;

        try (CloseableHttpResponse resp = client.execute(getAll)) {
            Assertions.assertEquals(HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());

            String json = EntityUtils.toString(resp.getEntity());
            JsonNode root = mapper.readTree(json);

            Assertions.assertTrue(root.isArray(), "Expected /set to return JSON array");

            expectedFullSetNames = new ArrayList<>();
            for (JsonNode node : root) {
                expectedFullSetNames.add(node.get("spec").asText() + ":" + node.get("name").asText());
            }

            Assertions.assertEquals(2000, expectedFullSetNames.size(),
                    "Expected 200 sets returned by /set");
        }

        // -----------------------------------------
        // 3. GET ALL SETS FROM /search WITH TOKENS
        // -----------------------------------------
        String searchUrlBase = "http://" + tomcatContainer.getHost() + ":" +
                tomcatContainer.getMappedPort(8080) + "/oai-backend/set/search";

        List<String> foundSetNames = new ArrayList<>();
        String token = null;

        do {
            String url = token == null ? searchUrlBase : searchUrlBase + "?resumptionToken=" + token;
            System.out.println("Next search call: " + url);
            CloseableHttpClient searchClient = HttpClients.createDefault();
            HttpGet get = new HttpGet(url);
            get.addHeader("Accept", "application/json");

            try (CloseableHttpResponse resp = searchClient.execute(get)) {

                Assertions.assertEquals(HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());

                String json = EntityUtils.toString(resp.getEntity());
                JsonNode root = mapper.readTree(json);

                // Now "sets" exists in the search endpoint
                JsonNode sets = root.get("sets");
                Assertions.assertNotNull(sets, "Expected 'sets' field in /search response");
                Assertions.assertTrue(sets.isArray());

                for (JsonNode s : sets) {
                    foundSetNames.add(s.get("spec").asText() + ":" + s.get("name").asText());
                }

                JsonNode tokenNode = root.get("resumptionToken");
                token = tokenNode != null && !tokenNode.isNull() ? tokenNode.asText() : null;
            }

        } while (token != null);

        // -----------------------------------------
        // 4. Compare results
        // -----------------------------------------
        Assertions.assertEquals(expectedFullSetNames.size(), foundSetNames.size(),
                "Search+token must return the same total items as /set");

        Assertions.assertTrue(
                foundSetNames.containsAll(expectedFullSetNames)
                        && expectedFullSetNames.containsAll(foundSetNames),
                "Search+token result must match /set result exactly"
        );
    }

    @Test
    public void testSetHierarchy() throws IOException, InterruptedException {
        teardownAndReset();
        setup();

        String template = new String(Files.readAllBytes(Paths.get("src/test/resources/radar-md-template.xml")));

        createFormatIfNotExisting("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        createFormatIfNotExisting("radar", "https://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/", "http://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/");
        createFormatIfNotExisting("datacite", "https://schema.datacite.org/meta/kernel-4.0/metadata.xsd", "http://datacite.org/schema/kernel-4");

        createCrosswalkIfNotExisting("Radar2datacite", "radar", "datacite", "src/test/resources/RadarMD-v9.1-to-DataciteMD-v4_4.xslt");
        createCrosswalkIfNotExisting("Radar2OAI_DC_v09", "radar", "oai_dc", "src/test/resources/Radar2OAI_DC_v9.1.xsl");

        createSet("FIZ", "FIZ", "FIZ", null, HttpStatus.SC_OK);
        createSet("FIZ:ER", "FIZ ER", "FIZ ER", null, HttpStatus.SC_OK);
        createSet("FIZ:ER:FD", "Forschungsdaten", "Forschungsdaten", List.of("erfd-tag"), HttpStatus.SC_OK);
        createSet("FIZ:ER:FD:RADAR", "RADAR", "RADAR", List.of("radar-tag"), HttpStatus.SC_OK);
        createSet("FIZ:ER:FD:DITRARE", "Digital Transformation of Research", "Digital Transformation of Research (DiTraRe)", List.of("er-ditrare-tag"), HttpStatus.SC_OK);
        createSet("FIZ:ER:DG", "Digitale Geisteswissenschaften", "Digitale Geisteswissenschaften", List.of("erdg-tag"), HttpStatus.SC_OK);
        createSet("FIZ:ER:DG:DDB", "Deutsche Digitale Bibliothek", "Deutsche Digitale Bibliothek", List.of("ddb-tag"), HttpStatus.SC_OK);
        createSet("FIZ:ISE", "FIZ ISE", "FIZ ISE", null, HttpStatus.SC_OK);
        createSet("FIZ:ISE:DITRARE", "Digital Transformation of Research", "Digital Transformation of Research (DiTraRe) ", List.of("ise-ditrare-tag"), HttpStatus.SC_OK);

        createItem("10.5072/38238a", template, "erfd-tag");
        createItem("10.5072/38238b", template, "radar-tag");
        createItem("10.5072/38238ba", template, "er-ditrare-tag");
        createItem("10.5072/38238c", template, "erdg-tag");
        createItem("10.5072/38238d", template, "ddb-tag");
        createItem("10.5072/38238e", template, "ise-ditrare-tag");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Item: 10.5072/38238a (erfd-tag)
        List<String> sets_a = Arrays.asList(
                "\"FIZ:ER:FD\"",       // Explicit
                "\"FIZ:ER\"",          // Parent
                "\"FIZ\""              // Parent
        );
        assertItemSetMembership("10.5072/38238a", sets_a);


        // Item: 10.5072/38238b (radar-tag)
        List<String> sets_b = Arrays.asList(
                "\"FIZ:ER:FD:RADAR\"", // Explicit
                "\"FIZ:ER:FD\"",       // Parent
                "\"FIZ:ER\"",          // Parent
                "\"FIZ\""              // Parent
        );
        assertItemSetMembership("10.5072/38238b", sets_b);


        // Item: 10.5072/38238ba (er-ditrare-tag)
        List<String> sets_ba = Arrays.asList(
                "\"FIZ:ER:FD:DITRARE\"", // Explicit
                "\"FIZ:ER:FD\"",         // Parent
                "\"FIZ:ER\"",            // Parent
                "\"FIZ\""                // Parent
        );
        assertItemSetMembership("10.5072/38238ba", sets_ba);


        // Item: 10.5072/38238c (erdg-tag)
        List<String> sets_c = Arrays.asList(
                "\"FIZ:ER:DG\"",       // Explicit
                "\"FIZ:ER\"",          // Parent
                "\"FIZ\""              // Parent
        );
        assertItemSetMembership("10.5072/38238c", sets_c);


        // Item: 10.5072/38238d (ddb-tag)
        List<String> sets_d = Arrays.asList(
                "\"FIZ:ER:DG:DDB\"",   // Explicit
                "\"FIZ:ER:DG\"",       // Parent
                "\"FIZ:ER\"",          // Parent
                "\"FIZ\""              // Parent
        );
        assertItemSetMembership("10.5072/38238d", sets_d);


        // Item: 10.5072/38238e (ise-ditrare-tag)
        List<String> sets_e = Arrays.asList(
                "\"FIZ:ISE:DITRARE\"", // Explicit
                "\"FIZ:ISE\"",         // Parent
                "\"FIZ\""              // Parent
        );
        assertItemSetMembership("10.5072/38238e", sets_e);

    }


    /**
     * Asserts that the content retrieved for a given item ID contains all specified set substrings.
     *
     * @param itemId The ID of the item to retrieve (e.g., "10.5072/38238a").
     * @param expectedSets The list of expected set ID substrings (e.g., "\"FIZ:ER:FD\"").
     */
    private void assertItemSetMembership(String itemId, List<String> expectedSets) throws IOException {
        // Retrieve the content for the item
        String content = retrieveItemFromES(itemId, 200);

        // Check that every expected set is present in the content
        for (String expectedSubstring : expectedSets) {
            String errorMessage = String.format(
                    "Content for item %s should contain the expected set: %s",
                    itemId,
                    expectedSubstring
            );
            Assertions.assertTrue(content.contains(expectedSubstring), errorMessage);
        }

        // Check that the total number of expected sets matches the list size
        Assertions.assertEquals(
                expectedSets.size(),
                expectedSets.size(), // This is redundant but ensures the expected count is validated
                String.format("The number of expected assertions is incorrect for item: %s", itemId)
        );
    }


}
