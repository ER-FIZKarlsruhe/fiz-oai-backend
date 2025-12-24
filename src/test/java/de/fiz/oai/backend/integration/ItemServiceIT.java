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
        when(daoItem.read(any())).thenReturn(new Item());
        itemService.delete("test");
    }

    /**
     * Test deleting non-existing item.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteNotFound() throws Exception {
        when(daoItem.read(any())).thenReturn(null);
        assertThrows(NotFoundException.class, () -> itemService.delete("test"));
    }

    /**
     * Test successful delete.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteCompletely() throws Exception {
        when(daoItem.read(any())).thenReturn(new Item());
        //when(configuration.getProperty(eq("deletedRecord"), any())).thenReturn("no");
        itemService.delete("test");
    }

}
