package de.fiz.oai.backend.models;

public class Crosswalk {

    private String name;
    private String formatFrom;
    private String formatTo;
    private byte[] xsltStylesheet;
    
    
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
    
    public String getFormatFrom() {
      return formatFrom;
    }
    
    public void setFormatFrom(String formatFrom) {
      this.formatFrom = formatFrom;
    }
    
    public String getFormatTo() {
      return formatTo;
    }
    
    public void setFormatTo(String formatTo) {
      this.formatTo = formatTo;
    }
    
    public byte[] getXsltStylesheet() {
      return xsltStylesheet;
    }
    
    public void setXsltStylesheet(byte[] xsltStylesheet) {
      this.xsltStylesheet = xsltStylesheet;
    }
    


  
}