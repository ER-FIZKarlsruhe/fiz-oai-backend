package de.fiz.oai.backend.dao.impl;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.utils.ClusterManager;
import de.fiz.oai.backend.utils.Configuration;

@Service
public class CassandraDAOItem implements DAOItem {

    public static final String ITEM_IDENTIFIER = "identifier";
    public static final String ITEM_DATESTAMP = "datestamp";
    public static final String ITEM_DELETEFLAG = "deleteflag";
    public static final String ITEM_INGESTFORMAT = "ingestFormat";
    public static final String ITEM_TAGS = "tags";
    
    public static final String TABLENAME_ITEM = "oai_item";

    public Item read(String identifier) throws IOException {
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
        item.setDatestamp(Configuration.dateFormat.format(resultRow.getTimestamp(ITEM_DATESTAMP)));
        item.setDeleteFlag(resultRow.getBool(ITEM_DELETEFLAG));
        item.setIngestFormat(resultRow.getString(ITEM_INGESTFORMAT));
        
        
        
        return item;
    }

    public Item create(Item item) throws IOException {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        if (StringUtils.isBlank(item.getIdentifier())) {
            throw new IllegalArgumentException("Item's identifier cannot be empty!");
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
        insertStmt.append(ITEM_TAGS);
        insertStmt.append(", ");
        insertStmt.append(ITEM_INGESTFORMAT);
        insertStmt.append(") VALUES (?, ?, ?, ?, ?)");

        PreparedStatement prepared = session.prepare(insertStmt.toString());

        BoundStatement bound = prepared.bind(item.getIdentifier(), item.getDatestamp(), item.isDeleteFlag(), item.getIngestFormat());
        ResultSet result = session.execute(bound);

        if(!result.wasApplied()) {
          throw new NotFoundException("The creation was not applied for the given item.");
        }
        
        return item;
    }

    //FIXME Move into a service class that has access to cassandra and elasticsearch
    public List<Item> search(Integer offset, Integer rows, String set, String format,String from,String until) {
        return null;
    }

    public void delete(String identifier) throws IOException {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        if (StringUtils.isBlank(identifier)) {
          throw new IllegalArgumentException("identifier cannot be empty!");
      }
        
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
        ResultSet result = session.execute(bound);
        
        if(!result.wasApplied()) {
          throw new NotFoundException("The deletion was not applied for the given identifier and format.");
        }
    }
}
