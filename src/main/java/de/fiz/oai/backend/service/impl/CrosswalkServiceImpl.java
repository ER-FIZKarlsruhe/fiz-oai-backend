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
package de.fiz.oai.backend.service.impl;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.shaded.minlog.Log;
import org.jvnet.hk2.annotations.Service;

import de.fiz.oai.backend.dao.DAOContent;
import de.fiz.oai.backend.dao.DAOCrosswalk;
import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.exceptions.AlreadyExistsException;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.models.Crosswalk;
import de.fiz.oai.backend.models.Format;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.models.SearchResult;
import de.fiz.oai.backend.service.ContentService;
import de.fiz.oai.backend.service.CrosswalkService;
import de.fiz.oai.backend.service.FormatService;
import de.fiz.oai.backend.service.ItemService;
import de.fiz.oai.backend.service.TransformerService;
import de.fiz.oai.backend.utils.Configuration;

@Service
public class CrosswalkServiceImpl implements CrosswalkService {

    @Inject
    DAOItem daoItem;

    @Inject
    DAOContent daoContent;

    @Inject
    DAOCrosswalk daoCrosswalk;

    @Inject
    FormatService formatService;

    @Inject
    ItemService itemService;

    @Inject
    ContentService contentService;

    @Inject
    TransformerService transformerService;

    @Override
    public Crosswalk read(String name) throws IOException {
        Crosswalk crosswalk = daoCrosswalk.read(name);
        return crosswalk;
    }

    @Override
    public Crosswalk create(Crosswalk crosswalk) throws IOException {
        // Does the crosswalk already exists?
        Crosswalk oldCrosswalk = daoCrosswalk.read(crosswalk.getName());
        if (oldCrosswalk != null) {
            throw new AlreadyExistsException("Crosswalk with name " + crosswalk.getName() + " already exist.");
        }

        // Does the from format (referenced by crosswalk) exists?
        Format from = formatService.read(crosswalk.getFormatFrom());
        if (from == null) {
            throw new NotFoundException("Format from " + crosswalk.getFormatFrom() + " not found.");
        }

        // Does the to format (referenced by crosswalk) exists?
        Format to = formatService.read(crosswalk.getFormatTo());
        if (to == null) {
            throw new NotFoundException("Forma to " + crosswalk.getFormatTo() + " not found.");
        }

        Crosswalk newCrosswalk = daoCrosswalk.create(crosswalk);
        return newCrosswalk;
    }

    @Override
    public Crosswalk update(Crosswalk crosswalk) throws IOException {
        // Does the format (referenced by crosswalk) exists?
        Crosswalk oldCrosswalk = daoCrosswalk.read(crosswalk.getName());
        if (oldCrosswalk == null) {
            throw new NotFoundException("Crosswalk with name " + crosswalk.getName() + " not found.");
        }

        // Does the format (referenced by crosswalk) exists?
        Format from = formatService.read(crosswalk.getFormatFrom());
        if (from == null) {
            throw new NotFoundException("Format from " + crosswalk.getFormatFrom() + " not found.");
        }

        // Does the format (referenced by crosswalk) exists?
        Format to = formatService.read(crosswalk.getFormatTo());
        if (to == null) {
            throw new NotFoundException("Forma to " + crosswalk.getFormatTo() + " not found.");
        }

        daoCrosswalk.delete(crosswalk.getName());
        Crosswalk newCrosswalk = daoCrosswalk.create(crosswalk);

        // TODO update TransformerService

        return newCrosswalk;
    }

    @Override
    public List<Crosswalk> readAll() throws IOException {
        List<Crosswalk> crosswalks = daoCrosswalk.readAll();

        return crosswalks;
    }

    @Override
    public void delete(String name) throws IOException {
        daoCrosswalk.delete(name);
    }

    /**
     * Process a Crosswalk for a set of items
     *
     * @param content             String name of the Crosswalk to process
     * @param updateItemTimestamp <code>true</true> if the related item timestamp should be updated
     * @param from                together with the until parameter, it defines a time range for searching items by the
     *                            datestamp, where the related crosswalkshould be processed
     * @param until               together with the from parameter, it defines a time range for searching item by the
     *                            datestamps, where the related crosswalkshould be processed
     * 
     */
    public void process(String name, boolean updateItemTimestamp, Date from, Date until) throws IOException {
        Log.info("Start process crosswalk for " + name);
        Log.info("updateItemTimestamp " + updateItemTimestamp);
        Log.info("from " + from);
        Log.info("until " + until);

        Crosswalk crosswalk = read(name);
        if (crosswalk == null) {
            throw new InvalidParameterException("Cannot find crosswalk by the given name");
        }

        Boolean searchMore = true;
        String searchMark = "";

        do {
            Log.info("search more items to process with searchMark " + searchMark);
            SearchResult<Item> result = itemService.search(100, null, crosswalk.getFormatFrom(), from, until, false,
                    searchMark);

            if (result.getSize() > 0) {
                Iterator<Item> itemIterator = result.getData().iterator();
                Item item = null;
                while (itemIterator.hasNext()) {
                    item = itemIterator.next();
                    processCrosswalkForItem(crosswalk, item, updateItemTimestamp);
                }
            }

            searchMark = result.getSearchMark();
        } while (searchMark != null);

        Log.info("End process crosswalk for " + name);

        if (updateItemTimestamp) {
            Log.warn("You have to reindex your search index manually to refresh the items timestamps!");
        }

        return;
    }

    private void processCrosswalkForItem(Crosswalk crosswalk, Item item, boolean updateItemTimestamp)
            throws IOException {
        Log.info("processCrosswalkForItem " + item);
        try {
            // Update content
            Content content = contentService.read(item.getIdentifier(), crosswalk.getFormatFrom());
            String newXml = transformerService.transform(content.getContent(), crosswalk.getName());
            Log.debug("newXml " + newXml);
            if (StringUtils.isNotBlank(newXml)) {
                Content crosswalkConten = new Content();
                crosswalkConten.setContent(newXml);
                crosswalkConten.setIdentifier(item.getIdentifier());
                crosswalkConten.setFormat(crosswalk.getFormatTo());
                daoContent.create(crosswalkConten); //In Cassandra create and update are the same!
            }

            // Update item timestamp
            // Do NOT update the index document here. Otherwise the changed item will popup again during paginated search!
            // Reindex must be done after all items are processed
            if (updateItemTimestamp) {
                String datestamp = Configuration.getDateformat().format(new Date());
                Log.info("Updateing item datestamp " + datestamp);
                item.setDatestamp(datestamp);
                daoItem.create(item); //In Cassandra create and update are the same!
            }
        } catch (Exception e) {
            Log.error("Exception", e);
            throw e;
        }
    }

}
