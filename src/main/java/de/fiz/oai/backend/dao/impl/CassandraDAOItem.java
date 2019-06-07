package de.fiz.oai.backend.dao.impl;

import com.datastax.driver.core.*;
import com.datastax.driver.core.utils.UUIDs;
import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.utils.ClusterManager;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

public class CassandraDAOItem implements DAOItem {

    public static final String ITEM_CONTENT = "content";
    public static final String ITEM_DELETEDFLAG = "deletedflag";
    public static final String ITEM_IDENTIFIER = "identifier";
    public static final String ITEM_UUID = "uuid";

    public Item read(String identifier) throws Exception {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        String query = "SELECT * FROM oai_item WHERE identifier='" + identifier + "'";
        ResultSet rs = session.execute(query);
        Row resultRow = rs.one();
        if (resultRow != null) {
            final Item item = new Item();
            item.setContent(resultRow.getString(ITEM_CONTENT));
            item.setDeleteFlag(resultRow.getBool(ITEM_DELETEDFLAG));
            item.setIdentifier(resultRow.getString(ITEM_IDENTIFIER));
            item.setUuid(resultRow.getUUID(ITEM_UUID));

            return item;
        }
        return null;
    }

    public Item create(Item item) throws Exception {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        if (StringUtils.isBlank(item.getContent())) {
            throw new IOException("Item's content cannot be empty!");
        }
        if (StringUtils.isBlank(item.getIdentifier())) {
            throw new IOException("Item's identifier cannot be empty!");
        }
        if (item.getDeleteFlag()==null) {
            item.setDeleteFlag(false);
        }
        if (item.getUuid()==null || StringUtils.isBlank(item.getUuid().toString())) {
            item.setUuid(UUIDs.timeBased());
        }

        StringBuilder insertStmt = new StringBuilder();
        insertStmt.append("INSERT INTO oai_item (");
        insertStmt.append(ITEM_CONTENT);
        insertStmt.append(", ");
        insertStmt.append(ITEM_DELETEDFLAG);
        insertStmt.append(", ");
        insertStmt.append(ITEM_IDENTIFIER);
        insertStmt.append(", ");
        insertStmt.append(ITEM_UUID);
        insertStmt.append(") VALUES (?, ?, ?, ?)");

        PreparedStatement prepared = session.prepare(insertStmt.toString());

        BoundStatement bound = prepared.bind(item.getContent(), item.getDeleteFlag(), item.getIdentifier(), item.getUuid());
        session.execute(bound);

        return item;
    }

    public List<Item> search(String sort, Integer offset, Integer rows, String query) {
        return null;
    }

    public void delete(String identifier) throws Exception {

        if (StringUtils.isBlank(identifier)) {
            throw new IOException("Item's identifier to delete cannot be empty!");
        }

        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        StringBuilder updateStmt = new StringBuilder();
        updateStmt.append("UPDATE oai_item SET ");
        updateStmt.append(ITEM_DELETEDFLAG);
        updateStmt.append("=? WHERE ");
        updateStmt.append(ITEM_IDENTIFIER);
        updateStmt.append("=?");

        PreparedStatement prepared = session.prepare(updateStmt.toString());

        BoundStatement bound = prepared.bind(true, identifier);
        session.execute(bound);
    }
}
