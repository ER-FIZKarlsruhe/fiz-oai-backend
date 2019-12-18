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

import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.utils.ClusterManager;

@Service
public class CassandraDAOItem implements DAOItem {

  public static final String ITEM_IDENTIFIER = "identifier";
  public static final String ITEM_DATESTAMP = "datestamp";
  public static final String ITEM_DELETEFLAG = "deleteflag";
  public static final String ITEM_INGESTFORMAT = "ingestFormat";
  public static final String ITEM_TAGS = "tags";

  public static final String TABLENAME_ITEM = "oai_item";

  private Map<String, PreparedStatement> preparedStatements = new HashMap<String, PreparedStatement>();

  public Item read(String identifier) throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    Session session = manager.getCassandraSession();

    PreparedStatement prepared = preparedStatements.get("read");
    if (prepared == null) {
      final StringBuilder selectStmt = new StringBuilder();
      selectStmt.append("SELECT * FROM ");
      selectStmt.append(TABLENAME_ITEM);
      selectStmt.append(" WHERE identifier=?");

      prepared = session.prepare(selectStmt.toString());
      preparedStatements.put("read", prepared);
    }

    BoundStatement bound = prepared.bind(identifier);

    ResultSet rs = session.execute(bound);
    Row resultRow = rs.one();
    if (resultRow != null) {
      final Item item = populateItem(resultRow);

      return item;
    }
    return null;
  }

  private Item populateItem(Row resultRow) {
    final Item item = new Item();
    item.setIdentifier(resultRow.getString(ITEM_IDENTIFIER));
    item.setDatestamp(resultRow.getString(ITEM_DATESTAMP));
    item.setDeleteFlag(resultRow.getBool(ITEM_DELETEFLAG));
    item.setIngestFormat(resultRow.getString(ITEM_INGESTFORMAT));

    return item;
  }

  public Item create(Item item) throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    Session session = manager.getCassandraSession();

    if (StringUtils.isBlank(item.getIdentifier())) {
      throw new IllegalArgumentException("Item's identifier cannot be empty!");
    }

    if (item.isDeleteFlag() == null) {
      item.setDeleteFlag(false);
    }

    PreparedStatement prepared = preparedStatements.get("create");
    if (prepared == null) {
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

      prepared = session.prepare(insertStmt.toString());
      preparedStatements.put("create", prepared);
    }

    BoundStatement bound = prepared.bind(item.getIdentifier(), item.getDatestamp(), item.isDeleteFlag(), item.getTags(),
        item.getIngestFormat());
    ResultSet result = session.execute(bound);

    if (!result.wasApplied()) {
      throw new NotFoundException("The creation was not applied for the given item.");
    }

    return item;
  }

  // FIXME Move into a service class that has access to cassandra and
  // elasticsearch
  public List<Item> search(Integer offset, Integer rows, String set, String format, String from, String until) {
    return null;
  }

  public void delete(String identifier) throws IOException {
    ClusterManager manager = ClusterManager.getInstance();
    Session session = manager.getCassandraSession();

    if (StringUtils.isBlank(identifier)) {
      throw new IllegalArgumentException("identifier cannot be empty!");
    }

    PreparedStatement prepared = preparedStatements.get("delete");
    if (prepared == null) {
      StringBuilder updateStmt = new StringBuilder();
      updateStmt.append("UPDATE ");
      updateStmt.append(TABLENAME_ITEM);
      updateStmt.append(" SET ");
      updateStmt.append(ITEM_DELETEFLAG);
      updateStmt.append("=? WHERE ");
      updateStmt.append(ITEM_IDENTIFIER);
      updateStmt.append("=?");

      prepared = session.prepare(updateStmt.toString());
      preparedStatements.put("delete", prepared);
    }

    BoundStatement bound = prepared.bind(true, identifier);
    ResultSet result = session.execute(bound);

    if (!result.wasApplied()) {
      throw new NotFoundException("The deletion was not applied for the given identifier and format.");
    }
  }
}
