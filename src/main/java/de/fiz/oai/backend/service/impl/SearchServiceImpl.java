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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.dao.DAOContent;
import de.fiz.oai.backend.dao.DAOFormat;
import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.dao.DAOSet;
import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.models.Set;
import de.fiz.oai.backend.models.reindex.ReindexStatus;
import de.fiz.oai.backend.service.SearchService;
import de.fiz.oai.backend.utils.Configuration;
import de.fiz.oai.backend.utils.ResourcesUtils;
import de.fiz.oai.backend.utils.XPathHelper;

@Service
public class SearchServiceImpl implements SearchService {

  private Logger LOGGER = LoggerFactory.getLogger(SearchServiceImpl.class);

  String elastisearchHost = Configuration.getInstance().getProperty("elasticsearch.host");

  int elastisearchPort = Integer.parseInt(Configuration.getInstance().getProperty("elasticsearch.port"));

  public static String ITEMS_ALIAS_INDEX_NAME = "items";

  public static String ITEMS_MAPPING_V6_FILENAME = "/WEB-INF/classes/elasticsearch/item_mapping_es_v6";
  public static String ITEMS_MAPPING_V7_FILENAME = "/WEB-INF/classes/elasticsearch/item_mapping_es_v7";

  @Context
  ServletContext servletContext;

  @Inject
  DAOItem daoItem;

  @Inject
  DAOContent daoContent;

  @Inject
  DAOFormat daoFormat;

  @Inject
  DAOSet daoSet;

  private ReindexStatus reindexStatus = null;

  private CompletableFuture<Boolean> reindexAllFuture;

  /**
   * 
   * @param item @throws IOException @throws
   */
  @Override
  public Map<String, Object> readDocument(Item item) throws IOException {
    try (RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")))) {
      GetRequest getRequest = new GetRequest(ITEMS_ALIAS_INDEX_NAME, "_doc", item.getIdentifier());

      GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
      Map<String, Object> sourceAsMap = getResponse.getSourceAsMap();

      return sourceAsMap;
    }
  }

  /**
   * 
   * @param item @throws IOException @throws
   */
  @Override
  public void createDocument(Item item) throws IOException {
    try (RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")))) {

      indexDocument(item, ITEMS_ALIAS_INDEX_NAME, client);
      LOGGER.info("Added item to search index");
    }
  }

  private void indexDocument(Item item, String indexName, RestHighLevelClient client) throws IOException {

    Map<String, Object> itemMap = item.toMap();

    // Add all available formats
    List<Content> allContents = daoContent.readFormats(item.getIdentifier());
    List<String> itemFormats = new ArrayList<>();
    if (allContents != null && !allContents.isEmpty()) {
      for (final Content pickedContent : allContents) {
        itemFormats.add(pickedContent.getFormat());
      }
    }
    itemMap.put("formats", itemFormats);

    // Add all the matching sets
    List<Set> allSets = daoSet.readAll();
    List<String> itemSets = new ArrayList<>();
    if (allSets != null && !allSets.isEmpty()) {

      for (final Set pickedSet : allSets) {
        // Check set membership via xPath
        Map<String, String> xPaths = pickedSet.getxPaths();
        if (allContents != null && !allContents.isEmpty()) {
          for (final Content pickedContent : allContents) {
            if (xPaths.containsKey(pickedContent.getFormat())) {
              final String xPathToCheck = xPaths.get(pickedContent.getFormat());
              if (XPathHelper.isTextValueMatching(pickedContent.getContent(), xPathToCheck)) {
                itemSets.add(pickedSet.getName());
              }
            }
          }
        }

        // Check set membership via item tags
        List<String> setTags = pickedSet.getTags();

        if (setTags != null && !setTags.isEmpty()) {
          for (String setTag : setTags) {
            if (item.getTags().contains(setTag)) {
              itemSets.add(pickedSet.getName());
            }
          }
        }
      }

    }
    itemMap.put("sets", itemSets);

    IndexRequest indexRequest = new IndexRequest();

    indexRequest.index(indexName);
    indexRequest.type("_doc");
    indexRequest.source(itemMap);
    indexRequest.id(item.getIdentifier());

    client.index(indexRequest, RequestOptions.DEFAULT);
  }

