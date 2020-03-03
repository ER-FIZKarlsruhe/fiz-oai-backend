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

public class Crosswalk {

    private String name;
    private String formatFrom;
    private String formatTo;
    private String xsltStylesheet;
    
    
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
    
    public String getXsltStylesheet() {
      return xsltStylesheet;
    }
    
    public void setXsltStylesheet(String xsltStylesheet) {
      this.xsltStylesheet = xsltStylesheet;
    }
    


  
}
