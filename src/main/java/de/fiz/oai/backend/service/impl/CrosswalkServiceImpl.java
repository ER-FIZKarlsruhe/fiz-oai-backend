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
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.fiz.oai.backend.models.crosswalk.CrosswalkProcessingStatus;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import de.fiz.oai.backend.service.SearchService;
import de.fiz.oai.backend.service.TransformerService;
import de.fiz.oai.backend.utils.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Service
@Singleton
public class CrosswalkServiceImpl implements CrosswalkService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrosswalkServiceImpl.class);
    
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
    SearchService searchService;

    @Inject
    ContentService contentService;

    @Inject
    TransformerService transformerService;

    private final AtomicBoolean processingRunning = new AtomicBoolean(false);

    private volatile CrosswalkProcessingStatus crosswalkProcessingStatus;
    private CompletableFuture<Boolean> processCrosswalkFuture;

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

        //Update pool entry in TransformerService
        try {
            LOGGER.error("Update Crosswalk in transformerService pool {}", crosswalk.getName());
            transformerService.updateTransformer(crosswalk.getName());
        } catch (Exception e) {
            LOGGER.error("Cannot update Crosswalk in transformerService pool", e);
        }
        
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

        //Remove pool entry and cached Templates from TransformerService
        try {
            LOGGER.error("Remove Crosswalk from transformerService pool {}", name);
            transformerService.updateTransformer(name);
        } catch (Exception e) {
            LOGGER.error("Cannot remove Crosswalk from transformerService pool", e);
        }
    }

    /**
     * Process a Crosswalk for a set of items
     *
     * @param updateItemTimestamp <code>true</true> if the related item timestamp should be updated
     * @param from                together with the until parameter, it defines a time range for searching items by the
     *                            datestamp, where the related crosswalkshould be processed
     * @param until               together with the from parameter, it defines a time range for searching item by the
     *                            datestamps, where the related crosswalkshould be processed
     * 
     */
    @Override
    public boolean process(String name,
                           boolean updateItemTimestamp,
                           Date from,
                           Date until) throws IOException {

        LOGGER.info("[PROCESS] Starting crosswalk processing: crosswalkName={}", name);

        Crosswalk crosswalk = read(name);
        if (crosswalk == null) {
            throw new InvalidParameterException("Cannot find crosswalk by the given name");
        }

        // ------------------------------------------------------
        // Atomic guard – only one process allowed at a time
        // ------------------------------------------------------
        if (!processingRunning.compareAndSet(false, true)) {
            LOGGER.warn("[STATUS] Crosswalk '{}' already running – aborting new start", name);
            return false;
        }

        // ------------------------------------------------------
        // Initialize status
        // ------------------------------------------------------
        crosswalkProcessingStatus = new CrosswalkProcessingStatus();
        crosswalkProcessingStatus.setCrosswalkName(name);
        crosswalkProcessingStatus.setProcessedCount(0);
        crosswalkProcessingStatus.setStartTime(
                ZonedDateTime.now(ZoneOffset.UTC).toString()
        );

        LOGGER.info("[STATUS] Processing initialized: crosswalk={}, startTime={}",
                name, crosswalkProcessingStatus.getStartTime());

        processCrosswalkFuture = CompletableFuture.supplyAsync(() -> {
            try {
                String searchMark = "";
                AtomicInteger atomicCounter = new AtomicInteger(0);

                do {
                    SearchResult<String> result = searchService.search(
                            100,
                            null,
                            crosswalk.getFormatFrom(),
                            from,
                            until,
                            searchMark
                    );

                    crosswalkProcessingStatus.setTotalCount(result.getTotal());

                    result.getData().parallelStream().forEach(itemId -> {
                        try {
                            processCrosswalkForItem(crosswalk, itemId, updateItemTimestamp);
                            atomicCounter.incrementAndGet();
                            crosswalkProcessingStatus.setProcessedCount(atomicCounter.get());
                        } catch (Exception e) {
                            LOGGER.error("[ITEM ERROR] Failed to process item: {}", itemId, e);
                        }
                    });

                    searchMark = result.getSearchMark();

                } while (StringUtils.isNotBlank(searchMark));

                LOGGER.info("[PROCESS] Crosswalk '{}' processing completed successfully", name);
                return true;

            } catch (Exception e) {
                LOGGER.error("[PROCESS] Error while processing crosswalk '{}'", name, e);
                return false;
            } finally {
                crosswalkProcessingStatus.setEndTime(ZonedDateTime.now(ZoneOffset.UTC).toString()
                );
                processingRunning.set(false);

                LOGGER.info("[STATUS] Processing finished: crosswalk={}, endTime={}",
                        name, crosswalkProcessingStatus.getEndTime());
            }
        });

        return true;
    }





    @Override
    public String getCrosswalkProcessingStatusVerbose() {
        StringBuilder statusString = new StringBuilder();
        if (crosswalkProcessingStatus == null) {
            statusString.append("Crosswalk process not started.");
        } else {
            statusString.append("Crosswalk process STARTED on ");
            statusString.append(crosswalkProcessingStatus.getStartTime());
            if (!StringUtils.isBlank(crosswalkProcessingStatus.getEndTime())) {
                statusString.append(" and FINISHED on ");
                statusString.append(crosswalkProcessingStatus.getEndTime());

            }
            statusString.append(".\n");
            statusString.append("Crosswalk ");
            statusString.append(crosswalkProcessingStatus.getCrosswalkName());

            statusString.append(crosswalkProcessingStatus.getTotalCount());
            statusString.append(".\n");

            double percProgress = 0;
            if (crosswalkProcessingStatus.getProcessedCount() > 0 && crosswalkProcessingStatus.getTotalCount() > 0) {
                percProgress = ((double) crosswalkProcessingStatus.getProcessedCount() / crosswalkProcessingStatus.getTotalCount()) * 100;
            }

            long hours = 0;
            long minutesOfHours = 0;
            int secondsOfMinutes = 0;
            long totalSecondsSoFar = 0;
            ZonedDateTime startZDT = null;
            if (StringUtils.isNotBlank(crosswalkProcessingStatus.getStartTime())) {
                startZDT = ZonedDateTime.parse(crosswalkProcessingStatus.getStartTime());
            }

            Duration timeLapsed;
            if (startZDT != null) {
                timeLapsed = Duration.between(startZDT,
                        StringUtils.isBlank(crosswalkProcessingStatus.getEndTime()) ? ZonedDateTime.now(ZoneOffset.UTC)
                                : ZonedDateTime.parse(crosswalkProcessingStatus.getEndTime()));
                hours = timeLapsed.toHours();
                minutesOfHours = timeLapsed.toMinutesPart();
                secondsOfMinutes = timeLapsed.toSecondsPart();
                totalSecondsSoFar = timeLapsed.toSeconds();
            }

            statusString.append("Progress: ");
            statusString.append(String.format("%.2f", percProgress));
            statusString.append(" % in ");
            statusString.append(hours);
            statusString.append(":");
            statusString.append(String.format("%02d", minutesOfHours));
            statusString.append(":");
            statusString.append(String.format("%02d", secondsOfMinutes));
            statusString.append(".\n");

            String eta = "";
            if (StringUtils.isBlank(crosswalkProcessingStatus.getEndTime()) && percProgress > 0 && totalSecondsSoFar > 0) {
                final double estimatedTotalSeconds = ((double) totalSecondsSoFar / percProgress) * 100;
                final ZonedDateTime etaZDT = startZDT.plusSeconds((long) estimatedTotalSeconds)
                        .withZoneSameInstant(ZoneOffset.UTC);
                eta = etaZDT.toString();
            }

            statusString.append("ETA: ");
            statusString.append(eta);
            statusString.append(".\n");
        }

        return statusString.toString();
    }

    private void processCrosswalkForItem(Crosswalk crosswalk, String itemId, boolean updateItemTimestamp)
            throws IOException {
        LOGGER.info("processCrosswalkForItem {}", itemId);
        try {
            // Update content
            Content content = contentService.read(itemId, crosswalk.getFormatFrom());
            String newXml = transformerService.transform(content.getContent(), crosswalk.getName());
            LOGGER.debug("newXml {}", newXml);
            if (StringUtils.isNotBlank(newXml)) {
                Content crosswalkConten = new Content();
                crosswalkConten.setContent(newXml);
                crosswalkConten.setIdentifier(itemId);
                crosswalkConten.setFormat(crosswalk.getFormatTo());
                daoContent.create(crosswalkConten); //In Cassandra create and update are the same!
            }

            // Update item timestamp
            // Do NOT update the index document here. Otherwise the changed item will popup again during paginated search!
            // Reindex must be done after all items are processed
            if (updateItemTimestamp) {
                Item item = itemService.read(itemId, null, false);
                String datestamp = Configuration.getDateformat().format(new Date());
                LOGGER.debug("Updateing item datestamp {}", datestamp);
                item.setDatestamp(datestamp);
                daoItem.create(item); //In Cassandra create and update are the same!
            }
        } catch (Exception e) {
            LOGGER.error("Exception", e);
            throw e;
        }
    }

}
