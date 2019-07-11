package de.fiz.oai.backend.models;

public class Content {

    private String identifier;
    private String format;
    private String content;
    
    
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
    
    public String getContent() {
      return content;
    }
    
    public void setContent(String content) {
      this.content = content;
    }

  
}
