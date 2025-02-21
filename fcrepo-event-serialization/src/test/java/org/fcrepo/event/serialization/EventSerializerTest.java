/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.event.serialization;

import org.junit.jupiter.api.Test;

/**
 * <p>
 * EventSerializerTest class.
 * </p>
 *
 * @author dbernstein
 */
public class EventSerializerTest extends EventSerializerTestBase {

    @Test
    public void testAsModel() {
        mockEvent(path);
        testModel(EventSerializer.toModel(mockEvent));
    }

}
