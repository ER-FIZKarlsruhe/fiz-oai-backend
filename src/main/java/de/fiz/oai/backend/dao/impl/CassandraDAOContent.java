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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

import de.fiz.oai.backend.dao.DAOContent;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.utils.ClusterManager;

@Service
public class CassandraDAOContent implements DAOContent {

  private static Logger LOGGER = LoggerFactory.getLogger(CassandraDAOContent.class);

  public static final String CONTENT_IDENTIFIER = "identifier";
  public static final String CONTENT_FORMAT = "format";
  public static final String CONTENT_CONTENT = "content";

  public static final String TABLENAME_CONTENT = "oai_content";

  // Bounds how many concurrent executeAsync() requests a single batched read fires at once, so a
  // large identifier list can't fan out into thousands of simultaneous in-flight requests and
  // overwhelm the connection/driver (DataStax recommends bounded concurrency over unbounded fan-out).
  private static final int MAX_CONCURRENT_ASYNC_READS = 50;

  private final Map<String, PreparedStatement> preparedStatements = new ConcurrentHashMap<>();

  public Content read(String identifier, String format) throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    PreparedStatement prepared = getOrPrepareRead(session);
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
  public Map<String, Content> read(Map<String, String> identifierToFormat) throws IOException {
    Map<String, Content> result = new LinkedHashMap<>();
    if (MapUtils.isEmpty(identifierToFormat)) {
      return result;
    }

    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    PreparedStatement prepared = getOrPrepareRead(session);

    // Fire each chunk concurrently, then join, instead of one blocking round trip per identifier -
    // capped at MAX_CONCURRENT_ASYNC_READS in flight at a time instead of unbounded fan-out.
    List<Map.Entry<String, String>> entries = new ArrayList<>(identifierToFormat.entrySet());
    for (int start = 0; start < entries.size(); start += MAX_CONCURRENT_ASYNC_READS) {
      List<Map.Entry<String, String>> chunk = entries.subList(start, Math.min(start + MAX_CONCURRENT_ASYNC_READS, entries.size()));

      Map<String, CompletionStage<AsyncResultSet>> pending = new LinkedHashMap<>();
      for (Map.Entry<String, String> entry : chunk) {
        pending.put(entry.getKey(), session.executeAsync(prepared.bind(entry.getKey(), entry.getValue())));
      }

      for (Map.Entry<String, CompletionStage<AsyncResultSet>> entry : pending.entrySet()) {
        AsyncResultSet rs = joinUnwrapped(entry.getValue());
        Row resultRow = rs.one();
        if (resultRow != null) {
          result.put(entry.getKey(), populateContent(resultRow));
        }
      }
    }

    return result;
  }

  private PreparedStatement getOrPrepareRead(CqlSession session) {
    return preparedStatements.computeIfAbsent("read",
        key -> session.prepare("SELECT * FROM " + TABLENAME_CONTENT + " WHERE identifier=? AND format=?"));
  }

  /**
   * Unwraps CompletionException so an async-request failure surfaces the same exception type a
   * blocking session.execute() call would have thrown, instead of always a CompletionException.
   */
  private static <T> T joinUnwrapped(CompletionStage<T> stage) {
    try {
      return stage.toCompletableFuture().join();
    } catch (CompletionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw e;
    }
  }

  @Override
  public List<Content> readFormats(String identifier) throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    PreparedStatement prepared = preparedStatements.computeIfAbsent("readFormats",
        key -> session.prepare("SELECT * FROM " + TABLENAME_CONTENT + " WHERE identifier=?"));
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

    PreparedStatement prepared = preparedStatements.computeIfAbsent("create", key -> {
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

      return session.prepare(insertStmt.toString());
    });
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

    PreparedStatement prepared = preparedStatements.computeIfAbsent("delete", key -> {
      StringBuilder deleteStmt = new StringBuilder();
      deleteStmt.append("DELETE FROM ");
      deleteStmt.append(TABLENAME_CONTENT);
      deleteStmt.append(" WHERE ");
      deleteStmt.append(CONTENT_IDENTIFIER);
      deleteStmt.append("=?");
      deleteStmt.append(" AND ");
      deleteStmt.append(CONTENT_FORMAT);
      deleteStmt.append("=?");

      return session.prepare(deleteStmt.toString());
    });

    BoundStatement bound = prepared.bind(identifier, format);
    ResultSet result = session.execute(bound);

    if (!result.wasApplied()) {
      throw new NotFoundException("The deletion was not applied for the given identifier and format.");
    }
  }


}
