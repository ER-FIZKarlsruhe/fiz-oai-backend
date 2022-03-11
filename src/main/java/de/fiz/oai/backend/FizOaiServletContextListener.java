/* 
 * Copyright 2021 FIZ Karlsruhe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ROLE_ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package de.fiz.oai.backend;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.utils.ClusterManager;

/**
 * @author Michael Hoppe
 *
 */
@WebListener
public class FizOaiServletContextListener implements ServletContextListener {
    private static final Logger LOG = LoggerFactory.getLogger(FizOaiServletContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
     // Do nothing.
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        ClusterManager.getInstance().shutdown();
    }
}
