/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.search.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author bbpennel
 */
public class PaginationInfoTest {
    @Test
    public void testDefaultConstructor() {
        final PaginationInfo info = new PaginationInfo();

        assertEquals(-1, info.getMaxResults());
        assertEquals(-1, info.getOffset());
        assertEquals(0, info.getTotalResults());
    }

    @Test
    public void testParameterizedConstructor() {
        final int maxResults = 25;
        final int offset = 50;
        final int totalResults = 100;

        final PaginationInfo info = new PaginationInfo(maxResults, offset, totalResults);

        assertEquals(maxResults, info.getMaxResults());
        assertEquals(offset, info.getOffset());
        assertEquals(totalResults, info.getTotalResults());
    }
}
