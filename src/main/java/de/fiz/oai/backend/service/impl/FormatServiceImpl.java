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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.jvnet.hk2.annotations.Service;

import de.fiz.oai.backend.dao.DAOFormat;
import de.fiz.oai.backend.models.Format;
import de.fiz.oai.backend.service.FormatService;

@Service
public class FormatServiceImpl implements FormatService {

  @Inject
  private DAOFormat daoFormat;
  
  @Override
  public Format read(String metadataPrefix) throws IOException {
    Format format = daoFormat.read(metadataPrefix);
    return format;
  }

  @Override
  public Format create(Format format) throws IOException {
    Format newFormat = daoFormat.create(format);
    return newFormat;
  }

  @Override
  public Format update(Format format) throws IOException {
    Format oldFormat = daoFormat.read(format.getMetadataPrefix());

    if (oldFormat == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }
    
    //In Cassandra create and update are the same
    Format updatedFormat = daoFormat.create(format);
    return updatedFormat;
  }
  
  @Override
  public List<Format> readAll() throws IOException {
    List<Format> formatList = daoFormat.readAll();
    return formatList;
  }

  @Override
  public void delete(String metadataPrefix) throws IOException {
    daoFormat.delete(metadataPrefix);
  }

}
