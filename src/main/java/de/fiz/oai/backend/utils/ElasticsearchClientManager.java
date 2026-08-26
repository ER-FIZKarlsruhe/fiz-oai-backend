/*
 * Copyright 2026 FIZ Karlsruhe - Leibniz-Institut fuer Informationsinfrastruktur GmbH
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
package de.fiz.oai.backend.utils;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds a single, long-lived {@link RestHighLevelClient} shared across the application, instead of
 * opening and tearing down a new HTTP connection pool on every Elasticsearch call.
 */
public class ElasticsearchClientManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchClientManager.class);

    private static ElasticsearchClientManager instance;

    private final RestHighLevelClient client;

    private ElasticsearchClientManager() {
        Configuration config = Configuration.getInstance();
        String host = config.getProperty("elasticsearch.host", "localhost");
        int port = Integer.parseInt(config.getProperty("elasticsearch.port", "8082"));
        LOGGER.info("Init Elasticsearch client for {}:{}", host, port);
        client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, "http")));
    }

    public static synchronized ElasticsearchClientManager getInstance() {
        if (instance == null) {
            instance = new ElasticsearchClientManager();
        }
        return instance;
    }

    public RestHighLevelClient getClient() {
        return client;
    }

    public void shutdown() {
        try {
            client.close();
        } catch (Exception e) {
            LOGGER.error("Exception on closing Elasticsearch client", e);
        }
    }
}
