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
package org.fcrepo.http.commons.api.rdf;


import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.hp.hpl.jena.rdf.model.ResourceFactory;

import org.fcrepo.kernel.identifiers.InternalIdentifierConverter;
import org.fcrepo.kernel.identifiers.NamespaceConverter;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Resource;
import com.sun.jersey.api.uri.UriBuilderImpl;

/**
 * <p>HttpIdentifierTranslatorTest class.</p>
 *
 * @author ajs6f
 */
public class HttpIdentifierTranslatorTest extends GraphSubjectsTest {

    @Override
    protected HttpIdentifierTranslator getTestObj() {
        final HttpIdentifierTranslator testObj =
            new HttpIdentifierTranslator(mockSession, MockNodeController.class, uriInfo);
        testObj.setTranslationChain(singletonList((InternalIdentifierConverter) new NamespaceConverter()));
        return testObj;
    }

    protected HttpIdentifierTranslator getTestObjTx(final String path) {
        final HttpIdentifierTranslator testObj =
            new HttpIdentifierTranslator(mockSessionTx, MockNodeController.class, getUriInfoImpl(path));
        testObj.setTranslationChain(singletonList((InternalIdentifierConverter) new NamespaceConverter()));
        return testObj;
    }


    @Test
    public void testGetGraphSubject() throws RepositoryException {
        final String expected = "http://localhost:8080/fcrepo/rest" + testPath;
        when(mockNode.getPath()).thenReturn(testPath);
        when(mockWorkspace.getName()).thenReturn("default");
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockNode.getSession()).thenReturn(mockSession);
        Resource actual = testObj.getSubject(mockNode.getPath());
        assertEquals(expected, actual.getURI());
        when(mockNode.getPath()).thenReturn(testPath + "/jcr:content");
        actual = testObj.getSubject(mockNode.getPath());
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
        Node actual = mockSession.getNode(testObj.getPathFromSubject(mockSubject));
        verify(mockSession).getNode(testPath);
        assertEquals(mockNode, actual);
        // test a bad subject
        when(mockSubject.getURI()).thenReturn(
                "http://localhost:8080/fcrepo/rest2" + testPath + "/bad");
        actual = mockSession.getNode(testObj.getPathFromSubject(mockSubject));
        assertEquals(null, actual);
        // test a non-existent path
        when(mockSubject.getURI()).thenReturn(
                "http://localhost:8080/fcrepo/rest" + testPath + "/bad");
        actual = mockSession.getNode(testObj.getPathFromSubject(mockSubject));
        assertEquals(null, actual);
        // test a fcr:content path
        when(mockSession.nodeExists(testPath + "/jcr:content")).thenReturn(true);
        when(mockSubject.getURI())
                .thenReturn(
                        "http://localhost:8080/fcrepo/rest" + testPath +
                                "/fcr:content");
        actual = mockSession.getNode(testObj.getPathFromSubject(mockSubject));
        verify(mockSession).getNode(testPath + "/jcr:content");
    }

    @Test
    public void testGetNodeFromGraphSubjectForNonJcrUrl() throws RepositoryException {

        when(mockWorkspace.getName()).thenReturn("default");
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);

        assertNull(mockSession.getNode(testObj.getPathFromSubject(
                createResource("http://localhost:8080/fcrepo/rest/abc/fcr:export?format=jcr/xml"))));
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
    public void testIsFedoraGraphSubjectWithTx() throws RepositoryException {
        final String txId = UUID.randomUUID().toString();
        final String testPathTx = "/" + txId + "/hello";

        when(mockSessionTx.getValueFactory()).thenReturn(mockValueFactory);
        final IdentifierTranslator testObjTx = getTestObjTx(testPathTx);
        when(mockSessionTx.getTxId()).thenReturn(txId);
        when(mockSubject.getURI()).thenReturn(
                "http://localhost:8080/fcrepo/rest/tx:" + txId + "/hello");
        when(mockSubject.isURIResource()).thenReturn(true);
        final boolean actual = testObjTx.isFedoraGraphSubject(mockSubject);
        verify(mockValueFactory).createValue("/hello", PropertyType.PATH);
        assertTrue("Must be valid GraphSubject", actual);
    }

    @Test
    public void testGetContext() {
        assertEquals(uriInfo.getRequestUri().toString(), testObj.getContext().getURI());
    }

    @Test
    public void testGetPathFromGraphSubject() throws RepositoryException {
        assertEquals("/abc", testObj.getPathFromSubject(
                ResourceFactory.createResource("http://localhost:8080/fcrepo/rest/abc")));
    }

    @Test
    public void testGetPathFromGraphSubjectForNonJcrUrl() throws RepositoryException {
        assertNull(testObj.getPathFromSubject(ResourceFactory.createResource("who-knows-what-this-is")));
    }

    @Test
    public void testIsCanonical() {
        assertTrue(((HttpIdentifierTranslator) testObj).isCanonical());
    }

    @Test
    public void testIsNotCanonicalWithinATx() {
        final String txId = UUID.randomUUID().toString();

        final HttpIdentifierTranslator testObjTx = getTestObjTx("/");
        when(mockSessionTx.getTxId()).thenReturn(txId);
        assertFalse(testObjTx.isCanonical());
    }

    @Test
    public void testGetCanonical() {
        final HttpIdentifierTranslator testObjTx = getTestObjTx("/");
        when(mockSessionTx.getTxId()).thenReturn("");
        assertFalse(testObjTx.isCanonical());

        assertTrue(testObjTx.getCanonical(true).isCanonical());
        assertFalse(testObjTx.getCanonical(true).getCanonical(false).isCanonical());
    }

    @Test
    public void testGetCanonicalWithinATx() throws RepositoryException {
        final HttpIdentifierTranslator testObjTx = getTestObjTx("/");
        when(mockSessionTx.getTxId()).thenReturn("txid");
        assertEquals("http://localhost:8080/fcrepo/rest/tx:txid/abc", testObjTx.getSubject("/abc").toString());
        assertEquals("http://localhost:8080/fcrepo/rest/abc",
                     testObjTx.getCanonical(true).getSubject("/abc").toString());

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

    @Test
    public void testGetHierarchyLevels() {
        assertTrue(testObj.getHierarchyLevels() >= 0);
    }

    @Test
    public void testSubjectPath() {
        final String path = "/abc";
        assertEquals(path, testObj.getSubjectPath(
                ResourceFactory.createResource("http://localhost:8080/fcrepo/rest" + path)));
    }

    @Path("/rest/{path}")
    protected class MockNodeController {

    }
}
