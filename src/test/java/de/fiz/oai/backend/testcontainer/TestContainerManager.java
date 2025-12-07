package de.fiz.oai.backend.testcontainer;


import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;

/**
 * This class starts the required test-containers before a test method is run.
 * It is required that the used test-containers set the container reuse flag,
 * so that each a container is only created and setup once for all integration tests.
 */
public abstract class TestContainerManager  {
    static boolean cassandraSetupComplete = false;
    static boolean elasticsearchSetupComplete = false;
    static boolean solrSetupComplete = false;
    static boolean tomcatComplete = false;

    static GenericContainer<?> tomcatContainer;

    private static final String WAR_FILE_PATH = "target/oai-backend.war";
    private static final String CONFIG_FILE_PATH = "src/test/resources/fiz-oai-backend-es.properties";
    private static final String SERVER_FILE_PATH = "docker/server.xml";
    //private static final String CONFIG_FILE_PATH = "src/test/resources/fiz-oai-backend-solr.properties";

    public static Network.NetworkImpl network;

    private static final Logger logger = LoggerFactory.getLogger(TestContainerManager.class);

    @BeforeClass
    public static void setup() throws IOException, InterruptedException {
        // 1. Network setup (Only create if not already handled)
        if (network == null) {
            // Testcontainers manages network creation idempotently, but checking 'network == null'
            // adds clarity and prevents repeated builder calls.
            network = Network.builder()
                    .createNetworkCmdModifier(createNetworkCmd -> createNetworkCmd.withName("oai-network"))
                    .build();
        }

        // 2. Cassandra Setup
        if (!cassandraSetupComplete) {
            try {
                // Container start logic (must be wrapped if start() can fail and you want to continue)
                CassandraTestContainer.container.start();

                // Configuration and setup flag update
                cassandraSetupComplete = CassandraTestContainer.setConfigProperties();
            } catch (Exception e) {
                // Log the exception instead of swallowing it
                System.err.println("Failed to start or configure Cassandra: " + e.getMessage());
                // Optionally, rethrow if Cassandra is mandatory: throw new IOException("Cassandra setup failed", e);
            }
        }

        // 3. Elasticsearch Setup
        if (!elasticsearchSetupComplete) {
            // NOTE: If Elasticsearch start fails, the method throws an exception and stops here.
            ElasticsearchTestContainer.container.start();

            // Configuration and setup flag update
            elasticsearchSetupComplete = ElasticsearchTestContainer.setConfigProperties();

            // Index/Alias creation must happen after configuration
            ElasticsearchTestContainer.createIndexAndAlias();
        }

        // 4. Solr Setup (Corrected flag update)
        if (!solrSetupComplete) {
            // NOTE: If Solr start fails, the method throws an exception and stops here.
            SolrTestContainer.container.start();

            // **OPTIMIZATION:** Explicitly check if the container is running and update the flag.
            solrSetupComplete = SolrTestContainer.container.isRunning();

            // Add setup logic here if needed (e.g., core creation, config file loading)
            // SolrTestContainer.createCores();
        }

        // 5. Tomcat Setup
        if (!tomcatComplete) {
            // Move MountableFile creation *inside* the block to ensure objects are fresh if needed,
            // although for static paths, this is fine.
            MountableFile warFile = MountableFile.forHostPath(new File(WAR_FILE_PATH).getAbsolutePath());
            MountableFile configFiles = MountableFile.forHostPath(new File(CONFIG_FILE_PATH).getAbsolutePath());
            MountableFile serverFile = MountableFile.forHostPath(new File(SERVER_FILE_PATH).getAbsolutePath());

            tomcatContainer = new GenericContainer<>("tomcat:11-jre25-temurin")
                    .withExposedPorts(8080)
                    .withNetwork(TestContainerManager.network)
                    .withCopyFileToContainer(warFile, "/usr/local/tomcat/webapps/oai-backend.war")
                    .withCopyFileToContainer(configFiles, "/usr/local/tomcat/conf/fiz-oai-backend.properties")
                    .withCopyFileToContainer(serverFile, "/usr/local/tomcat/conf/server.xml")
                    .withStartupTimeout(java.time.Duration.ofMinutes(5)); // Added robust startup timeout



            // NOTE: If Tomcat start fails, the method throws an exception and stops here.
            tomcatContainer.start();
            tomcatContainer.followOutput(new Slf4jLogConsumer(logger));
            // Configuration and setup flag update
            tomcatComplete = tomcatContainer.isRunning();
        }
    }

}