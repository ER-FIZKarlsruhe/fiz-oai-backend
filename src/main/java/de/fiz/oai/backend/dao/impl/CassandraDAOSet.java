package de.fiz.oai.backend.dao.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import de.fiz.oai.backend.dao.DAOSet;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Set;
import de.fiz.oai.backend.utils.ClusterManager;

@Service
public class CassandraDAOSet implements DAOSet {

  public static final String SET_NAME = "name";
  public static final String SET_SPEC = "spec";
  public static final String SET_DESCRIPTION = "description";
  public static final String SET_XPATHS = "xpaths";
  public static final String SET_STATUS = "status";

  public static final String TABLENAME_SET = "oai_set";

  private Map<String, PreparedStatement> preparedStatements = new HashMap<String, PreparedStatement>();

  public Set read(String name) throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    Session session = manager.getCassandraSession();

    PreparedStatement prepared = preparedStatements.get("read");
    if (prepared == null) {
      final StringBuilder selectStmt = new StringBuilder();
      selectStmt.append("SELECT * FROM ");
      selectStmt.append(TABLENAME_SET);
      selectStmt.append(" WHERE name=?");

      prepared = session.prepare(selectStmt.toString());
      preparedStatements.put("read", prepared);
    }
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
    set.setxPaths(resultRow.getMap(SET_XPATHS, String.class, String.class));
    set.setStatus(resultRow.getString(SET_STATUS));
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

    PreparedStatement prepared = preparedStatements.get("create");
    if (prepared == null) {
      StringBuilder insertStmt = new StringBuilder();
      insertStmt.append("INSERT INTO ");
      insertStmt.append(TABLENAME_SET);
      insertStmt.append(" (");
      insertStmt.append(SET_NAME);
      insertStmt.append(", ");
      insertStmt.append(SET_SPEC);
      insertStmt.append(", ");
      insertStmt.append(SET_DESCRIPTION);
      insertStmt.append(", ");
      insertStmt.append(SET_XPATHS);
      insertStmt.append(", ");
      insertStmt.append(SET_STATUS);
      insertStmt.append(") VALUES (?, ?, ?, ?, ?)");

      prepared = session.prepare(insertStmt.toString());
      preparedStatements.put("create", prepared);
    }
    BoundStatement bound = prepared.bind(set.getName(), set.getSpec(), set.getDescription(), set.getxPaths(), set.getStatus());
    session.execute(bound);

    return set;
  }

  public void delete(String name) throws IOException {

    if (StringUtils.isBlank(name)) {
      throw new IOException("Set's name to delete cannot be empty!");
    }

    ClusterManager manager = ClusterManager.getInstance();
    Session session = manager.getCassandraSession();

    PreparedStatement prepared = preparedStatements.get("delete");
    if (prepared == null) {
      StringBuilder deleteStmt = new StringBuilder();
      deleteStmt.append("DELETE FROM ");
      deleteStmt.append(TABLENAME_SET);
      deleteStmt.append(" WHERE ");
      deleteStmt.append(SET_NAME);
      deleteStmt.append("=?");

      prepared = session.prepare(deleteStmt.toString());
      preparedStatements.put("delete", prepared);
    }
    BoundStatement bound = prepared.bind(name);
    ResultSet result = session.execute(bound);

    if (!result.wasApplied()) {
      throw new NotFoundException("The deletion was not applied for the given identifier and format.");
    }
  }
}
