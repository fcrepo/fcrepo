/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.api;

import static com.hp.hpl.jena.graph.NodeFactory.createAnon;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static org.fcrepo.http.commons.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionManager;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.impl.FedoraResourceImpl;
import org.fcrepo.kernel.identifiers.PidMinter;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.services.VersionService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.util.Context;


/**
 * <p>FedoraNodesTest class.</p>
 *
 * @author awoods
 */
public class FedoraNodesTest {

    FedoraNodes testObj;

    @Mock
    private ObjectService mockObjects;

    @Mock
    private NodeService mockNodes;

    @Mock
    private VersionService mockVersions;

    @Mock
    private Node mockNode;

    @Mock
    private NodeType mockNodeType;

    @Mock
    private DatastreamService mockDatastreams;

    @Mock
    private Request mockRequest;

    @Mock
    private FedoraResourceImpl mockResource;

    Session mockSession;

    @Mock
    private FedoraObject mockObject;

    @Mock
    private Dataset mockDataset;

    private RdfStream mockRdfStream = new RdfStream().topic(createAnon());

    private RdfStream mockRdfStream2 = new RdfStream().topic(createAnon());
    private RdfStream mockRdfStream3 = new RdfStream().topic(createAnon());

    @Mock
    private Model mockModel;

    @Mock
    private Context mockContext;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private PidMinter mockPidMinter;

    @Mock
    private Date mockDate;

    private UriInfo mockUriInfo;

    @Mock
    private Value mockValue;

    @Mock
    private ValueFactory mockValueFactory;

    private String path = "/some/path";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new FedoraNodes(path);

        setField(testObj, "request", mockRequest);
        setField(testObj, "servletResponse", mockResponse);
        setField(testObj, "uriInfo", mockUriInfo);
        setField(testObj, "datastreamService", mockDatastreams);
        setField(testObj, "nodeService", mockNodes);
        setField(testObj, "versionService", mockVersions);
        this.mockUriInfo = getUriInfoImpl();
        setField(testObj, "pidMinter", mockPidMinter);
        setField(testObj, "objectService", mockObjects);
        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);
        final Workspace mockWorkspace = mock(Workspace.class);
        when(mockWorkspace.getName()).thenReturn("default");
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        final VersionManager mockVM = mock(VersionManager.class);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVM);
        when(mockDate.getTime()).thenReturn(0L);
        when(mockNode.getPath()).thenReturn("/test/path");
        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockObject.getPath()).thenReturn("/test/path");
        when(mockObject.getEtagValue()).thenReturn("XYZ");
        when(mockNodeType.getName()).thenReturn("nt:folder");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);

    }

    @Test
    public void testCopyObject() throws RepositoryException, URISyntaxException {

        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, path)).thenReturn(true);
        when(mockObject.getPath()).thenReturn(path);

        testObj.copyObject("http://localhost/fcrepo/bar");
        verify(mockNodes).copyObject(mockSession, path, "/bar");
    }

    @Test(expected = ClientErrorException.class)
    public void testCopyMissingObject() throws RepositoryException, URISyntaxException {

        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, path)).thenReturn(false);

        testObj.copyObject("http://localhost/fcrepo/bar");
    }

    @Test(expected = ServerErrorException.class)
    public void testCopyObjectWithBadDestination() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, path)).thenReturn(true);

        testObj.copyObject("http://somewhere/else/baz");

    }

    @Test(expected = WebApplicationException.class)
    public void testCopyObjectToExistingDestination() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, path)).thenReturn(true);
        doThrow(new RepositoryRuntimeException(new ItemExistsException()))
                .when(mockNodes).copyObject(mockSession, path, "/baz");

        final Response response = testObj.copyObject("http://localhost/fcrepo/baz");

        assertEquals(PRECONDITION_FAILED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testMoveObject() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockNodes.getObject(isA(Session.class), isA(String.class)))
            .thenReturn(mockObject);
        when(mockObject.getEtagValue()).thenReturn("");
        when(mockObject.getPath()).thenReturn(path);

        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, path)).thenReturn(true);

        testObj.moveObject("http://localhost/fcrepo/bar");
        verify(mockNodes).moveObject(mockSession, path, "/bar");
    }

    @Test(expected = ClientErrorException.class)
    public void testMoveMissingObject() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, path)).thenReturn(false);
        when(mockNodes.getObject(isA(Session.class), isA(String.class)))
            .thenReturn(mockObject);
        when(mockObject.getEtagValue()).thenReturn("");

        testObj.moveObject("http://localhost/fcrepo/bar");
    }

    @Test(expected = WebApplicationException.class)
    public void testMoveObjectToExistingDestination() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, path)).thenReturn(true);
        when(mockNodes.getObject(isA(Session.class), isA(String.class)))
            .thenReturn(mockObject);
        when(mockObject.getEtagValue()).thenReturn("");
        doThrow(new RepositoryRuntimeException(new ItemExistsException()))
                .when(mockNodes).moveObject(mockSession, path, "/baz");

        final Response response = testObj.moveObject("http://localhost/fcrepo/baz");

        assertEquals(PRECONDITION_FAILED.getStatusCode(), response.getStatus());
    }

    @Test(expected = ServerErrorException.class)
    public void testMoveObjectWithBadDestination() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.getObject(isA(Session.class), isA(String.class)))
            .thenReturn(mockObject);
        when(mockNodes.exists(mockSession, path)).thenReturn(true);
        when(mockObject.getEtagValue()).thenReturn("");

        testObj.moveObject("http://somewhere/else/baz");
    }

    @Test(expected = WebApplicationException.class)
    public void testCheckJcrNamespace() {
        final List<PathSegment> pathList = createPathList("anypath");
        pathList.add(createPathList("jcr:path").get(0));
        testObj.throwIfPathIncludesJcr(pathList);
    }

}
