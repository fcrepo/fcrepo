
package org.fcrepo.api;

import static org.fcrepo.api.TestHelpers.getUriInfoImpl;
import static org.fcrepo.services.PathService.getDatastreamJcrNodePath;
import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.eq;
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
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.fcrepo.Datastream;
import org.fcrepo.FedoraObject;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.identifiers.UUIDPidMinter;
import org.fcrepo.jaxb.responses.access.ObjectProfile;
import org.fcrepo.jaxb.responses.management.DatastreamProfile;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.session.SessionFactory;
import org.fcrepo.utils.FedoraJcrTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

public class FedoraDescribeTest {

    FedoraDescribe testObj;

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
        testObj = new FedoraDescribe();
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
    public void testDescribeDatastream() throws RepositoryException, IOException {
        final String pid = "FedoraDatastreamsTest1";
        final String path = "/objects/" + pid;
        final String dsId = "testDS";
        final Datastream mockDs = org.fcrepo.TestHelpers.mockDatastream(pid, dsId, null);
        when(mockDatastreams.getDatastream(path, dsId)).thenReturn(mockDs);
        Node mockNode = mock(Node.class);
        when(mockNode.getName()).thenReturn(dsId);
        Node mockParent = mock(Node.class);
        when(mockParent.getPath()).thenReturn(path);
        when(mockNode.getParent()).thenReturn(mockParent);
        when(mockNode.isNodeType("nt:file")).thenReturn(true);
        when(mockSession.getNode(path + "/" + dsId)).thenReturn(mockNode);
        final Response actual = testObj.describe(createPathList("objects",pid, dsId));
        assertNotNull(actual);
        verify(mockDatastreams).getDatastream(path, dsId);
        verify(mockSession, never()).save();
    }

}
