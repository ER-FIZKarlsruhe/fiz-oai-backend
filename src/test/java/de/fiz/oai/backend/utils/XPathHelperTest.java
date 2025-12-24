/*
 * Copyright 2019 FIZ Karlsruhe - Leibniz-Institut fuer Informationsinfrastruktur GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package de.fiz.oai.backend.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.xml.xpath.XPathExpressionException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.*;

public class XPathHelperTest {

  private static Logger LOGGER = LoggerFactory.getLogger(XPathHelperTest.class);

  @Test
  public void testEmptyParams() throws XPathExpressionException, SAXException {
    assertFalse(XPathHelper.isTextValueMatching(null, null));
    assertFalse(XPathHelper.isTextValueMatching(null, ""));
    assertFalse(XPathHelper.isTextValueMatching("", null));
    assertFalse(XPathHelper.isTextValueMatching("", ""));
    assertFalse(XPathHelper.isTextValueMatching(null, "a"));
    assertFalse(XPathHelper.isTextValueMatching("a", null));
    assertFalse(XPathHelper.isTextValueMatching("", "a"));
    assertFalse(XPathHelper.isTextValueMatching("a", ""));
  }

  @Test
  public void testWrong() throws XPathExpressionException, SAXException, IOException {
    String content;
    try (var is = XPathHelperTest.class.getResourceAsStream("/10.1007-BF01616320.xml")) {
      if (is == null) {
        throw new IllegalStateException("Test resource not found");
      }
      content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
    assertFalse(XPathHelper.isTextValueMatching(content,
            "/article/front/article-meta/contrib-group/contrib/name[surname='Giulio']"));
    assertFalse(
            XPathHelper.isTextValueMatching(content, "/article/front/article-meta/contrib-group/contrib/name/nickname"));
  }

  @Test
  public void testOK() throws XPathExpressionException, SAXException, IOException {
    String content;
    try (var is = XPathHelperTest.class.getResourceAsStream("/10.1007-BF01616320.xml")) {
      if (is == null) {
        throw new IllegalStateException("Test resource not found");
      }
      content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    LOGGER.info("content {}", content);
    assertNotNull(content);
    assertTrue(XPathHelper.isTextValueMatching(content, "/article/front/article-meta/contrib-group/contrib/name[normalize-space(surname)='Blume']"));

    assertTrue(XPathHelper.isTextValueMatching(content,"/article/front/article-meta/contrib-group/contrib/name/surname"));
  }


  @Test
  public void testOaiDc() throws XPathExpressionException, SAXException {
    String contentStr = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<qualifieddc xsi:noNamespaceSchemaLocation=\"http://dublincore.org/schemas/xmls/qdc/2008/02/11/qualifieddc.xsd\" " +
            "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
            "xmlns:dcterms=\"http://purl.org/dc/terms/\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\">"
            + "<dc:title>Hemophilia in focus</dc:title><dc:creator>Scharf, Rüdiger E.</dc:creator><dc:subject>Editorial</dc:subject>"
            + "<dc:publisher>Schattauer GmbH</dc:publisher><dc:date>2017-02</dc:date>"
            + "<dc:type>magazine</dc:type>"
            + "<dc:format>xml</dc:format><dc:format>pdf</dc:format><dcterms:accessRights>no</dcterms:accessRights>"
            + "<dc:identifier>10.1055/s-0037-1619832</dc:identifier><dcterms:medium>Hämostaseologie</dcterms:medium>"
            + "<dc:source>Hämostaseologie 2017; 37(02): 93-95</dc:source>"
            + "<dc:relation>http://www.thieme-connect.de/DOI/DOI?10.1055/s-0037-1619832</dc:relation>"
            + "<dc:rights>Schattauer GmbH </dc:rights></qualifieddc>";

    assertTrue(XPathHelper.isTextValueMatching(contentStr, "qualifieddc[dc:type='magazine']"));
    assertFalse(XPathHelper.isTextValueMatching(contentStr, "qualifieddc[dc:type='scientific']"));
  }

  @Test
  public void testMarc() throws XPathExpressionException, SAXException {
    String contentStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<collection xmlns=\"http://www.loc.gov/MARC21/slim\"\n" +
            "            xmlns:marc=\"http://www.loc.gov/MARC21/slim\"\n" +
            "            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "            xsi:schemaLocation=\"http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd\">\n" +
            "   <record>\n" +
            "      <leader>00966nam a2200265n  4500</leader>\n" +
            "      <controlfield tag=\"001\">1545122562729</controlfield>\n" +
            "   </record>\n" +
            "</collection>\n";

    assertTrue(XPathHelper.isTextValueMatching(contentStr, "collection/record[contains(leader, 'nam')]"));
    assertFalse(XPathHelper.isTextValueMatching(contentStr, "collection/record[contains(leader, 'naa')]"));
  }

}
