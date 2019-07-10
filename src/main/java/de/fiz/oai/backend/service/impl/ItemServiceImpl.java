package de.fiz.oai.backend.service.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.fiz.oai.backend.dao.DAOContent;
import de.fiz.oai.backend.dao.DAOCrosswalk;
import de.fiz.oai.backend.dao.DAOFormat;
import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.dao.DAOSet;
import de.fiz.oai.backend.exceptions.FormatValidationException;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.exceptions.UnknownFormatException;
import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.models.Crosswalk;
import de.fiz.oai.backend.models.Format;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.models.Set;
import de.fiz.oai.backend.service.ItemService;
import de.fiz.oai.backend.service.SearchService;
import de.fiz.oai.backend.utils.Configuration;

@Service
public class ItemServiceImpl implements ItemService {

  private Logger LOGGER = LoggerFactory.getLogger(ItemServiceImpl.class);


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

  @Override
  public Item read(String identifier, String format, Boolean readContent) throws IOException {
    final Item item = daoItem.read(identifier);
    LOGGER.info("getItem: " + item);

    if (readContent) {
      Content content = daoContent.read(identifier, format);
      item.setContent(content);
    }

    return item;
  }

  @Override
  public Item create(Item item) throws IOException {
    Item newItem = null;

    // Overwrite datestamp!
    item.setDatestamp(Configuration.dateFormat.format(new Date()));

    // IngestFormat exists?
    Format ingestFormat = daoFormat.read(item.getIngestFormat());
    if (ingestFormat == null) {
      throw new UnknownFormatException("Cannot find a Format for the given ingestFormat: " + item.getIngestFormat());
    }

    // Validate xml against xsd
    validate(ingestFormat.getSchemaLocation(), new String(item.getContent().getContent(), "UTF-8"));

    //Create Item
    newItem = daoItem.create(item);

    //Create Content
    daoContent.create(item.getContent());

    //Create Crosswalk content
    List<Crosswalk> crosswalks = daoCrosswalk.readAll();
    for (Crosswalk currentWalk: crosswalks) {
      if (currentWalk.getFormatFrom().equals(item.getIngestFormat())) {
        currentWalk.getXsltStylesheet();
        //TODO do transformation and save it into content
      }
    }
    
    //TODO For indexing its important that oai_dc content exits! 
    searchService.createDocument(newItem);

    return newItem;
  }

  @Override
  public Item update(Item item) throws IOException {
    Item oldItem = daoItem.read(item.getIdentifier());

    if (oldItem == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    // Overwrite datestamp!
    item.setDatestamp(Configuration.dateFormat.format(new Date()));

    // Format exists?
    Format ingestFormat = daoFormat.read(item.getIngestFormat());
    if (ingestFormat == null) {
      throw new UnknownFormatException("Cannot find a Fomat for the given ingestFormat: " + item.getIngestFormat());
    }

    // Validate xml against xsd
    validate(ingestFormat.getSchemaLocation(), new String(item.getContent().getContent(), "UTF-8"));

    //TODO delete all old content with item identifer
    
    Item updateItem = daoItem.create(item);

    daoContent.create(item.getContent());

    searchService.updateDocument(updateItem);

    //Create Crosswalks
    //TODO 1) search all crosswalks with inputFormat == ingestFormat
    //2) Perform xsltTransformation 
    

    
    return updateItem;
  }

  @Override
  public SearchResult<Item> search(Integer offset, Integer rows, String setName, String format, Date from, Date until,
      Boolean readContent) throws IOException {
    if (offset == null) {
      offset = 0;
    }

    //TODO make this default setting configurable!
    if (rows == null) {
      rows = 100;
    }

    Set set = daoSet.read(setName);
    
    if (set == null) {
      throw new NotFoundException("Set " + setName + " not found in the database");
    }
    
    final SearchResult<String> idResult = searchService.search(offset, rows, set, format, from, until);

    List<Item> itemList = new ArrayList<Item>();

    for (String s : idResult.getData()) {
      Item item = read(s, format, readContent);
      itemList.add(item);
    }

    SearchResult<Item> itemResult = new SearchResult<Item>();
    itemResult.setData(itemList);
    itemResult.setOffset(offset);
    itemResult.setSize(itemList.size());
    itemResult.setTotal(idResult.getTotal());

    return itemResult;
  }

  @Override
  public void delete(String identifier) throws IOException {

    //TODO read item and set delete flag, save to cassandra

    //TODO update index
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
    Source xsdSource = new StreamSource(new InputStreamReader(schemaLocationUrl.openStream()));
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema;
    try {
      schema = schemaFactory.newSchema(xsdSource);
      Validator validator = schema.newValidator();
      Source xmlSource = new StreamSource(new StringReader(xml));
      validator.validate(xmlSource);
    } catch (SAXException e) {
      throw new FormatValidationException(e.getMessage());
    }
  }

}
