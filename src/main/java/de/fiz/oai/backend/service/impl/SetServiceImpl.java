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
package de.fiz.oai.backend.service.impl;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import de.fiz.oai.backend.models.ListSetsResult;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import jakarta.inject.Singleton;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.dao.DAOSet;
import de.fiz.oai.backend.exceptions.AlreadyExistsException;
import de.fiz.oai.backend.models.Set;
import de.fiz.oai.backend.service.SearchService;
import de.fiz.oai.backend.service.SetService;

@Service
@Singleton
public class SetServiceImpl implements SetService {

  private static Logger LOGGER = LoggerFactory.getLogger(SetServiceImpl.class);

  private static final String MISSING_PARENT_SETS_MESSAGE = "Missing parent sets. Please create these sets.";

  @Inject
  DAOSet daoSet;

  @Inject
  SearchService searchService;

  @Override
  public Set read(String setSpec) throws IOException {
    return daoSet.read(setSpec);
  }

  @Override
  public Set create(Set set) throws IOException {

    // Check for existing set
    Set oldSet = read(set.getSpec());
    if (oldSet != null) {
      throw new AlreadyExistsException("Set " + oldSet.getSpec() + " already exists");
    }

    // Check parent hierarchy → should be 400 if missing
    validateParentSetsExist(set);

    daoSet.create(set);

    LOGGER.info("Creating Set {}. Triggering complete reindexing.", set.getSpec());

    return set;
  }

  @Override
  public Set update(Set set) throws IOException {
    Set oldSet = daoSet.read(set.getSpec());

    // If the *set itself* does not exist → 404
    if (oldSet == null) {
      throw new NotFoundException();
    }

    daoSet.create(set);

    return set;
  }

  @Override
  public List<Set> readAll() throws IOException {
    final List<Set> setList = daoSet.readAll();
    setList.sort(Comparator.comparing(Set::getFullName));

    return setList;
  }

  @Override
  public void delete(String setSpec) throws IOException {
    daoSet.delete(setSpec);
  }

  @Override
  public void migrateOaiSets() throws IOException {
    daoSet.migrate();
  }

  @Override
  public ListSetsResult listSets(String resumptionToken) {
    ListSetsResult result = daoSet.listSets(resumptionToken);
    return result;
  }


  /**
   * Validates that all parent setSpecs of the given set exist.
   * Example: for "FIZ:ER:DG" it checks that "FIZ" and "FIZ:ER" exist.
   *
   * If a parent is missing, this is considered a client error (400).
   */
  private void validateParentSetsExist(Set set) throws IOException {
    if (set == null) {
      return;
    }

    String spec = set.getSpec();
    if (spec == null || !spec.contains(":")) {
      // No hierarchy → nothing to validate
      return;
    }

    String[] parts = spec.split(":");
    if (parts.length <= 1) {
      return;
    }

    StringBuilder parentSpecBuilder = new StringBuilder();

    // Go up to (but not including) the full spec
    for (int i = 0; i < parts.length - 1; i++) {
      if (i > 0) {
        parentSpecBuilder.append(":");
      }
      parentSpecBuilder.append(parts[i]);

      String currentParentSpec = parentSpecBuilder.toString();
      Set parentSet = daoSet.read(currentParentSpec);
      if (parentSet == null) {
        LOGGER.warn("Parent set '{}' for '{}' does not exist.", currentParentSpec, spec);
        // -> 400 Bad Request for missing parent sets
        throw new BadRequestException(MISSING_PARENT_SETS_MESSAGE);
      }
    }
  }
}
