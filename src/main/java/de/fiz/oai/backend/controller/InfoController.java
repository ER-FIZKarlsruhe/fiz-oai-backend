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
package de.fiz.oai.backend.controller;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.fiz.oai.backend.service.TransformerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.utils.Configuration;

@Path("/info")
public class InfoController extends AbstractController{

    private static Logger LOGGER = LoggerFactory.getLogger(InfoController.class);

    @Inject
    TransformerService transformerService;

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public String getVersion() throws IOException {
        LOGGER.debug("getVersion called");
        return getClass().getPackage().getImplementationVersion();
    }

    @GET
    @Path("/configuration")
    @Produces(MediaType.TEXT_PLAIN)
    public String getConfigInfo() throws IOException {
      StringBuilder builder = new StringBuilder();
      for (Map.Entry<Object, Object> entry : Configuration.getInstance().getProperties().entrySet()) {
          if (entry.getKey().toString().toLowerCase().contains("password")) {
              builder.append(entry.getKey() + " : ***********\n");
          }
          else {
              builder.append(entry.getKey() + " : " + entry.getValue() + "\n");
          }
      }

      return builder.toString();
    }

    @GET
    @Path("/pool")
    @Produces(MediaType.TEXT_PLAIN)
    public String getPoolInfo() {
        return transformerService.info();
    }

}
