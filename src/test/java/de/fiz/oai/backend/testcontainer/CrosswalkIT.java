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

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


public class CrosswalkIT extends BaseInstance {

    @Test
    public void testCrudCrosswalks() throws IOException {
        createFormatIfNotExisting("test_format1", "http://abc.de/", "http://abc.de/test_format");
        createFormatIfNotExisting("test_format2", "http://abc.de/", "http://abc.de/test_format");
        createCrosswalkIfNotExisting("format1ToFormat2", "test_format1", "test_format2", "src/test/resources/RadarMD-v9.1-to-DataciteMD-v4_4.xslt");
        updateCrosswalk("format1ToFormat2", "test_format1", "test_format2", "src/test/resources/Radar2OAI_DC_v9.1.xsl");
        deleteCrosswalk("format1ToFormat2");
    }

    @Test
    public void testProcessCrosswalk() throws IOException, InterruptedException {
        String template = new String(Files.readAllBytes(
                Paths.get("src/test/resources/radar-md-template.xml")));

        createFormatIfNotExisting("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        createFormatIfNotExisting("radar", "https://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/", "http://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/");
        createFormatIfNotExisting("datacite", "https://schema.datacite.org/meta/kernel-4.0/metadata.xsd", "http://datacite.org/schema/kernel-4");

        createCrosswalkIfNotExisting("Radar2datacite", "radar", "datacite", "src/test/resources/RadarMD-v9.1-to-DataciteMD-v4_4.xslt");
        createCrosswalkIfNotExisting("Radar2OAI_DC_v09", "radar", "oai_dc", "src/test/resources/Radar2OAI_DC_v9.1.xsl");

        createSet("testset", "testset", "this is a testset",
                List.of("testtag"), HttpStatus.SC_OK);

        createItem("10.5072/38238", template, "testtag");

        Thread.sleep(1000);

        for (int i = 0; i <= 1000; i++) {
            createItem("10.5072/38238_" + i, template, "testtag");
        }

        // --- start async processing ---
        processCrosswalk("Radar2OAI_DC_v09", 200);

        boolean finished = false;
        boolean progressSeen = false;
        int maxWaitSeconds = 60;

        for (int i = 0; i < maxWaitSeconds; i++) {
            Thread.sleep(1000);

            String status = getCrosswalkStatus("Radar2OAI_DC_v09");
            System.out.println(status);

            // basic sanity checks
            Assertions.assertTrue(status.contains("Crosswalk"),
                    "Status should contain crosswalk info");

            if (status.contains("Progress:")) {
                progressSeen = true;
            }

            if (status.contains("FINISHED")) {
                finished = true;
                break;
            }
        }

        Assertions.assertTrue(progressSeen,
                "Progress endpoint never reported progress");

        Assertions.assertTrue(finished,
                "Crosswalk processing did not finish within timeout");
    }


    @Test
    public void testProcessCrosswalkCannotRunTwiceButCanRestart()
            throws IOException, InterruptedException {
        String template = new String(Files.readAllBytes(
                Paths.get("src/test/resources/radar-md-template.xml")));

        createFormatIfNotExisting("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        createFormatIfNotExisting("radar", "https://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/", "http://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset/");
        createFormatIfNotExisting("datacite", "https://schema.datacite.org/meta/kernel-4.0/metadata.xsd", "http://datacite.org/schema/kernel-4");

        createCrosswalkIfNotExisting("Radar2datacite", "radar", "datacite", "src/test/resources/RadarMD-v9.1-to-DataciteMD-v4_4.xslt");
        createCrosswalkIfNotExisting("Radar2OAI_DC_v09", "radar", "oai_dc", "src/test/resources/Radar2OAI_DC_v9.1.xsl");

        createSet("testset", "testset", "this is a testset",
                List.of("testtag"), HttpStatus.SC_OK);

        createItem("10.5072/38238", template, "testtag");

        Thread.sleep(1000);

        for (int i = 0; i < 1000; i++) {
            createItem("10.5072/38238_" + i, template, "testtag");
        }

        // -------------------------------------------------------
        // 1) Start processing (should succeed)
        // -------------------------------------------------------
        boolean startedFirst = processCrosswalk("Radar2OAI_DC_v09", 200);
        Assertions.assertTrue(startedFirst,
                "First process call should start successfully");

        // Give async job a moment to actually start
        Thread.sleep(500);

        // -------------------------------------------------------
        // 2) Try to start again while still running (must fail)
        // -------------------------------------------------------
        boolean startedSecond = processCrosswalk("Radar2OAI_DC_v09", HttpStatus.SC_CONFLICT);
        Assertions.assertFalse(startedSecond);

        // -------------------------------------------------------
        // 3) Wait until processing finishes
        // -------------------------------------------------------
        boolean finished = false;
        int maxWaitSeconds = 60;

        for (int i = 0; i < maxWaitSeconds; i++) {
            Thread.sleep(1000);

            String status = getCrosswalkStatus("Radar2OAI_DC_v09");
            System.out.println(status);

            if (status.contains("FINISHED")) {
                finished = true;
                break;
            }
        }

        Assertions.assertTrue(finished,
                "Crosswalk processing did not finish within timeout");

        // -------------------------------------------------------
        // 4) Start again after finishing (should succeed)
        // -------------------------------------------------------
        boolean startedAfterFinish = processCrosswalk("Radar2OAI_DC_v09", 200);
        Assertions.assertTrue(startedAfterFinish,
                "Process should be restartable after finishing");
    }



}
