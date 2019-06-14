package de.fiz.oai.backend;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;

import de.fiz.oai.backend.controller.ItemController;
import de.fiz.oai.backend.controller.VersionController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.utils.Configuration;
import de.fiz.oai.backend.utils.ClusterManager;
import de.fiz.oai.backend.utils.CassandraUtils;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.Cluster;

public class FizOAIBackendApplication extends Application {

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
    }

    public static FizOAIBackendApplication getInstance() {
        return instance;
    }

    public boolean isApplicationReady() {
        return applicationReady;
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(VersionController.class);
        classes.add(ItemController.class);
        return classes;
    }


}