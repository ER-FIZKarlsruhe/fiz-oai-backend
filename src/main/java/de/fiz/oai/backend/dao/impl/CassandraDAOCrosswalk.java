/*
 * Copyright 2019 FIZ Karlsruhe - Leibniz-Institut fuer Informationsinfrastruktur GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fiz.oai.backend.dao.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.session.Session;

import de.fiz.oai.backend.dao.DAOCrosswalk;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Crosswalk;
import de.fiz.oai.backend.utils.ClusterManager;

@Service
public class CassandraDAOCrosswalk implements DAOCrosswalk {

  public static final String CROSSWALK_NAME = "name";
  public static final String CROSSWALK_FORMAT_FROM = "formatFrom";
  public static final String CROSSWALK_FORMAT_TO = "formatTo";
  public static final String CROSSWALK_XSLT_STYLESHEET = "xsltStylesheet";

  public static final String TABLENAME_CROSSWALK = "oai_crosswalk";

  private Map<String, PreparedStatement> preparedStatements = new HashMap<String, PreparedStatement>();

  @Override
  public Crosswalk read(String metadataPrefix) throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    PreparedStatement prepared = preparedStatements.get("read");
    if (prepared == null) {
      final StringBuilder selectStmt = new StringBuilder();
      selectStmt.append("SELECT * FROM ");
      selectStmt.append(TABLENAME_CROSSWALK);
      selectStmt.append(" WHERE name=?");

      prepared = session.prepare(selectStmt.toString());
      preparedStatements.put("read", prepared);
    }
    BoundStatement bound = prepared.bind(metadataPrefix);

    ResultSet rs = session.execute(bound);
    Row resultRow = rs.one();
    if (resultRow != null) {
      final Crosswalk crosswalk = populateFormat(resultRow);

      return crosswalk;
    }
    return null;
  }

  @Override
  public List<Crosswalk> readAll() throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    final List<Crosswalk> allCrosswalks = new ArrayList<Crosswalk>();

    String query = "SELECT * FROM " + TABLENAME_CROSSWALK;
    ResultSet rs = session.execute(query);

    for (final Row row : rs) {
      final Crosswalk format = populateFormat(row);

      allCrosswalks.add(format);
    }

    return allCrosswalks;
  }

  private Crosswalk populateFormat(Row row) {
    final Crosswalk crosswalk = new Crosswalk();
    crosswalk.setName(row.getString(CROSSWALK_NAME));
    crosswalk.setFormatFrom(row.getString(CROSSWALK_FORMAT_FROM));
    crosswalk.setFormatTo(row.getString(CROSSWALK_FORMAT_TO));
    crosswalk.setXsltStylesheet(new String(row.getByteBuffer(CROSSWALK_XSLT_STYLESHEET).array()));
    return crosswalk;
  }

  @Override
  public Crosswalk create(Crosswalk crosswalk) throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    if (StringUtils.isBlank(crosswalk.getName())) {
      throw new IOException("Crosswalk name cannot be empty!");
    }

    PreparedStatement prepared = preparedStatements.get("create");
    if (prepared == null) {
      StringBuilder insertStmt = new StringBuilder();
      insertStmt.append("INSERT INTO ");
      insertStmt.append(TABLENAME_CROSSWALK);
      insertStmt.append(" (");
      insertStmt.append(CROSSWALK_NAME);
      insertStmt.append(", ");
      insertStmt.append(CROSSWALK_FORMAT_FROM);
      insertStmt.append(", ");
      insertStmt.append(CROSSWALK_FORMAT_TO);
      insertStmt.append(", ");
      insertStmt.append(CROSSWALK_XSLT_STYLESHEET);
      insertStmt.append(") VALUES (?, ?, ?, ?)");

      prepared = session.prepare(insertStmt.toString());
      preparedStatements.put("create", prepared);
    }
    ByteBuffer buffer = ByteBuffer.wrap(crosswalk.getXsltStylesheet().getBytes());

    BoundStatement bound = prepared.bind(crosswalk.getName(), crosswalk.getFormatFrom(), crosswalk.getFormatTo(),
        buffer);
    session.execute(bound);

    return crosswalk;
  }

  @Override
  public void delete(String name) throws IOException {

    if (StringUtils.isBlank(name)) {
      throw new IOException("Format name must not be empty!");
    }

    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    PreparedStatement prepared = preparedStatements.get("delete");
    if (prepared == null) {
      StringBuilder deleteStmt = new StringBuilder();
      deleteStmt.append("DELETE FROM ");
      deleteStmt.append(TABLENAME_CROSSWALK);
      deleteStmt.append(" WHERE ");
      deleteStmt.append(CROSSWALK_NAME);
      deleteStmt.append("=?");

      prepared = session.prepare(deleteStmt.toString());
      preparedStatements.put("delete", prepared);
    }
    BoundStatement bound = prepared.bind(name);
    ResultSet result = session.execute(bound);

    if (!result.wasApplied()) {
      throw new NotFoundException("The deletion was not applied for the given name.");
    }
  }
}
