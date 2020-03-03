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

import org.jvnet.hk2.annotations.Service;

import de.fiz.oai.backend.dao.DAOCrosswalk;
import de.fiz.oai.backend.models.Crosswalk;
import de.fiz.oai.backend.service.CrosswalkService;

@Service
public class CrosswalkServiceImpl implements CrosswalkService {

  @Inject
  DAOCrosswalk daoCrosswalk; 
  
  @Override
  public Crosswalk read(String name) throws IOException {
    Crosswalk crosswalk = daoCrosswalk.read(name);
    return crosswalk;
  }

  @Override
  public Crosswalk create(Crosswalk content) throws IOException {
    // TODO add more validations
    // Does the format (referenced by formatFrom) exists?
    // Does the format (referenced by formatTo) exists?
    
    Crosswalk newCrosswalk = daoCrosswalk.create(content);
    return newCrosswalk;
  }

  @Override
  public Crosswalk update(Crosswalk content) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Crosswalk> readAll() throws IOException {
    List<Crosswalk> crosswalks = daoCrosswalk.readAll();
    
    return crosswalks;
  }

  @Override
  public void delete(String name) throws IOException {
    daoCrosswalk.delete(name);
  }

}
