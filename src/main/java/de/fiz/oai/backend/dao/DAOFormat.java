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
package de.fiz.oai.backend.dao;

import java.io.IOException;
import java.util.List;

import org.jvnet.hk2.annotations.Contract;

import de.fiz.oai.backend.models.Format;

@Contract
public interface DAOFormat {


    /**
     * Read a Format.
     *
     * @param metadataPrefix
     *            the metadataPrefix
     * @return the Format
     */
    Format read(String metadataPrefix) throws IOException;

    /**
     * Create a new Format.
     *
     * @param Format
     *            the Format
     * @return the Format created (in case uuid are processed in the method)
     */
    Format create(Format Format) throws IOException;

    /**
     * Search for Formats.
     *
     * @return the Formats
     */
    List<Format> readAll() throws IOException;

    /**
     * Delete an Format.
     *
     * @param metadataPrefix
     *            the metadataPrefix
     */
    void delete(String metadataPrefix) throws IOException;
}
