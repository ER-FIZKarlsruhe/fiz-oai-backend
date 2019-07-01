package de.fiz.oai.backend.models;

public class Content {

    private String identifier;
    private String format;
    private byte[] content;
    
    
    public String getIdentifier() {
      return identifier;
    }
    public void setIdentifier(String identifier) {
      this.identifier = identifier;
    }
    public String getFormat() {
      return format;
    }
    public void setFormat(String format) {
      this.format = format;
    }
    public byte[] getContent() {
      return content;
    }
    public void setContent(byte[] content) {
      this.content = content;
    }

  
}
