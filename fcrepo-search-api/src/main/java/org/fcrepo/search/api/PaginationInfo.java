/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
