package de.fiz.oai.backend.service;

import java.io.IOException;
import java.util.Date;

import org.elasticsearch.action.get.GetResponse;
import org.jvnet.hk2.annotations.Contract;

import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;

@Contract
public interface SearchService {
  
  /**
   * Creates a new index if not existing 
   */
  void createIndex() throws IOException;
  
  /**
   * Drops the index
   */
  void dropIndex() throws IOException;
  
 /**
  * Reindex all documents
  */
  void reindexAll() throws IOException;
  
  /**
   * 
   * @param item
   */
  void createDocument(Item item) throws IOException;
  
  /**
   * 
   * @param item
   */
  void updateDocument(Item item) throws IOException;

  /**
   * 
   * @param item
   */
  void deleteDocument(Item item) throws IOException;
  
  /**
   * 
   * @param item
   */
  GetResponse readDocument(Item item) throws IOException;
  
  /**
   * 
   * @param offset
   * @param rows
   * @param set
   * @param format
   * @param fromDate
   * @param untilDate
   * @return
   * @throws IOException
   */
  SearchResult<String> search(Integer rows, String set, String format, Date fromDate, Date untilDate, Item lastItem) throws IOException;
  
//  /**
//   * 
//   * @param scrollId
//   * @return
//   * @throws IOException
//   */
//  SearchResult<String> search(String scrollId) throws IOException;
  
}
