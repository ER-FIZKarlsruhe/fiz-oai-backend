package de.fiz.oai.backend.models;

import java.util.List;

public class Item {

  private String identifier;

  private String datestamp;
  
  private Boolean deleteFlag;
  
  private List<String> sets;
  
  private String ingestFormat;

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

  public List<String> getSets() {
    return sets;
  }

  public void setSets(List<String> sets) {
    this.sets = sets;
  }

  public String getIngestFormat() {
    return ingestFormat;
  }

  public void setIngestFormat(String ingestFormat) {
    this.ingestFormat = ingestFormat;
  }

  @Override
  public String toString() {
    return "Item [identifier=" + identifier + ", datestamp=" + datestamp + ", deleteFlag=" + deleteFlag + ", sets="
        + sets + ", ingestFormat=" + ingestFormat + "]";
  }
  
}
