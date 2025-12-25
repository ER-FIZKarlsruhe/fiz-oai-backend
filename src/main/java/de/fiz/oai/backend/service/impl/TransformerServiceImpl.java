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
import java.time.Duration;
import java.util.Map;
import java.util.NoSuchElementException;

import jakarta.inject.Inject;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import jakarta.inject.Singleton;
import org.jvnet.hk2.annotations.Service;

import de.fiz.oai.backend.dao.DAOCrosswalk;
import de.fiz.oai.backend.models.Crosswalk;
import de.fiz.oai.backend.service.TransformerService;
import net.sf.saxon.lib.FeatureKeys;

/** Crosswalk transformations. */
@Service
@Singleton
public class TransformerServiceImpl implements TransformerService, KeyedObjectPool<String, Transformer> {

    /** Logger. */
    private static final Log LOGGER = LogFactory.getLog(TransformerServiceImpl.class);

    /** Maximum amount of transformer objects in the pool. */
    private final static int MAX_ACTIVE = 50;

    /** Maximum time to wait for borrowing an object. */
    private final static long MAX_WAIT = 30000L;

    /** Maximum time to live for a object in pool. */
    private final static long MAX_TIME_TO_LIVE = 1000 * 60 * 30;

    /** Time between eviction runs. */
    private static final long TIME_BETWEEN_EVICTION_RUNS = MAX_TIME_TO_LIVE / 2;

    /** Test for eviction while idle. */
    private static final boolean TEST_WHILE_IDLE = true;

    @Inject
    DAOCrosswalk daoCrosswalk;

    /** Pool for Transformer-Objects. */
    private GenericKeyedObjectPool<String, Transformer> pool;

    /** TransformerFactory for XSLT transforming. */
    private SAXTransformerFactory saxTransformerFactory;

    public TransformerServiceImpl() {
        LOGGER.info("Initialize TransformerPool for XSLT 3.0 (Saxon 12) ...");

        // Create transformerFactory as singleton using explicit Saxon implementation.
        TransformerFactory tf = new net.sf.saxon.TransformerFactoryImpl();

        try {
            tf.setAttribute(
                    FeatureKeys.XML_PARSER_FEATURE + "http://xml.org/sax/features/external-general-entities", false);
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

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

        // Create the pool
        LOGGER.info("Creating new transformerPool ...");
        this.pool = new GenericKeyedObjectPool<String, Transformer>(new KeyedPooledObjectFactory<String, Transformer>() {

            /** ${@inheritDoc} */
            @Override
            public void activateObject(String key, PooledObject<Transformer> trans) throws Exception {
                trans.getObject().clearParameters();
            }

            /** ${@inheritDoc} */
            @Override
            public void destroyObject(String key, PooledObject<Transformer> trans) throws Exception {
                // Do nothing here
            }

            /** ${@inheritDoc} */
            @Override
            public PooledObject<Transformer> makeObject(String key) throws Exception {
                Crosswalk crosswalk = daoCrosswalk.read(key);
                if (crosswalk == null) {
                    throw new RuntimeException("Couldn't find crosswalk for name " + key);
                }

                try (StringReader reader = new StringReader(crosswalk.getXsltStylesheet())) {
                    StreamSource xslSource = new StreamSource(reader);
                    Transformer transformer = saxTransformerFactory.newTransformer(xslSource);
                    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                    transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());

                    // Standard JAXP indent property (works in Saxon-HE)
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");

                    LOGGER.info("Created new transformer for crosswalk " + key);
                    return new DefaultPooledObject<>(transformer);
                }
            }

            /** ${@inheritDoc} */
            @Override
            public void passivateObject(String key, PooledObject<Transformer> trans) throws Exception {
                trans.getObject().clearParameters();
            }

            /** ${@inheritDoc} */
            @Override
            public boolean validateObject(String key, PooledObject<Transformer> trans) {
                // If time to live is over, return false
                if (System.currentTimeMillis() - trans.getCreateTime() > MAX_TIME_TO_LIVE) {
                    return false;
                }
                return true;
            }

        });
        assert this.pool != null;
        this.pool.setMaxTotal(MAX_ACTIVE);
        // Using Duration for compatibility with Commons Pool 2.12+
        this.pool.setMaxWait(Duration.ofMillis(MAX_WAIT));
        this.pool.setTestWhileIdle(TEST_WHILE_IDLE);
        this.pool.setTimeBetweenEvictionRuns(Duration.ofMillis(TIME_BETWEEN_EVICTION_RUNS));
    }

