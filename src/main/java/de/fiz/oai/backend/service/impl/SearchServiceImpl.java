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
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
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
import de.fiz.oai.backend.service.SearchService;
import de.fiz.oai.backend.utils.Configuration;
import de.fiz.oai.backend.utils.XPathHelper;

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

  @Inject
  DAOFormat daoFormat;

  @Inject
  DAOSet daoSet;

  /**
   * 
   * @param item @throws IOException @throws
   */
  @Override
  public Map<String, Object> readDocument(Item item) throws IOException {
    try (RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(new HttpHost(elastisearchHost, elastisearchPort, "http")))) {
      GetRequest getRequest = new GetRequest(ITEMS_INDEX_NAME, "_doc", item.getIdentifier());

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
        //Check set membership via xPath
        Map<String, String> xPaths = pickedSet.getxPaths();
        for (final Content pickedContent : allContents) {
          if (xPaths.containsKey(pickedContent.getFormat())) {
            final String xPathToCheck = xPaths.get(pickedContent.getFormat());
            if (XPathHelper.isTextValueMatching(pickedContent.getContent(), xPathToCheck)) {
              itemSets.add(pickedSet.getName());
            }
          }
        }

        //Check set membership via item tags
        List<String> setTags = pickedSet.getTags();
        if (setTags != null) {
          for (String setTag : setTags) {
            if (item.getTags().contains(setTag)) {
              itemSets.add(pickedSet.getName());
            }
          }
        }

      }
      itemMap.put("sets", itemSets);

      IndexRequest indexRequest = new IndexRequest();

      indexRequest.index(ITEMS_INDEX_NAME);
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
      updateRequest.index(ITEMS_INDEX_NAME);
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
      request.index(ITEMS_INDEX_NAME);
      request.type("_doc");
      request.id(item.getIdentifier());

      DeleteResponse deleteResponse = client.delete(request, RequestOptions.DEFAULT);
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

      SearchRequest searchRequest = new SearchRequest(ITEMS_INDEX_NAME);
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
