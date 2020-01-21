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
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
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
import de.fiz.oai.backend.models.Format;
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

  public static String ITEMS_MAPPING_V7_FILENAME = "/WEB-INF/classes/elasticsearch/item_mapping_es_v7";
  public static String ITEMS_MAPPING_V6_FILENAME = "/WEB-INF/classes/elasticsearch/item_mapping_es_v6";

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
      Map<String, Object> itemMap = item.toMap();

      // Add all available formats
      List<Content> allContents = daoContent.readFormats(item.getIdentifier());
      List<String> itemFormats = new ArrayList<>();
      for (final Content pickedContent : allContents) {
        itemFormats.add(pickedContent.getFormat());
      }
      itemMap.put("formats", itemFormats);

      // Add all the matching sets
      List<Set> allSets = daoSet.readAll();
      List<String> itemSets = new ArrayList<>();
      for (final Set pickedSet : allSets) {
        Map<String, String> xPaths = pickedSet.getxPaths();
        for (final Content pickedContent : allContents) {
          if (xPaths.containsKey(pickedContent.getFormat())) {
            final String xPathToCheck = xPaths.get(pickedContent.getFormat());
            if (XPathHelper.isTextValueMatching(pickedContent.getContent(), xPathToCheck)) {
              itemSets.add(pickedSet.getName());
            }
          }
        }
      }
      itemMap.put("sets", itemSets);

      IndexRequest indexRequest = new IndexRequest();

      indexRequest.index(ITEMS_ALIAS_INDEX_NAME);
      indexRequest.type("_doc");
      indexRequest.source(itemMap);
      indexRequest.id(item.getIdentifier());

      client.index(indexRequest, RequestOptions.DEFAULT);
      LOGGER.info("Added item to search index");
    }
  }

  /**
   * 
   * @param item @throws IOException @throws
   */
  @Override
  public void updateDocument(Item item) throws IOException {
    try (RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")))) {

      // TODO set and format matching for the update index

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
  public SearchResult<String> search(Integer rows, Object set, Object format, Date fromDate, Date untilDate,
      Item lastItem) throws IOException {

    LOGGER.info("DEBUG: rows: " + rows);
    LOGGER.info("DEBUG: format: " + format);
    LOGGER.info("DEBUG: lastItem: " + lastItem);

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

      if (set != null && !set.toString().isBlank()) {
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

      LOGGER.info("DEBUG: searchRequest: " + searchRequest.toString());

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
        LOGGER.info("searchSourceBuilder: " + searchSourceBuilder);
        LOGGER.info("newLastItemId: " + newLastItemId);
        LOGGER.info("newLastItem: " + newLastItem);

        Long timestamp = null;
        try {
          timestamp = Configuration.getDateformat().parse(newLastItem.getDatestamp()).getTime();
        } catch (ParseException e) {
          e.printStackTrace();
        }
        searchSourceBuilder.searchAfter(new Object[] { timestamp, newLastItem.getIdentifier() });
        searchRequest.source(searchSourceBuilder);

        LOGGER.info("DEBUG: currentLastItemId: " + newLastItemId);
        LOGGER.info("DEBUG: searchRequest next elements?: " + searchRequest.toString());
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
    LOGGER.info("CREATE status: indexName: " + indexName);
    LOGGER.info("CREATE status: mapping: " + mapping.substring(0, 30) + "...");
    if (StringUtils.isNotBlank(indexName) && StringUtils.isNotBlank(mapping)) {
      try (RestHighLevelClient client = new RestHighLevelClient(
          RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")))) {
        LOGGER.info("CREATE status: create request");
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        LOGGER.info("CREATE status: execute nd take response");
        CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
        LOGGER.info("CREATE status: createIndexResponse.isAcknowledged(): " + createIndexResponse.isAcknowledged());
        if (createIndexResponse.isAcknowledged()) {
          RestClient lowLevelClient = client.getLowLevelClient();

          LOGGER.info("CREATE status: build mapping");
          Request requestMapping = new Request("PUT", "/" + indexName + "/_mapping");
          requestMapping.setJsonEntity(mapping);
          LOGGER.info("CREATE status: put mapping");
          Response responseMapping = lowLevelClient.performRequest(requestMapping);
          LOGGER.info("CREATE status: responseMapping.getStatusLine().getStatusCode(): "
              + responseMapping.getStatusLine().getStatusCode());
//          AcknowledgedResponse putMappingResponse = client.indices().putMapping(requestMapping, RequestOptions.DEFAULT);
//          LOGGER.info("CREATE status: putMappingResponse.isAcknowledged(): " + putMappingResponse.isAcknowledged());
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
  public void reindexAll() throws IOException {
    if (reindexStatus != null && StringUtils.isBlank(reindexStatus.getEndTime())) {
      LOGGER.warn("REINDEX status: Reindex process already started since " + reindexStatus.getStartTime()
          + ". It will be blocked and restarted!");

      // Stop future process if already running
      if (reindexAllFuture != null) {
        while (!reindexAllFuture.isCancelled()) {
          reindexAllFuture.cancel(true);
          try {
            Thread.sleep(5000);
            LOGGER.warn("Attempt to stop the current Reindex process...");
          } catch (InterruptedException e) {
            // Wait a second for the concurrent task to be cancelled
          }
        }
        // Delete index in creation
        dropIndex(reindexStatus.getNewIndexName());
        LOGGER.info("REINDEX status: Index " + reindexStatus.getNewIndexName() + " dropped.");
      }
    }

    reindexStatus = new ReindexStatus();

    reindexStatus.setAliasName(ITEMS_ALIAS_INDEX_NAME);
    LOGGER.info("REINDEX status: Alias name: " + reindexStatus.getAliasName());

    reindexAllFuture = CompletableFuture.supplyAsync(() -> {

      try (RestHighLevelClient client = new RestHighLevelClient(
          RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")))) {
        GetAliasesRequest requestIndexWithAlias = new GetAliasesRequest(ITEMS_ALIAS_INDEX_NAME);
        GetAliasesResponse responseIndexWithAlias = client.indices().getAlias(requestIndexWithAlias,
            RequestOptions.DEFAULT);

        if (responseIndexWithAlias.getAliases().size() > 1) {
          Iterator<String> indexIterator = responseIndexWithAlias.getAliases().keySet().iterator();
          while (indexIterator.hasNext()) {
            reindexStatus.setOriginalIndexName(indexIterator.next().toString());
            LOGGER.info("REINDEX status: Original index name: " + reindexStatus.getOriginalIndexName());
            break;
          }
        }

        if (StringUtils.isBlank(reindexStatus.getOriginalIndexName())) {
          LOGGER.warn("REINDEX status: No existing indexes. Creating it from scratch.");
          reindexStatus.setOriginalIndexName(ITEMS_ALIAS_INDEX_NAME);
          LOGGER.info("REINDEX status: Original index name: " + reindexStatus.getOriginalIndexName());
          reindexStatus.setNewIndexName(ITEMS_ALIAS_INDEX_NAME + "1");
          LOGGER.info("REINDEX status: New index name: " + reindexStatus.getNewIndexName());
        } else {
          final String currentIndexVersionStr = reindexStatus.getOriginalIndexName()
              .substring(ITEMS_ALIAS_INDEX_NAME.length());
          final long currentIndexVersion = Long.parseLong(currentIndexVersionStr);
          final long newIndexVersion = currentIndexVersion + 1;
          final StringBuilder newIndexName = new StringBuilder();
          newIndexName.append(ITEMS_ALIAS_INDEX_NAME);
          newIndexName.append(String.valueOf(newIndexVersion));

          reindexStatus.setNewIndexName(newIndexName.toString());
          LOGGER.info("REINDEX status: New index name: " + reindexStatus.getNewIndexName());
        }

        if (StringUtils.isBlank(reindexStatus.getOriginalIndexName())
            || StringUtils.isBlank(reindexStatus.getNewIndexName())) {
          LOGGER.error("Not able to determine index names: original (" + reindexStatus.getOriginalIndexName()
              + ") or new (" + reindexStatus.getNewIndexName() + ")");
          return false;
        }

        LOGGER.info("REINDEX status: SKIP reindexing for testing alias renaming.");

//        MainResponse infoResponse = client.info(RequestOptions.DEFAULT);
//        String filenameItemsMapping = ITEMS_MAPPING_V6_FILENAME;
//        if (infoResponse.getVersion().after(Version.V_6_8_4)) {
//          filenameItemsMapping = ITEMS_MAPPING_V7_FILENAME;
//        }
//        LOGGER.info("REINDEX status: ES version found " + Version.displayVersion(infoResponse.getVersion(), false)
//            + " -> mapping " + filenameItemsMapping);
//
//        final String mapping = ResourcesUtils.getResourceFileAsString(filenameItemsMapping, servletContext);
//
//        if (StringUtils.isBlank(mapping)) {
//          LOGGER.error("REINDEX status: Not able to retrieve mapping " + filenameItemsMapping);
//        }
//
//
//        LOGGER.info("REINDEX status: Creating new index " + reindexStatus.getNewIndexName() + " with mapping "
//            + mapping.substring(0, 30) + "...");
//        if (!createIndex(reindexStatus.getNewIndexName(), mapping)) {
//          LOGGER.error(
//              "REINDEX status: Something went wrong while creating the new index " + reindexStatus.getNewIndexName());
//          return false;
//        }
//
//        reindexStatus.setTotalCount(daoItem.getCount());
//        reindexStatus.setItemResultSet(daoItem.getAllItemsResultSet());
//        LOGGER.info("REINDEX status: Total Items count: " + reindexStatus.getTotalCount());
//
//        if (reindexStatus.getTotalCount() < 1) {
//          LOGGER.warn("No items to reindex " + reindexStatus.getNewIndexName());
//          return false;
//        }
//
//        reindexStatus.setIndexedCount(0);
//        LOGGER.info("REINDEX status: Indexed Items count: " + reindexStatus.getIndexedCount());
//
//        reindexStatus.setStartTime(ZonedDateTime.now(ZoneOffset.UTC).toString());
//        LOGGER.info("REINDEX status: Start Time: " + reindexStatus.getStartTime());
//
//        Item mostRecentItem = null;
//
//        
//        do {
//          List<Item> bufferListItems = daoItem.getItemsFromResultSet(reindexStatus.getItemResultSet(), 100);
//
//          for (final Item pickedItem : bufferListItems) {
//            reindexDocument(pickedItem, reindexStatus.getNewIndexName(), client);
//            reindexStatus.setIndexedCount(reindexStatus.getIndexedCount() + 1);
//
//            // Keep the most recent Item identifier
//            if (mostRecentItem == null) {
//              mostRecentItem = pickedItem;
//            } else {
//              try {
//                if (Configuration.getDateformat().parse(mostRecentItem.getDatestamp())
//                    .before(Configuration.getDateformat().parse(pickedItem.getDatestamp()))) {
//                  mostRecentItem = pickedItem;
//                }
//              } catch (ParseException e) {
//                // leave mostRecentDateItem as it is
//              }
//            }
//          }
//
//          LOGGER.info("REINDEX status: " + reindexStatus.getIndexedCount() + " indexed out of "
//              + reindexStatus.getTotalCount() + ".");
//        } while (reindexStatus.getIndexedCount() < reindexStatus.getTotalCount());
//
//        // If in the meanwhile some new object has been inserted, reindex the new Items
//        if (daoItem.getCount() < reindexStatus.getIndexedCount()) {
//
//          LOGGER.warn("REINDEX status: New inserted items, current Items count " + daoItem.getCount()
//              + ", indexed count " + reindexStatus.getIndexedCount());
//
//          Date mostRecentItemDate = null;
//          try {
//            mostRecentItemDate = Configuration.getDateformat().parse(mostRecentItem.getDatestamp());
//          } catch (ParseException e) {
//            // Cannot establish a date from the most recent Item, do nothing
//          }
//
//          LOGGER.info("REINDEX status: most recent item reindexed date: "
//              + Configuration.getDateformat().format(mostRecentItemDate));
//
//          if (mostRecentItemDate != null) {
//            List<Format> allFormats = daoFormat.readAll();
//            List<Set> allSets = daoSet.readAll();
//
//            List<String> allFormatsStr = new ArrayList<String>();
//            List<String> allSetsStr = new ArrayList<String>();
//
//            for (final Format pickedFormat : allFormats) {
//              allFormatsStr.add(pickedFormat.getMetadataPrefix());
//            }
//            for (final Set pickedSet : allSets) {
//              allSetsStr.add(pickedSet.getName());
//            }
//
//            String nextLastItemIdentifier = mostRecentItem.getIdentifier();
//            do {
//              final Item lastItemToStart = daoItem.read(nextLastItemIdentifier);
//
//              nextLastItemIdentifier = null;
//              if (lastItemToStart != null) {
//
//                SearchResult<String> resultNewerItems = search(100, allFormatsStr, allSetsStr, mostRecentItemDate,
//                    new Date(), lastItemToStart);
//
//                for (String pickedItemIdentifier : resultNewerItems.getData()) {
//                  final Item newerItemRetrieved = daoItem.read(pickedItemIdentifier);
//                  if (newerItemRetrieved != null) {
//                    reindexDocument(newerItemRetrieved, reindexStatus.getNewIndexName(), client);
//                    reindexStatus.setIndexedCount(reindexStatus.getIndexedCount() + 1);
//                  }
//                }
//
//                if (!StringUtils.isBlank(resultNewerItems.getLastItemId())) {
//                  nextLastItemIdentifier = resultNewerItems.getLastItemId();
//                }
//              }
//            } while (!StringUtils.isBlank(nextLastItemIdentifier));
//          }
//        }

        // Switch alias from old index to new one
        RestClient lowLevelClient = client.getLowLevelClient();

        LOGGER.info("REINDEX status: Add new alias " + ITEMS_ALIAS_INDEX_NAME + " to index " + reindexStatus.getNewIndexName());
        Request requestNewAlias = new Request("POST", "/_aliases");
        requestNewAlias.setJsonEntity("{\n" + 
            "    \"actions\" : [\n" + 
            "        { \"add\" : { \"index\" : \"" + reindexStatus.getNewIndexName() + "\", \"alias\" : \"" + ITEMS_ALIAS_INDEX_NAME + "\" } }\n" + 
            "    ]\n" + 
            "}");
        LOGGER.info("CREATE status: execute new alias");
        Response responseNewAlias = lowLevelClient.performRequest(requestNewAlias);
        LOGGER.info("CREATE status: responseNewAlias.getStatusLine().getStatusCode()" + responseNewAlias.getStatusLine().getStatusCode());
        
        
//        IndicesAliasesRequest actionRequest = new IndicesAliasesRequest();
//
//        LOGGER.warn(
//            "REINDEX status: Add new alias " + ITEMS_ALIAS_INDEX_NAME + " to index " + reindexStatus.getNewIndexName());
//        AliasActions addNewIndexToAliasAction = new AliasActions(AliasActions.Type.ADD)
//            .index(reindexStatus.getNewIndexName()).alias(ITEMS_ALIAS_INDEX_NAME);
//        actionRequest.addAliasAction(addNewIndexToAliasAction);
//        LOGGER.warn("REINDEX status: execute new alias");
//        AcknowledgedResponse responseAliasNew = client.indices().updateAliases(actionRequest, RequestOptions.DEFAULT);
//        LOGGER.warn("REINDEX status: responseAliasNew.isAcknowledged(): " + responseAliasNew.isAcknowledged());
//        if (responseAliasNew.isAcknowledged()) {
//          LOGGER.warn("REINDEX status: Remove old alias " + ITEMS_ALIAS_INDEX_NAME + " to index "
//              + reindexStatus.getOriginalIndexName());
//          actionRequest = new IndicesAliasesRequest();
//          AliasActions removeOldIndexToAliasAction = new AliasActions(AliasActions.Type.REMOVE)
//              .index(reindexStatus.getOriginalIndexName()).alias(ITEMS_ALIAS_INDEX_NAME);
//          actionRequest.addAliasAction(removeOldIndexToAliasAction);
//          LOGGER.warn("REINDEX status: execute delete old alias");
//          AcknowledgedResponse responseAliasDeleteOld = client.indices().updateAliases(actionRequest,
//              RequestOptions.DEFAULT);
//          LOGGER.warn(
//              "REINDEX status: responseAliasDeleteOld.isAcknowledged(): " + responseAliasDeleteOld.isAcknowledged());
//
//          if (responseAliasDeleteOld.isAcknowledged()) {
            // Delete old index
            // TODO: uncomment it only when all the other previous steps are tested and
            // working!!!
//        AliasActions dropOldIndexAction = new AliasActions(AliasActions.Type.REMOVE_INDEX)
//            .index(reindexStatus.getOriginalIndexName());
//        actionRequest.addAliasAction(dropOldIndexAction);
//          }
//        }

      } catch (IOException e) {
        LOGGER.error("Something went wrong while processing the new index " + reindexStatus.getNewIndexName(), e);
        return false;
      }
      return true;

    });

  }

  private void reindexDocument(Item item, String indexName, RestHighLevelClient client) throws IOException {

    Map<String, Object> itemMap = item.toMap();

    // Add all available formats
    List<Content> allContents = daoContent.readFormats(item.getIdentifier());
    List<String> itemFormats = new ArrayList<String>();
    for (final Content pickedContent : allContents) {
      itemFormats.add(pickedContent.getFormat());
    }
    itemMap.put("formats", itemFormats);

    // Add all the matching sets
    List<Set> allSets = daoSet.readAll();
    List<String> itemSets = new ArrayList<String>();
    for (final Set pickedSet : allSets) {
      Map<String, String> xPaths = pickedSet.getxPaths();
      for (final Content pickedContent : allContents) {
        if (xPaths.containsKey(pickedContent.getFormat())) {
          final String xPathToCheck = xPaths.get(pickedContent.getFormat());
          if (XPathHelper.isTextValueMatching(pickedContent.getContent(), xPathToCheck)) {
            itemSets.add(pickedSet.getName());
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

}
