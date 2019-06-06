package de.fiz.oai.backend.dao;

import de.fiz.oai.backend.models.Item;

import java.util.List;

public interface DAOItem {

    /**
     * Create a new Item.
     *
     * @param identifier
     *            the identifier
     * @return the item
     */
    Item read(String identifier);

    /**
     * Create a new Item.
     *
     * @param item
     *            the item
     * @return the item created (in case uuid are processed in the method)
     */
    Item create(Item item);

    /**
     * Search for Items.
     *
     * @param sort
     *            ASC/DESC
     * @param offset
     *            the offset
     * @param rows
     *            the rows
     * @param query
     *            the query
     * @return the items
     */
    List<Item> search(String sort, Integer offset, Integer rows, String query);

    /**
     * Delete an Item.
     *
     * @param identifier
     *            the identifier
     */
    void delete(String identifier);
}
