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

import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class XsltHelper {

  /** TransformerFactory for XSLT transforming. */
  private static SAXTransformerFactory saxTransformerFactory;

  public static String transform(StringReader xml, StringReader xslt) throws IOException {
    try {
      final SAXSource xsltSource = new SAXSource(new InputSource(xslt));

      final Transformer transformer = getTransformerFactory().newTransformer(xsltSource);
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");

      final StreamResult result = new StreamResult(new StringWriter());
      final StreamSource source = new StreamSource(xml);
      // do the transformation
      transformer.transform(source, result);

      String resultString = result.getWriter().toString();
      return resultString;
    } catch (Exception e) {
      throw new IOException(e.getMessage());
    }
  }


  /**
   * Create transformerFactory as singleton.
   *
   * @return TransformerFactory
   * @throws TransformerFactoryConfigurationError
   * @throws TransformerConfigurationException
   */
  private static SAXTransformerFactory getTransformerFactory() throws TransformerFactoryConfigurationError, TransformerConfigurationException {
    if (saxTransformerFactory == null) {
      TransformerFactory tf = TransformerFactory.newInstance("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl", null);
      if (tf.getFeature(SAXTransformerFactory.FEATURE)) {
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "all");
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "all");
        tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        saxTransformerFactory = (SAXTransformerFactory) tf;
      } else {
        throw new RuntimeException("Couldn't instantiate a SAXTransformerFactory.");
      }
    }
    return saxTransformerFactory;
  }

}
