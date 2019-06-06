package de.fiz.oai.backend.dao.impl;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.utils.ClusterManager;

import javax.annotation.PostConstruct;
import java.util.List;

public class CassandraDAOItem implements DAOItem {

    public Item read(String identifier) throws Exception {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        String query = "SELECT * FROM oai_item WHERE identifier='" + identifier + "'";
        ResultSet rs = session.execute(query);
        Row resultRow = rs.one();
        if (resultRow != null) {
            final Item item = new Item();
            item.setContent(resultRow.getString("content"));
            item.setDeleteFlag(resultRow.getBool("deletedflag"));
            item.setIdentifier(resultRow.getString("identifier"));
            item.setUuid(resultRow.getUUID("uuid"));

            return item;
        }
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
