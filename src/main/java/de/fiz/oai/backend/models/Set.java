package de.fiz.oai.backend.models;

public class Set {

    private String name;
    
    private String spec;
    
    private String description;
    
    private String ingestFormat;
    
    private String xPath;

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

    public String getIngestFormat() {
      return ingestFormat;
    }

    public void setIngestFormat(String ingestFormat) {
      this.ingestFormat = ingestFormat;
    }

    public String getxPath() {
      return xPath;
    }

    public void setxPath(String xPath) {
      this.xPath = xPath;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

}
