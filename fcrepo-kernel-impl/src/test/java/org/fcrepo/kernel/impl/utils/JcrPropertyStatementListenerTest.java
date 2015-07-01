/**
 * Copyright 2015 DuraSpace, Inc.
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
import static javax.jcr.PropertyType.URI;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.getPropertyType;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;

import com.hp.hpl.jena.vocabulary.RDF;

import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.impl.rdf.JcrRdfTools;

import org.junit.Before;
import org.junit.Ignore;
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
@Ignore
public class JcrPropertyStatementListenerTest {

    private static final Logger LOGGER =
        getLogger(JcrPropertyStatementListenerTest.class);

    private JcrPropertyStatementListener testObj;

    @Mock
    private Node mockNode;

    @Mock
    private Session mockSession;

    private IdentifierConverter<Resource, FedoraResource> idTranslator;

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
    private JcrRdfTools mockJcrRdfTools;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private NodeTypeManager mockNodeTypeManager;

    @Mock
    private Model mockModel;

    private final Map<String, String> mockNsMapping = Collections.emptyMap();

    @Mock
    private NodePropertiesTools mockPropertiesTools;

    @Mock
    private com.hp.hpl.jena.rdf.model.RDFNode mockValue;

    private Resource mockResource;

    private FedoraResource resource;


    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        idTranslator = new DefaultIdentifierTranslator(mockSession);
        when(mockNode.getSession()).thenReturn(mockSession);
        testObj = new JcrPropertyStatementListener(idTranslator, mockJcrRdfTools);
        mockResource = idTranslator.toDomain("/xyz");
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
        resource = idTranslator.convert(mockResource);
        when(mockJcrRdfTools.skolemize(idTranslator, mockStatement)).thenReturn(mockStatement);
    }

    @Test
    public void testAddedIrrelevantStatement() {
        testObj.addedStatement(mockIrrelevantStatement);
    }

    @Test
    public void testAddedStatement() throws RepositoryException {
        when(mockSession.getNode("/some/path")).thenReturn(mockSubjectNode);
        testObj.addedStatement(mockStatement);
        verify(mockJcrRdfTools)
                .addProperty(resource, mockStatement.getPredicate(), mockStatement.getObject(), mockNsMapping);
        LOGGER.debug("Finished testAddedStatement()");
    }

    @Test(expected = RuntimeException.class)
    public void testAddedStatementRepositoryException()
            throws RepositoryException {

        doThrow(new RepositoryRuntimeException("")).when(mockJcrRdfTools)
                .addProperty(resource, mockPredicate, mockValue, mockNsMapping);

        testObj.addedStatement(mockStatement);
    }

    @Test
    public void testRemovedStatement() throws RepositoryException {
        testObj.removedStatement(mockStatement);
        verify(mockJcrRdfTools)
                .removeProperty(resource,
                        mockStatement.getPredicate(),
                        mockStatement.getObject(),
                        mockNsMapping);
    }

    @Test(expected = RuntimeException.class)
    public void testRemovedStatementRepositoryException()
            throws RepositoryException {

        doThrow(new RepositoryRuntimeException("")).when(mockJcrRdfTools)
                .removeProperty(resource, mockPredicate, mockValue, mockNsMapping);

        testObj.removedStatement(mockStatement);
    }

    @Test
    public void testRemovedIrrelevantStatement() {
        testObj.removedStatement(mockIrrelevantStatement);
        // this was ignored, but not a problem
    }

    @Test
    public void testAddRdfType() throws RepositoryException {

        when(mockSubjectNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNodeTypeManager);
        when(mockNodeTypeManager.hasNodeType("fedora:object")).thenReturn(true);

        when(mockSession.getNamespacePrefix(REPOSITORY_NAMESPACE))
                .thenReturn("fedora");
        final Model model = ModelFactory.createDefaultModel();
        final Resource type = model.createResource(REPOSITORY_NAMESPACE
                + "object");
        final Statement statement = model.createStatement(mockResource, RDF.type, type);
        when(mockSubjectNode.canAddMixin("fedora:object")).thenReturn(true);
        when(mockJcrRdfTools.skolemize(idTranslator, statement)).thenReturn(statement);
        testObj.addedStatement(statement);
        verify(mockJcrRdfTools).addMixin(resource, type, mockNsMapping);
    }

    @Test
    public void testRemoveRdfType() throws RepositoryException {

        when(mockSubjectNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNodeTypeManager);
        when(mockNodeTypeManager.hasNodeType("fedora:object")).thenReturn(true);
        when(mockSession.getNamespacePrefix(REPOSITORY_NAMESPACE)).thenReturn(
                "fedora");
        final Model model = createDefaultModel();
        final Resource type = model.createResource(REPOSITORY_NAMESPACE + "Container");
        model.add(mockResource, RDF.type, type);
        testObj.removedStatements(model);
        verify(mockJcrRdfTools).removeMixin(resource, type, mockNsMapping);
    }

    @Test
    public void testAddRdfTypeForNonMixin() throws RepositoryException {

        when(mockSubjectNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNodeTypeManager);
        when(mockNodeTypeManager.hasNodeType("fedora:object")).thenReturn(false);


        when(getPropertyType(mockSubjectNode, "rdf:type")).thenReturn(Optional.of(URI));

        when(mockSession.getNamespacePrefix(REPOSITORY_NAMESPACE))
                .thenReturn("fedora");
        final Model model = createDefaultModel();
        final Statement statement = model.createStatement(mockResource,
                type,
                model.createResource(REPOSITORY_NAMESPACE + "Container"));
        when(mockSubjectNode.canAddMixin("fedora:object")).thenReturn(true);
        when(mockJcrRdfTools.skolemize(idTranslator, statement)).thenReturn(statement);
        testObj.addedStatement(statement);
        verify(mockSubjectNode, never()).addMixin("fedora:object");
    }

    @Test
    public void testRemoveRdfTypeForNonMixin() throws RepositoryException {

        when(mockSubjectNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNodeTypeManager);
        when(mockNodeTypeManager.hasNodeType("fedora:object")).thenReturn(false);
        when(mockSession.getNamespacePrefix(REPOSITORY_NAMESPACE)).thenReturn("fedora");
        final Model model = createDefaultModel();
        model.add(mockResource, type, model.createResource(REPOSITORY_NAMESPACE + "Container"));
        testObj.removedStatements(model);
        verify(mockSubjectNode, never()).removeMixin("fedora:Container");
    }
}
