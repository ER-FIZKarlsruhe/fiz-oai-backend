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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
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

import de.fiz.oai.backend.dao.DAOContent;
import de.fiz.oai.backend.dao.DAOFormat;
import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.dao.DAOSet;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.models.reindex.ReindexStatus;
import de.fiz.oai.backend.service.SearchService;
import de.fiz.oai.backend.utils.Configuration;

@Service
public class SolrSearchServiceImpl implements SearchService {

    private static Logger LOGGER = LoggerFactory.getLogger(SolrSearchServiceImpl.class);

    private HttpSolrClient solrClient;

    private int commitWithin;

    private ReindexStatus reindexStatus = null;

    private CompletableFuture<Boolean> reindexAllFuture;

    @Inject
    DAOItem daoItem;

    @Inject
    DAOContent daoContent;

    @Inject
    DAOFormat daoFormat;

    @Inject
    DAOSet daoSet;

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
        boolean stopped = true;

        // Stop future process if already running
        if (reindexStatus != null && StringUtils.isBlank(reindexStatus.getEndTime())) {
          reindexStatus.setStopSignalReceived(true);
          if (reindexAllFuture != null) {
            int attempt = 0;
            while (!reindexAllFuture.isCancelled() && attempt <= stopAttempts) {
              attempt++;
              reindexAllFuture.cancel(true);
              try {
                Thread.sleep(millisecondsAttemptsDelay);
                LOGGER.warn("Attempt " + attempt + " of " + stopAttempts + " to stop the current Reindex process...");
              } catch (InterruptedException e) {
                stopped = false;
              }
            }
            if (reindexAllFuture.isCancelled()) {
              reindexStatus = null;
              stopped = true;
            }
          } else {
            reindexStatus = null;
            stopped = true;
          }
        }

        if (stopped) {
          LOGGER.info("Current reindex process stopped.");
        } else {
          LOGGER.warn("Current reindex process NOT stopped!");
        }

