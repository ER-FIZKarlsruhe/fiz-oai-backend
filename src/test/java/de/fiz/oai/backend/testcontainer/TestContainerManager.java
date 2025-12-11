package de.fiz.oai.backend.testcontainer;

import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;


public abstract class TestContainerManager {
    static boolean cassandraSetupComplete = false;
    static boolean elasticsearchSetupComplete = false;
    static boolean solrSetupComplete = false;
    static boolean tomcatComplete = false;

    static GenericContainer<?> tomcatContainer;

    private static final String WAR_FILE_PATH = "target/oai-backend.war";
    private static final String CONFIG_FILE_PATH = "src/test/resources/fiz-oai-backend-es.properties";
    private static final String SERVER_FILE_PATH = "docker/server.xml";

    public static Network.NetworkImpl network;

    private static final Logger logger = LoggerFactory.getLogger(TestContainerManager.class);

    @BeforeClass
    public static void setup() throws IOException, InterruptedException {
        System.setProperty("docker.host", "npipe:////./pipe/docker_engine");
        System.setProperty("org.testcontainers.dockerclient.providerConfig", "windows");

        // 1. Network setup (Only create if not already handled)
        if (network == null) {
            network = Network.builder()
                    .createNetworkCmdModifier(createNetworkCmd -> createNetworkCmd.withName("oai-network"))
                    .build();
        }

        // 2. Cassandra Setup
        if (!cassandraSetupComplete) {
            try {
                CassandraTestContainer.container.start();
                cassandraSetupComplete = CassandraTestContainer.setConfigProperties();
            } catch (Exception e) {
                System.err.println("Failed to start or configure Cassandra: " + e.getMessage());
            }
        }

        // 3. Elasticsearch Setup
        if (!elasticsearchSetupComplete) {
            ElasticsearchTestContainer.container.start();
            elasticsearchSetupComplete = ElasticsearchTestContainer.setConfigProperties();
            ElasticsearchTestContainer.createIndexAndAlias();
        }

        // 4. Solr Setup
        if (!solrSetupComplete) {
            SolrTestContainer.container.start();
            solrSetupComplete = SolrTestContainer.container.isRunning();
        }

        // 5. Tomcat Setup
        if (!tomcatComplete) {
            MountableFile warFile = MountableFile.forHostPath(new File(WAR_FILE_PATH).getAbsolutePath());
            MountableFile configFiles = MountableFile.forHostPath(new File(CONFIG_FILE_PATH).getAbsolutePath());
            MountableFile serverFile = MountableFile.forHostPath(new File(SERVER_FILE_PATH).getAbsolutePath());

            tomcatContainer = new GenericContainer<>("tomcat:11-jre25-temurin")
                    .withExposedPorts(8080)
                    .withNetwork(TestContainerManager.network)
                    .withCopyFileToContainer(warFile, "/usr/local/tomcat/webapps/oai-backend.war")
                    .withCopyFileToContainer(configFiles, "/usr/local/tomcat/conf/fiz-oai-backend.properties")
                    .withCopyFileToContainer(serverFile, "/usr/local/tomcat/conf/server.xml")
                    .withStartupTimeout(java.time.Duration.ofMinutes(5));

            tomcatContainer.start();
            tomcatContainer.followOutput(new Slf4jLogConsumer(logger));
            tomcatComplete = tomcatContainer.isRunning();
        }
    }

    /**
     * Stops all running containers and resets the setup flags.
     * This method can be used as an @AfterClass hook or explicitly between tests
     * to ensure a fresh environment for subsequent test runs.
     */
    @AfterClass
    public static void teardownAndReset() {
        logger.info("--- Starting container teardown and environment reset ---");

        // 1. Stop Tomcat
        if (tomcatContainer != null && tomcatContainer.isRunning()) {
            logger.info("Stopping Tomcat container...");
            tomcatContainer.stop();
            tomcatContainer = null;
        }
        tomcatComplete = false;

        // 2. Stop Solr
        if (SolrTestContainer.container != null && SolrTestContainer.container.isRunning()) {
            logger.info("Stopping Solr container...");
            SolrTestContainer.container.stop();
        }
        solrSetupComplete = false;

        // 3. Stop Elasticsearch
        if (ElasticsearchTestContainer.container != null && ElasticsearchTestContainer.container.isRunning()) {
            logger.info("Stopping Elasticsearch container...");
            ElasticsearchTestContainer.container.stop();
        }
        elasticsearchSetupComplete = false;

        // 4. Stop Cassandra
        if (CassandraTestContainer.container != null && CassandraTestContainer.container.isRunning()) {
            logger.info("Stopping Cassandra container...");
            CassandraTestContainer.container.stop();
        }
        cassandraSetupComplete = false;
        logger.info("--- Container teardown complete. Environment is fresh for next setup. ---");
    }
}