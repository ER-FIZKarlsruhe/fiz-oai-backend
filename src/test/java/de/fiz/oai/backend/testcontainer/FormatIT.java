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
package de.fiz.oai.backend.testcontainer;

import org.junit.Test;

import java.io.IOException;


public class FormatIT extends BaseInstance {



    @Test
    public void testCrudFormats() throws IOException {
        createFormatIfNotExisting("test_format", "http://abc.de/", "http://abc.de/test_format");
        updateFormat("test_format", "http://adc.de/new", "http://adc.de/test_format");
        deleteFormat("test_format");
    }



}
