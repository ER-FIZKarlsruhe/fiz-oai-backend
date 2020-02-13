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

import de.fiz.oai.backend.dao.DAOContent;
import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.service.ContentService;

@Service
public class ContentServiceImpl implements ContentService {

  @Inject
  DAOContent daoContent;
  
  @Override
  public Content read(String identifier, String format) throws IOException {
    Content content = daoContent.read(identifier, format);
    return content;
  }

  @Override
  public Content create(Content content) throws IOException {
    // TODO add more validations
    //Does the item (referenced by identifier) exists?
    //Does the format (referenced by format) exists?
   
    Content newContent = daoContent.create(content);

    return newContent;
  }

  @Override
  public Content update(Content content) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Content> readFormats(String identifier) throws IOException {
    List<Content> content = daoContent.readFormats(identifier);
    return content;
  }

  @Override
  public void delete(String identifier, String format) throws IOException {
    daoContent.delete(identifier, format);
  }

}
