/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.search.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A data structure representing the pagination information associated with a {@link org.fcrepo.search.api.SearchResult}
 *
 * @author dbernstein
 */
public class PaginationInfo {
    @JsonProperty
    private int offset = -1;
    @JsonProperty
    private int maxResults = -1;
    @JsonProperty
    private int totalResults;

    /**
     * Default constructor
     */
    public PaginationInfo() { }

    /**
     * Constructor
     *
     * @param maxResults max results asked off
     * @param offset     offset of the first result item
     * @param totalResults The total number of results
     */
    public PaginationInfo(final int maxResults, final int offset, final int totalResults) {
        this.maxResults = maxResults;
        this.offset = offset;
        this.totalResults = totalResults;
    }

    /**
     * @return The max results of the original query
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * @return The offset specified by original query
     */
    public int getOffset() {
        return offset;
    }

    /**
     * @return The total number of results for this query.
     */
    public int getTotalResults() {
        return this.totalResults;
    }
}
