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

import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.models.Item;

@Contract
public interface DAOContent {

  /**
   * Read a Set.
   *
   * @param identifier the name
   * @param  format
   * @return the Set
   */
  Content read(String identifier, String format) throws IOException;

  /**
   * Create a new Content.
   *
   * @param content the Content
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
  
  /**
   * Delete an Content.
   *
   * @param item the item
   */
  void delete(Item item) throws IOException;
}
