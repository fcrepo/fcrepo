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
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static javax.jcr.PropertyType.STRING;
import static javax.jcr.PropertyType.URI;
import static org.fcrepo.kernel.RdfLexicon.RESTAPI_NAMESPACE;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;

import com.hp.hpl.jena.vocabulary.RDF;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.impl.rdf.JcrRdfTools;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
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
public class JcrPropertyStatementListenerTest {

    private static final Logger LOGGER =
        getLogger(JcrPropertyStatementListenerTest.class);

    private JcrPropertyStatementListener testObj;

    @Mock
    private Node mockNode;

    @Mock
    private Session mockSession;

    private IdentifierConverter<Resource,Node> mockSubjects;

    @Mock
    private Statement mockStatement;

    @Mock
    private Statement mockIrrelevantStatement;

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

    @Mock
    private com.hp.hpl.jena.rdf.model.RDFNode mockValue;

    private Resource mockResource;


    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        mockSubjects = new DefaultIdentifierTranslator(mockSession);
        when(mockNode.getSession()).thenReturn(mockSession);
        testObj = JcrPropertyStatementListener.getListener(mockSubjects, mockSession, mockProblems, mockJcrRdfTools);
        mockResource = mockSubjects.toDomain("/xyz");
        when(mockStatement.getSubject()).thenReturn(mockResource);
        when(mockStatement.getPredicate()).thenReturn(mockPredicate);
        when(mockStatement.getModel()).thenReturn(mockModel);
        when(mockStatement.getObject()).thenReturn(mockValue);
        when(mockSession.getNode("/xyz")).thenReturn(mockSubjectNode);
        when(mockSubjectNode.getPath()).thenReturn("/xyz");

        when(mockIrrelevantStatement.getSubject()).thenReturn(mockSubject);
        when(mockIrrelevantStatement.getPredicate()).thenReturn(mockPredicate);
        when(mockIrrelevantStatement.getModel()).thenReturn(mockModel);
        when(mockIrrelevantStatement.getObject()).thenReturn(mockValue);

        when(mockModel.getNsPrefixMap()).thenReturn(mockNsMapping);
        mockSubjects = new DefaultIdentifierTranslator(mockSession);
    }

    @Test
    public void testAddedIrrelevantStatement() {
        testObj.addedStatement(mockIrrelevantStatement);
        // this was ignored, but not a problem
        verify(mockProblems, never()).add(any(Resource.class), any(Property.class), any(String.class));
    }

    @Test
    public void testAddedStatement() throws RepositoryException {
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

        doThrow(new RepositoryRuntimeException("")).when(mockJcrRdfTools)
                .addProperty(mockSubjectNode, mockPredicate, mockValue, mockNsMapping);

        testObj.addedStatement(mockStatement);
        verify(mockProblems, times(0)).add(any(Resource.class), any(Property.class), any(String.class));
    }

    @Test
    public void testRemovedStatement() throws RepositoryException {
        final String mockPropertyName = "mock:property";
        when(mockJcrRdfTools.getPropertyNameFromPredicate(mockSubjectNode, mockPredicate, mockNsMapping))
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

        doThrow(new RepositoryRuntimeException("")).when(mockJcrRdfTools)
                .removeProperty(mockSubjectNode, mockPredicate, mockValue, mockNsMapping);

        testObj.removedStatement(mockStatement);
        verify(mockProblems, times(0)).add(any(Resource.class), any(Property.class), any(String.class));
    }

    @Test
    public void testRemovedIrrelevantStatement() {
        testObj.removedStatement(mockIrrelevantStatement);
        // this was ignored, but not a problem
        verify(mockProblems, times(0)).add(any(Resource.class), any(Property.class), any(String.class));
    }

    @Test
    public void testAddRdfType() throws RepositoryException {

        when(mockSubjectNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNodeTypeManager);
        when(mockNodeTypeManager.hasNodeType("fedora:object")).thenReturn(true);

        when(mockSession.getNamespacePrefix(RESTAPI_NAMESPACE))
                .thenReturn("fedora");
        final Model model = ModelFactory.createDefaultModel();
        final Resource type = model.createResource(RESTAPI_NAMESPACE
                + "object");
        model.add(mockResource, RDF.type, type);
        when(mockSubjectNode.canAddMixin("fedora:object")).thenReturn(true);
        testObj.addedStatements(model);
        verify(mockJcrRdfTools).addMixin(mockSubjectNode, type, mockNsMapping);
    }

    @Test
    public void testRemoveRdfType() throws RepositoryException {

        when(mockSubjectNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNodeTypeManager);
        when(mockNodeTypeManager.hasNodeType("fedora:object")).thenReturn(true);
        when(mockSession.getNamespacePrefix(RESTAPI_NAMESPACE)).thenReturn(
                "fedora");
        final Model model = createDefaultModel();
        final Resource type = model.createResource(RESTAPI_NAMESPACE + "object");
        model.add(mockResource, RDF.type, type);
        testObj.removedStatements(model);
        verify(mockJcrRdfTools).removeMixin(mockSubjectNode, type, mockNsMapping);
    }

    @Test
    public void testAddRdfTypeForNonMixin() throws RepositoryException {

        when(mockSubjectNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNodeTypeManager);
        when(mockNodeTypeManager.hasNodeType("fedora:object")).thenReturn(false);


        when(mockPropertiesTools.getPropertyType(mockSubjectNode, "rdf:type"))
                .thenReturn(URI);

        when(mockSession.getNamespacePrefix(RESTAPI_NAMESPACE))
                .thenReturn("fedora");
        final Model model = createDefaultModel();
        model.add(mockResource, type, model.createResource(RESTAPI_NAMESPACE + "object"));
        when(mockSubjectNode.canAddMixin("fedora:object")).thenReturn(true);
        testObj.addedStatements(model);
        verify(mockSubjectNode, never()).addMixin("fedora:object");
        verify(mockProblems, times(0)).add(any(Resource.class), any(Property.class), any(String.class));
    }

    @Test
    public void testRemoveRdfTypeForNonMixin() throws RepositoryException {

        when(mockSubjectNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNodeTypeManager);
        when(mockNodeTypeManager.hasNodeType("fedora:object")).thenReturn(false);
        when(mockSession.getNamespacePrefix(RESTAPI_NAMESPACE)).thenReturn("fedora");
        final Model model = createDefaultModel();
        model.add(mockResource, type, model.createResource(RESTAPI_NAMESPACE + "object"));
        testObj.removedStatements(model);
        verify(mockSubjectNode, never()).removeMixin("fedora:object");
        verify(mockProblems, times(0)).add(any(Resource.class), any(Property.class), any(String.class));
    }
}
