package de.fiz.oai.backend.service;

import java.io.IOException;

import org.jvnet.hk2.annotations.Contract;

@Contract
public interface TransformerService {

    /**
     * Transform XML using crosswalk stylesheet.
     *
     * @param xml XML to transform
     * @param name Name of the crosswalk
     * @return Transformed XML
     * @throws IOException
     */
    public String transform(String xml, String name) throws IOException;

    /**
     * @return Information about pool
     */
    public String info();

    /**
     * Updates the XSLT transformer in the pool
     * 
     * @param name the name of the transformer
     */
    public void updateTransformer(String name)  throws Exception;
}
