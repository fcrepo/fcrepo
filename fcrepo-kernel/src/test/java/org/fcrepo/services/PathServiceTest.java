package org.fcrepo.services;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PathServiceTest {
    @Test
    public void testGetObjectJcrNodePath() throws Exception {
        assertEquals("objects/test:123", new PathService().getObjectJcrNodePath("test:123"));
    }

    @Test
    public void testGetDatastreamJcrNodePath() throws Exception {
        assertEquals("objects/test:123/asdf", new PathService().getDatastreamJcrNodePath("test:123", "asdf"));
    }
}
