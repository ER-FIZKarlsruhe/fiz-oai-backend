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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;

import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.utils.ClusterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CassandraDAOItem implements DAOItem {

  public static final String ITEM_IDENTIFIER = "identifier";
  public static final String ITEM_DATESTAMP = "datestamp";
  public static final String ITEM_DELETEFLAG = "deleteflag";
  public static final String ITEM_INGESTFORMAT = "ingestFormat";
  public static final String ITEM_TAGS = "tags";

  public static final String TABLENAME_ITEM = "oai_item";

  // Bounds how many concurrent executeAsync() requests a single batched read fires at once, so a
  // large identifier list can't fan out into thousands of simultaneous in-flight requests and
  // overwhelm the connection/driver (DataStax recommends bounded concurrency over unbounded fan-out).
  private static final int MAX_CONCURRENT_ASYNC_READS = 50;

  private final Map<String, PreparedStatement> preparedStatements = new ConcurrentHashMap<>();

  private static Logger LOGGER = LoggerFactory.getLogger(CassandraDAOItem.class);

  public Item read(String identifier) throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    PreparedStatement prepared = getOrPrepareRead(session);

    BoundStatement bound = prepared.bind(identifier);

    ResultSet rs = session.execute(bound);
    Row resultRow = rs.one();
    if (resultRow != null) {
      final Item item = populateItem(resultRow);

      return item;
    }
    return null;
  }

  public Map<String, Item> read(Collection<String> identifiers) throws IOException {
    Map<String, Item> result = new LinkedHashMap<>();
    if (CollectionUtils.isEmpty(identifiers)) {
      return result;
    }

    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    PreparedStatement prepared = getOrPrepareRead(session);

    // Fire each chunk concurrently, then join, instead of one blocking round trip per identifier -
    // capped at MAX_CONCURRENT_ASYNC_READS in flight at a time instead of unbounded fan-out.
    List<String> identifierList = new ArrayList<>(identifiers);
    for (int start = 0; start < identifierList.size(); start += MAX_CONCURRENT_ASYNC_READS) {
      List<String> chunk = identifierList.subList(start, Math.min(start + MAX_CONCURRENT_ASYNC_READS, identifierList.size()));

      Map<String, CompletionStage<AsyncResultSet>> pending = new LinkedHashMap<>();
      for (String identifier : chunk) {
        pending.put(identifier, session.executeAsync(prepared.bind(identifier)));
      }

      for (Map.Entry<String, CompletionStage<AsyncResultSet>> entry : pending.entrySet()) {
        AsyncResultSet rs = joinUnwrapped(entry.getValue());
        Row resultRow = rs.one();
        if (resultRow != null) {
          result.put(entry.getKey(), populateItem(resultRow));
        }
      }
    }

    return result;
  }

  private PreparedStatement getOrPrepareRead(CqlSession session) {
    return preparedStatements.computeIfAbsent("read",
        key -> session.prepare("SELECT * FROM " + TABLENAME_ITEM + " WHERE identifier=?"));
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

  private Item populateItem(Row resultRow) {
    final Item item = new Item();
    item.setIdentifier(resultRow.getString(ITEM_IDENTIFIER));
    item.setDatestamp(resultRow.getString(ITEM_DATESTAMP));
    item.setDeleteFlag(resultRow.getBoolean(ITEM_DELETEFLAG));
    item.setIngestFormat(resultRow.getString(ITEM_INGESTFORMAT));
    item.setTags(resultRow.getList(ITEM_TAGS, String.class));
    
    return item;
  }

  public Item create(Item item) throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    if (StringUtils.isBlank(item.getIdentifier())) {
      throw new IllegalArgumentException("Item's identifier cannot be empty!");
    }

    if (item.isDeleteFlag() == null) {
      item.setDeleteFlag(false);
    }

    PreparedStatement prepared = preparedStatements.computeIfAbsent("create", key -> {
      StringBuilder insertStmt = new StringBuilder();
      insertStmt.append("INSERT INTO ");
      insertStmt.append(TABLENAME_ITEM);
      insertStmt.append(" (");
      insertStmt.append(ITEM_IDENTIFIER);
      insertStmt.append(", ");
      insertStmt.append(ITEM_DATESTAMP);
      insertStmt.append(", ");
      insertStmt.append(ITEM_DELETEFLAG);
      insertStmt.append(", ");
      insertStmt.append(ITEM_TAGS);
      insertStmt.append(", ");
      insertStmt.append(ITEM_INGESTFORMAT);
      insertStmt.append(") VALUES (?, ?, ?, ?, ?)");

      return session.prepare(insertStmt.toString());
    });

    BoundStatement bound = prepared.bind(item.getIdentifier(), item.getDatestamp(), item.isDeleteFlag(), item.getTags(),
        item.getIngestFormat());
    ResultSet result = session.execute(bound);

    if (!result.wasApplied()) {
      throw new NotFoundException("The creation was not applied for the given item.");
    }

    return item;
  }

  public void delete(String identifier) throws IOException {

    if (StringUtils.isBlank(identifier)) {
      throw new IllegalArgumentException("identifier cannot be empty!");
    }

    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    PreparedStatement prepared = preparedStatements.computeIfAbsent("delete", key -> {
      StringBuilder updateStmt = new StringBuilder();
      updateStmt.append("DELETE FROM ");
      updateStmt.append(TABLENAME_ITEM);
      updateStmt.append(" WHERE ");
      updateStmt.append(ITEM_IDENTIFIER);
      updateStmt.append("=?");

      return session.prepare(updateStmt.toString());
    });

    BoundStatement bound = prepared.bind(identifier);
    ResultSet result = session.execute(bound);

    if (!result.wasApplied()) {
      throw new NotFoundException("The deletion was not applied for the given identifier and format.");
    }
  }

  public long getCount() throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    StringBuilder selectStmt = new StringBuilder();
    selectStmt.append("SELECT ");
    selectStmt.append(ITEM_IDENTIFIER);
    selectStmt.append(" FROM ");
    selectStmt.append(TABLENAME_ITEM);
    
    SimpleStatement statement = SimpleStatement.newInstance(selectStmt.toString());
    ResultSet prepareResult = session.execute(statement);
    long i = 0;
    
    //TODO this is the slow asynchronous approach. Replace it with the async one, see https://docs.datastax.com/en/developer/java-driver/4.4/manual/core/paging/#asynchronous-paging
    for (Row row : prepareResult) {
      i++;
    }
 
    return i;
  }

  public ResultSet getAllItemsResultSet() throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    CqlSession session = manager.getCassandraSession();

    StringBuilder selectStmt = new StringBuilder();
    selectStmt.append("SELECT ");
    selectStmt.append("*");
    selectStmt.append(" FROM ");
    selectStmt.append(TABLENAME_ITEM);

    return session.execute(selectStmt.toString());
  }

  public List<Item> getItemsFromResultSet(ResultSet resultSet, int itemsToRetrieve) throws IOException {
    List<Item> itemsRetrieved = new ArrayList<Item>();
    int i = 0;

    //TODO this is the slow synchronous approach. Replace it with the async one, see https://docs.datastax.com/en/developer/java-driver/4.4/manual/core/paging/#asynchronous-paging
    for (Row row : resultSet) {
        itemsRetrieved.add(populateItem(row));
        
        if (i < itemsToRetrieve) {
            i++;
        } else {
            break;
        }
    }

    return itemsRetrieved;
  }

}
