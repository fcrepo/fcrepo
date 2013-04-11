
package org.fcrepo.integration.services;

import static org.fcrepo.services.PathService.OBJECT_PATH;
import static org.fcrepo.services.PathService.getDatastreamJcrNodePath;
import static org.fcrepo.services.PathService.getObjectJcrNodePath;
import static org.junit.Assert.assertEquals;

import org.fcrepo.services.PathService;
import org.junit.Test;

public class PathServiceTest {

    @Test
    public void testGetObjectJcrNodePath() throws Exception {
        new PathService();
        assertEquals(OBJECT_PATH + "/test:123",
                getObjectJcrNodePath("test:123"));
    }

    @Test
    public void testGetDatastreamJcrNodePath() throws Exception {
        new PathService();
        assertEquals(OBJECT_PATH + "/test:123/asdf", getDatastreamJcrNodePath(
                "test:123", "asdf"));
    }
}
