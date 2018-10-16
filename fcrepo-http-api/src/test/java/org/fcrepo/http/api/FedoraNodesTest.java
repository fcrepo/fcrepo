/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.net.URISyntaxException;
import java.util.function.Supplier;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionManager;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.SecurityContext;

import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.http.commons.session.HttpSession;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.services.VersionService;
import org.fcrepo.kernel.modeshape.FedoraSessionImpl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


/**
 * <p>FedoraNodesTest class.</p>
 *
 * @author awoods
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class FedoraNodesTest {

    FedoraNodes testObj;

    FedoraSession testSession;

    HttpSession testHttpSession;

    @Mock
    private ContainerService mockObjects;

    @Mock
    private NodeService mockNodes;

    @Mock
    private VersionService mockVersions;

    @Mock
    private Node mockNode;

    @Mock
    private NodeType mockNodeType;

    @Mock
    private Request mockRequest;

    @Mock
    Session mockSession;

    @Mock
    private FedoraResource mockContainer;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private Supplier<String> mockPidMinter;

    private UriInfo mockUriInfo;

    @Mock
    private SecurityContext mockSecurityContext;

    private final String path = "/some/path";

    @Before
    public void setUp() throws Exception {
        testObj = new FedoraNodes(path);
        testSession = new FedoraSessionImpl(mockSession);
        testHttpSession = new HttpSession(testSession);

        setField(testObj, "request", mockRequest);
        setField(testObj, "servletResponse", mockResponse);
        setField(testObj, "uriInfo", mockUriInfo);
        setField(testObj, "nodeService", mockNodes);
        setField(testObj, "versionService", mockVersions);
        this.mockUriInfo = getUriInfoImpl();
        setField(testObj, "pidMinter", mockPidMinter);
        setField(testObj, "containerService", mockObjects);
        setField(testObj, "session", testHttpSession);
        setField(testObj, "securityContext", mockSecurityContext);
        final Workspace mockWorkspace = mock(Workspace.class);
        when(mockWorkspace.getName()).thenReturn("default");
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        final VersionManager mockVM = mock(VersionManager.class);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVM);
        when(mockNode.getPath()).thenReturn(path);
        when(mockContainer.getPath()).thenReturn(path);
        when(mockContainer.getEtagValue()).thenReturn("XYZ");
        when(mockNodeType.getName()).thenReturn("nt:folder");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockSession.getNode(path)).thenReturn(mockNode);

        setField(testObj, "idTranslator", new HttpResourceConverter(testHttpSession,
                    UriBuilder.fromUri("http://localhost/fcrepo/{path: .*}")));
    }

    @Test
    public void testCopyObject() throws RepositoryException, URISyntaxException {

        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(testSession, path)).thenReturn(true);
        when(mockContainer.getPath()).thenReturn(path);

        testObj.copyObject("http://localhost/fcrepo/bar");
        verify(mockNodes).copyObject(testSession, path, "/bar");
    }

    @Test(expected = ClientErrorException.class)
    public void testCopyMissingObject() throws RepositoryException, URISyntaxException {

        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(testSession, path)).thenReturn(false);

        testObj.copyObject("http://localhost/fcrepo/bar");
    }

    @Test(expected = ServerErrorException.class)
    public void testCopyObjectWithBadDestination() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(testSession, path)).thenReturn(true);

        testObj.copyObject("http://somewhere/else/baz");

    }

    @Test(expected = WebApplicationException.class)
    public void testCopyObjectToExistingDestination() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(testSession, path)).thenReturn(true);
        doThrow(new RepositoryRuntimeException(new ItemExistsException()))
                .when(mockNodes).copyObject(testSession, path, "/baz");

        final Response response = testObj.copyObject("http://localhost/fcrepo/baz");

        assertEquals(PRECONDITION_FAILED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testMoveObject() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockNodes.find(isA(FedoraSession.class), isA(String.class)))
            .thenReturn(mockContainer);
        when(mockContainer.getEtagValue()).thenReturn("");
        when(mockContainer.getPath()).thenReturn(path);

        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(testSession, path)).thenReturn(true);

        testObj.moveObject("http://localhost/fcrepo/bar");
        verify(mockNodes).moveObject(testSession, path, "/bar");
    }

    @Test(expected = ClientErrorException.class)
    public void testMoveMissingObject() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(testSession, path)).thenReturn(false);
        when(mockNodes.find(isA(FedoraSession.class), isA(String.class)))
            .thenReturn(mockContainer);
        when(mockContainer.getEtagValue()).thenReturn("");

        testObj.moveObject("http://localhost/fcrepo/bar");
    }

    @Test(expected = WebApplicationException.class)
    public void testMoveObjectToExistingDestination() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(testSession, path)).thenReturn(true);
        when(mockNodes.find(isA(FedoraSession.class), isA(String.class)))
            .thenReturn(mockContainer);
        when(mockContainer.getEtagValue()).thenReturn("");
        doThrow(new RepositoryRuntimeException(new ItemExistsException()))
                .when(mockNodes).moveObject(testSession, path, "/baz");

        final Response response = testObj.moveObject("http://localhost/fcrepo/baz");

        assertEquals(PRECONDITION_FAILED.getStatusCode(), response.getStatus());
    }

    @Test(expected = ServerErrorException.class)
    public void testMoveObjectWithBadDestination() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.find(isA(FedoraSession.class), isA(String.class)))
            .thenReturn(mockContainer);
        when(mockNodes.exists(testSession, path)).thenReturn(true);
        when(mockContainer.getEtagValue()).thenReturn("");

        testObj.moveObject("http://somewhere/else/baz");
    }

}
