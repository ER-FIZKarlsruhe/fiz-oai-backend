package de.fiz.oai.backend.dao.impl;

import com.datastax.driver.core.*;
import de.fiz.oai.backend.dao.DAOFormat;
import de.fiz.oai.backend.models.Format;
import de.fiz.oai.backend.models.Set;
import de.fiz.oai.backend.utils.ClusterManager;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CassandraDAOFormat implements DAOFormat {

    public static final String FORMAT_METADATAPREFIX = "metadataprefix";
    public static final String FORMAT_SCHEMALOCATION = "schemalocation";
    public static final String FORMAT_SCHEMANAMESPACE = "schemanamespace";
    public static final String FORMAT_CROSSWALKSTYLESHEET = "crosswalkstylesheet";
    public static final String FORMAT_IDENTIFIERXPATH = "identifierxpath";

    public Format read(String metadataPrefix) throws Exception {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        String query = "SELECT * FROM oai_format WHERE metadataprefix='" + metadataPrefix + "'";
        ResultSet rs = session.execute(query);
        Row resultRow = rs.one();
        if (resultRow != null) {
            final Format format = populateFormat(resultRow);

            return format;
        }
        return null;
    }
    public List<Format> readAll() throws Exception {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        final List<Format> allFormats = new ArrayList<Format>();

        String query = "SELECT * FROM oai_format";
        ResultSet rs = session.execute(query);
        for (final Row row : rs) {
            final Format format = populateFormat(row);

            allFormats.add(format);
        }

        return allFormats;
    }

    private Format populateFormat(Row row) {
        final Format format = new Format();
        format.setCrosswalkStyleSheet(row.getString(FORMAT_CROSSWALKSTYLESHEET));
        format.setIdentifierXpath(row.getString(FORMAT_IDENTIFIERXPATH));
        format.setMetadataPrefix(row.getString(FORMAT_METADATAPREFIX));
        format.setSchemaLocation(row.getString(FORMAT_SCHEMALOCATION));
        format.setSchemaNamespace(row.getString(FORMAT_SCHEMANAMESPACE));
        return format;
    }

    public Format create(Format format) throws Exception {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        if (StringUtils.isBlank(format.getMetadataPrefix())) {
            throw new IOException("Format's MetadataPrefix cannot be empty!");
        }

        StringBuilder insertStmt = new StringBuilder();
        insertStmt.append("INSERT INTO oai_format (");
        insertStmt.append(FORMAT_CROSSWALKSTYLESHEET);
        insertStmt.append(", ");
        insertStmt.append(FORMAT_IDENTIFIERXPATH);
        insertStmt.append(", ");
        insertStmt.append(FORMAT_METADATAPREFIX);
        insertStmt.append(", ");
        insertStmt.append(FORMAT_SCHEMALOCATION);
        insertStmt.append(", ");
        insertStmt.append(FORMAT_SCHEMANAMESPACE);
        insertStmt.append(") VALUES (?, ?, ?, ?, ?)");

        PreparedStatement prepared = session.prepare(insertStmt.toString());

        BoundStatement bound = prepared.bind(format.getCrosswalkStyleSheet(), format.getIdentifierXpath(), format.getMetadataPrefix(), format.getSchemaLocation(), format.getSchemaNamespace());
        session.execute(bound);

        return format;
    }

    public void delete(String metadataPrefix) throws Exception {

        if (StringUtils.isBlank(metadataPrefix)) {
            throw new IOException("Format's MetadataPrefix to delete cannot be empty!");
        }

        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        StringBuilder deleteStmt = new StringBuilder();
        deleteStmt.append("DELETE FROM oai_format WHERE ");
        deleteStmt.append(FORMAT_METADATAPREFIX);
        deleteStmt.append("=?");

        PreparedStatement prepared = session.prepare(deleteStmt.toString());

        BoundStatement bound = prepared.bind(metadataPrefix);
        session.execute(bound);
    }
}
