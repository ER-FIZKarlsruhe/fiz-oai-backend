/**
 * Copyright (c) 2024 Fachinformationszentrum Karlsruhe
 * <p>
 * Created by Michael Hoppe at 11/26/24
 */
package de.fiz.oai.backend.integration;

import de.fiz.oai.backend.dao.DAOContent;
import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.dao.DAOSet;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Content;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.service.ItemService;
import de.fiz.oai.backend.service.SearchService;
import de.fiz.oai.backend.service.impl.ItemServiceImpl;
import de.fiz.oai.backend.utils.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ItemServiceIT {

    private ItemService itemService;

    @Mock
    private SearchService searchService;

    @Mock
    private DAOItem daoItem;

    @Mock
    private DAOContent daoContent;

    @Mock
    private DAOSet daoSet;

    @Mock
    private Configuration configuration;




    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        itemService = new ItemServiceImpl();
        injectMocksIntoServices();
    }

    /**
     * Inject needed Mocks for ItemService
     */
    private void injectMocksIntoServices() {
        try {


            Field searchServiceField = ItemServiceImpl.class.getDeclaredField("searchService");
            searchServiceField.setAccessible(true);
            searchServiceField.set(itemService, searchService);

            Field daoItemField = ItemServiceImpl.class.getDeclaredField("daoItem");
            daoItemField.setAccessible(true);
            daoItemField.set(itemService, daoItem);

            Field daoContentField = ItemServiceImpl.class.getDeclaredField("daoContent");
            daoContentField.setAccessible(true);
            daoContentField.set(itemService, daoContent);

            Field daoSetField = ItemServiceImpl.class.getDeclaredField("daoSet");
            daoSetField.setAccessible(true);
            daoSetField.set(itemService, daoSet);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock into MyService", e);
        }
    }

    @Test
    public void testConfiguration() throws Exception {
        MockedStatic<Configuration> configurationStatic = Mockito.mockStatic(Configuration.class);

        configurationStatic.when(Configuration::getInstance).thenReturn(configuration);
        configurationStatic.when(Configuration::getDateformat).thenReturn(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        //when(configuration.getProperty(any(), any())).thenAnswer(invocation -> invocation.getRawArguments()[1]);
    }

    /**
     * Test successful delete.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteSuccessful() throws Exception {
        when(daoItem.read(any(String.class))).thenReturn(new Item());
        itemService.delete("test");
    }

    /**
     * Test deleting non-existing item.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteNotFound() throws Exception {
        when(daoItem.read(any(String.class))).thenReturn(null);
        assertThrows(NotFoundException.class, () -> itemService.delete("test"));
    }

    /**
     * Test successful delete.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteCompletely() throws Exception {
        when(daoItem.read(any(String.class))).thenReturn(new Item());
        //when(configuration.getProperty(eq("deletedRecord"), any())).thenReturn("no");
        itemService.delete("test");
    }

    /**
     * Regression test for a bug where the {@code format} parameter, when null, was reassigned
     * inside the read loop to the first item's ingestFormat and then reused for every subsequent
     * item, silently reading the wrong content for items with a different ingestFormat.
     *
     * @throws Exception
     */
    @Test
    public void testReadMultipleUsesEachItemsOwnIngestFormatWhenFormatIsNull() throws Exception {
        Item item1 = new Item();
        item1.setIdentifier("id1");
        item1.setIngestFormat("formatA");

        Item item2 = new Item();
        item2.setIdentifier("id2");
        item2.setIngestFormat("formatB");

        Map<String, Item> dbItems = new LinkedHashMap<>();
        dbItems.put("id1", item1);
        dbItems.put("id2", item2);
        when(daoItem.read(List.of("id1", "id2"))).thenReturn(dbItems);

        Content content1 = new Content();
        content1.setIdentifier("id1");
        content1.setFormat("formatA");
        content1.setContent("content-a");

        Content content2 = new Content();
        content2.setIdentifier("id2");
        content2.setFormat("formatB");
        content2.setContent("content-b");

        Map<String, String> expectedIdentifierToFormat = new LinkedHashMap<>();
        expectedIdentifierToFormat.put("id1", "formatA");
        expectedIdentifierToFormat.put("id2", "formatB");

        Map<String, Content> contents = new LinkedHashMap<>();
        contents.put("id1", content1);
        contents.put("id2", content2);
        when(daoContent.read(expectedIdentifierToFormat)).thenReturn(contents);

        Map<String, Object> doc1 = new HashMap<>();
        doc1.put("identifier", "id1");
        Map<String, Object> doc2 = new HashMap<>();
        doc2.put("identifier", "id2");
        when(searchService.readDocuments(any())).thenReturn(List.of(doc1, doc2));

        List<Item> result = itemService.read(List.of("id1", "id2"), null, true);

        assertEquals(2, result.size());
        Map<String, Item> byId = new LinkedHashMap<>();
        for (Item item : result) {
            byId.put(item.getIdentifier(), item);
        }
        assertEquals("content-a", byId.get("id1").getContent().getContent());
        assertEquals("content-b", byId.get("id2").getContent().getContent());
    }

}
