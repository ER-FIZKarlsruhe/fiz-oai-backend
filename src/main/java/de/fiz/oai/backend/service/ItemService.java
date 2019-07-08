package de.fiz.oai.backend.service;

import java.io.IOException;
import java.util.Date;

import org.jvnet.hk2.annotations.Contract;

import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;

@Contract
public interface ItemService {

  /**
   * Read an Item.
   *
   * @param identifier the identifier
   * @return the item
   */
  Item read(String identifier, String format, Boolean content) throws IOException;

  /**
   * Create a new Item.
   *
   * @param item the item
   * @return the item created
   */
  Item create(Item item) throws IOException;

  /**
   * Updates an existing Item.
   *
   * @param item the item
   * @return the item updated
   */
  Item update(Item item) throws IOException;


  /**
   * Search for Items.
   *
   * @param sort   ASC/DESC
   * @param offset the offset
   * @param rows   the rows
   * @param query  the query
   * @return the items
   */
  SearchResult<Item> search(Integer offset, Integer rows, String set, String format, Date from, Date until, Boolean content) throws IOException;

  /**
   * Delete an Item.
   *
   * @param identifier the identifier
   */
  void delete(String identifier) throws IOException;
  
}
