package de.fiz.oai.backend.testcontainer;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.wait.CassandraQueryWaitStrategy;

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
                    .withCreateContainerCmdModifier(cmd)
                    .waitingFor(new CassandraQueryWaitStrategy())
                    .withFileSystemBind(new File("src/test/resources/cassandra-test-configuration/cassandra.yaml").getAbsolutePath(), "/etc/cassandra/cassandra.yaml")
                    .withStartupTimeout(Duration.ofSeconds(5)).withReuse(true);

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
            session.execute("CREATE KEYSPACE IF NOT EXISTS %s WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };".formatted(cassandraKeyspace));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }
        System.setProperty("spring.data.cassandra.keyspace-name", cassandraKeyspace);
        System.setProperty("spring.data.cassandra.contact-points", contactPoint.getHostString());
        System.setProperty("spring.data.cassandra.port", String.valueOf(contactPoint.getPort()));
        System.setProperty("spring.data.cassandra.local-datacenter", datacenter);
        System.setProperty("spring.data.cassandra.username", "cassandra");
        System.setProperty("spring.data.cassandra.password", "cassandra");

        return true;
    }
}
