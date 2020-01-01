/*
 * Copyright 2019 FIZ Karlsruhe - Leibniz-Institut fuer Informationsinfrastruktur GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fiz.oai.backend.service;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

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
  Map<String, Object> readDocument(Item item) throws IOException;
  
  /**
   * 
   * @param offset
   * @param rows
   * @param set
   * @param format
   * @param fromDate
   * @param untilDate
   * @param lastItem
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
