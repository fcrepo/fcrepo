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

package org.fcrepo.utils;

import static javax.jcr.PropertyType.STRING;
import static org.fcrepo.utils.JcrRdfTools.getNodeFromGraphSubject;
import static org.fcrepo.utils.JcrRdfTools.getPropertyNameFromPredicate;
import static org.fcrepo.utils.JcrRdfTools.isFedoraGraphSubject;
import static org.fcrepo.utils.NodePropertiesTools.getPropertyType;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.rdf.GraphSubjects;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({JcrRdfTools.class, NodePropertiesTools.class})
public class JcrPropertyStatementListenerTest {

    private JcrPropertyStatementListener testObj;

    @Mock
    private Node mockNode;

    @Mock
    private Session mockSession;

    @Mock
    private GraphSubjects mockSubjects;

    @Mock
    private Statement mockStatement;

    @Mock
    private Resource mockSubject;

    @Mock
    private Property mockPredicate;

    @Mock
    private Node mockSubjectNode;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockNode.getSession()).thenReturn(mockSession);
        testObj = new JcrPropertyStatementListener(mockSubjects, mockSession);
        when(mockStatement.getSubject()).thenReturn(mockSubject);
        when(mockStatement.getPredicate()).thenReturn(mockPredicate);
    }

    @Test
    public void testAddedIrrelevantStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(isFedoraGraphSubject(mockSubjects, mockSubject)).thenReturn(false);
        testObj.addedStatement(mockStatement);
        verifyStatic(never());
        getNodeFromGraphSubject(any(GraphSubjects.class), any(Session.class),
                any(Resource.class));
        // this was ignored, but not a problem
        assertEquals(0, testObj.getProblems().size());
    }

    @Test
    public void testAddedProhibitedStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(isFedoraGraphSubject(mockSubjects, mockSubject)).thenReturn(true);
        when(getNodeFromGraphSubject(mockSubjects, mockSession, mockSubject))
                .thenReturn(mockSubjectNode);
        final String mockPropertyName = "jcr:property";
        when(getPropertyNameFromPredicate(mockSubjectNode, mockPredicate))
                .thenReturn(mockPropertyName);

        testObj.addedStatement(mockStatement);
        assertEquals(1, testObj.getProblems().size());
    }

    @Test
    public void testAddedStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(isFedoraGraphSubject(mockSubjects, mockSubject)).thenReturn(true);
        when(getNodeFromGraphSubject(mockSubjects, mockSession, mockSubject))
                .thenReturn(mockSubjectNode);
        final String mockPropertyName = "mock:property";
        when(getPropertyNameFromPredicate(mockSubjectNode, mockPredicate))
                .thenReturn(mockPropertyName);

        mockStatic(NodePropertiesTools.class);
        when(getPropertyType(mockSubjectNode, mockPropertyName)).thenReturn(
                STRING);
        testObj.addedStatement(mockStatement);
        assertEquals(0, testObj.getProblems().size());
    }

    @Test(expected = RuntimeException.class)
    public void testAddedStatementRepositoryException()
            throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(isFedoraGraphSubject(mockSubjects, mockSubject)).thenReturn(true);
        when(getNodeFromGraphSubject(mockSubjects, mockSession, mockSubject))
                .thenReturn(mockSubjectNode);
        when(getPropertyNameFromPredicate(mockSubjectNode, mockPredicate))
                .thenThrow(new RepositoryException());

        testObj.addedStatement(mockStatement);
    }

    @Test
    public void testRemovedStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(isFedoraGraphSubject(mockSubjects, mockSubject)).thenReturn(true);
        when(getNodeFromGraphSubject(mockSubjects, mockSession, mockSubject))
                .thenReturn(mockSubjectNode);
        final String mockPropertyName = "mock:property";
        when(getPropertyNameFromPredicate(mockSubjectNode, mockPredicate))
                .thenReturn(mockPropertyName);
        when(mockSubjectNode.hasProperty(mockPropertyName)).thenReturn(true);
        mockStatic(NodePropertiesTools.class);
        when(getPropertyType(mockSubjectNode, mockPropertyName)).thenReturn(
                STRING);
        testObj.removedStatement(mockStatement);
        assertEquals(0, testObj.getProblems().size());
    }

    @Test(expected = RuntimeException.class)
    public void testRemovedStatementRepositoryException()
            throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(isFedoraGraphSubject(mockSubjects, mockSubject)).thenReturn(true);
        when(getNodeFromGraphSubject(mockSubjects, mockSession, mockSubject))
                .thenReturn(mockSubjectNode);
        when(getPropertyNameFromPredicate(mockSubjectNode, mockPredicate))
                .thenThrow(new RepositoryException());

        testObj.removedStatement(mockStatement);
    }

    @Test
    public void testRemovedProhibitedStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(isFedoraGraphSubject(mockSubjects, mockSubject)).thenReturn(true);
        when(getNodeFromGraphSubject(mockSubjects, mockSession, mockSubject))
                .thenReturn(mockSubjectNode);
        final String mockPropertyName = "jcr:property";
        when(getPropertyNameFromPredicate(mockSubjectNode, mockPredicate))
                .thenReturn(mockPropertyName);
        when(mockSubjectNode.hasProperty(mockPropertyName)).thenReturn(true);
        mockStatic(NodePropertiesTools.class);
        when(getPropertyType(mockSubjectNode, mockPropertyName)).thenReturn(
                STRING);
        testObj.removedStatement(mockStatement);
        assertEquals(1, testObj.getProblems().size());
    }

    @Test
    public void testRemovedIrrelevantStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(isFedoraGraphSubject(mockSubjects, mockSubject)).thenReturn(false);
        testObj.removedStatement(mockStatement);
        verifyStatic(never());
        getNodeFromGraphSubject(any(GraphSubjects.class), any(Session.class),
                any(Resource.class));
        // this was ignored, but not a problem
        assertEquals(0, testObj.getProblems().size());
    }

}
