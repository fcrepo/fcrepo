/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.http.commons.api.rdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.fcrepo.http.commons.api.rdf.HttpGraphSubjects;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.hp.hpl.jena.rdf.model.Resource;
import com.sun.jersey.api.uri.UriBuilderImpl;

public class HttpGraphSubjectsTest extends GraphSubjectsTest {

    protected HttpGraphSubjects getTestObj() {
        return new HttpGraphSubjects(mockSession, MockNodeController.class,
                uriInfo);
    }

    @Test
    public void testGetGraphSubject() throws RepositoryException {
        final String expected = "http://localhost:8080/fcrepo/rest" + testPath;
        when(mockNode.getPath()).thenReturn(testPath);
        when(mockWorkspace.getName()).thenReturn("default");
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockNode.getSession()).thenReturn(mockSession);
        Resource actual = testObj.getGraphSubject(mockNode);
        assertEquals(expected, actual.getURI());
        when(mockNode.getPath()).thenReturn(testPath + "/jcr:content");
        actual = testObj.getGraphSubject(mockNode);
        assertEquals(expected + "/fcr:content", actual.getURI());
    }

    @Test
    public void testGetNodeFromGraphSubject() throws PathNotFoundException,
            RepositoryException {

        when(mockSession.nodeExists(testPath)).thenReturn(true);
        when(mockSession.getNode(testPath)).thenReturn(mockNode);
        when(mockWorkspace.getName()).thenReturn("default");
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        // test a good subject
        when(mockSubject.getURI()).thenReturn(
                "http://localhost:8080/fcrepo/rest" + testPath);
        when(mockSubject.isURIResource()).thenReturn(true);
        Node actual = testObj.getNodeFromGraphSubject(mockSubject);
        Mockito.verify(mockSession).getNode(testPath);
        assertEquals(mockNode, actual);
        // test a bad subject
        when(mockSubject.getURI()).thenReturn(
                "http://localhost:8080/fcrepo/rest2" + testPath + "/bad");
        actual = testObj.getNodeFromGraphSubject(mockSubject);
        assertEquals(null, actual);
        // test a non-existent path
        when(mockSubject.getURI()).thenReturn(
                "http://localhost:8080/fcrepo/rest" + testPath + "/bad");
        actual = testObj.getNodeFromGraphSubject(mockSubject);
        assertEquals(null, actual);
        // test a fcr:content path
        when(mockSession.nodeExists(testPath + "/jcr:content")).thenReturn(true);
        when(mockSubject.getURI())
                .thenReturn(
                        "http://localhost:8080/fcrepo/rest" + testPath +
                                "/fcr:content");
        actual = testObj.getNodeFromGraphSubject(mockSubject);
        verify(mockSession).getNode(testPath + "/jcr:content");
    }

    @Test
    public void testGetNodeFromGraphSubjectForNonJcrUrl() throws RepositoryException {

        when(mockWorkspace.getName()).thenReturn("default");
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);

        assertNull(testObj.getNodeFromGraphSubject(ResourceFactory.createResource("http://localhost:8080/fcrepo/rest/abc/fcr:export?format=jcr/xml")));
    }

    @Test
    public void testIsFedoraGraphSubject() {
        when(mockSubject.getURI()).thenReturn(
                "http://localhost:8080/fcrepo/rest/foo");
        when(mockSubject.isURIResource()).thenReturn(true);
        boolean actual = testObj.isFedoraGraphSubject(mockSubject);
        assertEquals(true, actual);
        when(mockSubject.getURI()).thenReturn("http://fedora/foo");
        actual = testObj.isFedoraGraphSubject(mockSubject);
        assertEquals(false, actual);
    }

    @Test
    public void testGetContext() {
        assertEquals(uriInfo.getRequestUri().toString(), testObj.getContext().getURI());
    }

    @Test
    public void testGetPathFromGraphSubject() throws RepositoryException {
        assertEquals("/abc", testObj.getPathFromGraphSubject(ResourceFactory.createResource("http://localhost:8080/fcrepo/rest/abc")));
    }

    @Test
    public void testGetPathFromGraphSubjectForNonJcrUrl() throws RepositoryException {
        assertNull(testObj.getPathFromGraphSubject(ResourceFactory.createResource("who-knows-what-this-is")));
    }

    protected static UriInfo getUriInfoImpl(final String path) {
        // UriInfo ui = mock(UriInfo.class,withSettings().verboseLogging());
        final UriInfo ui = mock(UriInfo.class);
        final UriBuilder ub = new UriBuilderImpl();
        ub.scheme("http");
        ub.host("localhost");
        ub.port(8080);
        ub.path("/fcrepo");

        final UriBuilder rb = new UriBuilderImpl();
        rb.scheme("http");
        rb.host("localhost");
        rb.port(8080);
        rb.path("/fcrepo/rest" + path);

        when(ui.getRequestUri()).thenReturn(
                URI.create("http://localhost:8080/fcrepo/rest" + path));
        when(ui.getBaseUri()).thenReturn(
                URI.create("http://localhost:8080/fcrepo"));
        when(ui.getBaseUriBuilder()).thenReturn(ub);
        when(ui.getAbsolutePathBuilder()).thenReturn(rb);

        return ui;
    }

    @Path("/rest/{path}")
    protected class MockNodeController {

    }
}
