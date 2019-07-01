package de.fiz.oai.backend.dao;

import java.io.IOException;
import java.util.List;

import de.fiz.oai.backend.models.Crosswalk;

public interface DAOCrosswalk {

  /**
   * Read a Set.
   *
   * @param name the name
   * @return the Set
   */
  Crosswalk read(String name) throws IOException;

  /**
   * Create a new Crosswalk.
   *
   * @param Crosswalk the Content
   * @return the Crosswalk created
   */
  Crosswalk create(Crosswalk content) throws IOException;

  
  /**
   * Read all Crosswalks.
   *
   * @return the Contents
   */
  List<Crosswalk> readAll() throws IOException;

  
  /**
   * Delete an Crosswalk.
   *
   * @param name name of a crosswalk
   */
  void delete(String name) throws IOException;
}
