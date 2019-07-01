package de.fiz.oai.backend.dao;

import java.io.IOException;
import java.util.List;

import de.fiz.oai.backend.models.Content;

public interface DAOContent {

  /**
   * Read a Set.
   *
   * @param name the name
   * @return the Set
   */
  Content read(String identifier, String format) throws IOException;

  /**
   * Create a new Content.
   *
   * @param Content the Content
   * @return the Content created
   */
  Content create(Content content) throws IOException;

  
  /**
   * Read all Contents for a given identifier.
   *
   * @return the Contents
   */
  List<Content> readFormats(String identifier) throws IOException;

  
  /**
   * Delete an Content.
   *
   * @param identifier the identifier
   */
  void delete(String identifier, String format) throws IOException;
}
