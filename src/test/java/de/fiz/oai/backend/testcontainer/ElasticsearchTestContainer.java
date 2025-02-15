package de.fiz.oai.backend.testcontainer;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * This class configures the Elasticsearch test-container.
 */
public class ElasticsearchTestContainer {
    private static final String password = "s3cret";
    public static final GenericContainer<ElasticsearchContainer> container =
            new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.6.0"))
                    .withExposedPorts(9200)
                    .withEnv("xpack.security.enabled", "false")
                    .withReuse(true);

    public static boolean setConfigProperties() {
        System.setProperty("elasticsearch.init", "true");
        System.setProperty("elasticsearch.http.port", container.getMappedPort(9200).toString());
        System.setProperty("elasticsearch.unicast.hosts", container.getHost());
        System.setProperty("elasticsearch.max_result_window", "100000000");
        return true;
    }
}