    @Override
    public String transform(String xml, String name) throws IOException {
        Transformer transformer = null;
        try (StringReader xmlReader = new StringReader(xml);
             StringWriter writer = new StringWriter()) {

            transformer = borrowObject(name);

            final StreamResult result = new StreamResult(writer);
            final StreamSource source = new StreamSource(xmlReader);
            // do the transformation
            transformer.transform(source, result);

            return writer.toString();
        } catch (Exception e) {
            if (transformer != null) {
                try {
                    invalidateObject(name, transformer);
                    transformer = null; // Mark as null so finally doesn't return it
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), e);
                }
            }
            throw new IOException(e);
        } finally {
            if (transformer != null) {
                try {
                    returnObject(name, transformer);
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
        }
    }

    /** ${@inheritDoc}
     * @throws Exception */
    @Override
    public void updateTransformer(String key) throws Exception {
        try {
            // Clear old instances to ensure fresh XSLT 3.0 logic is loaded
            this.pool.clear(key);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw ex;
        }
    }


    /** ${@inheritDoc} */
    @Override
    public void addObject(String key) throws Exception, IllegalStateException, UnsupportedOperationException {
        this.pool.addObject(key);
    }

    /** ${@inheritDoc} */
    @Override
    public Transformer borrowObject(String key) throws Exception, NoSuchElementException, IllegalStateException {
        return this.pool.borrowObject(key);
    }

    /** ${@inheritDoc} */
    @Override
    public void clear() throws Exception, UnsupportedOperationException {
        this.pool.clear();
    }

    /** ${@inheritDoc} */
    @Override
    public void clear(String key) throws Exception, UnsupportedOperationException {
        this.pool.clear(key);
    }

    /** ${@inheritDoc} */
    @Override
    public void close() {
        this.pool.close();
    }

    /** ${@inheritDoc} */
    @Override
    public int getNumActive() {
        return this.pool.getNumActive();
    }

    /** ${@inheritDoc} */
    @Override
    public int getNumActive(String key) {
        return this.pool.getNumActive(key);
    }

    /** ${@inheritDoc} */
    @Override
    public int getNumIdle() {
        return this.pool.getNumIdle();
    }

    /** ${@inheritDoc} */
    @Override
    public int getNumIdle(String key) {
        return this.pool.getNumIdle(key);
    }

    /** ${@inheritDoc} */
    @Override
    public void invalidateObject(String key, Transformer transformer) throws Exception {
        this.pool.invalidateObject(key, transformer);
    }

    /** ${@inheritDoc} */
    @Override
    public void returnObject(String key, Transformer transformer) throws Exception {
        this.pool.returnObject(key, transformer);
    }

    @Override
    public String info() {
        final int pad = 30;
        Map<String, Integer> activeMap = this.pool.getNumActivePerKey();
        Map<String, Integer> waiterMap = this.pool.getNumWaitersByKey();
        StringBuilder buf = new StringBuilder("TransformerPool\n")
                .append(StringUtils.leftPad("active: ", pad)).append(getNumActive()).append("\n")
                .append(StringUtils.leftPad("idle: ", pad)).append(getNumIdle()).append("\n")
                .append(StringUtils.leftPad("created: ", pad)).append(this.pool.getCreatedCount()).append("\n")
                .append(StringUtils.leftPad("borrowed: ", pad)).append(this.pool.getBorrowedCount()).append("\n")
                .append(StringUtils.leftPad("destroyed: ", pad)).append(this.pool.getDestroyedCount()).append("\n")
                .append(StringUtils.leftPad("max total: ", pad)).append(this.pool.getMaxTotal()).append("\n")
                .append(StringUtils.leftPad("max total per key: ", pad)).append(this.pool.getMaxTotalPerKey()).append("\n");
        for (String key : activeMap.keySet()) {
            buf.append(StringUtils.leftPad("active " + key + ": ", pad)).append(activeMap.get(key)).append("\n");
            buf.append(StringUtils.leftPad("idle " + key + ": ", pad)).append(this.pool.getNumIdle(key)).append("\n");
        }
        for (String key : waiterMap.keySet()) {
            buf.append(StringUtils.leftPad("waiters " + key + ": ", pad)).append(waiterMap.get(key)).append("\n");
        }

        return buf.toString();
    }
}