package de.fiz.oai.backend.utils;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Session.State;
import com.datastax.driver.core.SocketOptions;

public class ClusterManager {

    private static final int CASSANDRA_DEFAULT_PORT = 9042;
    private static ClusterManager instance;

    private static int DEFAULT_CASSANDRA_SESSIONS = 20;
    private final int numberOfCassandraSessions;

    private final Cluster cluster;
    private final String keyspace;

    private Session[] sessions = null;
    private int rrSessionCounter = 0;

    private ClusterManager() {
        Configuration config = Configuration.getInstance();
        keyspace = config.getProperty("cassandra.keyspace");
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
        if (cassandraSessionsStr != null) {
            try {
                cassandraSessions = Integer.parseInt(cassandraSessionsStr);
            } catch (NumberFormatException e) {
            }
        }
        numberOfCassandraSessions = cassandraSessions;
        sessions = new Session[numberOfCassandraSessions];
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
        Collection<InetSocketAddress> result = new HashSet<>();
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