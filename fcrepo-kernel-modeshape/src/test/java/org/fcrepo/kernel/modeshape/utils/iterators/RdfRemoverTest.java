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
package org.fcrepo.kernel.modeshape.utils.iterators;

import static com.hp.hpl.jena.graph.NodeFactory.createAnon;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static javax.jcr.PropertyType.STRING;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.modeshape.rdf.JcrRdfTools.getJcrNamespaceForRDFNamespace;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;

import com.google.common.collect.ImmutableMap;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;


/**
 * <p>RdfRemoverTest class.</p>
 *
 * @author ajs6f
 */
public class RdfRemoverTest {

    @Test
    public void testRemovingNonExistentProperty() throws Exception {

        when(mockNode.hasProperty(propertyShortName)).thenReturn(false);
        testRemover = new RdfRemover(mockGraphSubjects, mockSession, testStream);
        testRemover.operateOnProperty(descriptiveStmnt, resource);
        verifyZeroInteractions(mockProperty);
    }

    @Test
    public void testRemovingExistentProperty() throws Exception {

        when(mockNode.hasProperty(propertyShortName)).thenReturn(true);
        when(mockNode.getProperty(propertyShortName)).thenReturn(mockProperty);
        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockProperty.getValue()).thenReturn(mockValue);
        testRemover = new RdfRemover(mockGraphSubjects, mockSession, testStream);
        testRemover.operateOnProperty(descriptiveStmnt, resource);
        verify(mockProperty).remove();
    }

    @Test
    public void testRemovingExistentMixin() throws Exception {
        when(mockNode.isNodeType(mixinShortName)).thenReturn(true);
        testRemover = new RdfRemover(mockGraphSubjects, mockSession, testStream);
        testRemover.operateOnMixin(mixinStmnt.getObject().asResource(), resource);
        verify(mockNode).removeMixin(mixinShortName);
    }

    @Test
    public void testRemovingPrimaryType() throws Exception {

        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.isNodeType(mixinShortName)).thenReturn(true);

        testRemover = new RdfRemover(mockGraphSubjects, mockSession, testStream);
        testRemover.operateOnMixin(mixinStmnt.getObject().asResource(), resource);

        verify(mockNode, never()).removeMixin(mixinShortName);
    }

    @Test
    public void testRemovingNonExistentMixin() throws Exception {
        testRemover = new RdfRemover(mockGraphSubjects, mockSession, testStream);
        testRemover.operateOnMixin(mixinStmnt.getObject().asResource(), resource);
    }

    @Test
    public void testRemovingMixinThatCannotExistInRepo() throws Exception {
        when(mockNodeTypeManager.hasNodeType(mixinShortName)).thenReturn(false);
        testRemover =
            new RdfRemover(mockGraphSubjects, mockSession, testStream);
        testRemover.operateOnMixin(mixinStmnt.getObject().asResource(), resource);
        verify(mockNode, never()).removeMixin(mixinShortName);
    }

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockNode.getName()).thenReturn("mockNode");
        when(mockNode.getPath()).thenReturn("/mockNode");
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);
        when(
                mockSession
                        .getNamespacePrefix(getJcrNamespaceForRDFNamespace(type
                                .getNameSpace()))).thenReturn("rdf");
        when(mockValueFactory.createValue(description, STRING)).thenReturn(mockValue);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(
                mockNamespaceRegistry);
        when(mockNamespaceRegistry.getURI(propertyNamespacePrefix)).thenReturn(
                propertyNamespaceUri);
        when(mockNamespaceRegistry.getURI("rdf")).thenReturn(
                type.getNameSpace());
        when(mockNamespaceRegistry.isRegisteredUri(propertyNamespaceUri))
                .thenReturn(true);
        when(mockNamespaceRegistry.isRegisteredUri(type.getNameSpace()))
        .thenReturn(true);
        when(mockNamespaceRegistry.getPrefix(propertyNamespaceUri)).thenReturn(
                propertyNamespacePrefix);
        when(mockNamespaceRegistry.getPrefix(type.getNameSpace())).thenReturn(
                "rdf");
        when(mockWorkspace.getNodeTypeManager())
                .thenReturn(mockNodeTypeManager);
        when(mockNodeTypeManager.getNodeType(FEDORA_RESOURCE)).thenReturn(
                mockNodeType);
        when(mockNodeTypeManager.hasNodeType(mixinShortName)).thenReturn(true);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[] {});
        when(mockNode.canAddMixin(mixinShortName)).thenReturn(true);
        when(mockNodeType.getPropertyDefinitions()).thenReturn(
                new PropertyDefinition[] {mockPropertyDefinition});
        when(mockPropertyDefinition.isMultiple()).thenReturn(false);
        when(mockPropertyDefinition.getName()).thenReturn(propertyShortName);
        when(mockPropertyDefinition.getRequiredType()).thenReturn(STRING);
        when(mockGraphSubjects.reverse()).thenReturn(mockReverseGraphSubjects);
        // TODO? when(mockReverseGraphSubjects.convert(mockNode)).thenReturn(mockNodeSubject);
        when(mockTriples.hasNext()).thenReturn(true, true, false);
        when(mockTriples.next()).thenReturn(descriptiveTriple, mixinTriple);
        resource = new FedoraResourceImpl(mockNode);
        testStream = new RdfStream(mockTriples);
        testStream.namespaces(mockNamespaceMap);
    }


    private RdfRemover testRemover;

    private FedoraResource resource;

    @Mock
    private Node mockNode;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private ValueFactory mockValueFactory;

    @Mock
    private Value mockValue;

    @Mock
    private Value mockValue2;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Mock
    private Property mockProperty;

    @Mock
    private NodeTypeManager mockNodeTypeManager;

    @Mock
    private NodeType mockNodeType;

    @Mock
    private PropertyDefinition mockPropertyDefinition;

    @Mock
    private Session mockSession;

    @Mock
    private Iterator<Triple> mockTriples;


    private RdfStream testStream;



    @Mock
    private IdentifierConverter<Resource, FedoraResource> mockGraphSubjects;

    @Mock
    private IdentifierConverter<FedoraResource,Resource> mockReverseGraphSubjects;


    private static final Model m = createDefaultModel();

    private static final String propertyNamespacePrefix = "ex";

    private static final String propertyNamespaceUri =
        "http://www.example.com#";

    private static final String propertyBaseName = "example-property";

    private static final String propertyLongName = propertyNamespaceUri
            + propertyBaseName;

    private static final Map<String, String> mockNamespaceMap = ImmutableMap
            .of(propertyNamespacePrefix, propertyNamespaceUri, "rdf", type.getNameSpace());

    private static final String description = "Description.";

    private static final Triple descriptiveTriple = create(createAnon(),
            createURI(propertyLongName), createLiteral(description));

    private static final Statement descriptiveStmnt = m
            .asStatement(descriptiveTriple);

    private static final Resource mockNodeSubject = descriptiveStmnt
            .getSubject();

    private static final String mixinLongName = type.getNameSpace() + "someType";
    private static final String mixinShortName = "rdf" + ":" + "someType";

    private static final Resource mixinObject = createResource(mixinLongName);

    private static final Triple mixinTriple = create(mockNodeSubject.asNode(),
            type.asNode(), mixinObject.asNode());

    private static final String propertyShortName = propertyNamespacePrefix
            + ":" + propertyBaseName;

    private static final Statement mixinStmnt = m.asStatement(mixinTriple);

}
