
package org.fcrepo.api;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.services.PathService.getDatastreamJcrNodePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.io.IOUtils;
import org.fcrepo.Datastream;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.jaxb.responses.access.ObjectDatastreams;
import org.fcrepo.jaxb.responses.management.DatastreamFixity;
import org.fcrepo.jaxb.responses.management.DatastreamHistory;
import org.fcrepo.jaxb.responses.management.DatastreamProfile;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.session.SessionFactory;
import org.fcrepo.utils.DatastreamIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

import com.sun.jersey.multipart.MultiPart;

public class FedoraContentTest {

    FedoraContent testObj;

    DatastreamService mockDatastreams;

    LowLevelStorageService mockLow;

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
        //Function<HttpServletRequest, Session> mockFunction = mock(Function.class);
        mockDatastreams = mock(DatastreamService.class);
        mockLow = mock(LowLevelStorageService.class);

        testObj = new FedoraContent();
        testObj.setDatastreamService(mockDatastreams);
        testObj.setSecurityContext(mockSecurityContext);
        testObj.setHttpServletRequest(mockServletRequest);
        testObj.setLlStoreService(mockLow);
        //mockRepo = mock(Repository.class);
        final SessionFactory mockSessions = mock(SessionFactory.class);
        testObj.setSessionFactory(mockSessions);
        mockSession = mock(Session.class);
        when(
                mockSessions.getSession(any(SecurityContext.class),
                        any(HttpServletRequest.class))).thenReturn(mockSession);
        when(mockSession.getUserID()).thenReturn(mockUser);
        when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn(mockUser);

        testObj.setUriInfo(TestHelpers.getUriInfoImpl());
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testModifyContent() throws RepositoryException, IOException,
            InvalidChecksumException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId = "testDS";
        final String dsContent = "asdf";
        final String dsPath = getDatastreamJcrNodePath(pid, dsId);
        final InputStream dsContentStream = IOUtils.toInputStream(dsContent);
        when(mockDatastreams.exists(dsPath)).thenReturn(true);
        final Response actual =
                testObj.modifyContent(createPathList("objects",pid, dsId), null, dsContentStream);
        assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
        verify(mockDatastreams).createDatastreamNode(any(Session.class),
                eq(dsPath), anyString(), any(InputStream.class));
        verify(mockSession).save();
    }

    @Test
    public void testGetContent() throws RepositoryException,
            IOException {
        final String pid = "FedoraDatastreamsTest1";
        final String path = "/objects/" + pid;
        final String dsId = "testDS";
        final String dsContent = "asdf";
        final Datastream mockDs = org.fcrepo.TestHelpers.mockDatastream(pid, dsId, dsContent);
        when(mockDatastreams.getDatastream(path, dsId)).thenReturn(mockDs);
        final Request mockRequest = mock(Request.class);
        final Response actual =
                testObj.getContent(createPathList("objects",pid, dsId), mockRequest);
        verify(mockDs).getContent();
        verify(mockSession, never()).save();
        final String actualContent =
                IOUtils.toString((InputStream) actual.getEntity());
        assertEquals("asdf", actualContent);
    }

}