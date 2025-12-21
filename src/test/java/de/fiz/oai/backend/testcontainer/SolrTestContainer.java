package de.fiz.oai.backend.testcontainer;

import org.testcontainers.containers.SolrContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;


/**
 * This class configures the Elasticsearch test-container.
 */
public class SolrTestContainer extends SolrContainer  {

    public static final SolrContainer container =
            new SolrContainer(DockerImageName.parse("solr:8.3.0"))
                    .withNetwork(ElasticsearchInstanceIT.network)
                    .withExposedPorts(8983)
                    .withNetworkAliases("solr-oai")
                    .withStartupTimeout(Duration.ofSeconds(150))
                    .withReuse(true).withCopyFileToContainer(
                            org.testcontainers.utility.MountableFile.forHostPath("src/test/resources/schema.xml"),
                            "/opt/solr/server/solr/configsets/_default/conf/schema.xml"
                    );






}
