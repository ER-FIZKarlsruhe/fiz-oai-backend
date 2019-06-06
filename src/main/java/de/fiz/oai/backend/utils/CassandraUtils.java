package de.fiz.oai.backend.utils;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.dao.impl.CassandraDAOItem;
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
        session.execute("USE " + keyspace);
        session.execute("CREATE TABLE oai_item (uuid timeuuid, identifier text, deletedflag boolean, content text, PRIMARY KEY (uuid));");
        session.execute("CREATE INDEX IF NOT EXISTS oai_item_identifier_idx ON oai_item (identifier)");
    }



}