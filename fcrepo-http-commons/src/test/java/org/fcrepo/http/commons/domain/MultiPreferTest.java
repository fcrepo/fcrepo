/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.domain;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


/**
 * @author ajs6f
 * @since 30 Oct 2014
 *
 */
public class MultiPreferTest extends SinglePreferTest {

    @Override
    protected SinglePrefer createTestPreferTypeFromHeader(final String header) {
        return new MultiPrefer(header);
    }

    @Test
    public void testMultiConstructor() {
        final SinglePrefer first = new SinglePrefer("return=representation");
        final SinglePrefer second = new SinglePrefer("handling=strict");
        final MultiPrefer testPrefer = new MultiPrefer(newHashSet(first, second));
        // check to see that both headers were parsed
        assertTrue(testPrefer.hasReturn());
        assertEquals("representation", testPrefer.getReturn().getValue());
        assertTrue(testPrefer.hasHandling());
    }
}
