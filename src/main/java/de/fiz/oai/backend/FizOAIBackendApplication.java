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
package de.fiz.oai.backend;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.CqlSession;

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
                CqlSession session = cm.getCassandraSession();
                //LOGGER.info("Using Cassandra Driver {}", Cluster.getDriverVersion());
                //LOGGER.info("Connected to cluster: {}", cm.getCluster().getMetadata().getClusterName());
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