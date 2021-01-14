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
package de.fiz.oai.backend.utils;

import org.apache.commons.lang3.StringUtils;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;

import de.fiz.oai.backend.dao.impl.CassandraDAOContent;
import de.fiz.oai.backend.dao.impl.CassandraDAOCrosswalk;
import de.fiz.oai.backend.dao.impl.CassandraDAOFormat;
import de.fiz.oai.backend.dao.impl.CassandraDAOItem;
import de.fiz.oai.backend.dao.impl.CassandraDAOSet;

public class CassandraUtils {

    public static String getClusterTopologyInformation(CqlSession  session) {
        StringBuilder resultBuilder = new StringBuilder();
        SimpleStatement statement =
            SimpleStatement.newInstance("SELECT * FROM system.peers;");
        ResultSet rs = session.execute(statement);
        for (Row row : rs.all()) {
            resultBuilder.append(row.getInetAddress("peer") + " | ");
            resultBuilder.append(row.getString("data_center") + "\n");
        }
        return resultBuilder.toString();
    }

    public static void createTables(CqlSession session, String keyspace) {
    	
        // Create tables
        final StringBuilder useStmt = new StringBuilder();
        useStmt.append("USE ");
        useStmt.append(keyspace);
        session.execute(useStmt.toString());

        final StringBuilder createTableItemStmt = new StringBuilder();
        createTableItemStmt.append("CREATE TABLE IF NOT EXISTS ");
        createTableItemStmt.append(CassandraDAOItem.TABLENAME_ITEM);
        createTableItemStmt.append(" (");
        createTableItemStmt.append(CassandraDAOItem.ITEM_IDENTIFIER);
        createTableItemStmt.append(" text, ");
        createTableItemStmt.append(CassandraDAOItem.ITEM_DATESTAMP);
        createTableItemStmt.append(" text, ");
        createTableItemStmt.append(CassandraDAOItem.ITEM_DELETEFLAG);
        createTableItemStmt.append(" boolean, ");
        createTableItemStmt.append(CassandraDAOItem.ITEM_TAGS);
        createTableItemStmt.append(" list<text>, ");
        createTableItemStmt.append(CassandraDAOItem.ITEM_INGESTFORMAT);
        createTableItemStmt.append(" text, PRIMARY KEY (");
        createTableItemStmt.append(CassandraDAOItem.ITEM_IDENTIFIER);
        
        createTableItemStmt.append("));");
        session.execute(createTableItemStmt.toString());

        final StringBuilder createTableSetStmt = new StringBuilder();
        createTableSetStmt.append("CREATE TABLE IF NOT EXISTS ");
        createTableSetStmt.append(CassandraDAOSet.TABLENAME_SET);
        createTableSetStmt.append(" (");
        createTableSetStmt.append(CassandraDAOSet.SET_NAME);
        createTableSetStmt.append(" text, ");
        createTableSetStmt.append(CassandraDAOSet.SET_SPEC);
        createTableSetStmt.append(" text, ");
        createTableSetStmt.append(CassandraDAOSet.SET_DESCRIPTION);
        createTableSetStmt.append(" text, ");
        createTableSetStmt.append(CassandraDAOSet.SET_XPATHS);
        createTableSetStmt.append(" map<text, text>, ");
        createTableSetStmt.append(CassandraDAOSet.SET_TAGS);
        createTableSetStmt.append(" list<text>, PRIMARY KEY (");
        createTableSetStmt.append(CassandraDAOSet.SET_NAME);
        createTableSetStmt.append("));");
        session.execute(createTableSetStmt.toString());

        final StringBuilder createTableFormatStmt = new StringBuilder();
        createTableFormatStmt.append("CREATE TABLE IF NOT EXISTS ");
        createTableFormatStmt.append(CassandraDAOFormat.TABLENAME_FORMAT);
        createTableFormatStmt.append(" (");
        createTableFormatStmt.append(CassandraDAOFormat.FORMAT_METADATAPREFIX);
        createTableFormatStmt.append(" text, ");
        createTableFormatStmt.append(CassandraDAOFormat.FORMAT_SCHEMALOCATION);
        createTableFormatStmt.append(" text, ");
        createTableFormatStmt.append(CassandraDAOFormat.FORMAT_SCHEMANAMESPACE);
        createTableFormatStmt.append(" text, ");
        createTableFormatStmt.append(CassandraDAOFormat.FORMAT_IDENTIFIERXPATH);
        createTableFormatStmt.append(" text, PRIMARY KEY (");
        createTableFormatStmt.append(CassandraDAOFormat.FORMAT_METADATAPREFIX);
        createTableFormatStmt.append("));");
        session.execute(createTableFormatStmt.toString());
        
        final StringBuilder createTableContentStmt = new StringBuilder();
        createTableContentStmt.append("CREATE TABLE IF NOT EXISTS ");
        createTableContentStmt.append(CassandraDAOContent.TABLENAME_CONTENT);
        createTableContentStmt.append(" (");
        createTableContentStmt.append(CassandraDAOContent.CONTENT_IDENTIFIER);
        createTableContentStmt.append(" text, ");
        createTableContentStmt.append(CassandraDAOContent.CONTENT_FORMAT);
        createTableContentStmt.append(" text, ");
        createTableContentStmt.append(CassandraDAOContent.CONTENT_CONTENT);
        createTableContentStmt.append(" blob, PRIMARY KEY (");
        createTableContentStmt.append(CassandraDAOContent.CONTENT_IDENTIFIER + ", " + CassandraDAOContent.CONTENT_FORMAT);
        createTableContentStmt.append("));");
        session.execute(createTableContentStmt.toString());
        
        final StringBuilder createTableCrosswalkStmt = new StringBuilder();
        createTableCrosswalkStmt.append("CREATE TABLE IF NOT EXISTS ");
        createTableCrosswalkStmt.append(CassandraDAOCrosswalk.TABLENAME_CROSSWALK);
        createTableCrosswalkStmt.append(" (");
        createTableCrosswalkStmt.append(CassandraDAOCrosswalk.CROSSWALK_NAME);
        createTableCrosswalkStmt.append(" text, ");
        createTableCrosswalkStmt.append(CassandraDAOCrosswalk.CROSSWALK_FORMAT_FROM);
        createTableCrosswalkStmt.append(" text, ");
        createTableCrosswalkStmt.append(CassandraDAOCrosswalk.CROSSWALK_FORMAT_TO);
        createTableCrosswalkStmt.append(" text, ");
        createTableCrosswalkStmt.append(CassandraDAOCrosswalk.CROSSWALK_XSLT_STYLESHEET);
        createTableCrosswalkStmt.append(" blob, PRIMARY KEY (");
        createTableCrosswalkStmt.append(CassandraDAOCrosswalk.CROSSWALK_NAME);
        createTableCrosswalkStmt.append("));");
        session.execute(createTableCrosswalkStmt.toString());
    }



}