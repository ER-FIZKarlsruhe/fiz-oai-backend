package de.fiz.oai.backend.models;

public class Format {

  private String metadataPrefix;
  private String schemaLocation;
  private String schemaNamespace;
  private String identifierXpath;
  private String status;

  public String getMetadataPrefix() {
    return metadataPrefix;
  }

  public void setMetadataPrefix(String metadataPrefix) {
    this.metadataPrefix = metadataPrefix;
  }

  public String getSchemaLocation() {
    return schemaLocation;
  }

  public void setSchemaLocation(String schemaLocation) {
    this.schemaLocation = schemaLocation;
  }

  public String getSchemaNamespace() {
    return schemaNamespace;
  }

  public void setSchemaNamespace(String schemaNamespace) {
    this.schemaNamespace = schemaNamespace;
  }

  public String getIdentifierXpath() {
    return identifierXpath;
  }

  public void setIdentifierXpath(String identifierXpath) {
    this.identifierXpath = identifierXpath;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
