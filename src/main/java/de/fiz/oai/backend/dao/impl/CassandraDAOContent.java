package de.fiz.oai.backend.dao.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import de.fiz.oai.backend.dao.DAOContent;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.utils.ClusterManager;

public class CassandraDAOContent implements DAOContent {

    public static final String CONTENT_IDENTIFIER = "identifier";
    public static final String CONTENT_FORMAT = "format";
    public static final String CONTENT_CONTENT = "content";

    public static final String TABLENAME_CONTENT = "oai_content";

    public Content read(String identifier, String format) throws IOException {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        final StringBuilder selectStmt = new StringBuilder();
        selectStmt.append("SELECT * FROM ");
        selectStmt.append(TABLENAME_CONTENT);
        selectStmt.append(" WHERE identifier=? AND format=?");

        PreparedStatement prepared = session.prepare(selectStmt.toString());

        BoundStatement bound = prepared.bind(identifier, format);

        ResultSet rs = session.execute(bound);
        Row resultRow = rs.one();
        if (resultRow != null) {
            final Content content = populateContent(resultRow);

            return content;
        }
        return null;
    }

    
    @Override
    public List<Content> readFormats(String identifier) throws IOException {
      ClusterManager manager = ClusterManager.getInstance();
      Session session = manager.getCassandraSession();

      final StringBuilder selectStmt = new StringBuilder();
      selectStmt.append("SELECT * FROM ");
      selectStmt.append(TABLENAME_CONTENT);
      selectStmt.append(" WHERE identifier=?");

      PreparedStatement prepared = session.prepare(selectStmt.toString());

      BoundStatement bound = prepared.bind(identifier);

      ResultSet rs = session.execute(bound);
      List<Row> resultRows = rs.all();
      List<Content> contents = new ArrayList<Content>();
      if (resultRows != null) {
        for(Row currentRow: resultRows) {
          final Content content = populateContent(currentRow);
          contents.add(content);
        }
        
        return contents;
      }
      return null;
    }
    
    
    private Content populateContent(Row resultRow) {
        final Content content = new Content();
        content.setIdentifier(resultRow.getString(CONTENT_IDENTIFIER));
        content.setFormat(resultRow.getString(CONTENT_FORMAT));
        content.setContent(resultRow.getBytes(CONTENT_CONTENT).array());
        
        return content;
    }



    public Content create(Content content) throws IOException {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        if (StringUtils.isBlank(content.getIdentifier())) {
            throw new IOException("Contents name cannot be empty!");
        }
        
        if (StringUtils.isBlank(content.getFormat())) {
          throw new IOException("Contents format cannot be empty!");
        }

        if (content.getContent() == null || content.getContent().length == 0) {
          throw new IOException("Contents value cannot be empty!");
        }

        
        StringBuilder insertStmt = new StringBuilder();
        insertStmt.append("INSERT INTO ");
        insertStmt.append(TABLENAME_CONTENT);
        insertStmt.append(" (");
        insertStmt.append(CONTENT_IDENTIFIER);
        insertStmt.append(", ");
        insertStmt.append(CONTENT_FORMAT);
        insertStmt.append(", ");
        insertStmt.append(CONTENT_CONTENT);
        insertStmt.append(") VALUES (?, ?, ?)");

        PreparedStatement prepared = session.prepare(insertStmt.toString());

        BoundStatement bound = prepared.bind(content.getIdentifier(), content.getFormat(), content.getContent());
        session.execute(bound);

        return content;
    }

    public void delete(String identifier, String format) throws IOException {

        if (StringUtils.isBlank(identifier)) {
            throw new IOException("Content identifier to delete cannot be empty!");
        }

        if (StringUtils.isBlank(format)) {
          throw new IOException("Content format to delete cannot be empty!");
      }
        
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        StringBuilder deleteStmt = new StringBuilder();
        deleteStmt.append("DELETE FROM ");
        deleteStmt.append(TABLENAME_CONTENT);
        deleteStmt.append(" WHERE ");
        deleteStmt.append(CONTENT_IDENTIFIER);
        deleteStmt.append("=?");
        deleteStmt.append(" AND ");
        deleteStmt.append(CONTENT_FORMAT);
        deleteStmt.append("=?");
        
        PreparedStatement prepared = session.prepare(deleteStmt.toString());

        BoundStatement bound = prepared.bind(identifier, format);
        ResultSet result = session.execute(bound);
        
        if(!result.wasApplied()) {
          throw new NotFoundException("The deletion was not applied for the given identifier and format.");
        }
    }


}
