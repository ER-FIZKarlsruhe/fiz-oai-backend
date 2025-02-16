package de.fiz.oai.backend.testcontainer;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * This class configures the Elasticsearch test-container.
 */
public class SolrTestContainer {
    private static final String password = "s3cret";
    public static final GenericContainer<SolrContainer> container =
            new SolrContainer(DockerImageName.parse("solr:9.8"))
                    .withExposedPorts(8983)
                    .withStartupTimeout(Duration.ofSeconds(150))
                    .withReuse(true);

    public static boolean setConfigProperties() {
        System.setProperty("SOLR_HOST", "solr1");
        return true;
    }
}
