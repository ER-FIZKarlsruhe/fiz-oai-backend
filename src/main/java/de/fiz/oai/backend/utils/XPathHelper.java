package de.fiz.oai.backend.utils;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XPathHelper {

  private static String textValueXPath = "text()";

  public static Boolean isTextValueMatching(final String contentStr, final String xPathStr, final String valueStr) {

    if (!StringUtils.isBlank(contentStr) && !StringUtils.isBlank(xPathStr) && !StringUtils.isBlank(valueStr)) {
      try {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;
        builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(contentStr)));

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

        StringBuilder finalXPathStr = new StringBuilder();
        finalXPathStr.append(xPathStr);
        if (!xPathStr.endsWith(textValueXPath)) {
          if (!xPathStr.endsWith("/")) {
            finalXPathStr.append("/");
          }
          finalXPathStr.append(textValueXPath);
        }

        XPathExpression expr = xpath.compile(finalXPathStr.toString());
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        for (int i = 0; i < nodes.getLength(); i++) {
          if (valueStr.equals(nodes.item(i).getNodeValue())) {
            return true;
          }
        }

      } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
        e.printStackTrace();
      }
    }
    return false;
  }
}
