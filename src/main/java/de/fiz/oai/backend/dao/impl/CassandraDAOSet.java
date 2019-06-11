package de.fiz.oai.backend.dao.impl;

import com.datastax.driver.core.*;
import com.datastax.driver.core.utils.UUIDs;
import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.dao.DAOSet;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.Set;
import de.fiz.oai.backend.utils.ClusterManager;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CassandraDAOSet implements DAOSet {

    public static final String SET_NAME = "name";
    public static final String SET_SEARCHURL = "searchurl";
    public static final String SET_IDENTIFIERSELECTOR = "identifierselector";

    public Set read(String name) throws Exception {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        String query = "SELECT * FROM oai_set WHERE name='" + name + "'";
        ResultSet rs = session.execute(query);
        Row resultRow = rs.one();
        if (resultRow != null) {
            final Set set = populateSet(resultRow);

            return set;
        }
        return null;
    }

    private Set populateSet(Row resultRow) {
        final Set set = new Set();
        set.setIdentifierSelector(resultRow.getString(SET_IDENTIFIERSELECTOR));
        set.setName(resultRow.getString(SET_NAME));
        set.setSearchUrl(resultRow.getString(SET_SEARCHURL));
        return set;
    }

    public List<Set> readAll() throws Exception {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        final List<Set> allSets = new ArrayList<Set>();

        String query = "SELECT * FROM oai_set";
        ResultSet rs = session.execute(query);
        for (final Row row : rs) {
            final Set set = populateSet(row);

            allSets.add(set);
        }

        return allSets;
    }

    public Set create(Set set) throws Exception {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        if (StringUtils.isBlank(set.getName())) {
            throw new IOException("Set's name cannot be empty!");
        }

        StringBuilder insertStmt = new StringBuilder();
        insertStmt.append("INSERT INTO oai_set (");
        insertStmt.append(SET_NAME);
        insertStmt.append(", ");
        insertStmt.append(SET_SEARCHURL);
        insertStmt.append(", ");
        insertStmt.append(SET_IDENTIFIERSELECTOR);
        insertStmt.append(") VALUES (?, ?, ?)");

        PreparedStatement prepared = session.prepare(insertStmt.toString());

        BoundStatement bound = prepared.bind(set.getName(), set.getSearchUrl(), set.getIdentifierSelector());
        session.execute(bound);

        return set;
    }

    public void delete(String name) throws Exception {

        if (StringUtils.isBlank(name)) {
            throw new IOException("Set's name to delete cannot be empty!");
        }

        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        StringBuilder deleteStmt = new StringBuilder();
        deleteStmt.append("DELETE FROM oai_set WHERE ");
        deleteStmt.append(SET_NAME);
        deleteStmt.append("=?");

        PreparedStatement prepared = session.prepare(deleteStmt.toString());

        BoundStatement bound = prepared.bind(name);
        session.execute(bound);
    }
}
