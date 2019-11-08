package de.fiz.oai.backend.models;

import java.util.Map;

public class Set {

  private String name;

  private String spec;

  private String description;

  private Map<String, String> xPaths;

  private String status;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSpec() {
    return spec;
  }

  public void setSpec(String spec) {
    this.spec = spec;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Map<String, String> getxPaths() {
    return xPaths;
  }

  public void setxPaths(Map<String, String> xPaths) {
    this.xPaths = xPaths;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

}
