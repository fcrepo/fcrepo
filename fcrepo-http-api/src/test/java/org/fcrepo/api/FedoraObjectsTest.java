
package org.fcrepo.api;

import static org.fcrepo.api.TestHelpers.getUriInfoImpl;
import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.io.IOUtils;
import org.fcrepo.FedoraObject;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.identifiers.UUIDPidMinter;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.session.SessionFactory;
import org.fcrepo.utils.FedoraJcrTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

public class FedoraObjectsTest {

    FedoraObjects testObj;

    ObjectService mockObjects;
    
    DatastreamService mockDatastreams;

    Repository mockRepo;

    Session mockSession;

    SecurityContext mockSecurityContext;

    HttpServletRequest mockServletRequest;

    Principal mockPrincipal;

    String mockUser = "testuser";

    @Before
    public void setUp() throws LoginException, RepositoryException {
        mockSecurityContext = mock(SecurityContext.class);
        mockServletRequest = mock(HttpServletRequest.class);
        mockPrincipal = mock(Principal.class);
        mockObjects = mock(ObjectService.class);
        mockDatastreams = mock(DatastreamService.class);
        testObj = new FedoraObjects();
        testObj.setObjectService(mockObjects);
        testObj.setDatastreamService(mockDatastreams);
        mockRepo = mock(Repository.class);
        mockSession = mock(Session.class);
        when(mockSession.getUserID()).thenReturn(mockUser);
        when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn(mockUser);
        final SessionFactory mockSessions = mock(SessionFactory.class);
        when(mockSessions.getSession()).thenReturn(mockSession);
        when(
                mockSessions.getSession(any(SecurityContext.class),
                        any(HttpServletRequest.class))).thenReturn(mockSession);
        testObj.setSessionFactory(mockSessions);
        testObj.setUriInfo(getUriInfoImpl());
        testObj.setPidMinter(new UUIDPidMinter());
        testObj.setHttpServletRequest(mockServletRequest);
        testObj.setSecurityContext(mockSecurityContext);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testIngestAndMint() throws RepositoryException {
        //final Response actual = testObj.ingestAndMint(createPathList("objects"));
        //assertNotNull(actual);
        //assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
        //verify(mockSession).save();
    }

    @Test
    public void testModify() throws RepositoryException {
        final String pid = "testObject";
        final Response actual = testObj.modifyObject(createPathList(pid));
        assertNotNull(actual);
        assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
        // this verify will fail when modify is actually implemented, thus encouraging the unit test to be updated appropriately.
        verifyNoMoreInteractions(mockObjects);
        verify(mockSession).save();
    }

    @Test
    public void testCreateObject() throws RepositoryException, IOException, InvalidChecksumException {
        final String pid = "testObject";
        final String path = "/" + pid;
        final Response actual = testObj.createObject(
															createPathList(pid), null,
															FedoraJcrTypes.FEDORA_OBJECT, null, null, null, null
		);
        assertNotNull(actual);
        assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
        assertTrue(actual.getEntity().toString().endsWith(pid));
        verify(mockObjects).createObject(mockSession, path);
        verify(mockSession).save();
    }
    
    @Test
    public void testCreateDatastream() throws RepositoryException, IOException,
            InvalidChecksumException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId = "testDS";
        final String dsContent = "asdf";
        final String dsPath = "/" + pid + "/" + dsId;
        final InputStream dsContentStream = IOUtils.toInputStream(dsContent);
        Node mockNode = mock(Node.class);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockDatastreams.createDatastreamNode(
                any(Session.class), eq(dsPath), anyString(),
                eq(dsContentStream), anyString(), anyString())).thenReturn(mockNode);
        final Response actual =
                testObj.createObject(
                        createPathList(pid,dsId), "test label",
                        FedoraJcrTypes.FEDORA_DATASTREAM, null,
                        null, null, dsContentStream);
        assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
        verify(mockDatastreams).createDatastreamNode(any(Session.class),
															eq(dsPath), anyString(), any(InputStream.class), anyString(),
															anyString());
        verify(mockSession).save();
    }


    @Test
    public void testGetObjects() throws RepositoryException, IOException {
        final String pid = "testObject";
        final String childPid = "testChild";
        final String path = "/" + pid;
        final FedoraObject mockObj = mock(FedoraObject.class);
        when(mockObj.getName()).thenReturn(pid);
        Set<String> mockNames = new HashSet<String>(Arrays.asList(new String[]{childPid}));
        when(mockObjects.getObjectNames(path)).thenReturn(mockNames);
        when(mockObjects.getObjectNames(eq(path), any(String.class))).thenReturn(mockNames);
        Response actual = testObj.getObjects(createPathList(pid), null);
        assertNotNull(actual);
        String content = (String) actual.getEntity();
        assertTrue(content, content.contains(childPid));
        verify(mockSession, never()).save();
    }

    @Test
    public void testDeleteObject() throws RepositoryException {
        final String pid = "testObject";
        final String path = "/" + pid;
        final Response actual = testObj.deleteObject(createPathList(pid));
        assertNotNull(actual);
        assertEquals(Status.NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(mockObjects).deleteObject(mockSession, path);
        verify(mockSession).save();
    }
    
}
