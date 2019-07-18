package de.fiz.oai.backend.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class XsltHelper {

  public static String transform(InputStream xml, InputStream xslt) throws IOException {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

      final DocumentBuilder db = dbf.newDocumentBuilder();

      db.setEntityResolver(new EntityResolver() {

        @Override
        public InputSource resolveEntity(String publicId, String systemId) {
          return new InputSource(new StringReader(""));
        }
      });
      final Document doc = db.parse(xml);
      final Source xsltSource = new StreamSource(xslt);

      TransformerFactory factory = TransformerFactory.newInstance("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl", null);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "all");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "all");

      final Transformer transformer = factory.newTransformer(xsltSource);
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

      final StreamResult result = new StreamResult(new StringWriter());
      final Source source = new DOMSource(doc);
      transformer.transform(source, result);
      String resultString = result.getWriter().toString();

      return resultString;
    } catch (Exception e) {
      throw new IOException(e.getMessage());
    }
  }
}