package de.fiz.oai.backend.dao;

import java.io.IOException;
import java.util.List;

import org.jvnet.hk2.annotations.Contract;

import de.fiz.oai.backend.models.Set;

@Contract
public interface DAOSet {


    /**
     * Read a Set.
     *
     * @param name
     *            the name
     * @return the Set
     */
    Set read(String name) throws IOException;

    /**
     * Create a new Set.
     *
     * @param Set
     *            the Set
     * @return the Set created (in case uuid are processed in the method)
     */
    Set create(Set Set) throws IOException;

    /**
     * Search for Sets.
     *
     * @return the Sets
     */
    List<Set> readAll() throws IOException;

    /**
     * Delete an Set.
     *
     * @param name
     *            the name
     */
    void delete(String name) throws IOException;
}
