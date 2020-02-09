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

import de.fiz.oai.backend.dao.DAOContent;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.utils.ClusterManager;

@Service
public class CassandraDAOContent implements DAOContent {

  public static final String CONTENT_IDENTIFIER = "identifier";
  public static final String CONTENT_FORMAT = "format";
  public static final String CONTENT_CONTENT = "content";

  public static final String TABLENAME_CONTENT = "oai_content";

  private Map<String, PreparedStatement> preparedStatements = new HashMap<String, PreparedStatement>();

  public Content read(String identifier, String format) throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    PreparedStatement prepared = preparedStatements.get("read");
    if (prepared == null) {
      final StringBuilder selectStmt = new StringBuilder();
      selectStmt.append("SELECT * FROM ");
      selectStmt.append(TABLENAME_CONTENT);
      selectStmt.append(" WHERE identifier=? AND format=?");

      prepared = session.prepare(selectStmt.toString());
      preparedStatements.put("read", prepared);
    }
    BoundStatement bound = prepared.bind(identifier, format);

    ResultSet rs = session.execute(bound);
    Row resultRow = rs.one();
    if (resultRow != null) {
      final Content content = populateContent(resultRow);

      return content;
    }
    return null;
  }

  @Override
  public List<Content> readFormats(String identifier) throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    PreparedStatement prepared = preparedStatements.get("readFormats");
    if (prepared == null) {
      final StringBuilder selectStmt = new StringBuilder();
      selectStmt.append("SELECT * FROM ");
      selectStmt.append(TABLENAME_CONTENT);
      selectStmt.append(" WHERE identifier=?");

      prepared = session.prepare(selectStmt.toString());
      preparedStatements.put("readFormats", prepared);
    }
    BoundStatement bound = prepared.bind(identifier);

    ResultSet rs = session.execute(bound);
    List<Row> resultRows = rs.all();
    List<Content> contents = new ArrayList<Content>();
    if (resultRows != null) {
      for (Row currentRow : resultRows) {
        final Content content = populateContent(currentRow);
        contents.add(content);
      }

      return contents;
    }
    return null;
  }

  private Content populateContent(Row resultRow) {
    final Content content = new Content();
    content.setIdentifier(resultRow.getString(CONTENT_IDENTIFIER));
    content.setFormat(resultRow.getString(CONTENT_FORMAT));
    content.setContent(new String(resultRow.getByteBuffer(CONTENT_CONTENT).array()));

    return content;
  }

  public Content create(Content content) throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    if (StringUtils.isBlank(content.getIdentifier())) {
      throw new IOException("Contents name cannot be empty!");
    }

    if (StringUtils.isBlank(content.getFormat())) {
      throw new IOException("Contents format cannot be empty!");
    }

    if (content.getContent() == null || content.getContent().isEmpty()) {
      throw new IOException("Contents value cannot be empty!");
    }

    PreparedStatement prepared = preparedStatements.get("create");
    if (prepared == null) {
      StringBuilder insertStmt = new StringBuilder();
      insertStmt.append("INSERT INTO ");
      insertStmt.append(TABLENAME_CONTENT);
      insertStmt.append(" (");
      insertStmt.append(CONTENT_IDENTIFIER);
      insertStmt.append(", ");
      insertStmt.append(CONTENT_FORMAT);
      insertStmt.append(", ");
      insertStmt.append(CONTENT_CONTENT);
      insertStmt.append(") VALUES (?, ?, ?)");

      prepared = session.prepare(insertStmt.toString());
      preparedStatements.put("create", prepared);
    }
    ByteBuffer buffer = ByteBuffer.wrap(content.getContent().getBytes());

    BoundStatement bound = prepared.bind(content.getIdentifier(), content.getFormat(), buffer);
    session.execute(bound);

    return content;
  }

  public void delete(String identifier, String format) throws IOException {

    if (StringUtils.isBlank(identifier)) {
      throw new IOException("Content identifier to delete cannot be empty!");
    }

    if (StringUtils.isBlank(format)) {
      throw new IOException("Content format to delete cannot be empty!");
    }

    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    PreparedStatement prepared = preparedStatements.get("delete");
    if (prepared == null) {
      StringBuilder deleteStmt = new StringBuilder();
      deleteStmt.append("DELETE FROM ");
      deleteStmt.append(TABLENAME_CONTENT);
      deleteStmt.append(" WHERE ");
      deleteStmt.append(CONTENT_IDENTIFIER);
      deleteStmt.append("=?");
      deleteStmt.append(" AND ");
      deleteStmt.append(CONTENT_FORMAT);
      deleteStmt.append("=?");

      prepared = session.prepare(deleteStmt.toString());
      preparedStatements.put("delete", prepared);
    }

    BoundStatement bound = prepared.bind(identifier, format);
    ResultSet result = session.execute(bound);

    if (!result.wasApplied()) {
      throw new NotFoundException("The deletion was not applied for the given identifier and format.");
    }
  }

}
