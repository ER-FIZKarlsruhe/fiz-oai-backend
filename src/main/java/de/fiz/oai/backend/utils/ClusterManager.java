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
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;

import de.fiz.oai.backend.service.impl.ItemServiceImpl;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;

public class ClusterManager {

    private static final int CASSANDRA_DEFAULT_PORT = 9042;
    private static ClusterManager instance;

    private static int DEFAULT_CASSANDRA_SESSIONS = 20;
    private final int numberOfCassandraSessions;

    private final Cluster cluster;
    private final String keyspace;
    private final String replicationFactor;

    private Session[] sessions = null;
    private int rrSessionCounter = 0;

    private Logger LOGGER = LoggerFactory.getLogger(ClusterManager.class);
    
    private ClusterManager() {
        Configuration config = Configuration.getInstance();
        keyspace = config.getProperty("cassandra.keyspace");
        replicationFactor = config.getProperty("cassandra.replication.factor");
        String cassandraNodes = config.getProperty("cassandra.nodes");
        Collection<InetSocketAddress> addresses = parseCassandraHostConfig(cassandraNodes, config);

        String username = config.getProperty("cassandra.username");
        String password = config.getProperty("cassandra.password");

        Builder builder = Cluster.builder();
        builder.withSocketOptions(new SocketOptions()
                .setReadTimeoutMillis(60000));
        for (InetSocketAddress address : addresses) {
            builder.addContactPoint(address.getHostString());
            builder.withPort(address.getPort());
            if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password)) {
                builder.withCredentials(username, password);
            }
        }
        cluster = builder.build();
        String cassandraSessionsStr = config.getProperty("cassandra.sessions");
        int cassandraSessions = DEFAULT_CASSANDRA_SESSIONS;
        if (StringUtils.isNotBlank(cassandraSessionsStr)) {
            try {
                cassandraSessions = Integer.parseInt(cassandraSessionsStr);
            } catch (NumberFormatException e) {
              LOGGER.warn("Invalid value of property: cassandra.sessions", e);
            }
        }
        numberOfCassandraSessions = cassandraSessions;
        sessions = new Session[numberOfCassandraSessions];

        // Check and create keyspace and tables if not exists
        Session session = cluster.connect();
        CassandraUtils.createKeyspace(session, replicationFactor, keyspace);
        session.close();
    }

    public static ClusterManager getInstance() {
        if (instance == null) {
            instance = new ClusterManager();
        }
        return instance;
    }

    public Session getCassandraSession() {
        int currentSession;
        synchronized (this) {
            currentSession = rrSessionCounter++;
            if (rrSessionCounter  == numberOfCassandraSessions) {
                rrSessionCounter = 0;
            }
        }
        Session session = sessions[currentSession];
        if (session == null || session.isClosed()) {
            session = cluster.connect(keyspace);
            sessions[currentSession] = session;
        }
        return session;
    }

    public Cluster getCluster() {
        return cluster;
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