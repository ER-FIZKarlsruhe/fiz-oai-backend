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
package de.fiz.oai.backend.service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient.Builder;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.service.SearchService;
import de.fiz.oai.backend.utils.Configuration;

@Service
public class SolrSearchServiceImpl implements SearchService {

    private static Logger LOGGER = LoggerFactory.getLogger(SolrSearchServiceImpl.class);

    private HttpSolrClient solrClient;

    private int commitWithin;

    @Context
    ServletContext servletContext;

    /**
     * Constructor
     */
    public SolrSearchServiceImpl() {
        commitWithin = Integer.parseInt(Configuration.getInstance().getProperty("solr.commit.within", "30000"));
        solrClient = initSolrClient();
    }

    /**
     * 
     * @param item @throws IOException @throws
     */
    @Override
    public Map<String, Object> readDocument(Item item) throws IOException {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            SolrDocument doc = solrClient.getById(item.getIdentifier());
            if (doc == null) {
                return null;
            }
            Collection<String> fieldNames = doc.getFieldNames();
            for (String fieldName: fieldNames) {
                Collection<Object> values = doc.getFieldValues(fieldName);
                resultMap.put(fieldName, values);
            }
        }
        catch (Exception e) {
            throw new IOException(e.getMessage());
        }

        return resultMap;
    }

    /**
     * Create new item in index.
     *
     * @param item The item to create
     * @throws IOException
     */
    @Override
    public void createDocument(Item item) throws IOException {
        try {
            Map<String, Object> itemMap = item.toMap();
            SolrInputDocument solrDocument = new SolrInputDocument();
            for (Entry<String, Object> entry : itemMap.entrySet()) {
                solrDocument.addField(entry.getKey(), entry.getValue());
            }
            solrClient.add(solrDocument, commitWithin);
        }
        catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        LOGGER.info("Added/Updated item " + item.getIdentifier() + " to search index.");
    }

    /**
     * Update item in index.
     *
     * @param item The item to update
     * @throws IOException
     */
    @Override
    public void updateDocument(Item item) throws IOException {
        createDocument(item);
    }

    @Override
    public SearchResult<String> search(
        Integer rows, String set, String format, Date fromDate, Date untilDate, String searchMark) throws IOException {
        String decodedSearchMark = null;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("rows: {}", rows);
            LOGGER.debug("format: {}", format);
            LOGGER.debug("searchMark: {}", searchMark);
        }

        StringBuilder query = new StringBuilder();
        SearchResult<String> idResult = new SearchResult<>();
        try {
            //URL-Encode + decode searchMark.
            if (StringUtils.isBlank(searchMark)) {
                decodedSearchMark = CursorMarkParams.CURSOR_MARK_START;
            }
            else {
                decodedSearchMark = new String(Base64.getUrlDecoder().decode(searchMark), StandardCharsets.UTF_8);
            }
            Date finalFromDate = new SimpleDateFormat("yyyy-MM-dd").parse("0001-01-01");
            Date finalUntilDate = new SimpleDateFormat("yyyy-MM-dd").parse("9999-12-31");

            if (fromDate != null) {
                finalFromDate = fromDate;
            }
            if (untilDate != null) {
                finalUntilDate = untilDate;
            }
            query.append("datestamp:[").append(Configuration.getDateformat().format(finalFromDate));
            query.append(" TO ").append(Configuration.getDateformat().format(finalUntilDate)).append("]");
            if (StringUtils.isNotBlank(format)) {
                query.append(" AND formats:").append(ClientUtils.escapeQueryChars(format));
            }
            if (StringUtils.isNotBlank(set)) {
                query.append(" AND sets:").append(ClientUtils.escapeQueryChars(set));
            }
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query.toString());
            solrQuery.setFields("identifier");
            solrQuery.setRows(rows);
            solrQuery.addSort("datestamp", ORDER.asc);
            solrQuery.addSort("identifier", ORDER.asc);
            solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, decodedSearchMark);

            LOGGER.debug("searchRequest: {}", solrQuery.toString());

            QueryResponse rsp = solrClient.query(solrQuery);

            LOGGER.debug("searchResponse: {}", rsp.toString());

            List<String> idsRetrieved = new ArrayList<>();

            for (SolrDocument doc : rsp.getResults()) {
                idsRetrieved.add((String) doc.getFieldValue("identifier"));
            }

            idResult.setSize(idsRetrieved.size());
            idResult.setTotal(rsp.getResults().getNumFound());
            idResult.setData(idsRetrieved);

            // Retry solrQuery with nextCursorMark. If there are results, send the searchMark
            String nextCursorMark = rsp.getNextCursorMark();
            solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, nextCursorMark);
            solrQuery.setRows(1);
            rsp = solrClient.query(solrQuery);
            if (!rsp.getResults().isEmpty()) {
                //URL-Encode + decode searchMark.
                idResult.setSearchMark(Base64.getUrlEncoder().encodeToString(nextCursorMark.getBytes()));
            }
        }
        catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        return idResult;
    }

    /**
     * 
     * @param item @throws IOException @throws
     */
    @Override
    public void deleteDocument(Item item) throws IOException {
        try {
            solrClient.deleteById(item.getIdentifier(), commitWithin);
        }
        catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }
    
    @Override
    public void commit() throws IOException {
        try {
            solrClient.commit();
        }
        catch (SolrServerException e) {
            throw new IOException(e.getMessage());
        }
        
    }

    @Override
    public boolean createIndex(final String indexName, final String mapping) throws IOException {
        return false;
    }

    @Override
    public void dropIndex(final String indexName) throws IOException {
    }

    @Override
    public boolean stopReindexAll(final int stopAttempts, final int millisecondsAttemptsDelay) {
        return false;
    }

    @Override
    public boolean reindexAll() {
        return true;
    }

    @Override
    public String getReindexStatusVerbose() {
        return "Not Possible";
    }

    private HttpSolrClient initSolrClient() {
        Builder builder = new Builder(Configuration.getInstance().getProperty("solr.url"));
        CloseableHttpClient httpclient =
            HttpClients
                .custom().setProxy(null).setMaxConnPerRoute(128).setMaxConnTotal(256)
                .setConnectionTimeToLive(900, TimeUnit.SECONDS).build();
        builder.withHttpClient(httpclient);
        return builder.build();
    }

}
