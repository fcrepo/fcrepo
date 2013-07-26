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
import static org.fcrepo.utils.NodePropertiesTools.getPropertyType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.fcrepo.RdfLexicon;
import org.fcrepo.rdf.GraphSubjects;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.hp.hpl.jena.rdf.model.Model;
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
    
    @Mock
    private Model mockProblems;

    @Mock
    private JcrRdfTools mockJcrRdfTools;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        mockStatic(JcrRdfTools.class);

        when(JcrRdfTools.withContext(mockSubjects, mockSession)).thenReturn(mockJcrRdfTools);
        when(mockNode.getSession()).thenReturn(mockSession);
        testObj = JcrPropertyStatementListener.getListener(mockSubjects, mockSession, mockProblems);
        when(mockStatement.getSubject()).thenReturn(mockSubject);
        when(mockStatement.getPredicate()).thenReturn(mockPredicate);
    }

    @Test
    public void testAddedIrrelevantStatement() throws RepositoryException {
        when(mockSubjects.isFedoraGraphSubject(mockSubject)).thenReturn(false);
        testObj.addedStatement(mockStatement);
        // this was ignored, but not a problem
        verify(mockProblems, never()).add(any(Resource.class), any(Property.class), any(String.class));
    }

    @Test
    public void testAddedProhibitedStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(mockSubjects.isFedoraGraphSubject(mockSubject)).thenReturn(true);
        when(mockSubjects.getNodeFromGraphSubject(mockSubject))
                .thenReturn(mockSubjectNode);
        when(mockJcrRdfTools.isInternalProperty(mockSubjectNode, mockPredicate)).thenReturn(true);

        when(mockPredicate.getURI()).thenReturn("x");
        testObj.addedStatement(mockStatement);
        verify(mockProblems).add(any(Resource.class), eq(RdfLexicon.COULD_NOT_STORE_PROPERTY), eq("x"));
    }

    @Test
    public void testAddedStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(mockSubjects.isFedoraGraphSubject(mockSubject)).thenReturn(true);
        when(mockSubjects.getNodeFromGraphSubject(mockSubject))
                .thenReturn(mockSubjectNode);
        final String mockPropertyName = "mock:property";
        when(mockJcrRdfTools.getPropertyNameFromPredicate(mockSubjectNode, mockPredicate))
            .thenReturn(mockPropertyName);

        mockStatic(NodePropertiesTools.class);
        when(getPropertyType(mockSubjectNode, mockPropertyName)).thenReturn(
                STRING);
        testObj.addedStatement(mockStatement);
        verify(mockProblems, times(0)).add(any(Resource.class), any(Property.class), any(String.class));
    }

    @Test(expected = RuntimeException.class)
    public void testAddedStatementRepositoryException()
            throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(mockSubjects.isFedoraGraphSubject(mockSubject)).thenReturn(true);
        when(mockSubjects.getNodeFromGraphSubject(mockSubject))
                .thenReturn(mockSubjectNode);

        when(mockJcrRdfTools.getPropertyNameFromPredicate(mockSubjectNode, mockPredicate))
               .thenThrow(new RepositoryException());

        testObj.addedStatement(mockStatement);
        verify(mockProblems, times(0)).add(any(Resource.class), any(Property.class), any(String.class));
    }

    @Test
    public void testRemovedStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(mockSubjects.isFedoraGraphSubject(mockSubject)).thenReturn(true);
        when(mockSubjects.getNodeFromGraphSubject(mockSubject))
                .thenReturn(mockSubjectNode);
        final String mockPropertyName = "mock:property";
        when(mockJcrRdfTools.getPropertyNameFromPredicate(mockSubjectNode, mockPredicate))
                .thenReturn(mockPropertyName);
        when(mockSubjectNode.hasProperty(mockPropertyName)).thenReturn(true);
        mockStatic(NodePropertiesTools.class);
        when(getPropertyType(mockSubjectNode, mockPropertyName)).thenReturn(
                STRING);
        testObj.removedStatement(mockStatement);
        verify(mockProblems, times(0)).add(any(Resource.class), any(Property.class), any(String.class));
    }

    @Test(expected = RuntimeException.class)
    public void testRemovedStatementRepositoryException()
            throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(mockSubjects.isFedoraGraphSubject(mockSubject)).thenReturn(true);
        when(mockSubjects.getNodeFromGraphSubject(mockSubject))
                .thenReturn(mockSubjectNode);

        when(mockJcrRdfTools.getPropertyNameFromPredicate(mockSubjectNode, mockPredicate))
                .thenThrow(new RepositoryException());

        testObj.removedStatement(mockStatement);
        verify(mockProblems, times(0)).add(any(Resource.class), any(Property.class), any(String.class));
    }

    @Test
    public void testRemovedProhibitedStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(mockSubjects.isFedoraGraphSubject(mockSubject)).thenReturn(true);
        when(mockSubjects.getNodeFromGraphSubject(mockSubject))
                .thenReturn(mockSubjectNode);
        when(mockPredicate.getURI()).thenReturn("x");
        final String mockPropertyName = "jcr:property";
        when(mockJcrRdfTools.getPropertyNameFromPredicate(mockSubjectNode, mockPredicate))
                .thenReturn(mockPropertyName);
        when(mockJcrRdfTools.isInternalProperty(mockSubjectNode, mockPredicate)).thenReturn(true);
        when(mockSubjectNode.hasProperty(mockPropertyName)).thenReturn(true);
        mockStatic(NodePropertiesTools.class);
        when(getPropertyType(mockSubjectNode, mockPropertyName)).thenReturn(
                STRING);
        testObj.removedStatement(mockStatement);
        verify(mockProblems).add(any(Resource.class), eq(RdfLexicon.COULD_NOT_STORE_PROPERTY), eq("x"));
    }

    @Test
    public void testRemovedIrrelevantStatement() throws RepositoryException {
        when(mockSubjects.isFedoraGraphSubject(mockSubject)).thenReturn(false);
        testObj.removedStatement(mockStatement);
        // this was ignored, but not a problem
        verify(mockProblems, times(0)).add(any(Resource.class), any(Property.class), any(String.class));
    }

}
