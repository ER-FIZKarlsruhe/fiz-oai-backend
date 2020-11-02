/*
 * Copyright 2019 FIZ Karlsruhe - Leibniz-Institut fuer Informationsinfrastruktur GmbH
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
package de.fiz.oai.backend.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

public class XPathHelperTest {

  private String content;

  @Before
  public void init() throws IOException {
    String fullFilePath = getClass().getClassLoader().getResource("10.1007-BF01616320.xml").getPath();
    if (fullFilePath.startsWith("/C:")) {
      fullFilePath = fullFilePath.substring(1);
    }

    content = new String(Files.readAllBytes(Paths.get(fullFilePath)), StandardCharsets.UTF_8);
  }

  @Test
  public void testEmptyParams() {
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
  public void testWrong() {
    assertFalse(XPathHelper.isTextValueMatching(content,
        "/article/front/article-meta/contrib-group/contrib/name[surname='Giulio']"));
    assertFalse(
        XPathHelper.isTextValueMatching(content, "/article/front/article-meta/contrib-group/contrib/name/nickname"));


  }

  @Test
  public void testOK() {
    assertTrue(XPathHelper.isTextValueMatching(content,
        "/article/front/article-meta/contrib-group/contrib/name[surname='Blume']"));
    assertTrue(
        XPathHelper.isTextValueMatching(content, "/article/front/article-meta/contrib-group/contrib/name/surname"));


  }

  @Test
  public void testOaiDc() {
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
    String xPathStr = "qualifieddc[dc:type='magazine']";
    assertTrue(XPathHelper.isTextValueMatching(contentStr, xPathStr));
    xPathStr = "qualifieddc[dc:type='scientific']";
    assertFalse(XPathHelper.isTextValueMatching(contentStr, xPathStr));
  }

  @Test
  public void testMarc() {
    String contentStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<collection xmlns=\"http://www.loc.gov/MARC21/slim\"\n" +
            "            xmlns:marc=\"http://www.loc.gov/MARC21/slim\"\n" +
            "            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "            xsi:schemaLocation=\"http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd\">\n" +
            "   <record>\n" +
            "      <leader>00966nam a2200265n  4500</leader>\n" +
            "      <controlfield tag=\"001\">1545122562729</controlfield>\n" +
            "      <controlfield tag=\"003\">Georg Thieme Verlag ;</controlfield>\n" +
            "      <controlfield tag=\"005\">20181218094242.0</controlfield>\n" +
            "      <controlfield tag=\"007\">cr</controlfield>\n" +
            "      <controlfield tag=\"008\">170101s2017||||gw|a||||s|||||00||||ger||</controlfield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"020\">\n" +
            "         <subfield code=\"a\">9783132418165</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\"7\" ind2=\" \" tag=\"024\">\n" +
            "         <subfield code=\"a\">10.1055/b-005-143671</subfield>\n" +
            "         <subfield code=\"2\">doi</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\"4\" tag=\"050\">\n" +
            "         <subfield code=\"a\">RC346</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\"1\" ind2=\" \" tag=\"100\">\n" +
            "         <subfield code=\"a\">Hufschmidt, Andreas</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\"1\" ind2=\"0\" tag=\"245\">\n" +
            "         <subfield code=\"a\">Neurologie compact</subfield>\n" +
            "         <subfield code=\"b\">Für Klinik und Praxis</subfield>\n" +
            "         <subfield code=\"c\">Andreas Hufschmidt, Carl Hermann Lücking, Sebastian Rauer, Franz Xaver Glocker</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"250\">\n" +
            "         <subfield code=\"a\">7., überarbeitete Auflage</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"260\">\n" +
            "         <subfield code=\"a\">Stuttgart :</subfield>\n" +
            "         <subfield code=\"b\">Georg Thieme Verlag ;</subfield>\n" +
            "         <subfield code=\"c\">2017.</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\"1\" ind2=\" \" tag=\"542\">\n" +
            "         <subfield code=\"f\">© 2017 Georg Thieme Verlag KG</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\"1\" ind2=\"4\" tag=\"650\">\n" +
            "         <subfield code=\"a\">Neurologie, Neuropathologie, Klinische Neurowissenschaft</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\"1\" ind2=\" \" tag=\"700\">\n" +
            "         <subfield code=\"a\">Lücking, Carl Hermann</subfield>\n" +
            "         <subfield code=\"c\">Prof. em. Dr. med. Dr. h.c.</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\"1\" ind2=\" \" tag=\"700\">\n" +
            "         <subfield code=\"a\">Rauer, Sebastian</subfield>\n" +
            "         <subfield code=\"c\">Prof. Dr. med.</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\"1\" ind2=\" \" tag=\"700\">\n" +
            "         <subfield code=\"a\">Glocker, Franz Xaver</subfield>\n" +
            "         <subfield code=\"c\">Prof. Dr. med.</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\"0\" ind2=\"8\" tag=\"776\">\n" +
            "         <subfield code=\"i\">Print</subfield>\n" +
            "         <subfield code=\"z\">9783131171979</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\"4\" ind2=\"0\" tag=\"856\">\n" +
            "         <subfield code=\"u\">https://doi.org/10.1055/b-005-143671</subfield>\n" +
            "         <subfield code=\"q\">pdf</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"912\">\n" +
            "         <subfield code=\"a\">ZDB-34-THI</subfield>\n" +
            "      </datafield>\n" +
            "   </record>\n" +
            "</collection>\n";
    String xPathStr = "collection/record[contains(leader, 'nam')]";
    assertTrue(XPathHelper.isTextValueMatching(contentStr, xPathStr));
    xPathStr = "collection/record[contains(leader, 'naa')]";
    assertFalse(XPathHelper.isTextValueMatching(contentStr, xPathStr));
  }

}
