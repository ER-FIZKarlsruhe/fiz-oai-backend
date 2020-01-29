package de.fiz.oai.backend.models.reindex;

import com.datastax.driver.core.ResultSet;

public class ReindexStatus {

  private ResultSet itemResultSet = null;

  private String aliasName = null;

  private String originalIndexName = null;

  private String newIndexName = null;

  private long totalCount;

  private long indexedCount;

  private String startTime;

  private String endTime;

  private boolean stopSignalReceived;

  /**
   * @return the aliasName
   */
  public String getAliasName() {
    return aliasName;
  }

  /**
   * @param aliasName the aliasName to set
   */
  public void setAliasName(String aliasName) {
    this.aliasName = aliasName;
  }

  /**
   * @return the originalIndexName
   */
  public String getOriginalIndexName() {
    return originalIndexName;
  }

  /**
   * @param originalIndexName the originalIndexName to set
   */
  public void setOriginalIndexName(String originalIndexName) {
    this.originalIndexName = originalIndexName;
  }

  /**
   * @return the newIndexName
   */
  public String getNewIndexName() {
    return newIndexName;
  }

  /**
   * @param newIndexName the newIndexName to set
   */
  public void setNewIndexName(String newIndexName) {
    this.newIndexName = newIndexName;
  }

  /**
   * @return the totalCount
   */
  public long getTotalCount() {
    return totalCount;
  }

  /**
   * @param totalCount the totalCount to set
   */
  public void setTotalCount(long totalCount) {
    this.totalCount = totalCount;
  }

  /**
   * @return the indexedCount
   */
  public long getIndexedCount() {
    return indexedCount;
  }

  /**
   * @param indexedCount the indexedCount to set
   */
  public void setIndexedCount(long indexedCount) {
    this.indexedCount = indexedCount;
  }

  /**
   * @return the startTime
   */
  public String getStartTime() {
    return startTime;
  }

  /**
   * @param startTime the startTime to set
   */
  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  /**
   * @return the endTime
   */
  public String getEndTime() {
    return endTime;
  }

  /**
   * @param endTime the endTime to set
   */
  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  /**
   * @return the itemResultSet
   */
  public ResultSet getItemResultSet() {
    return itemResultSet;
  }

  /**
   * @param itemResultSet the itemResultSet to set
   */
  public void setItemResultSet(ResultSet itemResultSet) {
    this.itemResultSet = itemResultSet;
  }

  /**
   * @return the stopSignalReceived
   */
  public boolean isStopSignalReceived() {
    return stopSignalReceived;
  }

  /**
   * @param stopSignalReceived the stopSignalReceived to set
   */
  public void setStopSignalReceived(boolean stopSignalReceived) {
    this.stopSignalReceived = stopSignalReceived;
  }

}
