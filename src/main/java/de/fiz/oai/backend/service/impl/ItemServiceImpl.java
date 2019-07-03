package de.fiz.oai.backend.service.impl;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.service.ItemService;
import de.fiz.oai.backend.service.SearchService;

@Service
public class ItemServiceImpl implements ItemService {

  private Logger LOGGER = LoggerFactory.getLogger(ItemServiceImpl.class);

  SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-DD'T'hh:mm:ss'Z'");
  
  @Inject
  DAOItem daoItem;

  @Inject
  SearchService searchService;
  
  @Override
  public Item read(String identifier) throws IOException {
    final Item item = daoItem.read(identifier);
    LOGGER.info("getItem: " + item);
    
    return item;
  }

  @Override
  public Item create(Item item) throws IOException {
    Item newItem = null;
    
    //Overwrite datestamp!
    item.setDatestamp(dateFormat.format(new Date()));
    
    //Validate item
    //TODO IngestFormat: Exists?

    //TODO Xsd Validate the content against the ingestFormat! 
    
    newItem = daoItem.create(item);
    
    searchService.createDocument(newItem);
    
    return newItem;
  }
  
  @Override
  public Item update(Item item) throws IOException {
    Item oldItem = daoItem.read(item.getIdentifier());

    if (oldItem == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }
    
    //Overwrite datestamp!
    item.setDatestamp(dateFormat.format(new Date()));
    
    //Validate item
    //TODO ingestFormat exists?
    
    //TODO given sets exists?
    
    //TODO IngestFormat: Exists?
    //TODO Xsd Validate the content against the ingestFormat! 

    Item updateItem = daoItem.create(item);
    
    //TODO index item
    
    return updateItem;
  }

  @Override
  public SearchResult<Item> search(Integer offset, Integer rows, String set, String format, Date from, Date until) throws IOException {
    final SearchResult<String> idResult = searchService.search(offset, rows, set, format, from, until);
    
    //TODO As Cassandra for all items in itemIds
    
    SearchResult<Item> itemResult = new SearchResult<Item>();    
    itemResult.setOffset(offset);
    itemResult.setSize(idResult.getSize());
    itemResult.setTotal(idResult.getTotal());
    
    return itemResult;
  }

  
  @Override
  public void delete(String identifier) throws IOException {
    // TODO Auto-generated method stub

  }

}
