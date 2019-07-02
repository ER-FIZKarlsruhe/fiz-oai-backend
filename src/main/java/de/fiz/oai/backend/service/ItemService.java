package de.fiz.oai.backend.service;

import java.io.IOException;

import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;

public interface ItemService {

  /**
   * Read an Item.
   *
   * @param identifier the identifier
   * @return the item
   */
  Item read(String identifier) throws IOException;

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
  SearchResult<Item> search(Integer offset, Integer rows, String set, String format,String from,String until) throws IOException;

  /**
   * Delete an Item.
   *
   * @param identifier the identifier
   */
  void delete(String identifier) throws IOException;
  
}
