package de.fiz.oai.backend;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import de.fiz.oai.backend.utils.CassandraUtils;
import de.fiz.oai.backend.utils.ClusterManager;
import de.fiz.oai.backend.utils.Configuration;

public class FizOAIBackendApplication extends ResourceConfig {

    private static final long serialVersionUID = -1156196714908290948L;

    private Logger LOGGER = LoggerFactory.getLogger(FizOAIBackendApplication.class);

    private boolean applicationReady = false;

    private static FizOAIBackendApplication instance;

    public FizOAIBackendApplication() {
        instance = this;
        LOGGER.info("FIZ OAI Backend starting");
        Configuration config = Configuration.getInstance();
        if (config.isApplicationConfigured()) {
            try {
                ClusterManager cm = ClusterManager.getInstance();
                Session session = cm.getCassandraSession();
                LOGGER.info("Using Cassandra Driver {}", Cluster.getDriverVersion());
                LOGGER.info("Connected to cluster: {}", cm.getCluster().getMetadata().getClusterName());
                LOGGER.debug(CassandraUtils.getClusterTopologyInformation(session));
                applicationReady = true;
                LOGGER.info("FIZ OAI Backend has started and is now accepting requests");
            }
            catch (Exception e) {
                LOGGER.error("FIZ OAI Backend NOT started: {}", e.getLocalizedMessage());
            }
        }
        
        register(MultiPartFeature.class);
        register(new FizOAIBackendBinder());  
    }

    public static FizOAIBackendApplication getInstance() {
        return instance;
    }

    public boolean isApplicationReady() {
        return applicationReady;
    }

}