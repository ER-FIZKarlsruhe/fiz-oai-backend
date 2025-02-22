package de.fiz.oai.backend.testcontainer;

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
    public static final GenericContainer<ElasticsearchContainer> container =
            new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.17.13"))
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
