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

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;

/**
 * An interface defining search index management operations
 *
 * @author dbernstein
 */
public interface SearchIndex {

    /**
     * Adds or updates the index with the resource header information.
     * @param resourceHeaders The resource headers associated with the resource

     */
    void addUpdateIndex(ResourceHeaders resourceHeaders);

    /**
     * Adds or updates the index with the resource header information.
     * @param dbTxId The database transaction id
     * @param resourceHeaders The resource headers associated with the resource
     */
    void addUpdateIndex(String dbTxId, ResourceHeaders resourceHeaders);

    /**
     * Removes indexed fields associated with the specified Fedora ID
     * @param fedoraId The Fedora ID
     */
    void removeFromIndex(FedoraId fedoraId);

    /**
     * Performs a search based on the parameters and returns the result.
     *
     * @param parameters The parameters defining the search
     * @return The result of the search
     */
    SearchResult doSearch(SearchParameters parameters) throws InvalidQueryException;


    /**
     * Remove all persistent state associated with the index.
     */
    void reset();
}
