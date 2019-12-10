package de.fiz.oai.backend.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourcesUtils {

  private static Logger LOGGER = LoggerFactory.getLogger(ResourcesUtils.class);

  public static String getResourceFileAsString(final String fileName) throws IOException {
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    LOGGER.warn("TEMP-DEBUG: parameter fileName: " + fileName);
    StringBuilder finalFilename = new StringBuilder();
    if (!fileName.startsWith("/")) {
      finalFilename.append("/");
    }
    finalFilename.append(fileName);
    LOGGER.warn("TEMP-DEBUG: finalFilename: " + finalFilename.toString());
    try (InputStream is = classLoader.getResourceAsStream(finalFilename.toString())) {
      if (is == null) {
        LOGGER.warn(
            "TEMP-DEBUG: is is null, file URL: " + classLoader.getResource(finalFilename.toString()));
        return null;
      }
      try (InputStreamReader isr = new InputStreamReader(is); BufferedReader reader = new BufferedReader(isr)) {
        LOGGER.warn("TEMP-DEBUG: reding streaming");
        return reader.lines().collect(Collectors.joining(System.lineSeparator()));
      }
    }
  }

}
