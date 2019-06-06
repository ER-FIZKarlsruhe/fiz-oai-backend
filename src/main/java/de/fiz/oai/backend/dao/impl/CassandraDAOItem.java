package de.fiz.oai.backend.dao.impl;

import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.models.Item;

import java.util.List;

public class CassandraDAOItem implements DAOItem {

    public Item read(String identifier) {
        return null;
    }

    public Item create(Item item) {
        return null;
    }

    public List<Item> search(String sort, Integer offset, Integer rows, String query) {
        return null;
    }

    public void delete(String identifier) {

    }
}
