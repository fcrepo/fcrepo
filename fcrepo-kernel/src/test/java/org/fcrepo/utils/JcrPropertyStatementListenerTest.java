/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.rdf.GraphSubjects;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;


/**
 * @todo Add Documentation.
 * @author Benjamin Armintor
 * @date May 13, 2013
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({JcrRdfTools.class, NodePropertiesTools.class})
public class JcrPropertyStatementListenerTest {

    private JcrPropertyStatementListener testObj;

    private Node mockNode;

    private Session mockSession;

    private GraphSubjects mockSubjects;

    private Statement mockStatement;

    private Resource mockSubject;

    private Property mockPredicate;

    /**
     * @todo Add Documentation.
     */
    @Before
    public void setUp() throws RepositoryException {
        mockNode = mock(Node.class);
        mockSession = mock(Session.class);
        mockSubjects = mock(GraphSubjects.class);
        when(mockNode.getSession()).thenReturn(mockSession);
        testObj = new JcrPropertyStatementListener(mockSubjects, mockSession);
        mockSubject = mock(Resource.class);
        mockStatement = mock(Statement.class);
        when(mockStatement.getSubject()).thenReturn(mockSubject);
        mockPredicate = mock(Property.class);
        when(mockStatement.getPredicate()).thenReturn(mockPredicate);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testAddedIrrelevantStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(JcrRdfTools.isFedoraGraphSubject(mockSubjects, mockSubject))
            .thenReturn(false);
        testObj.addedStatement(mockStatement);
        verifyStatic(never());
        JcrRdfTools.getNodeFromGraphSubject(any(GraphSubjects.class),
                any(Session.class), any(Resource.class));
        // this was ignored, but not a problem
        assertEquals(0, testObj.getProblems().size());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testAddedProhibitedStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(JcrRdfTools.isFedoraGraphSubject(mockSubjects, mockSubject))
        .thenReturn(true);
        Node mockSubjectNode = mock(Node.class);
        when(JcrRdfTools.getNodeFromGraphSubject(
                mockSubjects, mockSession, mockSubject))
        .thenReturn(mockSubjectNode);

        String mockPropertyName = "jcr:property";
        when(JcrRdfTools.getPropertyNameFromPredicate(
                mockSubjectNode, mockPredicate))
        .thenReturn(mockPropertyName);

        testObj.addedStatement(mockStatement);
        assertEquals(1, testObj.getProblems().size());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testAddedStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(JcrRdfTools.isFedoraGraphSubject(mockSubjects, mockSubject))
        .thenReturn(true);
        Node mockSubjectNode = mock(Node.class);
        when(JcrRdfTools.getNodeFromGraphSubject(
                mockSubjects, mockSession, mockSubject))
        .thenReturn(mockSubjectNode);

        String mockPropertyName = "mock:property";
        when(JcrRdfTools.getPropertyNameFromPredicate(mockSubjectNode,
                                                      mockPredicate))
        .thenReturn(mockPropertyName);

        mockStatic(NodePropertiesTools.class);
        when(NodePropertiesTools.getPropertyType(mockSubjectNode,
                                                 mockPropertyName))
        .thenReturn(PropertyType.STRING);
        testObj.addedStatement(mockStatement);
        assertEquals(0, testObj.getProblems().size());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testRemovedStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(JcrRdfTools.isFedoraGraphSubject(mockSubjects, mockSubject))
        .thenReturn(true);
        Node mockSubjectNode = mock(Node.class);
        when(JcrRdfTools.getNodeFromGraphSubject(mockSubjects, mockSession,
                                                 mockSubject))
        .thenReturn(mockSubjectNode);

        String mockPropertyName = "mock:property";
        when(JcrRdfTools.getPropertyNameFromPredicate(mockSubjectNode,
                                                      mockPredicate))
        .thenReturn(mockPropertyName);

        when(mockSubjectNode.hasProperty(mockPropertyName))
        .thenReturn(true);

        mockStatic(NodePropertiesTools.class);
        when(NodePropertiesTools.getPropertyType(mockSubjectNode,
                                                 mockPropertyName))
        .thenReturn(PropertyType.STRING);
        testObj.removedStatement(mockStatement);
        assertEquals(0, testObj.getProblems().size());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testRemovedProhibitedStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(JcrRdfTools.isFedoraGraphSubject(mockSubjects,mockSubject))
        .thenReturn(true);
        Node mockSubjectNode = mock(Node.class);
        when(JcrRdfTools.getNodeFromGraphSubject(mockSubjects, mockSession,
                                                 mockSubject))
        .thenReturn(mockSubjectNode);

        String mockPropertyName = "jcr:property";
        when(JcrRdfTools.getPropertyNameFromPredicate(mockSubjectNode,
                                                      mockPredicate))
            .thenReturn(mockPropertyName);

        when(mockSubjectNode.hasProperty(mockPropertyName))
        .thenReturn(true);

        mockStatic(NodePropertiesTools.class);
        when(NodePropertiesTools.getPropertyType(mockSubjectNode,
                                                 mockPropertyName))
            .thenReturn(PropertyType.STRING);
        testObj.removedStatement(mockStatement);
        assertEquals(1, testObj.getProblems().size());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testRemovedIrrelevantStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(JcrRdfTools.isFedoraGraphSubject(mockSubjects, mockSubject))
            .thenReturn(false);
        testObj.removedStatement(mockStatement);
        verifyStatic(never());
        JcrRdfTools.getNodeFromGraphSubject(any(GraphSubjects.class),
                                            any(Session.class),
                                            any(Resource.class));
        // this was ignored, but not a problem
        assertEquals(0, testObj.getProblems().size());
    }

}
