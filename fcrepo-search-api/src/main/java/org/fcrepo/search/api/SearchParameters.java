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

import java.util.List;

/**
 * A pojo encapsulating the parameters of a search
 *
 * @author dbernstein
 */
public class SearchParameters {

    private final List<Condition> conditions;

    private final int offset;

    private final int maxResults;

    /**
     * Constructoor
     *
     * @param conditions The conditions
     * @param maxResults The max results
     * @param offset     The offset
     */
    public SearchParameters(final List<Condition> conditions, final int maxResults, final int offset) {
        this.conditions = conditions;
        this.maxResults = maxResults;
        this.offset = offset;
    }

    /**
     * The offset (zero-based)
     *
     * @return
     */
    public int getOffset() {
        return offset;
    }

    /**
     * The max number of results to return
     *
     * @return
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * The conditions limiting the search
     *
     * @return
     */
    public List<Condition> getConditions() {
        return conditions;
    }
}
