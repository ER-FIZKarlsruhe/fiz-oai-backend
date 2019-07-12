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
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.jvnet.hk2.annotations.Service;

import de.fiz.oai.backend.dao.DAOContent;
import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.models.Set;
import de.fiz.oai.backend.service.SearchService;
import de.fiz.oai.backend.utils.Configuration;
import de.fiz.oai.backend.utils.OaiDcHelper;

@Service
public class SearchServiceImpl implements SearchService {

  String elastisearchHost = Configuration.getInstance().getProperty("elasticsearch.host");

  int elastisearchPort = Integer.parseInt(Configuration.getInstance().getProperty("elasticsearch.port"));

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
    RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")));

    byte[] xmlContentByte = daoContent.read(item.getIdentifier(), "oai_dc").getContent();
    String oaiDcJson = OaiDcHelper.xmlToJson(new String(xmlContentByte, "UTF-8"));
    
    Map<String,Object> itemMap = item.toMap();
    itemMap.put("oai_dc", oaiDcJson);
    
    IndexRequest indexRequest = new IndexRequest();
    indexRequest.index("item");
    indexRequest.type("_doc");
    indexRequest.id(item.getIdentifier());
    indexRequest.source(itemMap);
    
    client.index(indexRequest, RequestOptions.DEFAULT);
  }

  /**
   * 
   * @param item @throws IOException @throws
   */
  @Override
  public void updateDocument(Item item) throws IOException {
    RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")));

    byte[] xmlContentByte = daoContent.read(item.getIdentifier(), "oai_dc").getContent();
    String oaiDcJson = OaiDcHelper.xmlToJson(new String(xmlContentByte, "UTF-8"));
    
    Map<String,Object> itemMap = item.toMap();
    itemMap.put("oai_dc", oaiDcJson);
    
    UpdateRequest updateRequest = new UpdateRequest();
    updateRequest.index("item");
    updateRequest.type("_doc");
    updateRequest.id(item.getIdentifier());
    updateRequest.doc(itemMap);
    client.update(updateRequest, RequestOptions.DEFAULT);
  }

  /**
   * 
   * @param item @throws IOException @throws
   */
  @Override
  public void deleteDocument(Item item) throws IOException {
    RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")));

    DeleteRequest request = new DeleteRequest();
    request.index("item");
    request.type("_doc");
    request.id(item.getIdentifier());

    DeleteResponse deleteResponse = client.delete(request, RequestOptions.DEFAULT);
  }

  @Override
  public SearchResult<String> search(Integer offset, Integer rows, Set set, String format, Date fromDate, Date untilDate) throws IOException {

    List<String> itemIds = new ArrayList<String>();

    RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")));
    final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.query(QueryBuilders.boolQuery().filter(QueryBuilders.rangeQuery("datestamp").from(fromDate).to(untilDate)).filter(QueryBuilders.termQuery("ingestFormat", format)));
	searchSourceBuilder.sort("datestamp", SortOrder.ASC);
    searchSourceBuilder.from(offset);
    searchSourceBuilder.size(rows);

//    if (fromDate != null || untilDate != null) {
//      RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("datestamp");
//      if (fromDate != null) {
//        rangeQueryBuilder.from(fromDate.getTime());
//      }
//
//      if (untilDate != null) {
//        rangeQueryBuilder.to(untilDate.getTime());
//      }
//
//      searchSourceBuilder.query(rangeQueryBuilder);
//    }

    if (set != null) {
      if (StringUtils.isNotBlank(set.getSearchQuery())) {
        //TODO query field oai_dc with searchQuery
      } else if(StringUtils.isNotBlank(set.getSearchTerm())) {
        //TODO query all fields or the searchTerm
      }  
      
      
      
    }

//    if (StringUtils.isNotEmpty(format)) {
//      TermQueryBuilder formatQuery = QueryBuilders.termQuery("formats", format);
//      searchSourceBuilder.query(formatQuery);
//    }

    // TODO sort

    final SearchRequest searchRequest = new SearchRequest("items");
    searchRequest.source(searchSourceBuilder);
    final SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

    SearchHits hits = searchResponse.getHits();

    Iterator<SearchHit> iterator = hits.iterator();

    while (iterator.hasNext()) {
      SearchHit searchHit = (SearchHit) iterator.next();
      itemIds.add(searchHit.getId());
      System.out.println("id: " + searchHit.getId());
    }

    SearchResult<String> idResult = new SearchResult<String>();
    idResult.setData(itemIds);
    idResult.setOffset(offset);
    idResult.setSize(itemIds.size());
    idResult.setTotal((int) hits.getTotalHits());// FIXME Bad

    return idResult;
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
