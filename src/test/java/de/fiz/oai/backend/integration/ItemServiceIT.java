/**
 * Copyright (c) 2024 Fachinformationszentrum Karlsruhe
 * <p>
 * Created by Michael Hoppe at 11/26/24
 */
package de.fiz.oai.backend.integration;

import de.fiz.oai.backend.dao.DAOContent;
import de.fiz.oai.backend.dao.DAOItem;
import de.fiz.oai.backend.dao.DAOSet;
import de.fiz.oai.backend.dao.impl.CassandraDAOItem;
import de.fiz.oai.backend.models.Item;
import de.fiz.oai.backend.service.ItemService;
import de.fiz.oai.backend.service.SearchService;
import de.fiz.oai.backend.service.impl.EsSearchServiceImpl;
import de.fiz.oai.backend.service.impl.ItemServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        itemService = new ItemServiceImpl();
        injectMocksIntoServices();
    }

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
    public void test1() throws Exception {
        when(daoItem.read(any())).thenReturn(new Item());
        itemService.delete("test");
        System.out.println("OK");
    }

    }
