package de.fiz.oai.backend.utils;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {

    public static SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd'T'hh:mm:ss'Z'");
  
    private Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private static final String CONFIG_FILENAME = "fiz-oai-backend.properties";

    private static Configuration instance;

    private Properties properties = new Properties();

    public Properties getProperties() {
      return properties;
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

    private void loadConfiguration() {
        if (readConfigFromFile(getConfigFolder(), CONFIG_FILENAME)) {
            printConfiguration();
            applicationConfigured = true;
        }
    }

    protected String getConfigFolder() {
        String confFolderPath = null;
        
        //Is a dedicated oai-backend conf folder defined?
        String oaiBackendConfRoot = System.getProperty("oai.backend.conf.folder");

        //Catalina conf is fallback
        String tomcatRoot = System.getProperty("catalina.base");
        
        if (oaiBackendConfRoot != null && !oaiBackendConfRoot.isEmpty()) {
          confFolderPath = new File(oaiBackendConfRoot).getAbsolutePath();
        } else if (tomcatRoot != null && !tomcatRoot.isEmpty()) {
          confFolderPath = new File(tomcatRoot, "conf").getAbsolutePath();
        }

        LOGGER.info("Use confFolderPath: " + confFolderPath);
        
        return confFolderPath;
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