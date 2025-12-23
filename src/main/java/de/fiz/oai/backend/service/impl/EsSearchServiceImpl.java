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
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
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

import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.models.reindex.ReindexStatus;
import de.fiz.oai.backend.service.ItemService;
import de.fiz.oai.backend.service.SearchService;
import de.fiz.oai.backend.utils.Configuration;
import de.fiz.oai.backend.utils.ResourcesUtils;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.core.Context;

@Service
@Singleton
public class EsSearchServiceImpl implements SearchService {

    private static Logger LOGGER = LoggerFactory.getLogger(EsSearchServiceImpl.class);

    String elastisearchHost = Configuration.getInstance().getProperty("elasticsearch.host", "localhost");

    int elastisearchPort = Integer.parseInt(Configuration.getInstance().getProperty("elasticsearch.port", "8082"));

    public static String ITEMS_ALIAS_INDEX_NAME = "items";

    public static String ITEMS_MAPPING_V7_FILENAME = "/WEB-INF/classes/elasticsearch/item_mapping_es_v7";
    public static String ITEMS_MAPPING_V7_FILENAME_UPDATE_MAPPING = "/WEB-INF/classes/elasticsearch/item_mapping_es_v7_update_mapping";

    @Context
    ServletContext servletContext;

    @Inject
    Provider<ItemService> itemProvider;

    @Inject
    DAOItem daoItem;


    private ReindexStatus reindexStatus = null;

    private CompletableFuture<Boolean> reindexAllFuture;

    private final AtomicBoolean reindexRunning = new AtomicBoolean(false);

