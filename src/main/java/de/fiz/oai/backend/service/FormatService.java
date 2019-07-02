package de.fiz.oai.backend.service;

import java.io.IOException;
import java.util.List;

import de.fiz.oai.backend.models.Format;

public interface FormatService {
  /**
   * Read a Format.
   *
   * @param metadataPrefix
   *            the metadataPrefix
   * @return the Format
   */
  Format read(String metadataPrefix) throws IOException;

  /**
   * Create a new Format.
   *
   * @param Format
   *            the Format
   * @return the Format created
   */
  Format create(Format Format) throws IOException;

  /**
   * Updates a Format.
   *
   * @param Format
   *            the Format
   * @return the Format updated
   */
  Format update(Format Format) throws IOException;
  
  /**
   * Search for Formats.
   *
   * @return the Formats
   */
  List<Format> readAll() throws IOException;

  /**
   * Delete an Format.
   *
   * @param metadataPrefix
   *            the metadataPrefix
   */
  void delete(String metadataPrefix) throws IOException;
}
