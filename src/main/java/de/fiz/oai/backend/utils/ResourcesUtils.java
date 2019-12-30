package de.fiz.oai.backend.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourcesUtils {

  private static Logger LOGGER = LoggerFactory.getLogger(ResourcesUtils.class);

  public static String getResourceFileAsString(final String fileName, final ServletContext servletContext) {
    try (InputStream is = servletContext.getResourceAsStream(fileName)) {
      if (is == null) {
        LOGGER.warn("TEMP-DEBUG: is is null, file URL: " + servletContext.getResource(fileName));
        return null;
      }
      try (InputStreamReader isr = new InputStreamReader(is); BufferedReader reader = new BufferedReader(isr)) {
        LOGGER.info("Reading file: " + fileName);
        return reader.lines().collect(Collectors.joining(System.lineSeparator()));
      }
    } catch (final IOException e) {
      LOGGER.error("Error retrieving source file: " + fileName, e);
      return null;
    }
  }

}
