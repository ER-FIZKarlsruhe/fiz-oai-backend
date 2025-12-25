/*
 * Copyright 2025 FIZ Karlsruhe - Leibniz-Institut fuer Informationsinfrastruktur GmbH
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
package de.fiz.oai.backend.testcontainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;


/**
 * This class configures the Elasticsearch test-container.
 */
public class ElasticsearchTestContainer {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchTestContainer.class);

    public static final GenericContainer<ElasticsearchContainer> container =
            new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.17.29"))
                    .withExposedPorts(9200)
                    .withNetwork(BaseInstance.network)
                    .withNetworkAliases("elasticsearch-oai")
                    .withEnv("xpack.security.enabled", "false")
                    .withReuse(true).waitingFor(Wait.forHttp("/"));

    public static boolean setConfigProperties() {
        System.setProperty("elasticsearch.init", "true");
        System.setProperty("elasticsearch.http.port", container.getMappedPort(9200).toString());
        System.setProperty("elasticsearch.unicast.hosts", container.getHost());
        System.setProperty("elasticsearch.max_result_window", "100000000");
        return true;
    }

    public static void createIndexAndAlias() throws IOException, InterruptedException {
        int mappedPort = container.getMappedPort(9200);
        String elasticsearchUrl = "http://localhost:" + mappedPort;

        String itemMappingFile = "src/test/resources/item_mapping_es_v7"; // Correct path
        String itemMapping = Files.readString(Paths.get(itemMappingFile));

        // 1. Create the index (with mapping)
        String createIndexUrl = elasticsearchUrl + "/items1";
        sendRequest(createIndexUrl, itemMapping, "PUT");

        // 2. Create the alias
        String createAliasUrl = elasticsearchUrl + "/items1/_alias/items";
        String aliasJson = "{\"actions\": [{\"add\": {\"index\": \"items1\", \"alias\": \"items\"}}]}";
        sendRequest(createAliasUrl, aliasJson, "PUT");
    }




    public static synchronized void resetElasticsearch()
            throws IOException, InterruptedException {

        int port = container.getMappedPort(9200);
        String baseUrl = "http://localhost:" + port;

        logger.info("Resetting Elasticsearch indices");

        // Delete ALL indices used by the app
        try {
            sendRequest(baseUrl + "/items1", "", "DELETE");
        } catch (Exception ignored) {}

        try {
            sendRequest(baseUrl + "/_all", "", "DELETE");
        } catch (Exception ignored) {}

        // Recreate index + alias
        createIndexAndAlias();

        // Force refresh so tests see data immediately
        sendRequest(baseUrl + "/items1/_refresh", "", "POST");

        logger.info("Elasticsearch index reset complete");
    }


    private static void sendRequest(String url, String data, String method) throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method(method, HttpRequest.BodyPublishers.ofString(data))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Response Status Code: " + response.statusCode());
        System.out.println("Response Body: " + response.body());
    }
}
