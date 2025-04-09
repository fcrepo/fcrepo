/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.search.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author bbpennel
 */
public class SearchParametersTest {
    private List<Condition> conditions;
    private List<Condition.Field> fields;
    private int maxResults;
    private int offset;
    private Condition.Field orderBy;
    private String order;
    private boolean includeTotalResultCount;

    private Condition condition;

    private Condition.Field field;

    @BeforeEach
    public void setUp() {
        field = Condition.Field.FEDORA_ID;
        condition = Condition.fromEnums(field, Condition.Operator.EQ, "test");
        conditions = new ArrayList<>();
        conditions.add(condition);

        fields = new ArrayList<>();
        fields.add(field);

        maxResults = 100;
        offset = 10;
        orderBy = field;
        order = "asc";
        includeTotalResultCount = true;
    }

    @Test
    public void testConstructorAndGetters() {
        final SearchParameters params = new SearchParameters(
                fields, conditions, maxResults, offset, orderBy, order, includeTotalResultCount);

        assertEquals(fields, params.getFields());
        assertEquals(conditions, params.getConditions());
        assertEquals(maxResults, params.getMaxResults());
        assertEquals(offset, params.getOffset());
        assertEquals(orderBy, params.getOrderBy());
        assertEquals(order, params.getOrder());
        assertEquals(includeTotalResultCount, params.isIncludeTotalResultCount());
    }

    @Test
    public void testToString() {
        final SearchParameters params = new SearchParameters(
                fields, conditions, maxResults, offset, orderBy, order, includeTotalResultCount);

        final String result = params.toString();

        assertTrue(result.contains("conditions=" + conditions), "toString should include conditions");
        assertTrue(result.contains("maxResults=" + maxResults), "toString should include maxResults");
        assertTrue(result.contains("offset=" + offset), "toString should include offset");
        assertTrue(result.contains("fields=" + fields), "toString should include fields");
        assertTrue(result.contains("orderBy=" + orderBy), "toString should include orderBy");
        assertTrue(result.contains("order=" + order), "toString should include order");
        assertTrue(result.contains("includeTotalResultCount=" + includeTotalResultCount),
                "toString should include includeTotalResultCount");
    }
}
