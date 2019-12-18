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
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.jvnet.hk2.annotations.Service;

import de.fiz.oai.backend.dao.DAOSet;
import de.fiz.oai.backend.models.Set;
import de.fiz.oai.backend.service.SetService;

@Service
public class SetServiceImpl implements SetService {

  @Inject
  DAOSet daoSet;

  @Override
  public Set read(String name) throws IOException {
    Set set = daoSet.read(name);
    
    return set;
  }

  @Override
  public Set create(Set set) throws IOException {
    daoSet.create(set);

    return set;
  }

  @Override
  public Set update(Set set) throws IOException {
    Set oldSet = daoSet.read(set.getName());

    if (oldSet == null) {
      throw new NotFoundException();
    }
    daoSet.create(set);

    return set;
  }
  
  
  @Override
  public List<Set> readAll() throws IOException {
    final List<Set> setList = daoSet.readAll();
    return setList;
  }

  @Override
  public void delete(String name) throws IOException {
    daoSet.delete(name);
  }

}
