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
package de.fiz.oai.backend.dao;

import java.io.IOException;
import java.util.List;

import org.jvnet.hk2.annotations.Contract;

import de.fiz.oai.backend.models.Item;

@Contract
public interface DAOItem {

  /**
   * Read an Item.
   *
   * @param identifier the identifier
   * @return the item
   */
  Item read(String identifier) throws IOException;

  /**
   * Create a new Item.
   *
   * @param item the item
   * @return the item created
   */
  Item create(Item item) throws IOException;


  /**
   * Search for Items.
   *
   * @param sort   ASC/DESC
   * @param offset the offset
   * @param rows   the rows
   * @param query  the query
   * @return the items
   */
  List<Item> search(Integer offset, Integer rows, String set, String format,String from,String until) throws IOException;

  /**
   * Delete an Item.
   *
   * @param identifier the identifier
   */
  void delete(String identifier) throws IOException;
}
