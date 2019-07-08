package de.fiz.oai.backend.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class OaiDcHelperTest {

  @Test
  public void xmlToJson() {
    String TEST_XML_STRING = "<oai_dc:dc\n" + 
        "         xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" + 
        "         xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" + 
        "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
        "         xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/\n" + 
        "                             http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" + 
        "            <dc:title>Studies of Unicorn Behaviour</dc:title>\n" + 
        "            <dc:identifier>http://repository.example.org/2003292</dc:identifier>\n" + 
        "            <dc:creator>Jane, Doe</dc:creator>\n" + 
        "            <dc:creator>John, Doe</dc:creator>\n" + 
        "            <dc:description>\n" + 
        "                Lorem ipsum dolor...\n" + 
        "            </dc:description>\n" + 
        "            <dc:subject>info:eu-repo/classification/ddc/590</dc:subject>\n" + 
        "            <dc:subject>Unicorns</dc:subject>\n" + 
        "            <dc:relation>info:eu-repo/grantAgreement/EC/FP7/1234556789/EU//UNICORN</dc:relation>\n" + 
        "            <dc:relation>info:eu-repo/semantics/altIdentifier/eissn/1234-5678</dc:relation>\n" + 
        "            <dc:relation>info:eu-repo/semantics/altIdentifier/pmid/123456789</dc:relation>\n" + 
        "            <dc:relation>info:eu-repo/semantics/altIdentifier/doi/10.1000/182</dc:relation>\n" + 
        "            <dc:relation>info:eu-repo/semantics/reference/doi/10.1234/789.1</dc:relation>\n" + 
        "            <dc:relation>info:eu-repo/semantics/dataset/doi/10.1234/789.1</dc:relation>\n" + 
        "            <dc:rights>info:eu-repo/semantics/openAccess</dc:rights>\n" + 
        "            <dc:rights>http://creativecommons.org/licenses/by-sa/2.0/uk/</dc:rights>\n" + 
        "            <dc:source>Journal Of Unicorn Research</dc:source>\n" + 
        "            <dc:publisher>Unicorn Press</dc:publisher>\n" + 
        "            <dc:date>2013</dc:date>\n" + 
        "            <dc:type>info:eu-repo/semantics/article</dc:type>\n" + 
        "        </oai_dc:dc>";

    
    String expectedJson = "{\"title\":[{\"value\":\"Studies of Unicorn Behaviour\",\"lang\":null}],\"creator\":[{\"value\":\"Jane, Doe\",\"lang\":null},{\"value\":\"John, Doe\",\"lang\":null}],\"subject\":[{\"value\":\"info:eu-repo/classification/ddc/590\",\"lang\":null},{\"value\":\"Unicorns\",\"lang\":null}],\"description\":[{\"value\":\"\\n                Lorem ipsum dolor...\\n            \",\"lang\":null}],\"publisher\":[{\"value\":\"Unicorn Press\",\"lang\":null}],\"contributor\":[],\"date\":[{\"value\":\"2013\",\"lang\":null}],\"type\":[{\"value\":\"info:eu-repo/semantics/article\",\"lang\":null}],\"format\":[],\"identifier\":[{\"value\":\"http://repository.example.org/2003292\",\"lang\":null}],\"source\":[{\"value\":\"Journal Of Unicorn Research\",\"lang\":null}],\"language\":[],\"relation\":[{\"value\":\"info:eu-repo/grantAgreement/EC/FP7/1234556789/EU//UNICORN\",\"lang\":null},{\"value\":\"info:eu-repo/semantics/altIdentifier/eissn/1234-5678\",\"lang\":null},{\"value\":\"info:eu-repo/semantics/altIdentifier/pmid/123456789\",\"lang\":null},{\"value\":\"info:eu-repo/semantics/altIdentifier/doi/10.1000/182\",\"lang\":null},{\"value\":\"info:eu-repo/semantics/reference/doi/10.1234/789.1\",\"lang\":null},{\"value\":\"info:eu-repo/semantics/dataset/doi/10.1234/789.1\",\"lang\":null}],\"coverage\":[],\"rights\":[{\"value\":\"info:eu-repo/semantics/openAccess\",\"lang\":null},{\"value\":\"http://creativecommons.org/licenses/by-sa/2.0/uk/\",\"lang\":null}]}";
    
    String json = OaiDcHelper.xmlToJson(TEST_XML_STRING);
    
    System.out.println("oai_dc_json: " + json);
    
    assertEquals(expectedJson, json);
  }
}
