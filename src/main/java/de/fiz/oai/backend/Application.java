package de.fiz.oai.backend;

import javax.servlet.GenericServlet;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.utils.Configuration;
import de.fiz.oai.backend.utils.ClusterManager;
import de.fiz.oai.backend.utils.CassandraUtils;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.Cluster;

public class Application extends GenericServlet {

    private static final long serialVersionUID = -1156196714908290948L;

    private Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private boolean applicationReady = false;

    private static Application instance;

    @Override
    public void init() throws ServletException {
        super.init();
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

    public static Application getInstance() {
        return instance;
    }

    public boolean isApplicationReady() {
        return applicationReady;
    }

    @Override
    public void service(ServletRequest arg0, ServletResponse arg1) throws ServletException, IOException {
    }

    @Override
    public String getServletInfo() {
        return "FIZ OAI Backend Application";
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            ClusterManager.getInstance().getCluster().close();
        }
        catch (Exception e) {
        }
    }
}