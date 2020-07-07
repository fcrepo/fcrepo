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

import com.google.common.base.MoreObjects;

import java.util.List;

/**
 * A pojo encapsulating the parameters of a search
 *
 * @author dbernstein
 */
public class SearchParameters {

    private final List<Condition> conditions;

    private final List<Condition.Field> fields;

    private final int offset;

    private final int maxResults;

    private final Condition.Field orderBy;

    private final String order;

    /**
     * Constructoor
     *
     * @param fields     The fields to be returned in the results
     * @param conditions The conditions
     * @param maxResults The max results
     * @param offset     The offset
     * @param orderBy    The field by which to order the results
     * @param order      The order: ie "asc" or "desc"
     */
    public SearchParameters(final List<Condition.Field> fields, final List<Condition> conditions, final int maxResults,
                            final int offset, final Condition.Field orderBy, final String order) {
        this.fields = fields;
        this.conditions = conditions;
        this.maxResults = maxResults;
        this.offset = offset;
        this.orderBy = orderBy;
        this.order = order;
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

    /**
     * Returns the list of fields to display in the results.
     *
     * @return
     */
    public List<Condition.Field> getFields() {
        return fields;
    }

    /**
     * Returns the field by which to order the results.
     *
     * @return
     */
    public Condition.Field getOrderBy() {
        return orderBy;
    }

    /**
     * Returns the order direction (asc or desc) of the results.
     *
     * @return
     */
    public String getOrder() {
        return order;
    }

    @Override
    public String toString() {
        final MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
        helper.add("conditions", conditions);
        helper.add("maxResults", maxResults);
        helper.add("offset", offset);
        helper.add("fields", fields);
        helper.add("orderBy", orderBy);
        helper.add("order", order);
        return helper.toString();
    }
}
