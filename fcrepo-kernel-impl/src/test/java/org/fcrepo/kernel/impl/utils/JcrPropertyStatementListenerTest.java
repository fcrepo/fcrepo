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
package org.fcrepo.kernel.impl.utils;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static javax.jcr.PropertyType.STRING;
import static javax.jcr.PropertyType.URI;
import static org.fcrepo.kernel.RdfLexicon.COULD_NOT_STORE_PROPERTY;
import static org.fcrepo.kernel.RdfLexicon.RESTAPI_NAMESPACE;
import static org.fcrepo.kernel.utils.TestHelpers.setField;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;

import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.impl.rdf.JcrRdfTools;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * <p>JcrPropertyStatementListenerTest class.</p>
 *
 * @author awoods
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({JcrRdfTools.class})
public class JcrPropertyStatementListenerTest {

    private static final Logger LOGGER =
        getLogger(JcrPropertyStatementListenerTest.class);

    private JcrPropertyStatementListener testObj;

    @Mock
    private Node mockNode;

    @Mock
    private Session mockSession;

    @Mock
    private IdentifierTranslator mockSubjects;

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

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private NodeTypeManager mockNodeTypeManager;

    @Mock
    private Model mockModel;

    private Map<String, String> mockNsMapping = Collections.emptyMap();

    @Mock
    private NodePropertiesTools mockPropertiesTools;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        mockStatic(JcrRdfTools.class);

