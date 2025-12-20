/*
 * Copyright 2025 FIZ Karlsruhe - Leibniz-Institut fuer Informationsinfrastruktur GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

}
