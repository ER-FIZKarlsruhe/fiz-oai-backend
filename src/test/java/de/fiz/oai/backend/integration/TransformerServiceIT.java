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
package de.fiz.oai.backend.integration;

import de.fiz.oai.backend.dao.DAOCrosswalk;
import de.fiz.oai.backend.models.Crosswalk;
import de.fiz.oai.backend.service.impl.TransformerServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class TransformerServiceIT {

    @Mock
    private DAOCrosswalk daoCrosswalk;

    @InjectMocks
    private TransformerServiceImpl transformerService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testXslt30Transformation() throws Exception {
        String crosswalkName = "xslt3test";

        // A simple XSLT 3.0 stylesheet using the 'xsl:map' feature
        // This would fail on XSLT 1.0 or 2.0 processors
        String xslt30Stylesheet =
                "<xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>" +
                        "  <xsl:template match='/'>" +
                        "    <output>" +
                        "      <xsl:variable name='myMap' as='map(xs:string, xs:string)' " +
                        "                    xmlns:map='http://www.w3.org/2005/xpath-functions/map' " +
                        "                    xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                        "        <xsl:map>" +
                        "          <xsl:map-entry key=\"'testKey'\" select=\"'Hello XSLT 3.0'\"/>" + // Changed 'value' to 'select'
                        "        </xsl:map>" +
                        "      </xsl:variable>" +
                        "      <xsl:value-of select=\"$myMap('testKey')\"/>" +
                        "    </output>" +
                        "  </xsl:template>" +
                        "</xsl:stylesheet>";

        Crosswalk crosswalk = new Crosswalk();
        crosswalk.setName(crosswalkName);
        crosswalk.setXsltStylesheet(xslt30Stylesheet);

        // Configure mock to return our XSLT 3.0 stylesheet
        when(daoCrosswalk.read(crosswalkName)).thenReturn(crosswalk);

        // Input XML (can be simple as we aren't processing the input nodes)
        String inputXml = "<root/>";

        // Execute transformation
        String result = transformerService.transform(inputXml, crosswalkName);

        // Verify result
        System.out.println("Transformation Result: " + result);
        assertTrue("Result should contain the value from the XSLT 3.0 map",
                result.contains("Hello XSLT 3.0"));
    }
}
