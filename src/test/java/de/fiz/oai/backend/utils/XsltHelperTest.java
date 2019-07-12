package de.fiz.oai.backend.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;

import junit.framework.Assert;

public class XsltHelperTest {

  @Test
  public void transformationTest() {
    
    String xml = "<persons>\n" + 
        "  <person username=\"JS1\">\n" + 
        "    <name>John</name>\n" + 
        "    <family-name>Smith</family-name>\n" + 
        "  </person>\n" + 
        "  <person username=\"MI1\">\n" + 
        "    <name>Morka</name>\n" + 
        "    <family-name>Ismincius</family-name>\n" + 
        "  </person>\n" + 
        "</persons>";
    
    String xslt = "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">\n" + 
        "  <xsl:output method=\"xml\" indent=\"yes\"/>\n" + 
        "\n" + 
        "  <xsl:template match=\"/persons\">\n" + 
        "    <root>\n" + 
        "      <xsl:apply-templates select=\"person\"/>\n" + 
        "    </root>\n" + 
        "  </xsl:template>\n" + 
        "\n" + 
        "  <xsl:template match=\"person\">\n" + 
        "    <name username=\"{@username}\">\n" + 
        "      <xsl:value-of select=\"name\" />\n" + 
        "    </name>\n" + 
        "  </xsl:template>\n" + 
        "\n" + 
        "</xsl:stylesheet>";
    
    String expectedResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>\r\n" + 
        "  <name username=\"JS1\">John</name>\r\n" + 
        "  <name username=\"MI1\">Morka</name>\r\n" + 
        "</root>\r\n";
    
    
    try {
      String newXml = XsltHelper.transform(new ByteArrayInputStream(xml.getBytes()), new ByteArrayInputStream(xslt.getBytes()));
      System.out.println("newXml " + newXml);
      assertTrue(newXml.contains("John"));
      assertTrue(newXml.contains("Morka"));
    } catch (IOException e) {
      Assert.fail();
    }
  }
}
