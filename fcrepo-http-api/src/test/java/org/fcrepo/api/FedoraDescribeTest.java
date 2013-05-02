
package org.fcrepo.api;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashSet;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.fcrepo.Datastream;
import org.fcrepo.identifiers.UUIDPidMinter;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.test.util.TestHelpers;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

public class FedoraDescribeTest {

    FedoraDescribe testObj;

    ObjectService mockObjects;
    
    DatastreamService mockDatastreams;

	LowLevelStorageService mockLow;

    Repository mockRepo;

    Session mockSession;


    @Before
    public void setUp() throws LoginException, RepositoryException {
        mockObjects = mock(ObjectService.class);
        mockDatastreams = mock(DatastreamService.class);
		mockLow = mock(LowLevelStorageService.class);
        testObj = new FedoraDescribe();
        testObj.setObjectService(mockObjects);
        testObj.setDatastreamService(mockDatastreams);
		testObj.setLlStoreService(mockLow);
        mockRepo = mock(Repository.class);
        testObj.setUriInfo(TestHelpers.getUriInfoImpl());
        testObj.setPidMinter(new UUIDPidMinter());

		mockSession = TestHelpers.mockSession(testObj);
    }

    @After
    public void tearDown() {

    }
    
    @Test
    public void testDescribeDatastream() throws RepositoryException, IOException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId = "testDS";
		final String path = "/" + pid + "/" + dsId;
        final Datastream mockDs = TestHelpers.mockDatastream(pid, dsId, null);
        when(mockDatastreams.getDatastream(mockSession, path)).thenReturn(mockDs);
        Node mockNode = mock(Node.class);
		when(mockNode.getSession()).thenReturn(mockSession);
		when(mockDs.getNode()).thenReturn(mockNode);
        when(mockNode.getName()).thenReturn(dsId);
        Node mockParent = mock(Node.class);
        when(mockParent.getPath()).thenReturn(path);
        when(mockNode.getParent()).thenReturn(mockParent);
		when(mockNode.getPath()).thenReturn(path);
        when(mockNode.isNodeType("nt:file")).thenReturn(true);
        when(mockSession.getNode(path)).thenReturn(mockNode);
		when(mockLow.getLowLevelCacheEntries(mockNode)).thenReturn(new HashSet<LowLevelCacheEntry>());
        final Response actual = testObj.describe(createPathList(pid, dsId));
        assertNotNull(actual);
        verify(mockDatastreams).getDatastream(mockSession, path);
        verify(mockSession, never()).save();
    }

}
