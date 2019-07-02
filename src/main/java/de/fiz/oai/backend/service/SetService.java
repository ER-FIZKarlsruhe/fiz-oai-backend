package de.fiz.oai.backend.service;

import java.io.IOException;
import java.util.List;

import de.fiz.oai.backend.models.Set;

public interface SetService {

  /**
   * Read a Set.
   *
   * @param name the name
   * @return the Set
   */
  Set read(String name) throws IOException;

  /**
   * Create a new Set.
   *
   * @param Set the Set
   * @return the Set created (in case uuid are processed in the method)
   */
  Set create(Set set) throws IOException;


  /**
   * Updates a Set.
   *
   * @param Set the Set
   * @return the Set created
   */
  Set update(Set set) throws IOException;
  
  /**
   * Search for Sets.
   *
   * @return the Sets
   */
  List<Set> readAll() throws IOException;

  /**
   * Delete an Set.
   *
   * @param name the name
   */
  void delete(String name) throws IOException;
}
