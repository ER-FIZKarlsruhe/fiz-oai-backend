package de.fiz.oai.backend.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Item {

  private String identifier;

  private String datestamp;
  
  private Boolean deleteFlag;
  
  private List<String> tags;
  
  private String ingestFormat;
  
  private Content content;

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getDatestamp() {
    return datestamp;
  }

  public void setDatestamp(String datestamp) {
    this.datestamp = datestamp;
  }

  public Boolean isDeleteFlag() {
    return deleteFlag;
  }

  public void setDeleteFlag(Boolean deleteFlag) {
    this.deleteFlag = deleteFlag;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public String getIngestFormat() {
    return ingestFormat;
  }

  public void setIngestFormat(String ingestFormat) {
    this.ingestFormat = ingestFormat;
  }

  
  public Content getContent() {
    return content;
  }

  public void setContent(Content content) {
    this.content = content;
  }

  public Map<String,Object> toMap(){
    final Map<String, Object> itemMap = new HashMap<String, Object>();
    itemMap.put("identifier", identifier);
    itemMap.put("datestamp", datestamp);
    itemMap.put("deleteFlag", deleteFlag);
    itemMap.put("ingestFormat", ingestFormat);
    return itemMap;
  }
  
  @Override
  public String toString() {
    return "Item [identifier=" + identifier + ", datestamp=" + datestamp + ", deleteFlag=" + deleteFlag + ", tags="
        + tags + ", ingestFormat=" + ingestFormat + "]";
  }
  
}
