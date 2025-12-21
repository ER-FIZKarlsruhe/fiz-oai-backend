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

import org.junit.After;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;

import java.io.File;

public abstract class TestContainerManager {

    private static final Logger logger =
            LoggerFactory.getLogger(TestContainerManager.class);

    /** JVM-global startup guard */
    private static final Object LOCK = new Object();
    private static volatile boolean STARTED = false;

    protected static Network network;
    protected static GenericContainer<?> tomcatContainer;

    private static final String WAR_FILE_PATH = "target/oai-backend.war";
    private static final String CONFIG_FILE_PATH =
            "src/test/resources/fiz-oai-backend-es.properties";
    private static final String SERVER_FILE_PATH = "docker/server.xml";

    /* -------------------------------------------------
     *  STARTUP — ONCE PER JVM
     * ------------------------------------------------- */
    @BeforeClass
    public static void startContainersOnce() throws Exception {
        if (STARTED) {
            return;
        }

        synchronized (LOCK) {
            if (STARTED) {
                return;
            }

            logger.info("Starting shared Testcontainers (once per JVM)");

            configureDocker();

            startNetwork();
            startCassandra();
            startElasticsearch();
            //FIXME Uncomment when SOLR tests are available
            //startSolr();
            startTomcat();

            registerShutdownHook();

            STARTED = true;
            logger.info("All test containers started successfully");
        }
    }

    /* -------------------------------------------------
     *  RESET — AFTER EACH TEST
     * ------------------------------------------------- */
    @After
    public void resetAfterEachTest() throws Exception {
        logger.info("Resetting Cassandra and Elasticsearch state");

        CassandraTestContainer.resetCassandra();
        ElasticsearchTestContainer.resetElasticsearch();

        logger.info("State reset complete");
    }

    private static void configureDocker() {
        System.setProperty("docker.host", "npipe:////./pipe/docker_engine");
        System.setProperty(
                "org.testcontainers.dockerclient.providerConfig",
                "windows"
        );
    }

    private static void startNetwork() {
        if (network == null) {
            network = Network.builder()
                    .createNetworkCmdModifier(
                            cmd -> cmd.withName("oai-network"))
                    .build();
        }
    }

    private static void startCassandra() {
        if (!CassandraTestContainer.container.isRunning()) {
            CassandraTestContainer.container.start();
            CassandraTestContainer.setConfigProperties();
        }
    }

    private static void startElasticsearch() throws Exception {
        if (!ElasticsearchTestContainer.container.isRunning()) {
            ElasticsearchTestContainer.container.start();
            ElasticsearchTestContainer.setConfigProperties();
            ElasticsearchTestContainer.createIndexAndAlias();
        }
    }

    private static void startSolr() {
        if (!SolrTestContainer.container.isRunning()) {
            SolrTestContainer.container.start();
        }
    }

    private static void startTomcat() {
        if (tomcatContainer != null && tomcatContainer.isRunning()) {
            return;
        }

        MountableFile warFile =
                MountableFile.forHostPath(
                        new File(WAR_FILE_PATH).getAbsolutePath());

        MountableFile configFile =
                MountableFile.forHostPath(
                        new File(CONFIG_FILE_PATH).getAbsolutePath());

        MountableFile serverFile =
                MountableFile.forHostPath(
                        new File(SERVER_FILE_PATH).getAbsolutePath());

        tomcatContainer = new GenericContainer<>("tomcat:11-jre25-temurin")
                .withExposedPorts(8080)
                .withNetwork(network)
                .withCopyFileToContainer(
                        warFile,
                        "/usr/local/tomcat/webapps/oai-backend.war")
                .withCopyFileToContainer(
                        configFile,
                        "/usr/local/tomcat/conf/fiz-oai-backend.properties")
                .withCopyFileToContainer(
                        serverFile,
                        "/usr/local/tomcat/conf/server.xml");

        tomcatContainer.start();
        tomcatContainer.followOutput(new Slf4jLogConsumer(logger));
    }

    /* -------------------------------------------------
     *  SHUTDOWN — JVM EXIT ONLY
     * ------------------------------------------------- */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Stopping test containers (JVM shutdown)");

            try {
                if (tomcatContainer != null) tomcatContainer.stop();
                CassandraTestContainer.container.stop();
                ElasticsearchTestContainer.container.stop();
                SolrTestContainer.container.stop();
            } catch (Exception e) {
                logger.warn("Error during container shutdown", e);
            }
        }));
    }
}
