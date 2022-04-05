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
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.fiz.oai.backend.dao.DAOContent;
import de.fiz.oai.backend.dao.DAOCrosswalk;
import de.fiz.oai.backend.dao.DAOFormat;
import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.dao.DAOSet;
import de.fiz.oai.backend.exceptions.AlreadyExistsException;
import de.fiz.oai.backend.exceptions.FormatValidationException;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.exceptions.UnknownFormatException;
import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.models.Crosswalk;
import de.fiz.oai.backend.models.Format;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.service.ItemService;
import de.fiz.oai.backend.service.SearchService;
import de.fiz.oai.backend.service.TransformerService;
import de.fiz.oai.backend.utils.Configuration;
import de.fiz.oai.backend.utils.XPathHelper;

@Service
public class ItemServiceImpl implements ItemService {

  private static Logger LOGGER = LoggerFactory.getLogger(ItemServiceImpl.class);

  @Inject
  DAOItem daoItem;

  @Inject
  DAOContent daoContent;

  @Inject
  DAOFormat daoFormat;

  @Inject
  DAOCrosswalk daoCrosswalk;

  @Inject
  DAOSet daoSet;

  @Inject
  SearchService searchService;

  @Inject
  TransformerService transformerService;

  @Override
  public Item read(String identifier, String format, Boolean readContent) throws IOException {
    Item item = daoItem.read(identifier);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("getItem: {}", item);
    }

    if (item != null) {

      if (readContent) {
        if (format == null) {
          format = item.getIngestFormat();
        }

        Content content = daoContent.read(identifier, format);
        item.setContent(content);
      }

      // Retrieve sets and formats from search-server
      Map<String, Object> searchResponse = searchService.readDocument(item);
      if (searchResponse != null) {
        if (searchResponse.get("sets") != null) {
          List<String> sets = searchResponse.get("sets") instanceof List<?> ? (List<String>) searchResponse.get("sets") : List.of((String) searchResponse.get("sets"));
          item.setSets(sets);
        }
        if (searchResponse.get("formats") != null) {
          List<String> formats = searchResponse.get("formats") instanceof List<?> ? (List<String>) searchResponse.get("formats") : List.of((String) searchResponse.get("formats"));
          item.setFormats(formats);
        }
      } else {
        LOGGER.warn("Couldn't find item ${item} in index.");
        item = null;
      }
    } else {
        LOGGER.warn("Couldn't find item ${item} in backend.");
      }

