package de.fiz.oai.backend.dao;

import java.io.IOException;
import java.util.List;

import de.fiz.oai.backend.models.Item;

public interface DAOItem {

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
   * Search for Items.
   *
   * @param sort   ASC/DESC
   * @param offset the offset
   * @param rows   the rows
   * @param query  the query
   * @return the items
   */
  List<Item> search(Integer offset, Integer rows, String set, String format,String from,String until) throws IOException;

  /**
   * Delete an Item.
   *
   * @param identifier the identifier
   */
  void delete(String identifier) throws IOException;
}
