package de.fiz.oai.backend.testcontainer;

import com.datastax.oss.driver.api.core.AllNodesFailedException;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.CassandraContainer;

import java.io.File;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * This class configures the Cassandra test-container.
 */
public class CassandraTestContainer extends CassandraContainer<CassandraTestContainer> {
    private static final Logger logger = LoggerFactory.getLogger(CassandraTestContainer.class);
    static int hostPort = 9042;
    static int containerExposedPort = 9042;
    private static Consumer<CreateContainerCmd> cmd = e -> e.withPortBindings(new PortBinding(Ports.Binding.bindPort(hostPort), new ExposedPort(containerExposedPort)));


    public static final CassandraTestContainer container =
            new CassandraTestContainer()
                    .withExposedPorts(hostPort)
                    .withNetwork(BaseInstance.network)
                    .withNetworkAliases("cassandra-oai")
                    .withCreateContainerCmdModifier(cmd)
                    .withEnv("CASSANDRA_CLUSTER_NAME", "TestCluster")
                    .withEnv("CASSANDRA_NUM_TOKENS", "8")
                    .withEnv("CASSANDRA_START_RPC", "true")
                    .withExposedPorts(9042)
                    .withStartupTimeout(java.time.Duration.ofMinutes(5)) // Increased timeout to 5 minutes
                    .withReuse(false);

    public CassandraTestContainer() {
        super("cassandra:4.1.0");
    }

    public static boolean setConfigProperties() {
        var contactPoint = container.getContactPoint();
        var datacenter = container.getLocalDatacenter();
        final String cassandraKeyspace = "fizoaibackend1";

        CqlSessionBuilder sessionBuilder = CqlSession
                .builder()
                .addContactPoint(contactPoint)
                .withLocalDatacenter(datacenter);

        try (CqlSession session = sessionBuilder.build()) {

            // 1. Create the Keyspace using SimpleStrategy for local tests
            session.execute(
                    "CREATE KEYSPACE IF NOT EXISTS " + cassandraKeyspace +
                            " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}"
            );

            // 2. Log success or perform other initialization tasks
            logger.info("Keyspace {} created successfully.", cassandraKeyspace);
            return true;
        } catch (AllNodesFailedException e) {
            // Handle connection/authentication failure
            logger.error("FAILURE: Failed to connect to Cassandra or execute keyspace creation.", e);
            throw new RuntimeException("Cassandra initialization failed: Check container and driver setup.", e);
        }
    }
}