    return item;
  }

  @Override
  public Item create(Item item) throws IOException {
	  
	// Check for existing item
	Item oldItem = read(item.getIdentifier(), item.getIngestFormat(), false);
	if (oldItem != null) {
		throw new AlreadyExistsException("item " + oldItem.getIdentifier() + " already exists");
	}

    // IngestFormat exists?
    Format ingestFormat = daoFormat.read(item.getIngestFormat());
    if (ingestFormat == null) {
      throw new UnknownFormatException("Cannot find a Format for the given ingestFormat: " + item.getIngestFormat());
    }

    // Validate xml against xsd
    // validate(ingestFormat.getSchemaLocation(), new
    // String(item.getContent().getContent(), "UTF-8"));

    // Overwrite datestamp!
    item.setDatestamp(StringUtils.isNotEmpty(item.getDatestamp()) ? item.getDatestamp() : Configuration.getDateformat().format(new Date()));

    // Create Item
    Item newItem = daoItem.create(item);

    // Create Content
    daoContent.create(item.getContent());

    Set<String> itemFormats = Stream.of(item.getIngestFormat()).collect(Collectors.toCollection(HashSet::new));

    // Create Crosswalk content
    createCrosswalks(item, itemFormats);

    addFormatsAndSets(newItem);
    searchService.createDocument(newItem);

    return newItem;
  }

  @Override
  public Item update(Item item) throws IOException {
    Item oldItem = read(item.getIdentifier(), null, false);

    if (oldItem == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    // Format exists?
    Format ingestFormat = daoFormat.read(item.getIngestFormat());
    if (ingestFormat == null) {
      throw new UnknownFormatException("Cannot find a Fomat for the given ingestFormat: " + item.getIngestFormat());
    }

    //Delete old content
    deleteAllContent(oldItem);

    // Overwrite datestamp!
    item.setDatestamp(StringUtils.isNotEmpty(item.getDatestamp()) ? item.getDatestamp() : Configuration.getDateformat().format(new Date()));

    Item updateItem = daoItem.create(item);
    daoContent.create(item.getContent());

    Set<String> itemFormats = Stream.of(item.getIngestFormat()).collect(Collectors.toCollection(HashSet::new));
    createCrosswalks(item, itemFormats);

    addFormatsAndSets(updateItem);
    searchService.updateDocument(updateItem);

    return updateItem;
  }

  @Override
  public SearchResult<Item> search(Integer rows, String setName, String format, Date from, Date until,
      Boolean readContent, String searchMark) throws IOException {
    // TODO make this default setting configurable!
    if (rows == null) {
      rows = 100;
    }

    if (rows > 1000) {
      throw new IOException("rows parameter must NOT be greater than 1000!");
    }

    final SearchResult<String> idResult = searchService.search(rows, setName, format, from, until, searchMark);

    List<Item> itemList = new ArrayList<>();

    for (String s : idResult.getData()) {
      Item item = read(s, format, readContent);
      if (item != null) {
          itemList.add(item);
      }
    }

    SearchResult<Item> itemResult = new SearchResult<>();
    itemResult.setData(itemList);
    itemResult.setSize(itemList.size());
    itemResult.setTotal(idResult.getTotal());
    itemResult.setSearchMark(idResult.getSearchMark());

    return itemResult;
  }

  @Override
  public void delete(String identifier) throws IOException {

    Item itemToDelete = daoItem.read(identifier);
    
    if (itemToDelete == null) {
        throw new NotFoundException("Item with id " + identifier + " was not found");
    }

    itemToDelete.setDeleteFlag(true);
    itemToDelete.setDatestamp(Configuration.getDateformat().format(new Date()));

    daoItem.create(itemToDelete);

    //first add all formats then delete content because formats are read from the content
    addFormatsAndSets(itemToDelete);
    deleteAllContent(itemToDelete);
    
    searchService.updateDocument(itemToDelete);
  }
  
  public void addFormatsAndSets(Item item) throws IOException {
      try {
          // Add all available formats
          List<Content> allContents = daoContent.readFormats(item.getIdentifier());
          List<String> itemFormats = new ArrayList<>();
          if (allContents != null && !allContents.isEmpty()) {
              for (final Content pickedContent : allContents) {
                  itemFormats.add(pickedContent.getFormat());
              }
          }
          item.setFormats(itemFormats);
    
          // Add all the matching sets
          List<de.fiz.oai.backend.models.Set> allSets = daoSet.readAll();
          List<String> itemSets = new ArrayList<>();
          if (allSets != null && !allSets.isEmpty()) {
    
              for (final de.fiz.oai.backend.models.Set pickedSet : allSets) {
                  // Check set membership via xPath
                  Map<String, String> xPaths = pickedSet.getxPaths();
                  if (allContents != null && !allContents.isEmpty()) {
                      for (final Content pickedContent : allContents) {
                          if (xPaths.containsKey(pickedContent.getFormat())) {
                              final String xPathToCheck = xPaths.get(pickedContent.getFormat());
                              if (XPathHelper.isTextValueMatching(pickedContent.getContent(), xPathToCheck)) {
                                  itemSets.add(pickedSet.getName());
                              }
                          }
                      }
                  }
    
                  // Check set membership via item tags
                  List<String> setTags = pickedSet.getTags();
    
                  if (setTags != null && !setTags.isEmpty()) {
                      for (String setTag : setTags) {
                          LOGGER.debug("item.getTags() " + item.getTags());
                          if (item.getTags() != null && item.getTags().contains(setTag)) {
                              itemSets.add(pickedSet.getSpec());
                          }
                      }
                  }
              }
    
          }
          item.setSets(itemSets);
      } catch(SAXException| XPathExpressionException e) {
          //Rethrow Exceptions from XPathHelper as IOException
          throw new IOException(e);
      }
  }

  /**
   * Validate xml against an XSD schemaLocation
   * 
   * @param schemaLocation
   * @param xml
   * @throws IOException
   */
  private void validate(String schemaLocation, String xml) throws IOException {
    URL schemaLocationUrl = new URL(schemaLocation);
    try {
      Source xsdSource = new StreamSource(new InputStreamReader(schemaLocationUrl.openStream()));
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      
      Schema schema = schemaFactory.newSchema(xsdSource);
      Validator validator = schema.newValidator();
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      
      Source xmlSource = new StreamSource(new StringReader(xml));
      validator.validate(xmlSource);
    } catch (SAXException e) {
      throw new FormatValidationException(e.getMessage());
    }
  }

  private void createCrosswalks(Item item, Set<String> itemFormats) throws IOException {
    List<Crosswalk> crosswalks = daoCrosswalk.readAll();
    for (Crosswalk currentWalk : crosswalks) {
      if (currentWalk.getFormatFrom().equals(item.getIngestFormat())) {
        String newXml = transformerService.transform(item.getContent().getContent(), currentWalk.getName());
        if (StringUtils.isNotBlank(newXml)) {
            Content crosswalkConten = new Content();
            crosswalkConten.setContent(newXml);
            crosswalkConten.setIdentifier(item.getIdentifier());
            crosswalkConten.setFormat(currentWalk.getFormatTo());
            daoContent.create(crosswalkConten);
            itemFormats.add(currentWalk.getFormatTo());
        }
        else {
            LOGGER.warn("XML IS EMPTY: " + currentWalk.getFormatTo() + ", " + item.getIdentifier());
        }
      }
    }
  }
  
  
  private void deleteAllContent(Item item) throws IOException {
    List<Content> allContents = daoContent.readFormats(item.getIdentifier());
    if (allContents != null && !allContents.isEmpty()) {
        for (final Content pickedContent : allContents) {
            daoContent.delete(item.getIdentifier(), pickedContent.getFormat());
        }
    }
  }

}
