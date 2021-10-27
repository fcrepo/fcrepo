/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.search.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class holds the search result data for a single page.
 * @author dbernstein
 */
public class SearchResult {
    @JsonProperty
    private PaginationInfo pagination;
    @JsonProperty
    private List<Map<String, Object>> items;

    /**
     * Default Constructor
     */
    public SearchResult() { }
    /**
     * Constructor
     *
     * @param items      The individual search result items
     * @param pagination The pagination info
     */
    public SearchResult(final List<Map<String, Object>> items, final PaginationInfo pagination) {
        checkNotNull(items, "items cannot be null");
        checkNotNull(pagination, "pagination cannot be null");
        this.items = items;
        this.pagination = pagination;
    }

    /**
     * The pagination information.
     * @return The pagination info
     */
    public PaginationInfo getPagination() {
        return this.pagination;
    }

    /**
     * The list of items returned by the search operation associated with the page indicated by the pagination info.
     * @return The list of items
     */
    public List<Map<String, Object>> getItems() {
        return this.items;
    }
}
