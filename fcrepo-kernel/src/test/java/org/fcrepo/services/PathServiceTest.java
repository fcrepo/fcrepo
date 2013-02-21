package org.fcrepo.services;

import org.junit.Test;

import static org.fcrepo.services.PathService.getDatastreamJcrNodePath;
import static org.fcrepo.services.PathService.getObjectJcrNodePath;
import static org.junit.Assert.assertEquals;

public class PathServiceTest {
    @Test
    public void testGetObjectJcrNodePath() throws Exception {
        new PathService();
        assertEquals("/objects/test:123", getObjectJcrNodePath("test:123"));
    }

    @Test
    public void testGetDatastreamJcrNodePath() throws Exception {
        new PathService();
        assertEquals("/objects/test:123/asdf", getDatastreamJcrNodePath("test:123", "asdf"));
    }
}
