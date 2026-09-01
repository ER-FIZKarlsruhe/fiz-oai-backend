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
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.elasticsearch.indices.PutMappingResponse;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesResponse;

import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.models.reindex.ReindexStatus;
import de.fiz.oai.backend.service.ItemService;
import de.fiz.oai.backend.service.SearchService;
import de.fiz.oai.backend.utils.Configuration;
import de.fiz.oai.backend.utils.ElasticsearchClientManager;
import de.fiz.oai.backend.utils.ResourcesUtils;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.core.Context;

@Service
@Singleton
public class EsSearchServiceImpl implements SearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsSearchServiceImpl.class);

    public static final String ITEMS_ALIAS_INDEX_NAME = "items";

    public static final String ITEMS_MAPPING_V7_FILENAME_UPDATE_MAPPING = "/WEB-INF/classes/elasticsearch/item_mapping_es_v7_update_mapping";

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
     * Reads documents from the Elasticsearch index for the given collection of items.
     * <p>
     * The documents are returned in the same order as the input items. If a document
     * for a given item identifier cannot be found, it is skipped and a warning is logged.
     *
     * @param items the collection of {@link Item} objects whose indexed documents should be read
     * @return a list of document source maps retrieved from Elasticsearch
     * @throws IOException if an error occurs while communicating with Elasticsearch
     */

    @Override
    public List<Map<String, Object>> readDocuments(Collection<Item> items) throws IOException {
        List<Map<String, Object>> documents = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(items)) {
            ElasticsearchClient client = getElasticsearchClient();
            List<String> identifiers = new ArrayList<>();
            for (Item item : items) {
                identifiers.add(item.getIdentifier());
            }

            MgetResponse<Map> response = client.mget(m -> m.index(ITEMS_ALIAS_INDEX_NAME).ids(identifiers), Map.class);

            // Map documents by their ID for quick lookup
            Map<String, Map<String, Object>> docMap = new LinkedHashMap<>();
            for (MultiGetResponseItem<Map> itemResponse : response.docs()) {
                if (itemResponse.isResult() && itemResponse.result().found()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> sourceMap = (Map<String, Object>) itemResponse.result().source();
                    if (sourceMap != null && StringUtils.isNotBlank((String) sourceMap.get("identifier"))) {
                        docMap.put((String) sourceMap.get("identifier"), sourceMap);
                    }
                }
            }

            // Reorder documents to match the input ID order
            for (String identifier : identifiers) {
                if (docMap.get(identifier) != null) {
                    documents.add(docMap.get(identifier));
                } else {
                    LOGGER.warn("Couldn't find item with id {} in search-index.", identifier);
                }
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
        LOGGER.info("Added item {} to search index.", item.getIdentifier());
    }


    private void indexDocument(Item item, String indexName) throws IOException {
        ElasticsearchClient client = getElasticsearchClient();
        Map<String, Object> itemMap = item.toMap();

        client.index(i -> i.index(indexName).id(item.getIdentifier()).document(itemMap));
    }

    /**
     * Update item in index.
     *
     * @param item The item to update
     * @throws IOException
     */
    @Override
    public void updateDocument(Item item) throws IOException {
        ElasticsearchClient client = getElasticsearchClient();
        Map<String, Object> itemMap = item.toMap();

        client.update(u -> u.index(ITEMS_ALIAS_INDEX_NAME).id(item.getIdentifier()).doc(itemMap), Map.class);
        LOGGER.info("Updated item {} in search index.", item.getIdentifier());
    }

    /**
     * Deletes the given item from the Elasticsearch index.
     *
     * @param item the {@link Item} to delete from the index
     * @throws IOException if an error occurs while communicating with Elasticsearch
     */

    @Override
    public void deleteDocument(Item item) throws IOException {
        ElasticsearchClient client = getElasticsearchClient();
        client.delete(d -> d.index(ITEMS_ALIAS_INDEX_NAME).id(item.getIdentifier()));
    }


    /**
     * Executes a search query against the Elasticsearch index.
     * <p>
     * The search supports pagination via {@code searchMark}, filtering by set and format,
     * and date range filtering using {@code fromDate} and {@code untilDate}.
     *
     * @param rows the maximum number of results to return
     * @param set the optional OAI-PMH set filter
     * @param format the metadata format to filter by
     * @param fromDate the optional start date (inclusive) for datestamp filtering
     * @param untilDate the optional end date (inclusive) for datestamp filtering
     * @param searchMark the pagination marker indicating where to continue the search
     * @return a {@link SearchResult} containing item identifiers and pagination information
     * @throws IOException if an error occurs while executing the search
     */

    @Override
    public SearchResult<String> search(Integer rows, String set, String format, Date fromDate, Date untilDate,
                                       String searchMark) throws IOException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("rows: {}", rows);
            LOGGER.debug("format: {}", format);
            LOGGER.debug("searchMark: {}", searchMark);
        }

        ElasticsearchClient client = getElasticsearchClient();
        try {
            Date finalFromDate = new SimpleDateFormat("yyyy-MM-dd").parse("0001-01-01");
            Date finalUntilDate = new SimpleDateFormat("yyyy-MM-dd").parse("9999-12-31");

            if (fromDate != null) {
                finalFromDate = fromDate;
            }
            if (untilDate != null) {
                finalUntilDate = untilDate;
            }

            final String fromDateString = Configuration.getDateformat().format(finalFromDate);
            final String untilDateString = Configuration.getDateformat().format(finalUntilDate);

            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
            boolQueryBuilder.filter(f -> f.range(r -> r.date(d -> d.field("datestamp").gte(fromDateString).lte(untilDateString))));
            boolQueryBuilder.filter(f -> f.term(t -> t.field("formats").value(format)));

            if (StringUtils.isNotBlank(set)) {
                boolQueryBuilder.filter(f -> f.term(t -> t.field("sets").value(set)));
            }

            Query query = new Query.Builder().bool(boolQueryBuilder.build()).build();

            List<SortOptions> sortOptions = List.of(
                    SortOptions.of(s -> s.field(f -> f.field("datestamp"))),
                    SortOptions.of(s -> s.field(f -> f.field("identifier"))));

            List<FieldValue> searchAfterValues = null;

            if (StringUtils.isNotBlank(searchMark)) {
                Long timestamp = null;
                Item lastItem = daoItem.read(searchMark);
                if (lastItem != null) {
                    //Read the timestamp from the Index!!! Reading the timestamp from the cassandra item can return adifferent value
                    //and than search_after will not work any more
                    List<Map<String, Object>> itemDocs = readDocuments(List.of(lastItem));
                    if (CollectionUtils.isNotEmpty(itemDocs)) {
                        LOGGER.info("itemDoc: {}", itemDocs.getFirst());
                        try {
                            timestamp = Configuration.getDateformat().parse((String) itemDocs.getFirst().get("datestamp")).getTime();
                        } catch (ParseException e) {
                            LOGGER.warn(e.getMessage());
                        }
                    } else {
                        LOGGER.warn("Item for searchMark {} not found", searchMark);
                    }
                } else {
                    LOGGER.warn("Item for searchMark {} not found", searchMark);
                }
                searchAfterValues = List.of(FieldValue.of(timestamp), FieldValue.of(lastItem.getIdentifier()));
            }

            SearchRequest searchRequest = buildSearchRequest(query, sortOptions, rows, searchAfterValues);

            LOGGER.debug("searchRequest: {}", searchRequest);

            SearchResponse<Void> searchResponse = client.search(searchRequest, Void.class);

            LOGGER.debug("searchResponse: {}", searchResponse.toString());

            List<Hit<Void>> searchHits = searchResponse.hits().hits();
            List<String> idsRetrieved = new ArrayList<>();

            for (Hit<Void> searchHit : searchHits) {
                idsRetrieved.add(searchHit.id());
            }

            SearchResult<String> idResult = new SearchResult<>();
            idResult.setSize(idsRetrieved.size());
            idResult.setTotal(searchResponse.hits().total().value());
            idResult.setData(idsRetrieved);

            // Send the searchMark if there are elements after it
            String newSearchMark = null;
            if (!idsRetrieved.isEmpty()) {
                newSearchMark = idsRetrieved.get(idsRetrieved.size() - 1);
                idResult.setSearchMark(newSearchMark);
            }
            if (StringUtils.isNotBlank(newSearchMark)) {

                Item newLastItem = daoItem.read(newSearchMark);
                LOGGER.info("searchRequest: {}", searchRequest);
                LOGGER.info("searchMark: {}", newSearchMark);
                LOGGER.info("newLastItem: {}", newLastItem);

                Long timestamp = null;
                try {
                    timestamp = Configuration.getDateformat().parse(newLastItem.getDatestamp()).getTime();
                } catch (ParseException e) {
                    LOGGER.error(e.getMessage(), e);
                }

                SearchRequest nextSearchRequest = buildSearchRequest(query, sortOptions, rows,
                        List.of(FieldValue.of(timestamp), FieldValue.of(newLastItem.getIdentifier())));

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("newSearchMark: {}", newSearchMark);
                    LOGGER.debug("searchRequest next elements?: {}", nextSearchRequest);
                }
                SearchResponse<Void> nextSearchResponse = client.search(nextSearchRequest, Void.class);
                if (nextSearchResponse.hits().hits().isEmpty()) {
                    idResult.setSearchMark(null);
                }
            }

            return idResult;

        } catch (Exception e) {
            throw new IOException(e);
        }

    }

    /**
     * Builds a fresh, immutable search request for the {@code items} alias with the given
     * query/sort, optionally continuing after the given {@code search_after} values.
     */
    private SearchRequest buildSearchRequest(Query query, List<SortOptions> sortOptions, Integer rows,
                                              List<FieldValue> searchAfterValues) {
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .index(ITEMS_ALIAS_INDEX_NAME)
                .query(query)
                .sort(sortOptions)
                .size(rows)
                .source(src -> src.fetch(false))
                .trackTotalHits(t -> t.enabled(true));

        if (searchAfterValues != null) {
            builder.searchAfter(searchAfterValues).from(0);
        }

        return builder.build();
    }


    /**
     * Creates a new Elasticsearch index with the given name and mapping.
     * <p>
     * The mapping resource is a bare {@code {"properties": {...}}} body (meant for the
     * {@code PUT /{index}/_mapping} endpoint), so the index is first created empty and the
     * mapping is applied to it in a second call, rather than folded into the create-index body.
     *
     * @param indexName the name of the index to create
     * @param mapping the JSON mapping definition for the index
     * @return {@code true} if the index and mapping were created successfully, {@code false} otherwise
     * @throws IOException if an error occurs while communicating with Elasticsearch
     */

    @Override
    public boolean createIndex(final String indexName, final String mapping) throws IOException {
        if (StringUtils.isNotBlank(indexName) && StringUtils.isNotBlank(mapping)) {
            ElasticsearchClient client = getElasticsearchClient();
            CreateIndexResponse createIndexResponse = client.indices().create(c -> c.index(indexName));
            if (createIndexResponse.acknowledged()) {
                try (StringReader mappingReader = new StringReader(mapping)) {
                    PutMappingResponse putMappingResponse = client.indices().putMapping(p -> p.index(indexName).withJson(mappingReader));
                    if (putMappingResponse.acknowledged()) {
                        return true;
                    }
                }
            }
        }
        LOGGER.info("CREATE status: something went wrong, return false");
        return false;
    }


    /**
     * Drops (deletes) the Elasticsearch index with the given name.
     *
     * @param indexName the name of the index to delete
     * @throws IOException if an error occurs while communicating with Elasticsearch
     */

    @Override
    public void dropIndex(final String indexName) throws IOException {
        if (StringUtils.isNotBlank(indexName)) {
            ElasticsearchClient client = getElasticsearchClient();
            client.indices().delete(d -> d.index(indexName));
        }
    }

    @Override
    public void commit() throws IOException {
        ElasticsearchClient client = getElasticsearchClient();
        client.indices().refresh(r -> r.index(ITEMS_ALIAS_INDEX_NAME));
    }

    @Override
    public boolean stopReindexAll(final int stopAttempts, final int millisecondsAttemptsDelay) {
        boolean stopped = true;

        // Stop future process if already running
        if (reindexStatus != null && StringUtils.isBlank(reindexStatus.getEndTime())) {
            reindexStatus.setStopSignalReceived(true);
            if (reindexAllFuture != null) {
                int attempt = 0;
                // CompletableFuture.cancel(true) does not actually interrupt a supplyAsync task
                // (its mayInterruptIfRunning has no effect there) - it only marks the future itself
                // cancelled, which happens almost immediately regardless of whether the background
                // task is still running. So this loop just gives the cooperative stop signal above
                // a moment to be noticed; it must NOT null out reindexStatus afterwards, since the
                // still-running task keeps dereferencing that same field until its own finally block
                // (which sets the end time and clears reindexRunning) completes.
                while (!reindexAllFuture.isCancelled() && attempt <= stopAttempts) {
                    attempt++;
                    reindexAllFuture.cancel(true);
                    try {
                        Thread.sleep(millisecondsAttemptsDelay);
                        LOGGER.warn("Attempt {} of {} to stop the current Reindex process...", attempt, stopAttempts);
                    } catch (InterruptedException e) {
                        stopped = false;
                    }
                }
            }
        }

        if (stopped) {
            LOGGER.info("Current reindex process stopped.");
        } else {
            LOGGER.warn("Current reindex process NOT stopped!");
        }

        return stopped;
    }


    /**
     * Starts a full reindex process using an automatically generated index name.
     *
     * @return {@code true} if the reindex process was started successfully, {@code false} otherwise
     */

    @Override
    public boolean reindexAll() {
        return reindexAll(null);
    }

    /**
     * Starts a full reindex process using the given index name.
     * <p>
     * If an index name is provided, it must already exist.
     * Only one reindex process can run at a time.
     *
     * @param indexName the name of an existing index to reindex into, or {@code null} to create a new one
     * @return {@code true} if the reindex process was started successfully, {@code false} otherwise
     */
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

            ElasticsearchClient client = getElasticsearchClient();
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

                List<Item> bufferListItems;

                do {
                    bufferListItems = daoItem.getItemsFromResultSet(reindexStatus.getItemResultSet(), 500);
                    boolean reindexAllStopOnException = Boolean.parseBoolean(Configuration.getInstance().getProperty(
                            "elasticsearch.reindexAllStopOnException", "false"));

                    // One multi-get for the whole batch instead of one existence-check GET per item.
                    Set<String> existingIdentifiers = StringUtils.isBlank(indexName)
                            ? Collections.emptySet()
                            : findExistingIdentifiers(client, indexName, bufferListItems);

                    AtomicBoolean stop = new AtomicBoolean(false);
                    // Counted locally and folded into reindexStatus once after the parallel section
                    // below completes, instead of every thread racily doing indexedCount = indexedCount + 1
                    // on the shared status object.
                    AtomicLong batchAttemptedCount = new AtomicLong();
                    Collection<Item> itemsToIndex = new ConcurrentLinkedQueue<>();

                    bufferListItems.parallelStream().forEach(item -> {
                        if (stop.get()) return;
                        try {
                            if (existingIdentifiers.contains(item.getIdentifier())) {
                                LOGGER.debug("Don't reindex {} as it already exists in the index", item.getIdentifier());
                            } else {
                                LOGGER.debug("Reindex now {}", item.getIdentifier());
                                itemService.addFormatsAndSets(item);
                                itemsToIndex.add(item);
                            }
                        } catch (Exception e) {
                            LOGGER.error("Reindex fails for {}", item.getIdentifier(), e);
                            if (reindexAllStopOnException) {
                                stop.set(true);
                            }
                        } finally {
                            batchAttemptedCount.incrementAndGet();
                        }
                    });

                    // One bulk request for the whole batch instead of one index() call per item.
                    if (!itemsToIndex.isEmpty()) {
                        bulkIndexDocuments(client, itemsToIndex, reindexStatus.getNewIndexName());
                    }

                    reindexStatus.setIndexedCount(reindexStatus.getIndexedCount() + batchAttemptedCount.get());

                    LOGGER.info("REINDEX status: {} indexed out of {}.", reindexStatus.getIndexedCount(), reindexStatus.getTotalCount());
                } while (!reindexStatus.isStopSignalReceived() && !bufferListItems.isEmpty());

                // If in the meanwhile some new object has been inserted, reindex the new Items
                if (!reindexStatus.isStopSignalReceived()) {
                    // Switch alias from old index to new one in a single atomic request
                    LOGGER.info("REINDEX status: Remove all old aliases of {}", ITEMS_ALIAS_INDEX_NAME);
                    List<String> allIndices = getAllIndexNames(client);
                    final String newIndexName = reindexStatus.getNewIndexName();

                    UpdateAliasesResponse aliasResponse = client.indices().updateAliases(u -> {
                        for (final String pickedIndex : allIndices) {
                            LOGGER.info("REINDEX status: execute remove alias " + ITEMS_ALIAS_INDEX_NAME + " to " + pickedIndex);
                            // mustExist(false): most of these indices never carried the alias in the
                            // first place, only the currently-aliased one does - that's fine, not an error.
                            u.actions(a -> a.remove(r -> r.index(pickedIndex).alias(ITEMS_ALIAS_INDEX_NAME).mustExist(false)));
                        }
                        LOGGER.info("REINDEX status: Add new alias " + ITEMS_ALIAS_INDEX_NAME + " to index " + newIndexName);
                        u.actions(a -> a.add(add -> add.index(newIndexName).alias(ITEMS_ALIAS_INDEX_NAME)));
                        return u;
                    });
                    LOGGER.info("REINDEX status: alias switch acknowledged: {}", aliasResponse.acknowledged());

                    if (aliasResponse.acknowledged()) {
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
                LOGGER.error("REINDEX status: Something went wrong while processing the new index {}", reindexStatus.getNewIndexName(), e);
                return false;
            } finally {
                reindexStatus.setEndTime(ZonedDateTime.now(ZoneOffset.UTC).toString());
                LOGGER.info("REINDEX status: End Time: {}", reindexStatus.getEndTime());

                reindexRunning.set(false);
            }
            return true;

        });

        return true;
    }

    /**
     * Returns a human-readable, verbose status description of the current
     * or last reindex process.
     *
     * @return a detailed status string describing the reindex progress
     */
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

            Duration timeLapsed;
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
            if (StringUtils.isBlank(reindexStatus.getEndTime()) && percProgress > 0 && totalSecondsSoFar > 0) {
                final double estimatedTotalSeconds = ((double) totalSecondsSoFar / percProgress) * 100;
                final ZonedDateTime etaZDT = startZDT.plusSeconds((long) estimatedTotalSeconds)
                        .withZoneSameInstant(ZoneOffset.UTC);
                eta = etaZDT.toString();
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
     * Checks, in a single Elasticsearch multi-get request, which of the given items' identifiers
     * already exist in the given index - instead of one existence-check GET per identifier.
     *
     * @param client the Elasticsearch client to use
     * @param indexName the name of the index to query
     * @param items the items whose identifiers should be checked
     * @return the identifiers that already exist in the index
     * @throws IOException if an error occurs while communicating with Elasticsearch
     */
    private Set<String> findExistingIdentifiers(ElasticsearchClient client, String indexName, Collection<Item> items) throws IOException {
        Set<String> existingIdentifiers = new HashSet<>();
        if (CollectionUtils.isEmpty(items)) {
            return existingIdentifiers;
        }

        List<String> ids = items.stream().map(Item::getIdentifier).toList();

        MgetResponse<Void> response = client.mget(m -> m
                .index(indexName)
                .ids(ids)
                .source(s -> s.fetch(false)), Void.class);

        for (MultiGetResponseItem<Void> itemResponse : response.docs()) {
            if (itemResponse.isResult() && itemResponse.result().found()) {
                existingIdentifiers.add(itemResponse.result().id());
            }
        }
        return existingIdentifiers;
    }

    /**
     * Indexes multiple items in a single Elasticsearch bulk request instead of one index() call
     * per item. Per-item failures reported by Elasticsearch are logged individually and do not
     * fail the whole batch.
     *
     * @param client the Elasticsearch client to use
     * @param items the items to index
     * @param indexName the index to write into
     * @throws IOException if an error occurs while communicating with Elasticsearch
     */
    private void bulkIndexDocuments(ElasticsearchClient client, Collection<Item> items, String indexName) throws IOException {
        BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
        for (Item item : items) {
            Map<String, Object> itemMap = item.toMap();
            bulkRequestBuilder.operations(op -> op.index(idx -> idx.index(indexName).id(item.getIdentifier()).document(itemMap)));
        }

        BulkResponse bulkResponse = client.bulk(bulkRequestBuilder.build());
        if (bulkResponse.errors()) {
            for (BulkResponseItem itemResponse : bulkResponse.items()) {
                if (itemResponse.error() != null) {
                    LOGGER.error("Reindex fails for {}: {}", itemResponse.id(), itemResponse.error().reason());
                }
            }
        }
    }

    /**
     * Retrieves the names of all indices currently present in Elasticsearch.
     *
     * @param client the Elasticsearch client to use
     * @return a list of all index names
     * @throws IOException if an error occurs while communicating with Elasticsearch
     */
    private List<String> getAllIndexNames(ElasticsearchClient client) throws IOException {
        GetIndexResponse responseIndex = client.indices().get(g -> g.index("*"));
        return new ArrayList<>(responseIndex.indices().keySet());
    }

    /**
     * Checks whether an Elasticsearch index with the given name exists.
     *
     * @param client the Elasticsearch client to use
     * @param indexName the name of the index to check
     * @return {@code true} if the index exists, {@code false} otherwise
     * @throws IOException if an error occurs while communicating with Elasticsearch
     */
    private boolean checkIndexExists(ElasticsearchClient client, String indexName) throws IOException {
        GetIndexResponse responseIndex = client.indices().get(g -> g.index(indexName));
        return responseIndex.indices() != null && !responseIndex.indices().isEmpty();
    }

    /**
     * Creates a new versioned index for reindexing.
     * <p>
     * The method determines the highest existing index version, increments it,
     * creates a new index with the appropriate mapping, and prepares alias switching.
     *
     * @param client the Elasticsearch client to use
     * @return {@code true} if the index was created successfully, {@code false} otherwise
     * @throws IOException if an error occurs while communicating with Elasticsearch
     */
    private boolean createNewIndex(ElasticsearchClient client) throws IOException {
        List<String> allIndices = getAllIndexNames(client);

        LOGGER.info("REINDEX status: Found {} indexes:", allIndices.size());
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
            LOGGER.error("Not able to determine index names: original ({}) or new ({})", reindexStatus.getOriginalIndexName(), reindexStatus.getNewIndexName());
            return false;
        }
        final String filenameItemsMapping = ITEMS_MAPPING_V7_FILENAME_UPDATE_MAPPING;
        final String mapping = ResourcesUtils.getResourceFileAsString(filenameItemsMapping, servletContext);
        if (StringUtils.isBlank(mapping)) {
            LOGGER.error("REINDEX status: Not able to retrieve mapping {}", filenameItemsMapping);
        }

        if (StringUtils.isBlank(reindexStatus.getOriginalIndexName())) {
            if (!createFirstAliasForIndex(client, mapping)) {
                return false;
            }
        }

        if (!createIndex(reindexStatus.getNewIndexName(), mapping)) {
            LOGGER.error("REINDEX status: Something went wrong while creating the new index {}", reindexStatus.getNewIndexName());
            return false;
        }
        return true;
    }

    /**
     * Creates the initial index and alias if no previous index exists.
     *
     * @param client the Elasticsearch client
     * @param mapping the JSON mapping definition for the index
     * @return {@code true} if the index and alias were created successfully, {@code false} otherwise
     * @throws IOException if an error occurs while communicating with Elasticsearch
     */

    private boolean createFirstAliasForIndex(ElasticsearchClient client, String mapping) throws IOException {
        LOGGER.warn("No previous indices found.");
        reindexStatus.setOriginalIndexName(ITEMS_ALIAS_INDEX_NAME + "0");
        if (!createIndex(reindexStatus.getOriginalIndexName(), mapping)) {
            LOGGER.error("REINDEX status: Something went wrong while creating the first index {}", reindexStatus.getOriginalIndexName());
            return false;
        }
        final String originalIndexName = reindexStatus.getOriginalIndexName();
        client.indices().updateAliases(u -> u.actions(a -> a.add(add -> add.index(originalIndexName).alias(ITEMS_ALIAS_INDEX_NAME))));
        return true;
    }

    /**
     * Returns the shared, long-lived Elasticsearch client. The underlying connection pool is
     * created once and reused for the lifetime of the application (see {@link ElasticsearchClientManager}).
     *
     * @return the shared {@link ElasticsearchClient}
     */
    private ElasticsearchClient getElasticsearchClient() {
        return ElasticsearchClientManager.getInstance().getClient();
    }

}
