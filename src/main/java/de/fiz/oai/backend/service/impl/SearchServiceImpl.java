package de.fiz.oai.backend.service.impl;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
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

  public static String ITEMS_MAPPING_FILENAME = "item_mapping_es_v7";

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
  public void createDocument(Item item) throws IOException {
    try (RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")))) {
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

      // String xmlContentByte = daoContent.read(item.getIdentifier(),
      // "oai_dc").getContent();
      // String oaiDcJson = OaiDcHelper.xmlToJson(xmlContentByte);

      Map<String, Object> itemMap = item.toMap();
      // itemMap.put("oai_dc", oaiDcJson);

      UpdateRequest updateRequest = new UpdateRequest();
      updateRequest.index(ITEMS_ALIAS_INDEX_NAME);
      updateRequest.type("_doc");
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

    LOGGER.info("DEBUG: rows: " + rows);
    LOGGER.info("DEBUG: format: " + format);
    LOGGER.info("DEBUG: lastItem: " + lastItem);

    try (RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")))) {

      final BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
      queryBuilder.filter(QueryBuilders.rangeQuery("datestamp").from(Configuration.getDateformat().format(fromDate))
          .to(Configuration.getDateformat().format(untilDate)));
      queryBuilder.filter(QueryBuilders.termQuery("formats", format));

      if (StringUtils.isNotEmpty(set)) {
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
      List<String> idsRetrieved = new ArrayList<String>();

      while (iterator.hasNext()) {
        SearchHit searchHit = iterator.next();
        idsRetrieved.add(searchHit.getId());
      }

      SearchResult<String> idResult = new SearchResult<String>();
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

    }

  }

  @Override
  public boolean createIndex(final String indexName, final String mapping) throws IOException {
    if (StringUtils.isNotBlank(indexName) && StringUtils.isNotBlank(mapping)) {
      try (RestHighLevelClient client = new RestHighLevelClient(
          RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")))) {
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        request.mapping("_doc", mapping, XContentType.JSON);
        CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
        if (createIndexResponse.isAcknowledged()) {
          return true;
        }
      }
    }
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
      LOGGER.warn("Reindex process already started since " + reindexStatus.getStartTime()
          + ". It will be blocked and restarted!");

      // Stop future process
      if (reindexAllFuture != null) {
        while (!reindexAllFuture.isCancelled()) {
          reindexAllFuture.cancel(true);
        }
      }

      dropIndex(reindexStatus.getNewIndexName());
    }

    reindexStatus = new ReindexStatus();

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
            break;
          }
        }

        if (StringUtils.isBlank(reindexStatus.getOriginalIndexName())) {
          LOGGER.warn("No existing indexes. Creating it from scratch.");
          reindexStatus.setNewIndexName(ITEMS_ALIAS_INDEX_NAME + "1");
        } else {
          final String currentIndexVersionStr = reindexStatus.getOriginalIndexName()
              .substring(ITEMS_ALIAS_INDEX_NAME.length());
          final long currentIndexVersion = Long.parseLong(currentIndexVersionStr);
          final long newIndexVersion = currentIndexVersion + 1;
          final StringBuilder newIndexName = new StringBuilder();
          newIndexName.append(ITEMS_ALIAS_INDEX_NAME);
          newIndexName.append(String.valueOf(newIndexVersion));

          reindexStatus.setNewIndexName(newIndexName.toString());
        }

        if (StringUtils.isBlank(reindexStatus.getOriginalIndexName())
            || StringUtils.isBlank(reindexStatus.getNewIndexName())) {
          LOGGER.error("Something went wrong while generating index names.");
          return false;
        }

        final String mapping = ResourcesUtils.getResourceFileAsString(ITEMS_MAPPING_FILENAME);

        if (!createIndex(reindexStatus.getNewIndexName(), mapping)) {
          LOGGER.error("Something went wrong while creating the new index " + reindexStatus.getNewIndexName());
          return false;
        }
      } catch (IOException e) {
        LOGGER.error("Something went wrong while processing the new index" + reindexStatus.getNewIndexName(), e);
        return false;
      }
      return true;

    });

  }

}
