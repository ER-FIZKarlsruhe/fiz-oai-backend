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
    public static final String SET_SPEC = "spec";
    public static final String SET_DESCRIPTION = "description";

    public static final String TABLENAME_SET = "oai_set";

    public Set read(String name) throws IOException {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        final StringBuilder selectStmt = new StringBuilder();
        selectStmt.append("SELECT * FROM ");
        selectStmt.append(TABLENAME_SET);
        selectStmt.append(" WHERE name=?");

        PreparedStatement prepared = session.prepare(selectStmt.toString());

        BoundStatement bound = prepared.bind(name);

        ResultSet rs = session.execute(bound);
        Row resultRow = rs.one();
        if (resultRow != null) {
            final Set set = populateSet(resultRow);

            return set;
        }
        return null;
    }

    private Set populateSet(Row resultRow) {
        final Set set = new Set();
        set.setSpec(resultRow.getString(SET_SPEC));
        set.setName(resultRow.getString(SET_NAME));
        set.setDescription(resultRow.getString(SET_DESCRIPTION));
        return set;
    }

    public List<Set> readAll() throws IOException {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        final List<Set> allSets = new ArrayList<Set>();

        String query = "SELECT * FROM " + TABLENAME_SET;
        ResultSet rs = session.execute(query);
        for (final Row row : rs) {
            final Set set = populateSet(row);

            allSets.add(set);
        }

        return allSets;
    }

    public Set create(Set set) throws IOException {
        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        if (StringUtils.isBlank(set.getName())) {
            throw new IOException("Set's name cannot be empty!");
        }

        StringBuilder insertStmt = new StringBuilder();
        insertStmt.append("INSERT INTO ");
        insertStmt.append(TABLENAME_SET);
        insertStmt.append(" (");
        insertStmt.append(SET_NAME);
        insertStmt.append(", ");
        insertStmt.append(SET_SPEC);
        insertStmt.append(", ");
        insertStmt.append(SET_DESCRIPTION);
        insertStmt.append(") VALUES (?, ?, ?)");

        PreparedStatement prepared = session.prepare(insertStmt.toString());

        BoundStatement bound = prepared.bind(set.getName(), set.getSpec(), set.getDescription());
        session.execute(bound);

        return set;
    }

    public void delete(String name) throws IOException {

        if (StringUtils.isBlank(name)) {
            throw new IOException("Set's name to delete cannot be empty!");
        }

        ClusterManager manager = ClusterManager.getInstance();
        Session session = manager.getCassandraSession();

        StringBuilder deleteStmt = new StringBuilder();
        deleteStmt.append("DELETE FROM ");
        deleteStmt.append(TABLENAME_SET);
        deleteStmt.append(" WHERE ");
        deleteStmt.append(SET_NAME);
        deleteStmt.append("=?");

        PreparedStatement prepared = session.prepare(deleteStmt.toString());

        BoundStatement bound = prepared.bind(name);
        session.execute(bound);
    }
}
