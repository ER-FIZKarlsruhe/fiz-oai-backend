package de.fiz.oai.backend.utils;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

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

}