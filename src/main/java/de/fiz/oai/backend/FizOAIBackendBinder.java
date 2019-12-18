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
package de.fiz.oai.backend;

import javax.inject.Singleton;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import de.fiz.oai.backend.dao.DAOContent;
import de.fiz.oai.backend.dao.DAOCrosswalk;
import de.fiz.oai.backend.dao.DAOFormat;
import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.dao.DAOSet;
import de.fiz.oai.backend.dao.impl.CassandraDAOContent;
import de.fiz.oai.backend.dao.impl.CassandraDAOCrosswalk;
import de.fiz.oai.backend.dao.impl.CassandraDAOFormat;
import de.fiz.oai.backend.dao.impl.CassandraDAOItem;
import de.fiz.oai.backend.dao.impl.CassandraDAOSet;
import de.fiz.oai.backend.service.ContentService;
import de.fiz.oai.backend.service.CrosswalkService;
import de.fiz.oai.backend.service.FormatService;
import de.fiz.oai.backend.service.ItemService;
import de.fiz.oai.backend.service.SearchService;
import de.fiz.oai.backend.service.SetService;
import de.fiz.oai.backend.service.impl.ContentServiceImpl;
import de.fiz.oai.backend.service.impl.CrosswalkServiceImpl;
import de.fiz.oai.backend.service.impl.FormatServiceImpl;
import de.fiz.oai.backend.service.impl.ItemServiceImpl;
import de.fiz.oai.backend.service.impl.SearchServiceImpl;
import de.fiz.oai.backend.service.impl.SetServiceImpl;

public class FizOAIBackendBinder extends AbstractBinder {
  @Override
  protected void configure() {
      bind(CassandraDAOContent.class).to(DAOContent.class).in(Singleton.class);
      bind(CassandraDAOCrosswalk.class).to(DAOCrosswalk.class).in(Singleton.class);
      bind(CassandraDAOFormat.class).to(DAOFormat.class).in(Singleton.class);
      bind(CassandraDAOItem.class).to(DAOItem.class).in(Singleton.class);
      bind(CassandraDAOSet.class).to(DAOSet.class).in(Singleton.class);

      bind(ContentServiceImpl.class).to(ContentService.class).in(Singleton.class);
      bind(CrosswalkServiceImpl.class).to(CrosswalkService.class).in(Singleton.class);
      bind(FormatServiceImpl.class).to(FormatService.class).in(Singleton.class);
      bind(ItemServiceImpl.class).to(ItemService.class).in(Singleton.class);
      bind(SearchServiceImpl.class).to(SearchService.class).in(Singleton.class);
      bind(SetServiceImpl.class).to(SetService.class).in(Singleton.class);
  }
}