        return stopped;
    }

    @Override
    public boolean reindexAll() {
        if (reindexStatus != null && StringUtils.isBlank(reindexStatus.getEndTime())) {
            LOGGER.warn("REINDEX status: Reindex process already started since " + reindexStatus.getStartTime()
                + ". Cannot continue until it finishes!");
            return false;
          }

          reindexStatus = new ReindexStatus();

          reindexStatus.setStopSignalReceived(false);

          reindexStatus.setAliasName(Configuration.getInstance().getProperty("solr.url"));
          LOGGER.info("REINDEX status: Alias name: {}", reindexStatus.getAliasName());

          reindexAllFuture = CompletableFuture.supplyAsync(() -> {

            try {
                reindexStatus.setTotalCount(daoItem.getCount());
                reindexStatus.setItemResultSet(daoItem.getAllItemsResultSet());
                LOGGER.info("REINDEX status: Total Items count: {}", reindexStatus.getTotalCount());

                if (reindexStatus.getTotalCount() < 1) {
                  LOGGER.warn("No items to reindex {}", reindexStatus.getNewIndexName());
                  return false;
                }

                reindexStatus.setIndexedCount(0);
                LOGGER.info("REINDEX status: Indexed Items count: {}", reindexStatus.getIndexedCount());

                reindexStatus.setStartTime(ZonedDateTime.now(ZoneOffset.UTC).toString());
                LOGGER.info("REINDEX status: Start Time: {}", reindexStatus.getStartTime());

                Item mostRecentItem = null;

                do {
                  List<Item> bufferListItems = daoItem.getItemsFromResultSet(reindexStatus.getItemResultSet(), 100);

                  for (final Item pickedItem : bufferListItems) {
                    indexDocument(pickedItem, reindexStatus.getNewIndexName(), elasticsearchClient);
                    reindexStatus.setIndexedCount(reindexStatus.getIndexedCount() + 1);

                    // Keep the most recent Item
                    if (mostRecentItem == null) {
                      mostRecentItem = pickedItem;
                    } else {
                      try {
                        if (Configuration.getDateformat().parse(mostRecentItem.getDatestamp())
                            .before(Configuration.getDateformat().parse(pickedItem.getDatestamp()))) {
                          mostRecentItem = pickedItem;
                        }
                      } catch (ParseException e) {
                        // leave mostRecentItem as it is
                      }
                    }
                  }

                  LOGGER.info("REINDEX status: " + reindexStatus.getIndexedCount() + " indexed out of "
                      + reindexStatus.getTotalCount() + ".");
                } while (reindexStatus.getIndexedCount() < reindexStatus.getTotalCount()
                    && !reindexStatus.isStopSignalReceived());

            } catch (IOException e) {
                LOGGER.error(
                    "REINDEX status: Something went wrong while processing the new index " + reindexStatus.getNewIndexName(),
                    e);
                return false;
              } finally {
                reindexStatus.setEndTime(ZonedDateTime.now(ZoneOffset.UTC).toString());
                LOGGER.info("REINDEX status: End Time: {}", reindexStatus.getEndTime());
              }
              return true;

            });

            return true;
    }

    @Override
    public String getReindexStatusVerbose() {
        StringBuilder statusString = new StringBuilder();
        if (reindexStatus == null) {
          statusString.append("Reindex process not started.");
        } else {
          statusString.append("Reindex process STARTED on ");
          statusString.append(reindexStatus.getStartTime());
          if (!StringUtils.isBlank(reindexStatus.getEndTime())) {
            statusString.append(" and FINISHED on ");
            statusString.append(reindexStatus.getEndTime());

          }
          statusString.append(".\n");
          statusString.append("Alias ");
          statusString.append(reindexStatus.getAliasName());
          statusString.append(" -> last index created ");
          statusString.append(reindexStatus.getNewIndexName());
          statusString.append(".\n");
          statusString.append("Previous index ");
          statusString.append(reindexStatus.getOriginalIndexName());
          statusString.append(".\n");
          statusString.append("Reindexed elements ");
          statusString.append(reindexStatus.getIndexedCount());
          statusString.append(" out of ");
          statusString.append(reindexStatus.getTotalCount());
          statusString.append(".\n");

          double percProgress = 0;
          if (reindexStatus.getIndexedCount() > 0 && reindexStatus.getTotalCount() > 0) {
            percProgress = ((double) reindexStatus.getIndexedCount() / reindexStatus.getTotalCount()) * 100;
          }

          long hours = 0;
          long minutesOfHours = 0;
          int secondsOfMinutes = 0;
          long totalSecondsSoFar = 0;
          ZonedDateTime startZDT = null;
          if (StringUtils.isNotBlank(reindexStatus.getStartTime())) {
            startZDT = ZonedDateTime.parse(reindexStatus.getStartTime());
          }

          Duration timeLapsed = null;
          if (startZDT != null) {
            timeLapsed = Duration.between(startZDT,
                StringUtils.isBlank(reindexStatus.getEndTime()) ? ZonedDateTime.now(ZoneOffset.UTC)
                    : ZonedDateTime.parse(reindexStatus.getEndTime()));
            hours = timeLapsed.toHours();
            minutesOfHours = timeLapsed.toMinutesPart();
            secondsOfMinutes = timeLapsed.toSecondsPart();
            totalSecondsSoFar = timeLapsed.toSeconds();
          }

          statusString.append("Progress: ");
          statusString.append(String.format("%.2f", percProgress));
          statusString.append(" % in ");
          statusString.append(hours);
          statusString.append(":");
          statusString.append(String.format("%02d", minutesOfHours));
          statusString.append(":");
          statusString.append(String.format("%02d", secondsOfMinutes));
          statusString.append(".\n");

          String eta = "";
          if (StringUtils.isBlank(reindexStatus.getEndTime()) && percProgress > 0 && totalSecondsSoFar > 0
              && startZDT != null) {
            final double estimatedTotalSeconds = ((double) totalSecondsSoFar / percProgress) * 100;
            final ZonedDateTime etaZDT = startZDT.plusSeconds((long) estimatedTotalSeconds)
                .withZoneSameInstant(ZoneOffset.UTC);
            if (etaZDT != null) {
              eta = etaZDT.toString();
            }
          }

          statusString.append("ETA: ");
          statusString.append(eta);
          statusString.append(".\n");
          statusString.append("Stop signal sent: ");
          statusString.append(reindexStatus.isStopSignalReceived());
          statusString.append(".\n");
        }

        return statusString.toString();
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
