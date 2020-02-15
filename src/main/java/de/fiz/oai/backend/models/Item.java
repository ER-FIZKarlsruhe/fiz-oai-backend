/*
 * Copyright 2019 FIZ Karlsruhe - Leibniz-Institut fuer Informationsinfrastruktur GmbH
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
package de.fiz.oai.backend.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Item {

  private String identifier;

  private String datestamp;

  private Boolean deleteFlag;

  private List<String> sets;
  
  private List<String> formats;
  
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

  public List<String> getSets() {
    return sets;
  }

  public void setSets(List<String> sets) {
    this.sets = sets;
  }

  public List<String> getFormats() {
    return formats;
  }

  public void setFormats(List<String> formats) {
    this.formats = formats;
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

  public Map<String, Object> toMap() {
    final Map<String, Object> itemMap = new HashMap<String, Object>();
    itemMap.put("identifier", identifier);
    itemMap.put("datestamp", datestamp);
    itemMap.put("deleteFlag", deleteFlag);
    itemMap.put("ingestFormat", ingestFormat);
    itemMap.put("sets", sets);
    return itemMap;
  }

  @Override
  public String toString() {
    return "Item [identifier=" + identifier + ", datestamp=" + datestamp + ", deleteFlag=" + deleteFlag + ", sets=" + sets
            + ", tags=" + tags + ", ingestFormat=" + ingestFormat + "]";
  }

}
