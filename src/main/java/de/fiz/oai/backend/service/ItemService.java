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

import org.jvnet.hk2.annotations.Contract;

import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;

@Contract
public interface ItemService {

  /**
   * Read an Item.
   *
   * @param identifier the identifier
   * @return the item
   */
  Item read(String identifier, String format, Boolean content) throws IOException;

  /**
   * Create a new Item.
   *
   * @param item the item
   * @return the item created
   */
  Item create(Item item) throws IOException;

  /**
   * Updates an existing Item.
   *
   * @param item the item
   * @return the item updated
   */
  Item update(Item item) throws IOException;


  /**
   * Search for Items.
   *
   * @param rows   the rows
   * @param set
   * @param format
   * @param from
   * @param until
   * @param content
   * @param searchMark
   * @return the items
   */
  SearchResult<Item> search(Integer rows, String set, String format, Date from, Date until, Boolean content, String searchMark) throws IOException;

  /**
   * Delete an Item.
   *
   * @param identifier the identifier
   */
  void delete(String identifier) throws IOException;
  
  /**
   * Fill formats and sets in Item-Object
   * @param item
   * @return item
   * @throws IOException
   */
  void addFormatsAndSets(Item item) throws IOException;
  
}
