/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.search.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author bbpennel
 */
public class SearchResultTest {

    private PaginationInfo paginationInfo;
    private List<Map<String, Object>> items;

    @BeforeEach
    public void setUp() {
        paginationInfo = new PaginationInfo();
        items = new ArrayList<>();
        final Map<String, Object> item1 = new HashMap<>();
        item1.put("id", "test1");
        item1.put("title", "Test Item 1");
        final Map<String, Object> item2 = new HashMap<>();
        item2.put("id", "test2");
        item2.put("title", "Test Item 2");
        items.add(item1);
        items.add(item2);
    }

    @Test
    public void testConstructorWithValidParameters() {
        final SearchResult result = new SearchResult(items, paginationInfo);
        assertNotNull(result, "SearchResult should be created successfully");
        assertEquals(items, result.getItems(), "Items should match what was passed to constructor");
        assertEquals(paginationInfo, result.getPagination(),
                "PaginationInfo should match what was passed to constructor");
    }

    @Test
    public void testConstructorWithNullItems() {
        final var exception = assertThrows(NullPointerException.class,
                () -> new SearchResult(null, paginationInfo));
        assertEquals("items cannot be null", exception.getMessage(),
                "Exception message should indicate items is null");
    }

    @Test
    public void testConstructorWithNullPagination() {
        final NullPointerException exception = assertThrows(NullPointerException.class,
                () -> new SearchResult(items, null));
        assertEquals("pagination cannot be null", exception.getMessage(),
                "Exception message should indicate pagination is null");
    }

    @Test
    public void testDefaultConstructor() throws Exception {
        final SearchResult result = new SearchResult();
        assertNull(result.getPagination());
        assertNull(result.getItems());
    }

    @Test
    public void testGetItems() {
        final SearchResult result = new SearchResult(items, paginationInfo);
        assertEquals(items, result.getItems());
        assertEquals(2, result.getItems().size());
        assertEquals("test1", result.getItems().get(0).get("id"));
        assertEquals("Test Item 2", result.getItems().get(1).get("title"));
    }

    @Test
    public void testGetPagination() {
        final SearchResult result = new SearchResult(items, paginationInfo);
        assertEquals(paginationInfo, result.getPagination());
    }
}