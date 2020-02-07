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

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
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

  public static Boolean isTextValueMatching(final String contentStr, final String xPathStr) {

    class NamespaceResolver implements NamespaceContext {
      //Store the source document to search the namespaces
      private Document sourceDocument;

      public NamespaceResolver(Document document) {
        sourceDocument = document;
      }

      //The lookup for the namespace uris is delegated to the stored document.
      public String getNamespaceURI(String prefix) {
        if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
          return sourceDocument.lookupNamespaceURI(null);
        } else {
          return sourceDocument.lookupNamespaceURI(prefix);
        }
      }

      public String getPrefix(String namespaceURI) {
        return sourceDocument.lookupPrefix(namespaceURI);
      }

      @SuppressWarnings("rawtypes")
      public Iterator getPrefixes(String namespaceURI) {
        return null;
      }
    }

    if (!StringUtils.isBlank(contentStr) && !StringUtils.isBlank(xPathStr)) {
      try {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        if (xPathStr.contains(":")) {
          factory.setNamespaceAware(true);
        }
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(contentStr)));

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();
        xpath.setNamespaceContext(new NamespaceResolver(doc));
        XPathExpression expr = xpath.compile(xPathStr);
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() > 0) {
          return true;
        }

      } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

}
