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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {

    private Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private static final String CONFIG_FILENAME = "fiz-oai-backend.properties";

    private static Configuration instance;

    private Properties properties = new Properties();

    public Properties getProperties() {
        return properties;
    }

    public static SimpleDateFormat getDateformat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return dateFormat;
    }

    private boolean applicationConfigured = false;

    private Configuration() {
        loadConfiguration();
    }

    public static synchronized Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    public String getProperty(String name) {
        return properties.getProperty(name);
    }

    public String getProperty(String name, String defaultValue) {
        return properties.getProperty(name, defaultValue);
    }

    private void loadConfiguration() {
        if (readConfigFromFile(getConfigFolder(), CONFIG_FILENAME)) {
            printConfiguration();
            applicationConfigured = true;
        }
        else {
            LOGGER.error("Couldnt find configuration-file");
        }
    }

    protected String getConfigFolder() {
        String confFolderPath = null;

        // Is a dedicated oai-backend conf folder defined?
        String oaiBackendConfRoot = System.getProperty("oai.backend.conf.folder");

        // Catalina conf is fallback
        String tomcatRoot = System.getProperty("catalina.base");

        if (oaiBackendConfRoot != null && !oaiBackendConfRoot.isEmpty()) {
            confFolderPath = new File(oaiBackendConfRoot).getAbsolutePath();
        }
        else if (tomcatRoot != null && !tomcatRoot.isEmpty()) {
            confFolderPath = new File(tomcatRoot, "conf").getAbsolutePath();
        }

        LOGGER.info("Use confFolderPath: {}", confFolderPath);

        return confFolderPath;
    }

    protected boolean readConfigFromFile(String folder, String filename) {

        File file = new File(folder, filename);
        try {
            Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
            try {
                properties.load(reader);
                return true;
            }
            finally {
                reader.close();
            }
        }
        catch (Exception e) {
            LOGGER.error("Unable to read property file: {}", file.getAbsolutePath());
        }
        try {
            InputStream in = Configuration.class.getClassLoader().getResourceAsStream(filename);
            if (in != null) {
                properties.load(in);
                return true;
            }
        }
        catch (IOException e) {
            LOGGER.error("Unable to read properties from ClassLoader");
        }
        try {
            InputStream in = new FileInputStream(filename);
            if (in != null) {
                properties.load(in);
                return true;
            }
        }
        catch (IOException e) {
            LOGGER.warn("Could not open " + filename);
        }
        return false;
    }

    public boolean isApplicationConfigured() {
        return applicationConfigured;
    }

    public void printConfiguration() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (entry.getKey().toString().toLowerCase().contains("password")) {
                builder.append(entry.getKey() + " : ***********\n");
            }
            else {
                builder.append(entry.getKey() + " : " + entry.getValue() + "\n");
            }
        }
        LOGGER.info("Using the following configuration: \n{}", builder.toString());
    }

}