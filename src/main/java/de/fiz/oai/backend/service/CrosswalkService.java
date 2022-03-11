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
import java.util.List;

import org.jvnet.hk2.annotations.Contract;

import de.fiz.oai.backend.models.Crosswalk;

@Contract
public interface CrosswalkService {

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
   * @param content Crosswalk the Content
   * @return the Crosswalk created
   */
  Crosswalk create(Crosswalk content) throws IOException;

  /**
   * Updates a Crosswalk.
   *
   * @param content Crosswalk the Content
   * @return the Crosswalk created
   */
  Crosswalk update(Crosswalk content) throws IOException;

  
  /**
   * Process a Crosswalk for a set of items
   *
   * @param content String name of the Crosswalk to process
   * @param updateItemTimestamp <code>true</true> if the related item timestamp should be updated
   * @param from together with the until parameter, it defines a time range for searching items by the datestamp, where the related crosswalk should be processed
   * @param until together with the from parameter, it defines a time range for searching item by the datestamps, where the related crosswalk should be processed
   * 
   * @return the Crosswalk created
   */
  void process(String name, boolean keepItemTimestamp, Date from, Date until) throws IOException;

  
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
