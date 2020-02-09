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

import de.fiz.oai.backend.dao.DAOFormat;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Format;
import de.fiz.oai.backend.utils.ClusterManager;

@Service
public class CassandraDAOFormat implements DAOFormat {

  public static final String FORMAT_METADATAPREFIX = "metadataprefix";
  public static final String FORMAT_SCHEMALOCATION = "schemalocation";
  public static final String FORMAT_SCHEMANAMESPACE = "schemanamespace";
  public static final String FORMAT_IDENTIFIERXPATH = "identifierxpath";
  public static final String FORMAT_STATUS = "status";

  public static final String TABLENAME_FORMAT = "oai_format";

  private Map<String, PreparedStatement> preparedStatements = new HashMap<String, PreparedStatement>();

  public Format read(String metadataPrefix) throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    PreparedStatement prepared = preparedStatements.get("read");
    if (prepared == null) {
      final StringBuilder selectStmt = new StringBuilder();
      selectStmt.append("SELECT * FROM ");
      selectStmt.append(TABLENAME_FORMAT);
      selectStmt.append(" WHERE metadataprefix=?");

      prepared = session.prepare(selectStmt.toString());
      preparedStatements.put("read", prepared);
    }
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
    CqlSession session = manager.getCassandraSession();

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
    format.setStatus(row.getString(FORMAT_STATUS));
    return format;
  }

  public Format create(Format format) throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    if (StringUtils.isBlank(format.getMetadataPrefix())) {
      throw new IOException("Format's MetadataPrefix cannot be empty!");
    }

    PreparedStatement prepared = preparedStatements.get("create");
    if (prepared == null) {
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
      insertStmt.append(", ");
      insertStmt.append(FORMAT_STATUS);
      insertStmt.append(") VALUES (?, ?, ?, ?, ?)");

      prepared = session.prepare(insertStmt.toString());
      preparedStatements.put("create", prepared);
    }
    BoundStatement bound = prepared.bind(format.getIdentifierXpath(), format.getMetadataPrefix(),
        format.getSchemaLocation(), format.getSchemaNamespace());
    session.execute(bound);

    return format;
  }

  public void delete(String metadataPrefix) throws IOException {

    if (StringUtils.isBlank(metadataPrefix)) {
      throw new IOException("Format's MetadataPrefix to delete cannot be empty!");
    }

    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    PreparedStatement prepared = preparedStatements.get("delete");
    if (prepared == null) {
      StringBuilder deleteStmt = new StringBuilder();
      deleteStmt.append("DELETE FROM ");
      deleteStmt.append(TABLENAME_FORMAT);
      deleteStmt.append(" WHERE ");
      deleteStmt.append(FORMAT_METADATAPREFIX);
      deleteStmt.append("=?");

      prepared = session.prepare(deleteStmt.toString());
      preparedStatements.put("delete", prepared);
    }
    BoundStatement bound = prepared.bind(metadataPrefix);
    ResultSet result = session.execute(bound);

    if (!result.wasApplied()) {
      throw new NotFoundException("The deletion was not applied for the given identifier and format.");
    }
  }
}
