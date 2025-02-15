package de.fiz.oai.backend.testcontainer;


import com.github.dockerjava.api.DockerClient;
import org.junit.Before;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.MountableFile;

import java.io.File;

/**
 * This class starts the required test-containers before a test method is run.
 * It is required that the used test-containers set the container reuse flag,
 * so that each a container is only created and setup once for all integration tests.
 */
public class TestContainerManager  {
    boolean cassandraSetupComplete = false;
    boolean elasticsearchSetupComplete = false;
    boolean tomcatComplete = false;

    GenericContainer<?> tomcatContainer;

    private static final String WAR_FILE_PATH = "target/oai-backend.war";
    private static final String CONFIG_FILE_PATH = "src/test/resources/fiz-oai-backend.properties";

    public static Network.NetworkImpl network;

    @Before
    public void setup() {

        String customNetworkName = "my-custom-network";

        network =  Network.builder().createNetworkCmdModifier(createNetworkCmd -> createNetworkCmd.withName("oai-network")).build();

        if (!cassandraSetupComplete) {
            try {
                CassandraTestContainer.container.start();
            } catch(Exception e) {}

            cassandraSetupComplete = CassandraTestContainer.setConfigProperties();
        }

        if (!elasticsearchSetupComplete) {
            ElasticsearchTestContainer.container.start();
            elasticsearchSetupComplete = ElasticsearchTestContainer.setConfigProperties();
        }

        if (!tomcatComplete) {
            MountableFile warFile = MountableFile.forHostPath(new File(WAR_FILE_PATH).getAbsolutePath());

            MountableFile configFiles = MountableFile.forHostPath(
                    new File(CONFIG_FILE_PATH).getAbsolutePath());

            tomcatContainer = new GenericContainer<>("tomcat:11-jre21-temurin")
                    .withExposedPorts(8080)
                    .withNetwork(TestContainerManager.network)
                    .withCopyFileToContainer(warFile, "/usr/local/tomcat/webapps/oai-backend.war")
                    .withCopyFileToContainer(configFiles, "/usr/local/tomcat/conf/fiz-oai-backend.properties");

            tomcatContainer.start();
            tomcatComplete = tomcatContainer.isRunning();
        }
    }
}