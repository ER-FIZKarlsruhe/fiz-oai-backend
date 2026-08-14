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
package de.fiz.oai.backend.service.impl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.inject.Inject;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import jakarta.inject.Singleton;
import org.jvnet.hk2.annotations.Service;

import de.fiz.oai.backend.dao.DAOCrosswalk;
import de.fiz.oai.backend.models.Crosswalk;
import de.fiz.oai.backend.service.TransformerService;
import net.sf.saxon.lib.FeatureKeys;

/** Crosswalk transformations. */
@Service
@Singleton
public class TransformerServiceImpl implements TransformerService {

    /** Logger. */
    private static final Log LOGGER = LogFactory.getLog(TransformerServiceImpl.class);

    @Inject
    DAOCrosswalk daoCrosswalk;

    /** TransformerFactory for XSLT transforming. */
    private SAXTransformerFactory saxTransformerFactory;

    /** Compiled stylesheets per crosswalk name. Compiling is the expensive part; creating a Transformer from an
     *  already-compiled Templates via newTransformer() is cheap, so no pooling of Transformer instances is needed. */
    private final ConcurrentMap<String, Templates> templatesCache = new ConcurrentHashMap<>();

    public TransformerServiceImpl() {
        LOGGER.info("Initialize TransformerFactory for XSLT 3.0 (Saxon 12) ...");

        // Create transformerFactory as singleton using explicit Saxon implementation.
        TransformerFactory tf = new net.sf.saxon.TransformerFactoryImpl();

        try {
            // Harden against XXE: disable both general and parameter external entities on the
            // underlying SAX parser, and forbid external DTD / stylesheet access via the
            // standard JAXP attributes (same pattern used for SchemaFactory/Validator elsewhere).
            tf.setAttribute(
                    FeatureKeys.XML_PARSER_FEATURE + "http://xml.org/sax/features/external-general-entities", false);
            tf.setAttribute(
                    FeatureKeys.XML_PARSER_FEATURE + "http://xml.org/sax/features/external-parameter-entities", false);
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

            if (tf.getFeature(SAXTransformerFactory.FEATURE)) {
                saxTransformerFactory = (SAXTransformerFactory) tf;
            } else {
                LOGGER.error("Couldn't instantiate a SAXTransformerFactory.");
                throw new RuntimeException("Couldn't instantiate a SAXTransformerFactory.");
            }
        } catch (TransformerConfigurationException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

        assert saxTransformerFactory != null;
    }

    @Override
    public String transform(String xml, String name) throws IOException {
        Templates templates = getTemplates(name);

        try (StringReader xmlReader = new StringReader(xml);
             StringWriter writer = new StringWriter()) {

            Transformer transformer = templates.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());

            // Standard JAXP indent property (works in Saxon-HE)
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            final StreamResult result = new StreamResult(writer);
            final StreamSource source = new StreamSource(xmlReader);
            // do the transformation
            transformer.transform(source, result);

            return writer.toString();
        } catch (TransformerException e) {
            throw new IOException(e);
        }
    }

    /** Returns the compiled stylesheet for the given crosswalk, compiling and caching it on first use. */
    private Templates getTemplates(String name) throws IOException {
        try {
            return templatesCache.computeIfAbsent(name, this::compileTemplates);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException(e);
        }
    }

    private Templates compileTemplates(String name) {
        try {
            Crosswalk crosswalk = daoCrosswalk.read(name);
            if (crosswalk == null) {
                throw new RuntimeException("Couldn't find crosswalk for name " + name);
            }

            try (StringReader reader = new StringReader(crosswalk.getXsltStylesheet())) {
                StreamSource xslSource = new StreamSource(reader);
                LOGGER.info("Compiling stylesheet for crosswalk " + name);
                return saxTransformerFactory.newTemplates(xslSource);
            }
        } catch (TransformerConfigurationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** ${@inheritDoc} */
    @Override
    public void updateTransformer(String key) throws Exception {
        // Drop the cached compiled stylesheet so a fresh one is compiled on next use
        this.templatesCache.remove(key);
    }

    @Override
    public String info() {
        StringBuilder buf = new StringBuilder("TransformerService\n")
                .append("cached stylesheets: ").append(templatesCache.size()).append("\n");
        for (String key : templatesCache.keySet()) {
            buf.append("  ").append(key).append("\n");
        }

        return buf.toString();
    }
}
