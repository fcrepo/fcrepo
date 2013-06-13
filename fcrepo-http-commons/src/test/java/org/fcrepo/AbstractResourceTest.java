package org.fcrepo;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.identifiers.PidMinter;
import org.fcrepo.services.NodeService;
import org.fcrepo.session.SessionFactory;
import org.fcrepo.test.util.PathSegmentImpl;
import org.fcrepo.utils.NamespaceTools;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({NamespaceTools.class})
public class AbstractResourceTest {

    AbstractResource testObj;
    
    @Before
    public void setUp() {
        testObj = new AbstractResource() {
            
        };
    }
    
    @Test
    public void testInitialize() throws RepositoryException {
        SessionFactory mockSessions = mock(SessionFactory.class);
        Session mockSession = mock(Session.class);
        NamespaceRegistry mockNames = mock(NamespaceRegistry.class);
        mockStatic(NamespaceTools.class);
        when(NamespaceTools.getNamespaceRegistry(any(Session.class))).thenReturn(mockNames);
        when(mockSessions.getSession()).thenReturn(mockSession);
        testObj.setSessionFactory(mockSessions);
        testObj.initialize();
    }
    
    @Test
    public void testSetPidMinter() {
        PidMinter mockPids = mock(PidMinter.class);
        testObj.setPidMinter(mockPids);
        assertEquals(mockPids, testObj.pidMinter);
    }
    
    @Test
    public void testSessionMachinerySetters() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        SessionFactory mockSession = mock(SessionFactory.class);
        SecurityContext mockContext = mock(SecurityContext.class);
        testObj.setHttpServletRequest(mockRequest);
        testObj.setSessionFactory(mockSession);
        testObj.setSecurityContext(mockContext);
        testObj.getAuthenticatedSession();
        verify(mockSession).getSession(mockContext, mockRequest);
    }

    @Test
    public void testSetNodeService() {
        NodeService mockNodes = mock(NodeService.class);
        testObj.setNodeService(mockNodes);
        assertEquals(mockNodes, testObj.nodeService);
    }
    
    @Test
    public void testSetUriInfo() {
        UriInfo mockUris = mock(UriInfo.class);
        testObj.setUriInfo(mockUris);
        assertEquals(mockUris, testObj.uriInfo);
    }
    
    @Test
    public void testToPath() {
        List<PathSegment> pathList = PathSegmentImpl.createPathList("foo", "", "bar", "baz");
        // empty path segments ('//') should be suppressed
        String expected = "/foo/bar/baz";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathWorkspace() {
        List<PathSegment> pathList = PathSegmentImpl.createPathList("workspace:abc", "bar", "baz");
        // workspaces should be ignored
        String expected = "/bar/baz";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }


    @Test
    public void testToPathWorkspaceInSomeOtherSegment() {
        List<PathSegment> pathList = PathSegmentImpl.createPathList("asdf", "workspace:abc", "bar", "baz");
        // workspaces should be ignored
        String expected = "/asdf/workspace:abc/bar/baz";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathWorkspaceWithEmptyPrefix() {
        List<PathSegment> pathList = PathSegmentImpl.createPathList("", "workspace:abc", "bar", "baz");
        // workspaces should be ignored
        String expected = "/bar/baz";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathTransaction() {
        List<PathSegment> pathList = PathSegmentImpl.createPathList("tx:abc", "bar", "baz");
        // workspaces should be ignored
        String expected = "/bar/baz";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathTxInSomeOtherSegment() {
        List<PathSegment> pathList = PathSegmentImpl.createPathList("asdf", "tx:abc", "bar", "baz");
        // workspaces should be ignored
        String expected = "/asdf/tx:abc/bar/baz";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathTxWithEmptyPrefix() {
        List<PathSegment> pathList = PathSegmentImpl.createPathList("", "tx:abc", "bar", "baz");
        // workspaces should be ignored
        String expected = "/bar/baz";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathUuid() {
        List<PathSegment> pathList = PathSegmentImpl.createPathList("[foo]");
        String expected = "[foo]";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathEmpty() {
        List<PathSegment> pathList = PathSegmentImpl.createPathList();
        // empty path segments ('//') should be suppressed
        String expected = "/";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }
}
