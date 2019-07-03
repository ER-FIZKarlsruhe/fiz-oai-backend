package de.fiz.oai.backend.service;

import java.io.IOException;
import java.util.List;

import org.jvnet.hk2.annotations.Contract;

import de.fiz.oai.backend.models.Content;

@Contract
public interface ContentService {

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
   * Updates a Content.
   *
   * @param Content the Content
   * @return the Content created
   */
  Content update(Content content) throws IOException;
  
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
   * @param format the format
   */
  void delete(String identifier, String format) throws IOException;
}
