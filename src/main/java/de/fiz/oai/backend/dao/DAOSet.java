package de.fiz.oai.backend.dao;

import de.fiz.oai.backend.models.Set;

import java.util.List;

public interface DAOSet {


    /**
     * Read a Set.
     *
     * @param name
     *            the name
     * @return the Set
     */
    Set read(String name) throws Exception;

    /**
     * Create a new Set.
     *
     * @param Set
     *            the Set
     * @return the Set created (in case uuid are processed in the method)
     */
    Set create(Set Set) throws Exception;

    /**
     * Search for Sets.
     *
     * @return the Sets
     */
    List<Set> readAll() throws Exception;

    /**
     * Delete an Set.
     *
     * @param name
     *            the name
     */
    void delete(String name) throws Exception;
}
