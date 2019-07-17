package de.fiz.oai.backend.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.fiz.oai.backend.dao.DAOContent;
import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.models.Set;
import de.fiz.oai.backend.service.SearchService;
import de.fiz.oai.backend.utils.Configuration;
import de.fiz.oai.backend.utils.OaiDcHelper;

@Service
public class SearchServiceImpl implements SearchService {

  private Logger LOGGER = LoggerFactory.getLogger(SearchServiceImpl.class);

  String elastisearchHost = Configuration.getInstance().getProperty("elasticsearch.host");

  int elastisearchPort = Integer.parseInt(Configuration.getInstance().getProperty("elasticsearch.port"));

  public static String ITEMS_INDEX_NAME = "items";

  @Inject
  DAOItem daoItem;

  @Inject
  DAOContent daoContent;

  /**
   * 
   * @param item @throws IOException @throws
   */
  @Override
  public void createDocument(Item item) throws IOException {
    try (RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")))) {
      Map<String, Object> itemMap = item.toMap();

      // Index oai_dc version of the item
      Content content = daoContent.read(item.getIdentifier(), "oai_dc");
      if (content != null) {
        String xmlContentByte = content.getContent();
        String oaiDcJson = OaiDcHelper.xmlToJson(xmlContentByte);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> map = mapper.readValue(oaiDcJson, Map.class);
        itemMap.put("oai_dc", map);
      }

      IndexRequest indexRequest = new IndexRequest();

      indexRequest.index(ITEMS_INDEX_NAME);
      indexRequest.type("_doc");
      indexRequest.id(item.getIdentifier().replace("-", "\\-"));
      indexRequest.source(itemMap);

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

      String xmlContentByte = daoContent.read(item.getIdentifier(), "oai_dc").getContent();
      String oaiDcJson = OaiDcHelper.xmlToJson(xmlContentByte);

      Map<String, Object> itemMap = item.toMap();
      itemMap.put("oai_dc", oaiDcJson);

      UpdateRequest updateRequest = new UpdateRequest();
      updateRequest.index(ITEMS_INDEX_NAME);
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
      request.index(ITEMS_INDEX_NAME);
      request.type("_doc");
      request.id(item.getIdentifier());

      DeleteResponse deleteResponse = client.delete(request, RequestOptions.DEFAULT);
    }
  }

//  @Override
//  public SearchResult<String> search(String scrollId) throws IOException {
//    RestHighLevelClient client = new RestHighLevelClient(
//        RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")));
//
//    SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
//    SearchResponse scrollResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
//    scrollId = scrollResponse.getScrollId();
//    SearchHits scrollhits = scrollResponse.getHits();
//    LOGGER.info("scrollhits " + scrollhits.totalHits);
//
//    Iterator<SearchHit> iterator = scrollhits.iterator();
//    List<String> itemRetrieved = new ArrayList<String>();
//
//    while (iterator.hasNext()) {
//      SearchHit searchHit = iterator.next();
//      itemRetrieved.add(searchHit.getId());
//    }
//
//    SearchResult<String> idResult = new SearchResult<String>();
//    idResult.setSize(itemRetrieved.size());
//    idResult.setTotal(scrollhits.totalHits);
//    idResult.setData(itemRetrieved);
//    idResult.setScrollId(scrollId);
//
//    return idResult;
//  }

  @Override
  public SearchResult<String> search(Integer rows, Set set, String format, Date fromDate, Date untilDate)
      throws IOException {

    try (RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")))) {

      final BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
      queryBuilder.filter(QueryBuilders.rangeQuery("datestamp").from(fromDate).to(untilDate));
      queryBuilder.filter(QueryBuilders.termQuery("formats", format));

      if (set != null) {
        if (StringUtils.isNotBlank(set.getSearchQuery())) {
          queryBuilder.filter(QueryBuilders.queryStringQuery(set.getSearchQuery()));
        } else if (StringUtils.isNotBlank(set.getSearchTerm())) {
          queryBuilder.filter(QueryBuilders.termQuery("content", set.getSearchTerm()));
        }
      }

      final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
      searchSourceBuilder.query(queryBuilder);
      searchSourceBuilder.sort("datestamp", SortOrder.ASC);
      searchSourceBuilder.size(rows);

      final Scroll scroll = new Scroll(TimeValue.timeValueHours(24));
      SearchRequest searchRequest = new SearchRequest(ITEMS_INDEX_NAME);
      searchRequest.scroll(scroll);
      searchRequest.source(searchSourceBuilder);

      SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
      String scrollId = searchResponse.getScrollId();

      SearchHits searchHits = searchResponse.getHits();
      Iterator<SearchHit> iterator = searchHits.iterator();
      List<String> itemRetrieved = new ArrayList<String>();

      while (iterator.hasNext()) {
        SearchHit searchHit = iterator.next();
        itemRetrieved.add(searchHit.getId());
      }

      SearchResult<String> idResult = new SearchResult<String>();
      idResult.setSize(itemRetrieved.size());
      idResult.setTotal(searchResponse.getHits().totalHits);
      idResult.setData(itemRetrieved);
      return idResult;

    }

  }

  @Override
  public void createIndex() {
    // TODO Auto-generated method stub

  }

  @Override
  public void dropIndex() {
    // TODO Auto-generated method stub

  }

  @Override
  public void reindexAll() {
    // TODO Auto-generated method stub

  }

}
