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
import java.util.List;

import org.jvnet.hk2.annotations.Contract;

import de.fiz.oai.backend.models.Set;

@Contract
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
