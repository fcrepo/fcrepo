/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author cabeer
 * @author ajs6f
 */
public class SinglePreferTest  {

    protected SinglePrefer createTestPreferTypeFromHeader(final String header) {
        return new SinglePrefer(header);
    }

    @Test
    public void testHasReturn() {
        final SinglePrefer prefer = createTestPreferTypeFromHeader("return=representation");

        assertTrue(prefer.hasReturn());
    }

    @Test
    public void testGetReturn() {
        final SinglePrefer prefer = createTestPreferTypeFromHeader("return=representation");

        assertEquals("representation", prefer.getReturn().getValue());
    }

    @Test
    public void testGetReturnParameters() {
        final SinglePrefer prefer =
                createTestPreferTypeFromHeader("return=representation; "
                        + "include=\"http://www.w3.org/ns/ldp#PreferMinimalContainer\"");

        assertTrue(prefer.hasReturn());
        assertEquals("representation", prefer.getReturn().getValue());

        final String returnParams = prefer.getReturn().getParams().get("include");
        assertTrue(returnParams.contains("http://www.w3.org/ns/ldp#PreferMinimalContainer"));
    }

    @Test
    public void testHasHandling() {
        final SinglePrefer prefer = createTestPreferTypeFromHeader("handling=strict");

        assertTrue(prefer.hasHandling());
    }

    @Test
    public void testGetHandling() {
        final SinglePrefer prefer = createTestPreferTypeFromHeader("handling=lenient");

        assertEquals("lenient", prefer.getHandling().getValue());
    }

    @Test
    public void testGetHandlingParameters() {
        final SinglePrefer prefer =
                createTestPreferTypeFromHeader("handling=lenient; some=\"parameter\"");

        assertTrue(prefer.hasHandling());
        assertEquals("lenient", prefer.getHandling().getValue());

        final String returnParams = prefer.getHandling().getParams().get("some");
        assertTrue(returnParams.contains("parameter"));
    }
}
