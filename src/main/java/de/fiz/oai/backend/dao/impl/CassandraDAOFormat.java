package de.fiz.oai.backend.dao.impl;

import com.datastax.driver.core.*;
import de.fiz.oai.backend.dao.DAOFormat;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Format;
import de.fiz.oai.backend.utils.ClusterManager;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CassandraDAOFormat implements DAOFormat {

    public static final String FORMAT_METADATAPREFIX = "metadataprefix";
    public static final String FORMAT_SCHEMALOCATION = "schemalocation";
    public static final String FORMAT_SCHEMANAMESPACE = "schemanamespace";
    public static final String FORMAT_IDENTIFIERXPATH = "identifierxpath";

    public static final String TABLENAME_FORMAT = "oai_format";

    public Format read(String metadataPrefix) throws IOException {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        final StringBuilder selectStmt = new StringBuilder();
        selectStmt.append("SELECT * FROM ");
        selectStmt.append(TABLENAME_FORMAT);
        selectStmt.append(" WHERE metadataprefix=?");

        PreparedStatement prepared = session.prepare(selectStmt.toString());

        BoundStatement bound = prepared.bind(metadataPrefix);

        ResultSet rs = session.execute(bound);
        Row resultRow = rs.one();
        if (resultRow != null) {
            final Format format = populateFormat(resultRow);

            return format;
        }
        return null;
    }
    
    
    public List<Format> readAll() throws IOException {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        final List<Format> allFormats = new ArrayList<Format>();

        String query = "SELECT * FROM " + TABLENAME_FORMAT;
        ResultSet rs = session.execute(query);
        for (final Row row : rs) {
            final Format format = populateFormat(row);

            allFormats.add(format);
        }

        return allFormats;
    }

    private Format populateFormat(Row row) {
        final Format format = new Format();
        format.setIdentifierXpath(row.getString(FORMAT_IDENTIFIERXPATH));
        format.setMetadataPrefix(row.getString(FORMAT_METADATAPREFIX));
        format.setSchemaLocation(row.getString(FORMAT_SCHEMALOCATION));
        format.setSchemaNamespace(row.getString(FORMAT_SCHEMANAMESPACE));
        return format;
    }

    public Format create(Format format) throws IOException {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        if (StringUtils.isBlank(format.getMetadataPrefix())) {
            throw new IOException("Format's MetadataPrefix cannot be empty!");
        }

        StringBuilder insertStmt = new StringBuilder();
        insertStmt.append("INSERT INTO ");
        insertStmt.append(TABLENAME_FORMAT);
        insertStmt.append(" (");
        insertStmt.append(FORMAT_IDENTIFIERXPATH);
        insertStmt.append(", ");
        insertStmt.append(FORMAT_METADATAPREFIX);
        insertStmt.append(", ");
        insertStmt.append(FORMAT_SCHEMALOCATION);
        insertStmt.append(", ");
        insertStmt.append(FORMAT_SCHEMANAMESPACE);
        insertStmt.append(") VALUES (?, ?, ?, ?, ?)");

        PreparedStatement prepared = session.prepare(insertStmt.toString());

        BoundStatement bound = prepared.bind(format.getIdentifierXpath(), format.getMetadataPrefix(), format.getSchemaLocation(), format.getSchemaNamespace());
        session.execute(bound);

        return format;
    }

    public void delete(String metadataPrefix) throws IOException {

        if (StringUtils.isBlank(metadataPrefix)) {
            throw new IOException("Format's MetadataPrefix to delete cannot be empty!");
        }

        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        StringBuilder deleteStmt = new StringBuilder();
        deleteStmt.append("DELETE FROM ");
        deleteStmt.append(TABLENAME_FORMAT);
        deleteStmt.append(" WHERE ");
        deleteStmt.append(FORMAT_METADATAPREFIX);
        deleteStmt.append("=?");

        PreparedStatement prepared = session.prepare(deleteStmt.toString());

        BoundStatement bound = prepared.bind(metadataPrefix);
        ResultSet result = session.execute(bound);
        
        if(!result.wasApplied()) {
          throw new NotFoundException("The deletion was not applied for the given identifier and format.");
        }
    }
}