  /**
   * 
   * @param item @throws IOException @throws
   */
  @Override
  public void updateDocument(Item item) throws IOException {
    try (RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")))) {

      Map<String, Object> itemMap = item.toMap();

      UpdateRequest updateRequest = new UpdateRequest();
      updateRequest.index(ITEMS_ALIAS_INDEX_NAME);
      updateRequest.type("_doc");
      updateRequest.id(item.getIdentifier());
      updateRequest.doc(itemMap);
      client.update(updateRequest, RequestOptions.DEFAULT);

    }
  }

  /**
   * 
   * @param item @throws IOException @throws
   */
  @Override
  public void deleteDocument(Item item) throws IOException {
    try (RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")))) {

      DeleteRequest request = new DeleteRequest();
      request.index(ITEMS_ALIAS_INDEX_NAME);
      request.type("_doc");
      request.id(item.getIdentifier());

      client.delete(request, RequestOptions.DEFAULT);
    }
  }

  @Override
  public SearchResult<String> search(Integer rows, String set, String format, Date fromDate, Date untilDate,
      Item lastItem) throws IOException {

    LOGGER.info("DEBUG: rows: {}", rows);
    LOGGER.info("DEBUG: format: {}", format);
    LOGGER.info("DEBUG: lastItem: {}", lastItem);

    try (RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")))) {

      final BoolQueryBuilder queryBuilder = new BoolQueryBuilder();

      Date finalFromDate = new SimpleDateFormat("yyyy-MM-dd").parse("0001-01-01");
      Date finalUntilDate = new SimpleDateFormat("yyyy-MM-dd").parse("9999-12-31");

      if (fromDate != null) {
        finalFromDate = fromDate;
      }
      if (untilDate != null) {
        finalUntilDate = untilDate;
      }

      queryBuilder
          .filter(QueryBuilders.rangeQuery("datestamp").from(Configuration.getDateformat().format(finalFromDate))
              .to(Configuration.getDateformat().format(finalUntilDate)));
      queryBuilder.filter(QueryBuilders.termQuery("formats", format));

      if (StringUtils.isNotBlank(set)) {
        queryBuilder.filter(QueryBuilders.termQuery("sets", set));
      }

      final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

      FieldSortBuilder datestampBuilder = SortBuilders.fieldSort("datestamp");
      FieldSortBuilder identifierBuilder = SortBuilders.fieldSort("identifier");
      searchSourceBuilder.query(queryBuilder);
      searchSourceBuilder.sort(datestampBuilder);
      searchSourceBuilder.sort(identifierBuilder);
      searchSourceBuilder.size(rows);
      searchSourceBuilder.fetchSource(false);

      if (lastItem != null) {
        Long timestamp = null;
        try {
          timestamp = Configuration.getDateformat().parse(lastItem.getDatestamp()).getTime();
        } catch (ParseException e) {
          e.printStackTrace();
        }
        searchSourceBuilder.searchAfter(new Object[] { timestamp, lastItem.getIdentifier() });
        searchSourceBuilder.from(0);
      }

      SearchRequest searchRequest = new SearchRequest(ITEMS_ALIAS_INDEX_NAME);
      searchRequest.source(searchSourceBuilder);

      LOGGER.info("DEBUG: searchRequest: {}", searchRequest.toString());

      SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

      SearchHits searchHits = searchResponse.getHits();
      Iterator<SearchHit> iterator = searchHits.iterator();
      List<String> idsRetrieved = new ArrayList<>();

      while (iterator.hasNext()) {
        SearchHit searchHit = iterator.next();
        idsRetrieved.add(searchHit.getId());
      }

      SearchResult<String> idResult = new SearchResult<>();
      idResult.setSize(idsRetrieved.size());
      idResult.setTotal(searchResponse.getHits().totalHits);
      idResult.setData(idsRetrieved);

      // Send the lastItemId if there are elements after it
      String newLastItemId = null;
      if (idsRetrieved.size() > 0) {
        newLastItemId = idsRetrieved.get(idsRetrieved.size() - 1);
        idResult.setLastItemId(newLastItemId);
      }
      Item newLastItem = null;
      if (StringUtils.isNotBlank(newLastItemId)) {

        newLastItem = daoItem.read(newLastItemId);
        LOGGER.info("searchSourceBuilder: {}", searchSourceBuilder);
        LOGGER.info("newLastItemId: {}", newLastItemId);
        LOGGER.info("newLastItem: {}", newLastItem);

        Long timestamp = null;
        try {
          timestamp = Configuration.getDateformat().parse(newLastItem.getDatestamp()).getTime();
        } catch (ParseException e) {
          e.printStackTrace();
        }
        searchSourceBuilder.searchAfter(new Object[] { timestamp, newLastItem.getIdentifier() });
        searchRequest.source(searchSourceBuilder);

        LOGGER.info("DEBUG: currentLastItemId: {}", newLastItemId);
        LOGGER.info("DEBUG: searchRequest next elements?: {}", searchRequest.toString());
        searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        if (searchResponse.getHits().getHits().length == 0) {
          idResult.setLastItemId(null);
        }
      }

      return idResult;

    } catch (Exception e) {
      throw new IOException(e);
    }

  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean createIndex(final String indexName, final String mapping) throws IOException {
    if (StringUtils.isNotBlank(indexName) && StringUtils.isNotBlank(mapping)) {
      try (RestHighLevelClient client = new RestHighLevelClient(
          RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")))) {
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
        if (createIndexResponse.isAcknowledged()) {
          RestClient lowLevelClient = client.getLowLevelClient();

          Request requestMapping = new Request("PUT", "/" + indexName + "/_mapping");
          requestMapping.setJsonEntity(mapping);
          Response responseMapping = lowLevelClient.performRequest(requestMapping);
          if (responseMapping.getStatusLine().getStatusCode() == 200
              || responseMapping.getStatusLine().getStatusCode() == 204) {
            return true;
          }
        }
      }
    }
    LOGGER.info("CREATE status: something went wrong, return false");
    return false;
  }

  @Override
  public void dropIndex(final String indexName) throws IOException {
    if (StringUtils.isNotBlank(indexName)) {
      try (RestHighLevelClient client = new RestHighLevelClient(
          RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")))) {
        DeleteIndexRequest request = new DeleteIndexRequest(indexName);
        client.indices().delete(request, RequestOptions.DEFAULT);
      }
    }
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

    reindexStatus.setAliasName(ITEMS_ALIAS_INDEX_NAME);
    LOGGER.info("REINDEX status: Alias name: {}", reindexStatus.getAliasName());

    reindexAllFuture = CompletableFuture.supplyAsync(() -> {

      try (RestHighLevelClient client = new RestHighLevelClient(
          RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")))) {

        ClusterHealthRequest requestAllIndexes = new ClusterHealthRequest();
        ClusterHealthResponse responseAllIndexes = client.cluster().health(requestAllIndexes, RequestOptions.DEFAULT);
        java.util.Set<String> allIndexes = responseAllIndexes.getIndices().keySet();

        LOGGER.info("REINDEX status: Found " + allIndexes.size() + " indexes:");
        int maximumIndexFound = 0;
        for (final String pickedIndex : allIndexes) {
          LOGGER.info("REINDEX status: {}", pickedIndex);
          if (pickedIndex.startsWith(ITEMS_ALIAS_INDEX_NAME)) {
            final String suffixIndex = pickedIndex.substring(ITEMS_ALIAS_INDEX_NAME.length());
            LOGGER.info("REINDEX status: " + pickedIndex + " -> suffix: " + suffixIndex);
            if (!StringUtils.isBlank(suffixIndex) && StringUtils.isNumeric(suffixIndex)) {
              int pickedNumIndexFound = Integer.parseInt(suffixIndex);
              if (pickedNumIndexFound > maximumIndexFound) {
                maximumIndexFound = pickedNumIndexFound;
                reindexStatus.setOriginalIndexName(pickedIndex);
              }
            }
          }
        }

        int newIndexVersion = maximumIndexFound + 1;
        final StringBuilder newIndexName = new StringBuilder();
        newIndexName.append(ITEMS_ALIAS_INDEX_NAME);
        newIndexName.append(String.valueOf(newIndexVersion));
        reindexStatus.setNewIndexName(newIndexName.toString());
        LOGGER.info("REINDEX status: New index name: {}", reindexStatus.getNewIndexName());

        if (StringUtils.isBlank(reindexStatus.getOriginalIndexName())
            || StringUtils.isBlank(reindexStatus.getNewIndexName())) {
          LOGGER.error("Not able to determine index names: original (" + reindexStatus.getOriginalIndexName()
              + ") or new (" + reindexStatus.getNewIndexName() + ")");
          return false;
        }

        MainResponse infoResponse = client.info(RequestOptions.DEFAULT);
        String filenameItemsMapping = ITEMS_MAPPING_V6_FILENAME;
        if (infoResponse.getVersion().after(Version.V_6_8_4)) {
          filenameItemsMapping = ITEMS_MAPPING_V7_FILENAME;
        }
        LOGGER.info("REINDEX status: ES version found " + Version.displayVersion(infoResponse.getVersion(), false)
            + " -> mapping " + filenameItemsMapping);

        final String mapping = ResourcesUtils.getResourceFileAsString(filenameItemsMapping, servletContext);

        if (StringUtils.isBlank(mapping)) {
          LOGGER.error("REINDEX status: Not able to retrieve mapping {}", filenameItemsMapping);
        }

        LOGGER.info("REINDEX status: Creating new index " + reindexStatus.getNewIndexName() + " with mapping "
            + mapping.substring(0, 30) + "...");

        if (!createIndex(reindexStatus.getNewIndexName(), mapping)) {
          LOGGER.error(
              "REINDEX status: Something went wrong while creating the new index " + reindexStatus.getNewIndexName());
          return false;
        }

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
            indexDocument(pickedItem, reindexStatus.getNewIndexName(), client);
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

        // If in the meanwhile some new object has been inserted, reindex the new Items
        if (!reindexStatus.isStopSignalReceived()) {

          // Switch alias from old index to new one
          RestClient lowLevelClient = client.getLowLevelClient();

          LOGGER.info("REINDEX status: Remove all old aliases of {}", ITEMS_ALIAS_INDEX_NAME);
          for (final String pickedIndex : allIndexes) {
            Request requestDeleteOldAlias = new Request("POST", "/_aliases");
            requestDeleteOldAlias
                .setJsonEntity("{\n" + "    \"actions\" : [\n" + "        { \"remove\" : { \"index\" : \"" + pickedIndex
                    + "\", \"alias\" : \"" + ITEMS_ALIAS_INDEX_NAME + "\" } }\n" + "    ]\n" + "}");
            LOGGER.info("REINDEX status: execute remove alias " + ITEMS_ALIAS_INDEX_NAME + " to " + pickedIndex);
            try {
              Response responseDeleteOldAlias = lowLevelClient.performRequest(requestDeleteOldAlias);
              LOGGER.info("REINDEX status: responseDeleteOldAlias.getStatusLine().getStatusCode()"
                  + responseDeleteOldAlias.getStatusLine().getStatusCode());
            } catch (Exception e) {
              if (e instanceof ResponseException
                  && ((ResponseException) e).getResponse().getStatusLine().getStatusCode() == 404) {
                LOGGER.info(
                    "REINDEX status: alias " + ITEMS_ALIAS_INDEX_NAME + " to " + pickedIndex + " not found to delete.");
              } else {
                LOGGER.error("REINDEX status: something went wrong while deleting alias " + ITEMS_ALIAS_INDEX_NAME
                    + " to " + pickedIndex, e);
              }
            }
          }

          LOGGER.info("REINDEX status: Add new alias " + ITEMS_ALIAS_INDEX_NAME + " to index "
              + reindexStatus.getNewIndexName());
          Request requestNewAlias = new Request("POST", "/_aliases");
          requestNewAlias.setJsonEntity(
              "{\n" + "    \"actions\" : [\n" + "        { \"add\" : { \"index\" : \"" + reindexStatus.getNewIndexName()
                  + "\", \"alias\" : \"" + ITEMS_ALIAS_INDEX_NAME + "\" } }\n" + "    ]\n" + "}");
          LOGGER.info("REINDEX status: execute new alias");
          Response responseNewAlias = lowLevelClient.performRequest(requestNewAlias);
          LOGGER.info("REINDEX status: responseNewAlias.getStatusLine().getStatusCode()"
              + responseNewAlias.getStatusLine().getStatusCode());

          if (responseNewAlias.getStatusLine().getStatusCode() < 300) {
            // Delete old index
            dropIndex(reindexStatus.getOriginalIndexName());
          }
        } else {
          // Stop signal received, log all the informations
          LOGGER.warn("REINDEX status: stop signal received. Current reindex status so far:");
          LOGGER.warn("REINDEX status: Alias: {}", reindexStatus.getAliasName());
          LOGGER.warn("REINDEX status: New index (to drop): {}", reindexStatus.getNewIndexName());
          LOGGER.warn("REINDEX status: Previous index: {}", reindexStatus.getOriginalIndexName());
          LOGGER.warn("REINDEX status: Count total: {}", reindexStatus.getTotalCount());
          LOGGER.warn("REINDEX status: Count indexed: {}", reindexStatus.getIndexedCount());
          LOGGER.warn("REINDEX status: Start time: {}", reindexStatus.getStartTime());
          dropIndex(reindexStatus.getNewIndexName());
        }

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

}
