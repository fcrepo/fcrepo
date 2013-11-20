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

package org.fcrepo.kernel.utils.iterators;

import static com.hp.hpl.jena.graph.NodeFactory.createAnon;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static javax.jcr.PropertyType.STRING;
import static javax.jcr.PropertyType.UNDEFINED;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.rdf.JcrRdfTools.getJcrNamespaceForRDFNamespace;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.NamespaceException;
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

import org.fcrepo.kernel.exception.MalformedRdfException;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;
import com.google.common.collect.ImmutableMap;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;


public class RdfAdderTest {

    private RdfAdder testAdder;

    private static final Model m = createDefaultModel();

    private static final String propertyNamespacePrefix = "ex";

    private static final String propertyNamespaceUri =
        "http://www.example.com#";

    private static final String propertyBaseName = "example-property";

    private static final String propertyLongName = propertyNamespaceUri
            + propertyBaseName;

    private static final String propertyShortName = propertyNamespacePrefix
            + ":" + propertyBaseName;

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


    private static final Statement mixinStmnt = m.asStatement(mixinTriple);


    @Test
    public void testAddingProperty() throws Exception {
        testAdder = new RdfAdder(mockGraphSubjects, mockSession, testStream);
        when(mockNode.setProperty(propertyShortName, mockValue, UNDEFINED)).thenReturn(mockProperty);
        testAdder.operateOnProperty(descriptiveStmnt, mockNode);
        verify(mockNode).setProperty(propertyShortName, mockValue, UNDEFINED);

    }

    @Test
    public void testAddingModelWithStreamNamespace() throws Exception {
        testAdder = new RdfAdder(mockGraphSubjects, mockSession, testStream);
        testAdder.operateOnMixin(mixinStmnt.getObject().asResource(), mockNode);
        verify(mockNode).addMixin(anyString());
    }

    @Test(expected = MalformedRdfException.class)
    public void testAddingWithBadNamespace() throws Exception {
        // we drop our stream namespace map
        testStream = new RdfStream(mockTriples);
        when(
                mockSession
                        .getNamespacePrefix(getJcrNamespaceForRDFNamespace(type
                                .getNameSpace()))).thenThrow(new NamespaceException("Expected."));
        testAdder = new RdfAdder(mockGraphSubjects, mockSession, testStream);
        testAdder.operateOnMixin(mixinStmnt.getObject().asResource(), mockNode);
    }

    @Test
    public void testAddingWithRepoNamespace() throws Exception {
        // we drop our stream namespace map
        testStream = new RdfStream(mockTriples);
        when(
                mockSession
                        .getNamespacePrefix(getJcrNamespaceForRDFNamespace(type
                                .getNameSpace()))).thenReturn("rdf");
        testAdder = new RdfAdder(mockGraphSubjects, mockSession, testStream);
        testAdder.operateOnMixin(mixinStmnt.getObject().asResource(), mockNode);
    }

    @Test(expected = MalformedRdfException.class)
    public void testAddingWithBadMixinOnNode() throws Exception {
        when(mockNode.canAddMixin(mixinShortName)).thenReturn(false);
        testAdder = new RdfAdder(mockGraphSubjects, mockSession, testStream);
        testAdder.operateOnMixin(mixinStmnt.getObject().asResource(), mockNode);
    }

    @Test(expected = MalformedRdfException.class)
    public void testAddingWithBadMixinForRepo() throws Exception {
        when(mockNodeTypeManager.hasNodeType(mixinShortName)).thenReturn(false);
        testAdder = new RdfAdder(mockGraphSubjects, mockSession, testStream);
        testAdder.operateOnMixin(mixinStmnt.getObject().asResource(), mockNode);
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
        when(mockGraphSubjects.getGraphSubject(mockNode)).thenReturn(
                mockNodeSubject);
        when(mockTriples.hasNext()).thenReturn(true, true, false);
        when(mockTriples.next()).thenReturn(descriptiveTriple, mixinTriple);
        testStream = new RdfStream(mockTriples);
        testStream.namespaces(mockNamespaceMap);
    }


    @Mock
    private Node mockNode;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private ValueFactory mockValueFactory;

    @Mock
    private Value mockValue;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

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
    private GraphSubjects mockGraphSubjects;

    @Mock
    private Property mockProperty;

}