        when(JcrRdfTools.withContext(mockSubjects, mockSession)).thenReturn(mockJcrRdfTools);
        when(mockNode.getSession()).thenReturn(mockSession);
        testObj = JcrPropertyStatementListener.getListener(mockSubjects, mockSession, mockProblems);
        when(mockStatement.getSubject()).thenReturn(mockSubject);
        when(mockStatement.getPredicate()).thenReturn(mockPredicate);
        setField(testObj, "propertiesTools", mockPropertiesTools);
        when(mockStatement.getModel()).thenReturn(mockModel);
        when(mockModel.getNsPrefixMap()).thenReturn(mockNsMapping);
    }

    @Test
    public void testAddedIrrelevantStatement() {
        when(mockSubjects.isFedoraGraphSubject(mockSubject)).thenReturn(false);
        testObj.addedStatement(mockStatement);
        // this was ignored, but not a problem
        verify(mockProblems, never()).add(any(Resource.class), any(Property.class), any(String.class));
    }

    @Test
    public void testAddedProhibitedStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(mockSubjects.isFedoraGraphSubject(mockSubject)).thenReturn(true);
        //when(mockSession.getNode(mockSubjects.getPathFromSubject(mockSubject))).thenReturn(mockSubjectNode);
        when(mockSubjects.getPathFromSubject(mockSubject)).thenReturn("/some/path");
        when(mockSession.getNode("/some/path")).thenReturn(mockSubjectNode);
        when(mockJcrRdfTools.isInternalProperty(mockSubjectNode, mockPredicate)).thenReturn(true);

        when(mockPredicate.getURI()).thenReturn("x");
        testObj.addedStatement(mockStatement);
        verify(mockProblems).add(any(Resource.class), eq(COULD_NOT_STORE_PROPERTY), eq("x"));
    }

    @Test
    public void testAddedStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(mockSubjects.isFedoraGraphSubject(mockSubject)).thenReturn(true);
        when(mockSubjects.getPathFromSubject(mockSubject)).thenReturn("/some/path");
        when(mockSession.getNode("/some/path")).thenReturn(mockSubjectNode);
        final String mockPropertyName = "mock:property";
        when(
                mockJcrRdfTools.getPropertyNameFromPredicate(mockSubjectNode,
                        mockPredicate, mockNsMapping)).thenReturn(
                mockPropertyName);
        when(mockPropertiesTools.getPropertyType(mockSubjectNode, mockPropertyName))
                .thenReturn(STRING);
        testObj.addedStatement(mockStatement);
        verify(mockProblems, times(0)).add(any(Resource.class), any(Property.class), any(String.class));
        LOGGER.debug("Finished testAddedStatement()");
    }

    @Test(expected = RuntimeException.class)
    public void testAddedStatementRepositoryException()
            throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(mockSubjects.isFedoraGraphSubject(mockSubject)).thenReturn(true);
        when(mockSession.getNode(mockSubjects.getPathFromSubject(mockSubject))).thenReturn(mockSubjectNode);

        when(mockJcrRdfTools.getPropertyNameFromPredicate(mockSubjectNode, mockPredicate, mockNsMapping))
               .thenThrow(new RepositoryException());

        testObj.addedStatement(mockStatement);
        verify(mockProblems, times(0)).add(any(Resource.class), any(Property.class), any(String.class));
    }

    @Test
    public void testRemovedStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(mockSubjects.isFedoraGraphSubject(mockSubject)).thenReturn(true);
        when(mockSession.getNode(mockSubjects.getPathFromSubject(mockSubject))).thenReturn(mockSubjectNode);
        final String mockPropertyName = "mock:property";
        when(mockJcrRdfTools.getPropertyNameFromPredicate(mockSubjectNode, mockPredicate))
                .thenReturn(mockPropertyName);
        when(mockSubjectNode.hasProperty(mockPropertyName)).thenReturn(true);
        when(mockPropertiesTools.getPropertyType(mockSubjectNode, mockPropertyName)).thenReturn(
                STRING);
        testObj.removedStatement(mockStatement);
        verify(mockProblems, times(0)).add(any(Resource.class), any(Property.class), any(String.class));
    }

    @Test(expected = RuntimeException.class)
    public void testRemovedStatementRepositoryException()
            throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(mockSubjects.isFedoraGraphSubject(mockSubject)).thenReturn(true);
        when(mockSession.getNode(mockSubjects.getPathFromSubject(mockSubject))).thenReturn(mockSubjectNode);

        when(mockJcrRdfTools.getPropertyNameFromPredicate(mockSubjectNode, mockPredicate))
                .thenThrow(new RepositoryException());

        testObj.removedStatement(mockStatement);
        verify(mockProblems, times(0)).add(any(Resource.class), any(Property.class), any(String.class));
    }

    @Test
    public void testRemovedProhibitedStatement() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        when(mockSubjects.isFedoraGraphSubject(mockSubject)).thenReturn(true);
        when(mockSession.getNode(mockSubjects.getPathFromSubject(mockSubject))).thenReturn(mockSubjectNode);
        when(mockPredicate.getURI()).thenReturn("x");
        final String mockPropertyName = "jcr:property";
        when(mockJcrRdfTools.getPropertyNameFromPredicate(mockSubjectNode, mockPredicate))
                .thenReturn(mockPropertyName);
        when(mockJcrRdfTools.isInternalProperty(mockSubjectNode, mockPredicate)).thenReturn(true);
        when(mockSubjectNode.hasProperty(mockPropertyName)).thenReturn(true);
        when(mockPropertiesTools.getPropertyType(mockSubjectNode, mockPropertyName)).thenReturn(
                STRING);
        testObj.removedStatement(mockStatement);
        verify(mockProblems).add(any(Resource.class), eq(COULD_NOT_STORE_PROPERTY), eq("x"));
    }

    @Test
    public void testRemovedIrrelevantStatement() {
        when(mockSubjects.isFedoraGraphSubject(mockSubject)).thenReturn(false);
        testObj.removedStatement(mockStatement);
        // this was ignored, but not a problem
        verify(mockProblems, times(0)).add(any(Resource.class), any(Property.class), any(String.class));
    }

    @Test
    public void testAddRdfType() throws RepositoryException {

        final Resource resource = createResource("xyz");
        when(mockSubjects.isFedoraGraphSubject(resource)).thenReturn(true);
        //when(mockSession.getNode(mockSubjects.getPathFromSubject(resource))).thenReturn(mockSubjectNode);
        when(mockSubjects.getPathFromSubject(resource)).thenReturn("/xyz");
        when(mockSession.getNode("/xyz")).thenReturn(mockSubjectNode);

        when(mockSubjectNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNodeTypeManager);
        when(mockNodeTypeManager.hasNodeType("fedora:object")).thenReturn(true);

        when(mockSession.getNamespacePrefix(RESTAPI_NAMESPACE))
                .thenReturn("fedora");
        final Model model = ModelFactory.createDefaultModel();
        model.add(resource, type, model.createResource(RESTAPI_NAMESPACE
                + "object"));
        when(mockSubjectNode.canAddMixin("fedora:object")).thenReturn(true);
        testObj.addedStatements(model);
        verify(mockSubjectNode).addMixin("fedora:object");
    }

    @Test
    public void testRemoveRdfType() throws RepositoryException {

        final Resource resource = createResource();
        when(mockSubjects.isFedoraGraphSubject(resource)).thenReturn(true);
        when(mockSession.getNode(mockSubjects.getPathFromSubject(resource))).thenReturn(mockSubjectNode);

        when(mockSubjectNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNodeTypeManager);
        when(mockNodeTypeManager.hasNodeType("fedora:object")).thenReturn(true);
        when(mockSession.getNamespacePrefix(RESTAPI_NAMESPACE)).thenReturn(
                "fedora");
        final Model model = createDefaultModel();
        model.add(resource, type, model.createResource(RESTAPI_NAMESPACE + "object"));
        testObj.removedStatements(model);
        verify(mockSubjectNode).removeMixin("fedora:object");
    }

    @Test
    public void testAddRdfTypeForNonMixin() throws RepositoryException {

        final Resource resource = createResource("xyz");
        when(mockSubjects.isFedoraGraphSubject(resource)).thenReturn(true);
        when(mockSubjects.getPathFromSubject(resource)).thenReturn("/xyz");
        when(mockSession.getNode("/xyz")).thenReturn(mockSubjectNode);

        when(mockSubjectNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNodeTypeManager);
        when(mockNodeTypeManager.hasNodeType("fedora:object")).thenReturn(false);


        when(mockPropertiesTools.getPropertyType(mockSubjectNode, "rdf:type"))
                .thenReturn(URI);

        when(mockSession.getNamespacePrefix(RESTAPI_NAMESPACE))
                .thenReturn("fedora");
        final Model model = createDefaultModel();
        model.add(resource, type, model.createResource(RESTAPI_NAMESPACE + "object"));
        when(mockSubjectNode.canAddMixin("fedora:object")).thenReturn(true);
        testObj.addedStatements(model);
        verify(mockSubjectNode, never()).addMixin("fedora:object");
        verify(mockProblems, times(0)).add(any(Resource.class), any(Property.class), any(String.class));
    }

    @Test
    public void testRemoveRdfTypeForNonMixin() throws RepositoryException {

        final Resource resource = createResource();
        when(mockSubjects.isFedoraGraphSubject(resource)).thenReturn(true);
        when(mockSession.getNode(mockSubjects.getPathFromSubject(resource))).thenReturn(mockSubjectNode);

        when(mockSubjectNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNodeTypeManager);
        when(mockNodeTypeManager.hasNodeType("fedora:object")).thenReturn(false);
        when(mockSession.getNamespacePrefix(RESTAPI_NAMESPACE)).thenReturn("fedora");
        final Model model = createDefaultModel();
        model.add(resource, type, model.createResource(RESTAPI_NAMESPACE + "object"));
        testObj.removedStatements(model);
        verify(mockSubjectNode, never()).removeMixin("fedora:object");
        verify(mockProblems, times(0)).add(any(Resource.class), any(Property.class), any(String.class));
    }
}
