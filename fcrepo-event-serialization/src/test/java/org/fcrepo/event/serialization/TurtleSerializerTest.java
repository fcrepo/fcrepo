/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.event.serialization;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * <p>
 * TurtleSerializerTest class.
 * </p>
 *
 * @author acoburn
 * @author dbernstein
 */
public class TurtleSerializerTest extends EventSerializerTestBase {
    @Test
    public void testTurtle() {
        mockEvent(path);
        final EventSerializer serializer = new TurtleSerializer();
        final String ttl = serializer.serialize(mockEvent);
        assertTrue(ttl.contains("<http://localhost:8080/fcrepo/rest/path/to/resource>"));
    }

}
