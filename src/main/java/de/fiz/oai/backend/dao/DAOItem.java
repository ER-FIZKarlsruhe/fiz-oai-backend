package de.fiz.oai.backend.dao;

import java.io.IOException;
import java.util.List;

import org.jvnet.hk2.annotations.Contract;

import com.datastax.driver.core.ResultSet;

import de.fiz.oai.backend.models.Item;

@Contract
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
   * Delete an Item.
   *
   * @param identifier the identifier
   */
  void delete(String identifier) throws IOException;

  /**
   * Get count.
   */
  long getCount() throws IOException;

  /**
   * Get all items as ResultSet.
   */
  ResultSet getAllItemsResultSet() throws IOException;

  /**
   * Get list of Items given an already prepared ResultSet.
   */
  List<Item> getItemsFromResultSet(ResultSet resultSet, int itemsToRetrieve) throws IOException;
}
