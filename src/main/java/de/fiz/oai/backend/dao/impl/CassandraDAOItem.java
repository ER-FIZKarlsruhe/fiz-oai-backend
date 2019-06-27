package de.fiz.oai.backend.dao.impl;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.utils.ClusterManager;

public class CassandraDAOItem implements DAOItem {

    public static final String ITEM_IDENTIFIER = "identifier";
    public static final String ITEM_DATESTAMP = "datestamp";
    public static final String ITEM_DELETEFLAG = "deleteflag";
    public static final String ITEM_SETS = "sets";
    public static final String ITEM_INGESTFORMAT = "ingestFormat";

    public static final String TABLENAME_ITEM = "oai_item";

    public Item read(String identifier) throws Exception {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        final StringBuilder selectStmt = new StringBuilder();
        selectStmt.append("SELECT * FROM ");
        selectStmt.append(TABLENAME_ITEM);
        selectStmt.append(" WHERE identifier=?");

        PreparedStatement prepared = session.prepare(selectStmt.toString());

        BoundStatement bound = prepared.bind(identifier);

        ResultSet rs = session.execute(bound);
        Row resultRow = rs.one();
        if (resultRow != null) {
            final Item item = populateItem(resultRow);

            return item;
        }
        return null;
    }

    private Item populateItem(Row resultRow) {
        final Item item = new Item();
        item.setIdentifier(resultRow.getString(ITEM_IDENTIFIER));
        item.setDatestamp(resultRow.getString(ITEM_DATESTAMP));
        item.setDeleteFlag(resultRow.getBool(ITEM_DELETEFLAG));
        item.setSets(resultRow.getList(ITEM_SETS, String.class));
        item.setIngestFormat(resultRow.getString(ITEM_INGESTFORMAT));
        
        
        
        return item;
    }

    public Item create(Item item) throws Exception {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        if (StringUtils.isBlank(item.getIdentifier())) {
            throw new IOException("Item's identifier cannot be empty!");
        }
 
        if (item.isDeleteFlag() == null) {
            item.setDeleteFlag(false);
        }


        StringBuilder insertStmt = new StringBuilder();
        insertStmt.append("INSERT INTO ");
        insertStmt.append(TABLENAME_ITEM);
        insertStmt.append(" (");
        insertStmt.append(ITEM_IDENTIFIER);
        insertStmt.append(", ");
        insertStmt.append(ITEM_DATESTAMP);
        insertStmt.append(", ");
        insertStmt.append(ITEM_DELETEFLAG);
        insertStmt.append(", ");
        insertStmt.append(ITEM_SETS);
        insertStmt.append(", ");
        insertStmt.append(ITEM_INGESTFORMAT);
        insertStmt.append(") VALUES (?, ?, ?, ?)");

        PreparedStatement prepared = session.prepare(insertStmt.toString());

        BoundStatement bound = prepared.bind(item.getIdentifier(), item.getDatestamp(), item.isDeleteFlag(), item.getSets(), item.getIngestFormat());
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
        updateStmt.append("UPDATE ");
        updateStmt.append(TABLENAME_ITEM);
        updateStmt.append(" SET ");
        updateStmt.append(ITEM_DELETEFLAG);
        updateStmt.append("=? WHERE ");
        updateStmt.append(ITEM_IDENTIFIER);
        updateStmt.append("=?");

        PreparedStatement prepared = session.prepare(updateStmt.toString());

        BoundStatement bound = prepared.bind(true, identifier);
        session.execute(bound);
    }
}
