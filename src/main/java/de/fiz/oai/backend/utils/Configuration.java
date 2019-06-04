package de.fiz.oai.backend.utils;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {

    private Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private static final String CONFIG_FILENAME = "fiz-oai-backend.properties";

    private static Configuration instance;

    private Properties properties = new Properties();

    private boolean applicationConfigured = false;

    public static synchronized Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    public String getProperty(String name) {
        return properties.getProperty(name);
    }

    private void loadConfiguration() {
        if (readConfigFromFile(getTomcatConfigFolder(), CONFIG_FILENAME)) {
            printConfiguration();
            applicationConfigured = true;
        }
    }

    protected String getTomcatConfigFolder() {
        String tomcatRoot = System.getProperty("catalina.base");
        if (tomcatRoot == null || tomcatRoot.length() == 0) {
            return null;
        }
        File folder = new File(tomcatRoot, "conf");
        return folder.getAbsolutePath();
    }

    protected boolean readConfigFromFile(String folder, String filename) {

        File file = new File(folder, filename);
        try {
            Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
            try {
                properties.load(reader);
                return true;
            } finally {
                reader.close();
            }
        } catch (Throwable e) {
            LOGGER.error("Unable to read property file: {}", file.getAbsolutePath());
            return false;
        }
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
        LOGGER.info("Using the following configuration: \n" + builder.toString());
    }

}