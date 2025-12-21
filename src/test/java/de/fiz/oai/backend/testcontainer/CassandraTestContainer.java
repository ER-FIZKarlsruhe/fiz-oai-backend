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

import com.datastax.oss.driver.api.core.AllNodesFailedException;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.CassandraContainer;

import java.util.function.Consumer;

/**
 * This class configures the Cassandra test-container.
 */
public class CassandraTestContainer extends CassandraContainer<CassandraTestContainer> {

    private static final Logger logger = LoggerFactory.getLogger(CassandraTestContainer.class);

    static int hostPort = 9042;
    static int containerExposedPort = 9042;
    private static Consumer<CreateContainerCmd> cmd = e -> e.withPortBindings(new PortBinding(Ports.Binding.bindPort(hostPort), new ExposedPort(containerExposedPort)));

    private static final String CASSANDRA_KEYSPACE = "fizoaibackend1";

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

        CqlSessionBuilder sessionBuilder = CqlSession
                .builder()
                .addContactPoint(contactPoint)
                .withLocalDatacenter(datacenter);

        try (CqlSession session = sessionBuilder.build()) {

            // 1. Create the Keyspace using SimpleStrategy for local tests
            session.execute(
                    "CREATE KEYSPACE IF NOT EXISTS " + CASSANDRA_KEYSPACE +
                            " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}"
            );

            // 2. Log success or perform other initialization tasks
            logger.info("Keyspace {} created successfully.", CASSANDRA_KEYSPACE);
            return true;
        } catch (AllNodesFailedException e) {
            // Handle connection/authentication failure
            logger.error("FAILURE: Failed to connect to Cassandra or execute keyspace creation.", e);
            throw new RuntimeException("Cassandra initialization failed: Check container and driver setup.", e);
        }
    }

    public static void resetCassandra() {
        logger.info("Resetting Cassandra data (TRUNCATE tables)");

        var contactPoint = container.getContactPoint();
        var datacenter = container.getLocalDatacenter();

        try (CqlSession session = CqlSession.builder()
                .addContactPoint(contactPoint)
                .withLocalDatacenter(datacenter)
                .build()) {

            // Fetch all tables in the keyspace
            session.getMetadata()
                    .getKeyspace(CASSANDRA_KEYSPACE)
                    .ifPresent(keyspace -> keyspace.getTables().keySet().forEach(table -> {
                        String cql = String.format(
                                "TRUNCATE %s.%s",
                                CASSANDRA_KEYSPACE,
                                table.asInternal()
                        );
                        session.execute(cql);
                    }));

            logger.info("Cassandra tables truncated successfully");

        } catch (Exception e) {
            throw new RuntimeException("Failed to reset Cassandra", e);
        }
    }

}