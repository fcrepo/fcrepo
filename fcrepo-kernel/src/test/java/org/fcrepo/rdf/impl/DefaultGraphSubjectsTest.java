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

package org.fcrepo.rdf.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.hp.hpl.jena.rdf.model.Resource;

public class DefaultGraphSubjectsTest {

    private DefaultGraphSubjects testObj;

    @Mock
    Node mockNode;

    @Mock
    Resource mockSubject;

    @Mock
    Session mockSession;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new DefaultGraphSubjects(mockSession);
    }

    @Test
    public void testGetGraphSubject() throws RepositoryException {
        final String testPath = "/foo/bar";
        final String expected = "info:fedora" + testPath;
        when(mockNode.getPath()).thenReturn(testPath);
        Resource actual = testObj.getGraphSubject(mockNode);
        assertEquals(expected, actual.getURI());
        when(mockNode.getPath()).thenReturn(testPath + "/jcr:content");
        actual = testObj.getGraphSubject(mockNode);
        assertEquals(expected + "/fcr:content", actual.getURI());
    }

    @Test
    public void testGetNodeFromGraphSubject() throws PathNotFoundException,
            RepositoryException {
        final String expected = "/foo/bar";
        when(mockSession.nodeExists(expected)).thenReturn(true);
        when(mockSession.getNode(expected)).thenReturn(mockNode);
        // test a good subject
        when(mockSubject.getURI()).thenReturn("info:fedora" + expected);
        when(mockSubject.isURIResource()).thenReturn(true);
        Node actual = testObj.getNodeFromGraphSubject(mockSession, mockSubject);
        assertEquals(mockNode, actual);
        // test a bad subject
        when(mockSubject.getURI()).thenReturn(
                "info:fedora2" + expected + "/bad");
        actual = testObj.getNodeFromGraphSubject(mockSession, mockSubject);
        assertEquals(null, actual);
        // test a non-existent path
        when(mockSubject.getURI())
                .thenReturn("info:fedora" + expected + "/bad");
        actual = testObj.getNodeFromGraphSubject(mockSession, mockSubject);
        assertEquals(null, actual);
        // test a fcr:content path
        when(mockSubject.getURI()).thenReturn(
                "info:fedora" + expected + "/fcr:content");
        actual = testObj.getNodeFromGraphSubject(mockSession, mockSubject);
        verify(mockSession).getNode(expected + "/jcr:content");
    }

    @Test
    public void testIsFedoraGraphSubject() {
        when(mockSubject.getURI()).thenReturn("info:fedora/foo");
        when(mockSubject.isURIResource()).thenReturn(true);
        boolean actual = testObj.isFedoraGraphSubject(mockSubject);
        assertEquals(true, actual);
        when(mockSubject.getURI()).thenReturn("http://fedora/foo");
        actual = testObj.isFedoraGraphSubject(mockSubject);
        assertEquals(false, actual);
    }

}