    /**
     * @param items @throws IOException @throws
     */
    @Override
    public List<Map<String, Object>> readDocuments(Collection<Item> items) throws IOException {
        List<Map<String, Object>> documents = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(items)) {
            RestHighLevelClient client = getElasticsearchClient();
            try {
                MultiGetRequest mgetRequest = new MultiGetRequest();
                List<String> identifiers = new ArrayList<>();
                for (Item item : items) {
                    identifiers.add(item.getIdentifier());
                }
                for (String identifier : identifiers) {
                    mgetRequest.add(new MultiGetRequest.Item(ITEMS_ALIAS_INDEX_NAME, identifier));
                }
                MultiGetResponse response = client.mget(mgetRequest, RequestOptions.DEFAULT);

                // Map documents by their ID for quick lookup
                Map<String, Map<String, Object>> docMap = new LinkedHashMap<>();
                for (MultiGetItemResponse itemResponse : response.getResponses()) {
                    Map<String, Object> sourceMap = itemResponse.getResponse().getSourceAsMap();
                    if (sourceMap != null && StringUtils.isNotBlank((String) sourceMap.get("identifier"))) {
                        docMap.put((String) sourceMap.get("identifier"), sourceMap);
                    }
                }

                // Reorder documents to match the input ID order
                for (String identifier : identifiers) {
                    if (docMap.get(identifier) != null) {
                        documents.add(docMap.get(identifier));
                    } else {
                        LOGGER.warn("Couldn't find item with id " + identifier + " in search-index.");
                    }
                }
            } finally {
                closeElasticsearchClient(client);
            }
        }
        return documents;
    }

    /**
     * Create new item in index.
     *
     * @param item The item to create
     * @throws IOException
     */
    @Override
    public void createDocument(Item item) throws IOException {
        indexDocument(item, ITEMS_ALIAS_INDEX_NAME);
        LOGGER.info("Added item " + item.getIdentifier() + " to search index.");
    }


    private void indexDocument(Item item, String indexName) throws IOException {
        RestHighLevelClient client = getElasticsearchClient();
        try {
            Map<String, Object> itemMap = item.toMap();

            IndexRequest indexRequest = new IndexRequest();
            indexRequest.index(indexName);
            indexRequest.type("_doc");
            indexRequest.source(itemMap);
            indexRequest.id(item.getIdentifier());

            IndexResponse response = client.index(indexRequest, RequestOptions.DEFAULT);

            if (response == null || response.getResult() == null) {
                throw new IOException("Elasticsearch response is null or malformed.");
            }
        } finally {
            closeElasticsearchClient(client);
        }
    }

    /**
     * Update item in index.
     *
     * @param item The item to update
     * @throws IOException
     */
    @Override
    public void updateDocument(Item item) throws IOException {
        RestHighLevelClient client = getElasticsearchClient();
        try {
            Map<String, Object> itemMap = item.toMap();

            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.index(ITEMS_ALIAS_INDEX_NAME);
            updateRequest.type("_doc");
            updateRequest.id(item.getIdentifier());
            updateRequest.doc(itemMap);

            client.update(updateRequest, RequestOptions.DEFAULT);
            LOGGER.info("Updated item " + item.getIdentifier() + " in search index.");
        } finally {
            closeElasticsearchClient(client);
        }
    }

    /**
     * @param item @throws IOException @throws
     */
    @Override
    public void deleteDocument(Item item) throws IOException {
        RestHighLevelClient client = getElasticsearchClient();
        try {
            DeleteRequest request = new DeleteRequest();
            request.index(ITEMS_ALIAS_INDEX_NAME);
            request.type("_doc");
            request.id(item.getIdentifier());

            client.delete(request, RequestOptions.DEFAULT);
        } finally {
            closeElasticsearchClient(client);
        }
    }

    @Override
    public SearchResult<String> search(Integer rows, String set, String format, Date fromDate, Date untilDate,
                                       String searchMark) throws IOException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("rows: {}", rows);
            LOGGER.debug("format: {}", format);
            LOGGER.debug("searchMark: {}", searchMark);
        }

        RestHighLevelClient client = getElasticsearchClient();
        try {
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
            searchSourceBuilder.trackTotalHits(true);


            if (StringUtils.isNotBlank(searchMark)) {
                Long timestamp = null;
                Item lastItem = daoItem.read(searchMark);
                if (lastItem != null) {
                    //Read the timestamp from the Index!!! Reading the timestamp from the cassandra item can return adifferent value
                    //and than search_after will not work any more
                    List<Map<String, Object>> itemDocs = readDocuments(List.of(lastItem));
                    if (CollectionUtils.isNotEmpty(itemDocs)) {
                        LOGGER.info("itemDoc: {}", itemDocs.get(0));
                        try {
                            timestamp = Configuration.getDateformat().parse((String) itemDocs.get(0).get("datestamp")).getTime();
                        } catch (ParseException e) {
                            LOGGER.warn(e.getMessage());
                        }
                    } else {
                        LOGGER.warn("Item for searchMark {} not found", searchMark);
                    }
                } else {
                    LOGGER.warn("Item for searchMark {} not found", searchMark);
                }
                searchSourceBuilder.searchAfter(new Object[]{timestamp, lastItem.getIdentifier()});
                searchSourceBuilder.from(0);
            }

            SearchRequest searchRequest = new SearchRequest(ITEMS_ALIAS_INDEX_NAME);
            searchRequest.source(searchSourceBuilder);

            LOGGER.debug("searchRequest: {}", searchRequest.toString());

            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            LOGGER.debug("searchResponse: {}", searchResponse.toString());

            SearchHits searchHits = searchResponse.getHits();
            Iterator<SearchHit> iterator = searchHits.iterator();
            List<String> idsRetrieved = new ArrayList<>();

            while (iterator.hasNext()) {
                SearchHit searchHit = iterator.next();
                idsRetrieved.add(searchHit.getId());
            }

            SearchResult<String> idResult = new SearchResult<>();
            idResult.setSize(idsRetrieved.size());
            idResult.setTotal(searchResponse.getHits().getTotalHits().value);
            idResult.setData(idsRetrieved);

            // Send the searchMark if there are elements after it
            String newSearchMark = null;
            if (idsRetrieved.size() > 0) {
                newSearchMark = idsRetrieved.get(idsRetrieved.size() - 1);
                idResult.setSearchMark(newSearchMark);
            }
            Item newLastItem = null;
            if (StringUtils.isNotBlank(newSearchMark)) {

                newLastItem = daoItem.read(newSearchMark);
                LOGGER.info("searchSourceBuilder: {}", searchSourceBuilder);
                LOGGER.info("searchMark: {}", newSearchMark);
                LOGGER.info("newLastItem: {}", newLastItem);

                Long timestamp = null;
                try {
                    timestamp = Configuration.getDateformat().parse(newLastItem.getDatestamp()).getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                searchSourceBuilder.searchAfter(new Object[]{timestamp, newLastItem.getIdentifier()});
                searchRequest.source(searchSourceBuilder);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("newSearchMark: {}", newSearchMark);
                    LOGGER.debug("searchRequest next elements?: {}", searchRequest.toString());
                }
                searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
                if (searchResponse.getHits().getHits().length == 0) {
                    idResult.setSearchMark(null);
                }
            }

            return idResult;

        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            closeElasticsearchClient(client);
        }

    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean createIndex(final String indexName, final String mapping) throws IOException {
        if (StringUtils.isNotBlank(indexName) && StringUtils.isNotBlank(mapping)) {
            RestHighLevelClient client = getElasticsearchClient();
            try {
                CreateIndexRequest request = new CreateIndexRequest(indexName);
                CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
                if (createIndexResponse.isAcknowledged()) {
                    RestClient lowLevelClient = client.getLowLevelClient();

                    Request requestMapping = new Request("PUT", "/" + indexName + "/_mapping");
                    requestMapping.setJsonEntity(mapping);
                    Response responseMapping = lowLevelClient.performRequest(requestMapping);
                    if (responseMapping.getStatusLine().getStatusCode() == HttpStatus.SC_OK
                            || responseMapping.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                        return true;
                    }
                }
            } finally {
                closeElasticsearchClient(client);
            }
        }
        LOGGER.info("CREATE status: something went wrong, return false");
        return false;
    }

    @Override
    public void dropIndex(final String indexName) throws IOException {
        if (StringUtils.isNotBlank(indexName)) {
            RestHighLevelClient client = getElasticsearchClient();
            try {
                DeleteIndexRequest request = new DeleteIndexRequest(indexName);
                client.indices().delete(request, RequestOptions.DEFAULT);
            } finally {
                closeElasticsearchClient(client);
            }
        }
    }

    @Override
    public void commit() throws IOException {
        RestHighLevelClient client = getElasticsearchClient();
        try {
            RefreshRequest request = new RefreshRequest(ITEMS_ALIAS_INDEX_NAME);
            client.indices().refresh(request, RequestOptions.DEFAULT);
        } finally {
            closeElasticsearchClient(client);
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
        return reindexAll(null);
    }


    @Override
    public boolean reindexAll(String indexName) {
        // ------------------------------------------------------
        // Atomic guard – only one process allowed at a time
        // ------------------------------------------------------
        if (!reindexRunning.compareAndSet(false, true)) {
            LOGGER.warn("[STATUS] Reindex '{}' already running – aborting new start");
            return false;
        }

        ItemService itemService = itemProvider.get();

        reindexStatus = new ReindexStatus();

        reindexStatus.setStopSignalReceived(false);

        reindexStatus.setAliasName(ITEMS_ALIAS_INDEX_NAME);
        LOGGER.info("REINDEX status: Alias name: {}", reindexStatus.getAliasName());

        reindexAllFuture = CompletableFuture.supplyAsync(() -> {

            RestHighLevelClient client = getElasticsearchClient();
            try {
                if (StringUtils.isBlank(indexName)) {
                    if (!createNewIndex(client)) {
                        return false;
                    }
                }
                else {
                    if (!checkIndexExists(client, indexName)) {
                        LOGGER.error("Index with name {} doesnt exist", indexName);
                        return false;
                    }
                    reindexStatus.setNewIndexName(indexName);
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

                List<Item> bufferListItems = null;

                do {
                    bufferListItems = daoItem.getItemsFromResultSet(reindexStatus.getItemResultSet(), 500);
                    boolean reindexAllStopOnException = Boolean.parseBoolean(Configuration.getInstance().getProperty(
                            "elasticsearch.reindexAllStopOnException", "false"));
                    AtomicBoolean stop = new AtomicBoolean(false);
                    bufferListItems.parallelStream().forEach(item -> {
                        if (stop.get()) return;
                        try {
                            if (StringUtils.isBlank(indexName) || !itemExistsInIndex(client, indexName, item.getIdentifier())) {
                                LOGGER.debug("Reindex now " + item.getIdentifier());
                                itemService.addFormatsAndSets(item);
                                indexDocument(item, reindexStatus.getNewIndexName());
                            }
                            else {
                                LOGGER.debug("Don't reindex " + item.getIdentifier() + " as it already exists in the index");
                            }
                        } catch (Exception e) {
                            LOGGER.error("Reindex fails for " + item.getIdentifier(), e);
                            if (reindexAllStopOnException) {
                                stop.set(true);
                            }
                        } finally {
                            reindexStatus.setIndexedCount(reindexStatus.getIndexedCount() + 1);
                        }
                    });

                    LOGGER.info("REINDEX status: " + reindexStatus.getIndexedCount() + " indexed out of "
                            + reindexStatus.getTotalCount() + ".");
                } while (!reindexStatus.isStopSignalReceived() && !bufferListItems.isEmpty());

                // If in the meanwhile some new object has been inserted, reindex the new Items
                if (!reindexStatus.isStopSignalReceived()) {
                    RestClient lowLevelClient = client.getLowLevelClient();
                    // Switch alias from old index to new one
                    LOGGER.info("REINDEX status: Remove all old aliases of {}", ITEMS_ALIAS_INDEX_NAME);
                    List<String> allIndices = getAllIndexNames(client);
                    for (final String pickedIndex : allIndices) {
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
                closeElasticsearchClient(client);
                reindexStatus.setEndTime(ZonedDateTime.now(ZoneOffset.UTC).toString());
                LOGGER.info("REINDEX status: End Time: {}", reindexStatus.getEndTime());

                reindexRunning.set(false);
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

    /**
     * Check if a Document with given identifier exists in index.
     *
     * @param client
     * @param itemIdentifier
     * @return
     */
    private boolean itemExistsInIndex(RestHighLevelClient client, String indexName, String itemIdentifier) throws IOException {
        GetRequest getRequest = new GetRequest(indexName, itemIdentifier);
        GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
        return getResponse.isExists();
    }

    /**
     * Get all Index-Names.
     *
     * @param client
     * @return
     * @throws IOException
     */
    private List<String> getAllIndexNames(RestHighLevelClient client) throws IOException {
        GetIndexRequest requestIndex = new GetIndexRequest("*");
        GetIndexResponse responseIndex = client.indices().get(requestIndex, RequestOptions.DEFAULT);
        String[] index = responseIndex.getIndices();
        return List.of(index);
    }

    /**
     * Check if Index with given indexName exists.
     *
     * @param client
     * @param indexName
     * @return
     * @throws IOException
     */
    private boolean checkIndexExists(RestHighLevelClient client, String indexName) throws IOException {
        GetIndexRequest requestIndex = new GetIndexRequest(indexName);
        GetIndexResponse responseIndex = client.indices().get(requestIndex, RequestOptions.DEFAULT);
        String[] index = responseIndex.getIndices();
        if (index != null && index.length > 0) {
            return true;
        }
        return false;
    }

    /**
     *
     * @return
     */
    private boolean createNewIndex(RestHighLevelClient client) throws IOException {
        List<String> allIndices = getAllIndexNames(client);

        LOGGER.info("REINDEX status: Found " + allIndices.size() + " indexes:");
        int maximumIndexFound = 0;
        for (final String pickedIndex : allIndices) {
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

        if (StringUtils.isBlank(reindexStatus.getNewIndexName())) {
            LOGGER.error("Not able to determine index names: original (" + reindexStatus.getOriginalIndexName()
                                 + ") or new (" + reindexStatus.getNewIndexName() + ")");
            return false;
        }
        final String filenameItemsMapping = ITEMS_MAPPING_V7_FILENAME_UPDATE_MAPPING;
        final String mapping = ResourcesUtils.getResourceFileAsString(filenameItemsMapping, servletContext);
        if (StringUtils.isBlank(mapping)) {
            LOGGER.error("REINDEX status: Not able to retrieve mapping {}", filenameItemsMapping);
        }

        RestClient lowLevelClient = client.getLowLevelClient();

        if (StringUtils.isBlank(reindexStatus.getOriginalIndexName())) {
            if (!createFirstAliasForIndex(lowLevelClient, mapping)) {
                return false;
            }
        }

        if (!createIndex(reindexStatus.getNewIndexName(), mapping)) {
            LOGGER.error(
                    "REINDEX status: Something went wrong while creating the new index " + reindexStatus.getNewIndexName());
            return false;
        }
        return true;
    }

    /**
     * Execute if first Alias for index is created. Create Alias.
     *
     * @param lowLevelClient
     * @param mapping
     * @throws IOException
     */
    private boolean createFirstAliasForIndex(RestClient lowLevelClient, String mapping) throws IOException {
        LOGGER.warn("No previous indices found.");
        reindexStatus.setOriginalIndexName(ITEMS_ALIAS_INDEX_NAME + "0");
        if (!createIndex(reindexStatus.getOriginalIndexName(), mapping)) {
            LOGGER.error("REINDEX status: Something went wrong while creating the first index " + reindexStatus.getOriginalIndexName());
            return false;
        }
        Request requestNewAlias = new Request("POST", "/_aliases");
        requestNewAlias.setJsonEntity(
                "{\n" + "    \"actions\" : [\n" + "        { \"add\" : { \"index\" : \"" + reindexStatus.getOriginalIndexName()
                        + "\", \"alias\" : \"" + ITEMS_ALIAS_INDEX_NAME + "\" } }\n" + "    ]\n" + "}");
        lowLevelClient.performRequest(requestNewAlias);
        return true;
    }

    /**
     * Check elasticsearchClient synchronized and return it.
     *
     * @return RestHighLevelClient
     * @throws IOException
     */
    private synchronized RestHighLevelClient getElasticsearchClient() {
        return new RestHighLevelClient(RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")));
    }

    private void closeElasticsearchClient(RestHighLevelClient client) {
        if (client != null) {
            try {
                client.getLowLevelClient().close();
            } catch (Exception e) {
            }
        }
    }

}
