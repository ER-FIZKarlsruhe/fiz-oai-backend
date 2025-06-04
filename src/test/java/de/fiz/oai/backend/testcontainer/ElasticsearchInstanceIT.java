package de.fiz.oai.backend.testcontainer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


public class ElasticsearchInstanceIT extends BaseInstance {


    @Test
    public void testReindexAll() throws IOException {
        String template = new String(Files.readAllBytes(Paths.get("src/test/resources/radar-md-template.xml")));

        createFormat("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        createFormat("radar", "https://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/", "http://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/");
        createFormat("datacite", "https://schema.datacite.org/meta/kernel-4.0/metadata.xsd", "http://datacite.org/schema/kernel-4");

        createCrosswalk("Radar2datacite", "radar", "datacite", "src/test/resources/RadarMD-v9.1-to-DataciteMD-v4_4.xslt");
        createCrosswalk("Radar2OAI_DC_v09", "radar", "oai_dc", "src/test/resources/Radar2OAI_DC_v9.1.xsl");

        createSet("testset", "testset", "this is a testset", List.of( "testtag"));

        createItem("10.5072/38238", template);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        checkItemsInIndex(1);
        String result = searchItems("radar", true, null);
        Assertions.assertTrue(result.contains("\"searchMark\":null"));
        Assertions.assertTrue(result.contains("\"total\":1,\"size\":1"));

        reindexElasticsearch("items2");
        reindexElasticsearch("items3");

        for(int i = 0; i <= 1000; i++) {
            createItem("10.5072/38238_" + i, template);
        }

        //Wait a bit, that ES has all documents in the index
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        checkItemsInIndex(1002);

        int countSearchWithMarks = 1;
        result = searchItems("oai_dc" , false, null);
        SearchResult<Item> itemResult = convertStringToSearchResult(result);
        String searchMark = itemResult.getSearchMark();
        Assertions.assertTrue(searchMark != null);
        System.out.println("searchMark " + searchMark);

        while (searchMark != null) {
            countSearchWithMarks++;
            result = searchItems("oai_dc" , false, searchMark);
            itemResult = convertStringToSearchResult(result);
            searchMark = itemResult.getSearchMark();
            System.out.println("searchMark " + searchMark);
        }

        Assertions.assertEquals(11,countSearchWithMarks);
    }


    @Test
    public void testReindexItem() throws IOException {
        String template = new String(Files.readAllBytes(Paths.get("src/test/resources/radar-md-template.xml")));

        createFormat("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        createFormat("radar", "https://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/", "http://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/");
        createFormat("datacite", "https://schema.datacite.org/meta/kernel-4.0/metadata.xsd", "http://datacite.org/schema/kernel-4");

        createCrosswalk("Radar2datacite", "radar", "datacite", "src/test/resources/RadarMD-v9.1-to-DataciteMD-v4_4.xslt");
        createCrosswalk("Radar2OAI_DC_v09", "radar", "oai_dc", "src/test/resources/Radar2OAI_DC_v9.1.xsl");

        createSet("testset", "testset", "this is a testset", List.of( "testtag"));

        createItem("10.5072/38238", template);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        checkItemsInIndex(1);
        String result = searchItems("radar", true, null);
        Assertions.assertTrue(result.contains("\"searchMark\":null"));
        Assertions.assertTrue(result.contains("\"total\":1,\"size\":1"));

        //reindex item okay
        reindexItem("10.5072/38238", HttpStatus.SC_NO_CONTENT);

        //reindex item not found
        reindexItem("10.5072/3823fgdf8", HttpStatus.SC_NOT_FOUND);
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
        createFormat("test_format", "http://abc.de/", "http://abc.de/test_format");
        updateFormat("test_format", "http://adc.de/new", "http://adc.de/test_format");
        deleteFormat("test_format");
    }

    @Test
    public void testCrudCrosswalks() throws IOException {
        createFormat("test_format1", "http://abc.de/", "http://abc.de/test_format");
        createFormat("test_format2", "http://abc.de/", "http://abc.de/test_format");
        createCrosswalk("format1ToFormat2", "test_format1", "test_format2", "src/test/resources/RadarMD-v9.1-to-DataciteMD-v4_4.xslt");
        updateCrosswalk("format1ToFormat2", "test_format1", "test_format2", "src/test/resources/Radar2OAI_DC_v9.1.xsl");
        deleteCrosswalk("format1ToFormat2");
    }

    @Test
    public void testCrudSets() throws IOException {
        createSet("testset1", "testset1", "this is a testset1", List.of( "testtag"));
        updateSet("testset1", "testset1chenged", "Changed testset1", List.of( "testtag"));
        deleteSet("testset1");
    }

}
