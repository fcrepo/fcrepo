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
package org.fcrepo.kernel.rdf.impl;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.rdf.impl.DefaultIdentifierTranslator.RESOURCE_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * <p>DefaultGraphSubjectsTest class.</p>
 *
 * @author awoods
 */
public class DefaultGraphSubjectsTest {

    private DefaultIdentifierTranslator testObj;

    @Mock
    Node mockNode;

    @Mock
    Resource mockSubject;

    @Mock
    Session mockSession;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new DefaultIdentifierTranslator();
    }

    @Test
    public void testGetGraphSubject() throws RepositoryException {
        final String testPath = "/foo/bar";
        final String expected = RESOURCE_NAMESPACE + testPath.substring(1);
        when(mockNode.getPath()).thenReturn(testPath);
        Resource actual = testObj.getSubject(mockNode.getPath());
        assertEquals(expected, actual.getURI());
        when(mockNode.getPath()).thenReturn(testPath + "/jcr:content");
        actual = testObj.getSubject(mockNode.getPath());
        assertEquals(expected + "/fcr:content", actual.getURI());
    }

    @Test
    public void testGetNodeFromGraphSubject() throws RepositoryException {
        final String expected = "/foo/bar";
        when(mockSession.nodeExists(expected)).thenReturn(true);
        when(mockSession.nodeExists(expected + "/bad")).thenReturn(false);
        when(mockSession.getNode(expected)).thenReturn(mockNode);
        // test a good subject
        when(mockSubject.getURI()).thenReturn(RESOURCE_NAMESPACE + expected.substring(1));
        when(mockSubject.isURIResource()).thenReturn(true);
        Node actual = mockSession.getNode(testObj.getPathFromSubject(mockSubject));
        assertEquals(mockNode, actual);
        // test a bad subject
        when(mockSubject.getURI()).thenReturn(
                "info:fedora2" + expected + "/bad");
        actual = mockSession.getNode(testObj.getPathFromSubject(mockSubject));
        assertEquals("Somehow got a Node from a bad RDF subject!", null, actual);
        // test a non-existent path
        when(mockSubject.getURI()).thenReturn(RESOURCE_NAMESPACE + expected.substring(1) + "/bad");
        actual = mockSession.getNode(testObj.getPathFromSubject(mockSubject));
        assertEquals("Somehow got a Node from a non-existent RDF subject!",
                null, actual);
        // test a fcr:content path
        when(mockSubject.getURI()).thenReturn(RESOURCE_NAMESPACE + expected.substring(1) + "/fcr:content");
        when(mockSession.nodeExists(expected + "/jcr:content")).thenReturn(true);
        actual = mockSession.getNode(testObj.getPathFromSubject(mockSubject));
        verify(mockSession).getNode(expected + "/jcr:content");
    }

    @Test
    public void testGetPathFromGraphSubject() throws RepositoryException {
        final String expected = "/foo/bar";
        // test a good subject
        when(mockSubject.getURI())
            .thenReturn(RESOURCE_NAMESPACE + expected.substring(1));
        when(mockSubject.isURIResource()).thenReturn(true);
        String actual = testObj.getPathFromSubject(mockSubject);
        assertEquals(expected, actual);
        // test a bad subject
        when(mockSubject.getURI()).thenReturn(
                                                 "info:fedora2/" + expected.substring(1) + "/bad");
        actual = testObj.getPathFromSubject(mockSubject);
        assertNull(actual);
        // test a fcr:content path
        when(mockSubject.getURI()).thenReturn(RESOURCE_NAMESPACE + expected.substring(1) + "/fcr:content");
        actual = testObj.getPathFromSubject(mockSubject);
        assertEquals(expected + "/jcr:content", actual);
    }

    @Test
    public void testIsFedoraGraphSubject() {
        when(mockSubject.getURI()).thenReturn(RESOURCE_NAMESPACE + "foo");
        when(mockSubject.isURIResource()).thenReturn(true);
        boolean actual = testObj.isFedoraGraphSubject(mockSubject);
        assertEquals(true, actual);
        when(mockSubject.getURI()).thenReturn("http://fedora/foo");
        actual = testObj.isFedoraGraphSubject(mockSubject);
        assertEquals(false, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testIsFedoraGraphSubjectWithNull() {
        testObj.isFedoraGraphSubject(null);
    }

    @Test
    public void testIsFedoraGraphSubjectWithBlankNode() {
        final Boolean actual = testObj.isFedoraGraphSubject(createResource());
        assertFalse("Misrecognized an RDF blank node as a Fedora resource!",
                actual);
    }

    @Test
    public void testGetContext() {
        final Resource context = testObj.getContext();
        assertTrue("Context was returned other than as an RDF blank node!",
                context.isAnon());
    }

    @Test
    public void testGetHierarchyLevels() {
        assertTrue(testObj.getHierarchyLevels() >= 0);
    }
}
