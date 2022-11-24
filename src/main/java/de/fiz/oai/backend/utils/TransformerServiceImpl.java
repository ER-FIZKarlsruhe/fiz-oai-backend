/**
 * Copyright (c) 27.02.20 Fachinformationszentrum Karlsruhe
 */
package de.fiz.oai.backend.utils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.inject.Inject;
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
import org.jvnet.hk2.annotations.Service;

import de.fiz.oai.backend.dao.DAOCrosswalk;
import de.fiz.oai.backend.models.Crosswalk;
import de.fiz.oai.backend.service.TransformerService;
import net.sf.saxon.lib.FeatureKeys;

/** Crosswalk transformations. */
@Service
public class TransformerServiceImpl implements TransformerService, KeyedObjectPool<String, Transformer> {

    /** Logger. */
    private static Log LOGGER = LogFactory.getLog(TransformerServiceImpl.class);

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
        LOGGER.info("Initialize TransformerPool ...");

        // Create transformerFactory as singleton.
        TransformerFactory tf = TransformerFactory.newInstance("com.saxonica.config.EnterpriseTransformerFactory", null);
        if (tf.getFeature(SAXTransformerFactory.FEATURE)) {
            try {
                tf.setAttribute(
                    FeatureKeys.XML_PARSER_FEATURE + "http://xml.org/sax/features/external-general-entities", false);
                tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            } catch (TransformerConfigurationException e) {
                LOGGER.error(e.getMessage(), e);
            }
            saxTransformerFactory = (SAXTransformerFactory) tf;

        } else {
            LOGGER.error("Couldn't instantiate a SAXTransformerFactory.");
            throw new RuntimeException("Couldn't instantiate a SAXTransformerFactory.");
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

                StreamSource xslSource = new StreamSource(new StringReader(crosswalk.getXsltStylesheet()));
                Transformer transformer = saxTransformerFactory.newTransformer(xslSource);
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                transformer.setOutputProperty(OutputKeys.ENCODING, String.valueOf(StandardCharsets.UTF_8));
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                LOGGER.info("Created new transformer for crosswalk " + key);
                return new DefaultPooledObject<>(transformer);
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
        this.pool.setMaxWaitMillis(MAX_WAIT);
        this.pool.setTestWhileIdle(TEST_WHILE_IDLE);
        this.pool.setTimeBetweenEvictionRunsMillis(TIME_BETWEEN_EVICTION_RUNS);
    }

    @Override
    public String transform(String xml, String name) throws IOException {
        Transformer transformer = null;
        try (StringReader xmlReader = new StringReader(xml)) {

            transformer = borrowObject(name);

            final StreamResult result = new StreamResult(new StringWriter());
            final StreamSource source = new StreamSource(xmlReader);
            // do the transformation
            transformer.transform(source, result);

            String resultString = result.getWriter().toString();
            return resultString;
        } catch (Exception e) {
            if (transformer != null) {
                try {
                    invalidateObject(name, transformer);
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
            this.pool.addObject(key);
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
