package org.fcrepo.api.repository;

import org.fcrepo.api.FedoraUnnamedObjects;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.identifiers.UUIDPidMinter;
import org.fcrepo.services.NodeService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.test.util.TestHelpers;
import org.fcrepo.utils.FedoraJcrTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FedoraRepositoryUnnamedObjectsTest {

    FedoraRepositoryUnnamedObjects testObj;

    Session mockSession;

    ObjectService mockObjects;
    NodeService mockNodeService;

    @Before
    public void setUp() throws RepositoryException {
        mockObjects = mock(ObjectService.class);
        mockNodeService = mock(NodeService.class);
        testObj = new FedoraRepositoryUnnamedObjects();
        mockSession = TestHelpers.mockSession(testObj);
        testObj.setNodeService(mockNodeService);
        testObj.setObjectService(mockObjects);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testIngestAndMint() throws RepositoryException, IOException,
                                                   InvalidChecksumException {
        final UUIDPidMinter mockMint = mock(UUIDPidMinter.class);
        testObj.setPidMinter(mockMint);
        when(mockMint.mintPid()).thenReturn("uuid-123");

        final Response actual =
                testObj.ingestAndMint(FedoraJcrTypes.FEDORA_OBJECT, null, null, null, null);
        assertNotNull(actual);
        assertEquals(Response.Status.CREATED.getStatusCode(), actual.getStatus());
        assertTrue(actual.getEntity().toString().endsWith("uuid-123"));
        verify(mockObjects).createObject(mockSession, "/uuid-123");
        verify(mockNodeService).exists(mockSession, "/uuid-123");
        verify(mockSession).save();

    }
}
