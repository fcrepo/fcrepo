/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.observer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * <p>EventTypeTest class.</p>
 *
 * @author ajs6f
 */
public class EventTypeTest {

    @Test()
    public void testValueOf() {
        assertEquals(EventType.RESOURCE_CREATION, EventType.valueOf("RESOURCE_CREATION"));
    }
}
