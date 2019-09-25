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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.net.URISyntaxException;
import java.util.function.Supplier;

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
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.ItemExistsException;
import org.fcrepo.kernel.api.exception.RepositoryException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.services.VersionService;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


/**
 * <p>FedoraNodesTest class.</p>
 *
 * @author awoods
 */
@Ignore // TODO fix these tests
@RunWith(MockitoJUnitRunner.Silent.class)
public class FedoraNodesTest {

    FedoraNodes testObj;

    Transaction testSession;

    HttpSession testHttpSession;

    @Mock
    private ContainerService mockObjects;

    @Mock
    private NodeService mockNodes;

    @Mock
    private VersionService mockVersions;


    @Mock
    private Request mockRequest;

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
        testSession = null;
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
        when(mockContainer.getPath()).thenReturn(path);
        when(mockContainer.getEtagValue()).thenReturn("XYZ");

        setField(testObj, "idTranslator", new HttpResourceConverter(testHttpSession,
                    UriBuilder.fromUri("http://localhost/fcrepo/{path: .*}")));
    }

    @Test
    public void testCopyObject() throws Exception, URISyntaxException {

        when(mockNodes.exists(testSession, path)).thenReturn(true);
        when(mockContainer.getPath()).thenReturn(path);

        testObj.copyObject("http://localhost/fcrepo/bar");
        verify(mockNodes).copyObject(testSession, path, "/bar");
    }

    @Test(expected = ClientErrorException.class)
    public void testCopyMissingObject() throws Exception, URISyntaxException {

        when(mockNodes.exists(testSession, path)).thenReturn(false);

        testObj.copyObject("http://localhost/fcrepo/bar");
    }

    @Test(expected = ServerErrorException.class)
    public void testCopyObjectWithBadDestination() throws RepositoryException, URISyntaxException {
        when(mockNodes.exists(testSession, path)).thenReturn(true);

        testObj.copyObject("http://somewhere/else/baz");

    }

    @Test(expected = WebApplicationException.class)
    public void testCopyObjectToExistingDestination() throws RepositoryException, URISyntaxException {
        when(mockNodes.exists(testSession, path)).thenReturn(true);
        doThrow(new RepositoryRuntimeException(new ItemExistsException("exists")))
                .when(mockNodes).copyObject(testSession, path, "/baz");

        final Response response = testObj.copyObject("http://localhost/fcrepo/baz");

        assertEquals(PRECONDITION_FAILED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testMoveObject() throws RepositoryException, URISyntaxException {
        when(mockNodes.find(isA(Transaction.class), isA(String.class)))
            .thenReturn(mockContainer);
        when(mockContainer.getEtagValue()).thenReturn("");
        when(mockContainer.getPath()).thenReturn(path);

        when(mockNodes.exists(testSession, path)).thenReturn(true);
        when(mockContainer.getStateToken()).thenReturn("");
        when(mockRequest.getMethod()).thenReturn("MOVE");

        testObj.moveObject("http://localhost/fcrepo/bar");
        verify(mockNodes).moveObject(testSession, path, "/bar");
    }

    @Test(expected = ClientErrorException.class)
    public void testMoveMissingObject() throws RepositoryException, URISyntaxException {
        when(mockNodes.exists(testSession, path)).thenReturn(false);
        when(mockNodes.find(isA(Transaction.class), isA(String.class)))
            .thenReturn(mockContainer);
        when(mockContainer.getEtagValue()).thenReturn("");

        testObj.moveObject("http://localhost/fcrepo/bar");
    }

    @Test(expected = WebApplicationException.class)
    public void testMoveObjectToExistingDestination() throws RepositoryException, URISyntaxException {
        when(mockNodes.exists(testSession, path)).thenReturn(true);
        when(mockNodes.find(isA(Transaction.class), isA(String.class)))
            .thenReturn(mockContainer);
        when(mockContainer.getEtagValue()).thenReturn("");
        doThrow(new RepositoryRuntimeException(new ItemExistsException("test")))
                .when(mockNodes).moveObject(testSession, path, "/baz");
        when(mockContainer.getStateToken()).thenReturn("");
        when(mockRequest.getMethod()).thenReturn("MOVE");

        final Response response = testObj.moveObject("http://localhost/fcrepo/baz");

        assertEquals(PRECONDITION_FAILED.getStatusCode(), response.getStatus());
    }

    @Test(expected = ServerErrorException.class)
    public void testMoveObjectWithBadDestination() throws RepositoryException, URISyntaxException {
        when(mockNodes.find(isA(Transaction.class), isA(String.class)))
            .thenReturn(mockContainer);
        when(mockNodes.exists(testSession, path)).thenReturn(true);
        when(mockContainer.getEtagValue()).thenReturn("");
        when(mockContainer.getStateToken()).thenReturn("");
        when(mockRequest.getMethod()).thenReturn("MOVE");
        testObj.moveObject("http://somewhere/else/baz");
    }

}
