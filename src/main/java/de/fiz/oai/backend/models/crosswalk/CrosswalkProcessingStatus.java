/*
 * Copyright 2025 FIZ Karlsruhe - Leibniz-Institut fuer Informationsinfrastruktur GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fiz.oai.backend.models.crosswalk;

import com.datastax.oss.driver.api.core.cql.ResultSet;

public class CrosswalkProcessingStatus {

  private ResultSet itemResultSet = null;

  private String crosswalkName = null;

  private long totalCount;

  private long processedCount;

  private String startTime;

  private String endTime;

  private boolean stopSignalReceived;

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
  public long getProcessedCount() {
    return processedCount;
  }

  /**
   * @param processedCount the indexedCount to set
   */
  public void setProcessedCount(long processedCount) {
    this.processedCount = processedCount;
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

    public String getCrosswalkName() {
        return crosswalkName;
    }

    public void setCrosswalkName(String crosswalkName) {
        this.crosswalkName = crosswalkName;
    }
}
