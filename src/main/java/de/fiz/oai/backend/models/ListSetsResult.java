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
package de.fiz.oai.backend.models;

import java.util.List;

public class ListSetsResult {

    private final List<Set> sets;
    private final String resumptionToken;  // null if no more pages
    private final int cursor;              // position of first record in this page
    private final Integer completeListSize; // optional

    public ListSetsResult(List<Set> sets, String resumptionToken, int cursor, Integer completeListSize) {
        this.sets = sets;
        this.resumptionToken = resumptionToken;
        this.cursor = cursor;
        this.completeListSize = completeListSize;
    }

    public List<Set> getSets() {
        return sets;
    }

    public String getResumptionToken() {
        return resumptionToken;
    }

    public int getCursor() {
        return cursor;
    }

    public Integer getCompleteListSize() {
        return completeListSize;
    }

}
