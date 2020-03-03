package de.fiz.oai.backend.service;

import org.jvnet.hk2.annotations.Contract;

import java.io.IOException;

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
}
