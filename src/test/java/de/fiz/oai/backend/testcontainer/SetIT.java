package de.fiz.oai.backend.testcontainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.List;

public class SetIT extends BaseInstance {

    @Test
    public void testCrudSets() throws IOException {
        createSet("testset1", "testset1", "this is a testset1",
                List.of("testtag"), HttpStatus.SC_OK);

        updateSet("testset1", "testset1chenged",
                "Changed testset1", List.of("testtag"));

        deleteSet("testset1");

        // BAD request, missing parent nodes
        createSet("A:B:C", "A:B:C",
                "this is a test hierarchy", List.of("testtag"),
                HttpStatus.SC_BAD_REQUEST);

        // Create root node
        createSet("A", "A", "this is a root set",
                List.of("testtag"), HttpStatus.SC_OK);

        // BAD request, missing parent
        createSet("A:B:C", "A:B:C",
                "this is a test hierarchy", List.of("testtag"),
                HttpStatus.SC_BAD_REQUEST);

        // Create parent node
        createSet("A:B", "A:B",
                "this is a parent set", List.of("testtag"),
                HttpStatus.SC_OK);

        // Finally OK
        createSet("A:B:C", "C",
                "this is a test hierarchy", List.of("testtag"),
                HttpStatus.SC_OK);
    }

    @Test
    public void testSetSearchWithResumptionToken() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String baseUrl = "http://" + tomcatContainer.getHost() + ":" +
                tomcatContainer.getMappedPort(8080) +
                "/oai-backend/set/";

        // -----------------------------------------
        // 1. CREATE 2000 SETS
        // -----------------------------------------
        for (int i = 1; i <= 2000; i++) {
            createSet(
                    "spec_" + i,
                    "set" + i,
                    "description_" + i,
                    List.of("tag"),
                    HttpStatus.SC_OK
            );
        }

        // -----------------------------------------
        // 2. GET ALL SETS FROM /set
        // -----------------------------------------
        List<String> expectedFullSetNames = new ArrayList<>();

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse resp =
                     client.execute(new HttpGet(baseUrl))) {

            Assertions.assertEquals(
                    HttpStatus.SC_OK,
                    resp.getStatusLine().getStatusCode()
            );

            JsonNode root = mapper.readTree(
                    EntityUtils.toString(resp.getEntity())
            );

            Assertions.assertTrue(root.isArray());

            for (JsonNode node : root) {
                expectedFullSetNames.add(
                        node.get("spec").asText() + ":" +
                                node.get("name").asText()
                );
            }

            Assertions.assertEquals(2000, expectedFullSetNames.size());
        }

        // -----------------------------------------
        // 3. SEARCH WITH RESUMPTION TOKENS
        // -----------------------------------------
        String searchUrlBase = "http://" + tomcatContainer.getHost() + ":" +
                tomcatContainer.getMappedPort(8080) +
                "/oai-backend/set/search";

        List<String> foundSetNames = new ArrayList<>();
        String token = null;

        do {
            String url = token == null
                    ? searchUrlBase
                    : searchUrlBase + "?resumptionToken=" + token;

            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse resp =
                         client.execute(new HttpGet(url))) {

                Assertions.assertEquals(
                        HttpStatus.SC_OK,
                        resp.getStatusLine().getStatusCode()
                );

                JsonNode root = mapper.readTree(
                        EntityUtils.toString(resp.getEntity())
                );

                JsonNode sets = root.get("sets");
                Assertions.assertNotNull(sets);
                Assertions.assertTrue(sets.isArray());

                for (JsonNode s : sets) {
                    foundSetNames.add(
                            s.get("spec").asText() + ":" +
                                    s.get("name").asText()
                    );
                }

                JsonNode tokenNode = root.get("resumptionToken");
                token = tokenNode != null && !tokenNode.isNull()
                        ? tokenNode.asText()
                        : null;
            }
        } while (token != null);

        // -----------------------------------------
        // 4. COMPARE RESULTS
        // -----------------------------------------
        Assertions.assertEquals(
                expectedFullSetNames.size(),
                foundSetNames.size()
        );

        Assertions.assertTrue(
                foundSetNames.containsAll(expectedFullSetNames)
                        && expectedFullSetNames.containsAll(foundSetNames)
        );
    }

    @Test
    public void testSetHierarchy() throws IOException, InterruptedException {
        String template = Files.readString(
                Paths.get("src/test/resources/radar-md-template.xml")
        );

        createFormatIfNotExisting("oai_dc",
                "http://www.openarchives.org/OAI/2.0/oai_dc.xsd",
                "http://www.openarchives.org/OAI/2.0/oai_dc/");

        createFormatIfNotExisting("radar",
                "https://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/",
                "http://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/");

        createFormatIfNotExisting("datacite",
                "https://schema.datacite.org/meta/kernel-4.0/metadata.xsd",
                "http://datacite.org/schema/kernel-4");

        createCrosswalkIfNotExisting("Radar2datacite",
                "radar", "datacite",
                "src/test/resources/RadarMD-v9.1-to-DataciteMD-v4_4.xslt");

        createCrosswalkIfNotExisting("Radar2OAI_DC_v09",
                "radar", "oai_dc",
                "src/test/resources/Radar2OAI_DC_v9.1.xsl");

        // Sets
        createSet("FIZ", "FIZ", "FIZ", null, HttpStatus.SC_OK);
        createSet("FIZ:ER", "FIZ ER", "FIZ ER", null, HttpStatus.SC_OK);
        createSet("FIZ:ER:FD", "Forschungsdaten",
                "Forschungsdaten", List.of("erfd-tag"),
                HttpStatus.SC_OK);

        // Items
        createItem("10.5072/38238a", template, "erfd-tag");

        Thread.sleep(1000);

        assertItemSetMembership(
                "10.5072/38238a",
                List.of("\"FIZ:ER:FD\"", "\"FIZ:ER\"", "\"FIZ\"")
        );
    }

    private void assertItemSetMembership(
            String itemId,
            List<String> expectedSets
    ) throws IOException {

        String content = retrieveItemFromES(itemId, 200);

        for (String expected : expectedSets) {
            Assertions.assertTrue(
                    content.contains(expected),
                    "Missing expected set " + expected
            );
        }
    }
}
