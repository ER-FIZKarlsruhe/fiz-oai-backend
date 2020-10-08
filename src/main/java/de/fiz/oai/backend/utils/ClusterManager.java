/*
 * Copyright 2019 FIZ Karlsruhe - Leibniz-Institut fuer Informationsinfrastruktur GmbH
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
package de.fiz.oai.backend.utils;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;

public class ClusterManager {

    private static final int CASSANDRA_DEFAULT_PORT = 9042;

    private static ClusterManager instance;

    private static int DEFAULT_CASSANDRA_SESSIONS = 20;
    
    private static long DEFAULT_REQUEST_TIMEOUT = 2;

    private final int numberOfCassandraSessions;

    private final String keyspace;

    private final String replicationFactor;

    private final String datacenter;

    private CqlSession[] sessions = null;

    private int rrSessionCounter = 0;

    CqlSessionBuilder oiaBuilder;

    private Logger LOGGER = LoggerFactory.getLogger(ClusterManager.class);

    private ClusterManager() {
        LOGGER.info("Init cluster manager");
        Configuration config = Configuration.getInstance();
        keyspace = config.getProperty("cassandra.keyspace");
        datacenter = config.getProperty("cassandra.datacenter");
        replicationFactor = config.getProperty("cassandra.replication.factor");
        String cassandraNodes = config.getProperty("cassandra.nodes");
        Collection<InetSocketAddress> addresses = parseCassandraHostConfig(cassandraNodes, config);
        LOGGER.info("Found keyspace {}", keyspace);
        LOGGER.info("Found datacenter {}", datacenter);
        LOGGER.info("Found replicationFactor {}", replicationFactor);
        LOGGER.info("Found cassandraNodes {}", cassandraNodes);

        String username = config.getProperty("cassandra.username");
        String password = config.getProperty("cassandra.password");
        LOGGER.info("Found username {}", username);
        LOGGER.info("Found password {}", "***");

        long requestTimeout = DEFAULT_REQUEST_TIMEOUT;
        try {
            requestTimeout = Integer.parseInt(config.getProperty("cassandra.requesttimeout"));
        }
        catch (NumberFormatException e) {
            LOGGER.warn("Invalid value of property: cassandra.requesttimeout", e);
        }
        LOGGER.info("Found requesttimeout {}", requestTimeout);

        oiaBuilder = CqlSession.builder();
        oiaBuilder.withKeyspace(keyspace);
        oiaBuilder.withLocalDatacenter(datacenter);

        DriverConfigLoader configLoader =
            DriverConfigLoader
                .programmaticBuilder().withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(requestTimeout))
                .build();

        if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password)) {
            oiaBuilder.withAuthCredentials(username, password);
        }

        for (InetSocketAddress address : addresses) {
            LOGGER.info("Found address {}", address);
            int containerPort = address.getPort();
            LOGGER.info("Found containerPort {}", containerPort);

            oiaBuilder.addContactPoint(new InetSocketAddress(address.getHostString(), containerPort));
        }

        String cassandraSessionsStr = config.getProperty("cassandra.sessions");
        int cassandraSessions = DEFAULT_CASSANDRA_SESSIONS;
        if (StringUtils.isNotBlank(cassandraSessionsStr)) {
            try {
                cassandraSessions = Integer.parseInt(cassandraSessionsStr);
            }
            catch (NumberFormatException e) {
                LOGGER.warn("Invalid value of property: cassandra.sessions", e);
            }
        }
        numberOfCassandraSessions = cassandraSessions;
        sessions = new CqlSession[numberOfCassandraSessions];

        // Check and create keyspace and tables if not exists
        CqlSession session = oiaBuilder.withConfigLoader(configLoader).build();
        CassandraUtils.createKeyspace(session, replicationFactor, keyspace);
        session.close();
    }

    public static ClusterManager getInstance() {
        if (instance == null) {
            instance = new ClusterManager();
        }
        return instance;
    }

    public CqlSession getCassandraSession() {
        int currentSession;
        synchronized (this) {
            currentSession = rrSessionCounter++;
            if (rrSessionCounter == numberOfCassandraSessions) {
                rrSessionCounter = 0;
            }
        }
        CqlSession session = sessions[currentSession];
        if (session == null || session.isClosed()) {
            session = oiaBuilder.build();
            sessions[currentSession] = session;
        }
        return session;
    }

    private Collection<InetSocketAddress> parseCassandraHostConfig(String cassandraConfigStr, Configuration config) {
        // TODO: need to support IPv6 addresses here at some point
        Collection<InetSocketAddress> result = new HashSet<InetSocketAddress>();
        String[] splits = cassandraConfigStr.split(",");
        for (String split : splits) {
            String[] server = split.split(":");
            int port = CASSANDRA_DEFAULT_PORT;
            if (server.length > 1) {
                port = Integer.parseInt(server[1]);
            }
            result.add(new InetSocketAddress(server[0], port));
        }
        return result;
    }
}