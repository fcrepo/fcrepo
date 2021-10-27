/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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

    private final boolean includeTotalResultCount;
    /**
     * Constructoor
     *
     * @param fields     The fields to be returned in the results
     * @param conditions The conditions
     * @param maxResults The max results
     * @param offset     The offset
     * @param orderBy    The field by which to order the results
     * @param order      The order: ie "asc" or "desc"
     * @param includeTotalResultCount A flag indicating whether or not to return the total result count.
     */
    public SearchParameters(final List<Condition.Field> fields, final List<Condition> conditions, final int maxResults,
                            final int offset, final Condition.Field orderBy, final String order,
                            final boolean includeTotalResultCount) {
        this.fields = fields;
        this.conditions = conditions;
        this.maxResults = maxResults;
        this.offset = offset;
        this.orderBy = orderBy;
        this.order = order;
        this.includeTotalResultCount = includeTotalResultCount;
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

    /**
     * Returns flag indicating whether or not to include the total result count in the query results.
     * @return
     */
    public boolean isIncludeTotalResultCount() {
        return includeTotalResultCount;
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
        helper.add("includeTotalResultCount", includeTotalResultCount);
        return helper.toString();
    }
}
