package de.fiz.oai.backend.dao;

import de.fiz.oai.backend.models.Format;

import java.util.List;

public interface DAOFormat {


    /**
     * Read a Format.
     *
     * @param metadataPrefix
     *            the metadataPrefix
     * @return the Format
     */
    Format read(String metadataPrefix) throws Exception;

    /**
     * Create a new Format.
     *
     * @param Format
     *            the Format
     * @return the Format created (in case uuid are processed in the method)
     */
    Format create(Format Format) throws Exception;

    /**
     * Search for Formats.
     *
     * @return the Formats
     */
    List<Format> readAll() throws Exception;

    /**
     * Delete an Format.
     *
     * @param metadataPrefix
     *            the metadataPrefix
     */
    void delete(String metadataPrefix) throws Exception;
}
