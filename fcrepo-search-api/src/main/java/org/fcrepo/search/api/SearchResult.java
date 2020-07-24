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
