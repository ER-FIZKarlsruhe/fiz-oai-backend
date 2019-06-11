package de.fiz.oai.backend.utils;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.dao.impl.CassandraDAOFormat;
import de.fiz.oai.backend.dao.impl.CassandraDAOItem;
import de.fiz.oai.backend.dao.impl.CassandraDAOSet;
import org.apache.commons.lang3.StringUtils;

public class CassandraUtils {

    public static String getClusterTopologyInformation(Session session) {
        StringBuilder resultBuilder = new StringBuilder();
        String query = "SELECT * FROM system.peers;";
        ResultSet rs = session.execute(query);
        for (Row row : rs.all()) {
            resultBuilder.append(row.getInet("peer") + " | ");
            resultBuilder.append(row.getString("data_center") + "\n");
        }
        return resultBuilder.toString();
    }

    public static void createKeyspace(Session session, String replicationFactor, String keyspace) throws Exception {
        if (StringUtils.isBlank(replicationFactor)) {
            throw new Exception("Cannot create keyspace " + keyspace + " because the property cassandra.replication.factor is not set.");
        }

        final StringBuilder createStmt = new StringBuilder();
        createStmt.append("CREATE KEYSPACE IF NOT EXISTS ");
        createStmt.append(keyspace);
        createStmt.append(" WITH REPLICATION = ");
        createStmt.append(replicationFactor);

        session.execute(createStmt.toString());

        // Create tables
        final StringBuilder useStmt = new StringBuilder();
        useStmt.append("USE ");
        useStmt.append(keyspace);
        session.execute(useStmt.toString());

        final StringBuilder createTableItemStmt = new StringBuilder();
        createTableItemStmt.append("CREATE TABLE ");
        createTableItemStmt.append(CassandraDAOItem.TABLENAME_ITEM);
        createTableItemStmt.append(" (");
        createTableItemStmt.append(CassandraDAOItem.ITEM_UUID);
        createTableItemStmt.append(" timeuuid, ");
        createTableItemStmt.append(CassandraDAOItem.ITEM_IDENTIFIER);
        createTableItemStmt.append(" text, ");
        createTableItemStmt.append(CassandraDAOItem.ITEM_DELETEDFLAG);
        createTableItemStmt.append(" boolean, ");
        createTableItemStmt.append(CassandraDAOItem.ITEM_CONTENT);
        createTableItemStmt.append(" text, PRIMARY KEY (");
        createTableItemStmt.append(CassandraDAOItem.ITEM_UUID);
        createTableItemStmt.append("));");
        session.execute(createTableItemStmt.toString());

        final StringBuilder createIndexItemStmt = new StringBuilder();
        createIndexItemStmt.append("CREATE INDEX IF NOT EXISTS oai_item_identifier_idx ON ");
        createIndexItemStmt.append(CassandraDAOItem.TABLENAME_ITEM);
        createIndexItemStmt.append(" (");
        createIndexItemStmt.append(CassandraDAOItem.ITEM_IDENTIFIER);
        createIndexItemStmt.append(")");
        session.execute(createIndexItemStmt.toString());

        final StringBuilder createTableSetStmt = new StringBuilder();
        createTableSetStmt.append("CREATE TABLE ");
        createTableSetStmt.append(CassandraDAOSet.TABLENAME_SET);
        createTableSetStmt.append(" (");
        createTableSetStmt.append(CassandraDAOSet.SET_NAME);
        createTableSetStmt.append(" text, ");
        createTableSetStmt.append(CassandraDAOSet.SET_SEARCHURL);
        createTableSetStmt.append(" text, ");
        createTableSetStmt.append(CassandraDAOSet.SET_IDENTIFIERSELECTOR);
        createTableSetStmt.append(" text, PRIMARY KEY (");
        createTableSetStmt.append(CassandraDAOSet.SET_NAME);
        createTableSetStmt.append("));");
        session.execute(createTableSetStmt.toString());

        final StringBuilder createTableFormatStmt = new StringBuilder();
        createTableFormatStmt.append("CREATE TABLE ");
        createTableFormatStmt.append(CassandraDAOFormat.TABLENAME_FORMAT);
        createTableFormatStmt.append(" (");
        createTableFormatStmt.append(CassandraDAOFormat.FORMAT_METADATAPREFIX);
        createTableFormatStmt.append(" text, ");
        createTableFormatStmt.append(CassandraDAOFormat.FORMAT_SCHEMALOCATION);
        createTableFormatStmt.append(" text, ");
        createTableFormatStmt.append(CassandraDAOFormat.FORMAT_SCHEMANAMESPACE);
        createTableFormatStmt.append(" text, ");
        createTableFormatStmt.append(CassandraDAOFormat.FORMAT_CROSSWALKSTYLESHEET);
        createTableFormatStmt.append(" text, ");
        createTableFormatStmt.append(CassandraDAOFormat.FORMAT_IDENTIFIERXPATH);
        createTableFormatStmt.append(" text, PRIMARY KEY (");
        createTableFormatStmt.append(CassandraDAOFormat.FORMAT_METADATAPREFIX);
        createTableFormatStmt.append("));");
        session.execute(createTableFormatStmt.toString());
    }



}