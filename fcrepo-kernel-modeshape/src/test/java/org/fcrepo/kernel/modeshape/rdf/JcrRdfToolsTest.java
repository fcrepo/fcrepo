/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.rdf;

import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDstring;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static javax.jcr.PropertyType.LONG;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.STRING;
import static javax.jcr.PropertyType.URI;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.FIELD_DELIMITER;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.FROZEN_NODE;
import static org.fcrepo.kernel.modeshape.rdf.JcrRdfTools.getJcrNamespaceForRDFNamespace;
import static org.fcrepo.kernel.modeshape.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Function;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.utils.CacheEntry;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.modeshape.testutilities.TestPropertyIterator;
import org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.modeshape.jcr.value.BinaryValue;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

/**
 * <p>JcrRdfToolsTest class.</p>
 *
 * @author awoods
 * @author ajs6f
 */
public class JcrRdfToolsTest implements FedoraTypes {

    private DefaultIdentifierTranslator testSubjects;

    private JcrRdfTools testObj;

    /*
     * Also see enormous list of mock fields at bottom.
     */

    @Before
    public final void setUp() throws RepositoryException {
        initMocks(this);
        testSubjects = new DefaultIdentifierTranslator(mockSession);
        buildMockNodeAndSurroundings();
        testObj = new JcrRdfTools(testSubjects, mockSession);
    }

    private void buildMockNodeAndSurroundings() throws RepositoryException {
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getRepository()).thenReturn(mockRepository);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNsRegistry);
        when(mockNsRegistry.isRegisteredUri("some:")).thenReturn(true);
        when(mockNsRegistry.getPrefix("some:")).thenReturn("some");
        when(mockSession.getRootNode()).thenReturn(mockRootNode);
        when(mockParent.getPath()).thenReturn("/test");
        when(mockParent.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getParent()).thenReturn(mockParent);
        when(mockNode.getNodes()).thenReturn(mockNodes);
        when(mockNodes.hasNext()).thenReturn(false);
        when(mockNodes.next()).thenThrow(new NoSuchElementException());
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getMixinNodeTypes()).thenReturn(
                new NodeType[] {mockNodeType});
        when(mockNodeType.getName()).thenReturn("jcr:someType");
        when(mockNodeType.getChildNodeDefinitions()).thenReturn(
                new NodeDefinition[] {mock(NodeDefinition.class)});
        when(mockNode.getProperties()).thenReturn(
                new TestPropertyIterator(mockProperty),
                new TestPropertyIterator(mockProperty),
                new TestPropertyIterator(mockProperty));
        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockProperty.getParent()).thenReturn(mockNode);
        when(mockProperty.getName()).thenReturn("some:property");
        when(mockProperty.getValue()).thenReturn(mockValue);
        when(mockProperty.getType()).thenReturn(STRING);
        when(mockProperty.getParent()).thenReturn(mockNode);
        when(mockNode.isNodeType(FROZEN_NODE)).thenReturn(false);
        when(mockProperty.getDefinition()).thenReturn(mockPropertyDefinition);
        when(mockPropertyDefinition.isProtected()).thenReturn(false);
        when(mockValue.getString()).thenReturn("abc");
        when(mockParent.getProperties()).thenReturn(mockParentProperties);
        when(mockParentProperties.hasNext()).thenReturn(false);
        when(mockNodeType.getSupertypes()).thenReturn(new NodeType[] {mockNodeType});
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);
        when(mockHashNode.getName()).thenReturn("#");
        when(mockFedoraResource.getNode()).thenReturn(mockNode);
    }

    @Test
    public final void shouldMapReferenceValuesToJcrPropertyValues()
        throws RepositoryException {

        final RDFNode n = testSubjects.toDomain("/abc");

        // node references
        when(mockSession.getNode("/abc")).thenReturn(mockNode);
        when(mockSession.nodeExists("/abc")).thenReturn(true);
        testObj.createValue(mockValueFactory, n, REFERENCE);
        verify(mockValueFactory).createValue(mockNode, false);
        testObj.createValue(mockValueFactory, n, WEAKREFERENCE);
        verify(mockValueFactory).createValue(mockNode, true);
    }

    @Test
    public final void shouldMapValuesIntoExistingIntoJcrPropertyTypes()
            throws RepositoryException {

        final RDFNode n = createTypedLiteral(0);

        testObj.createValue(mockValueFactory, n, LONG);
        verify(mockValueFactory).createValue("0", LONG);

        final RDFNode resource = createResource("info:xyz");

        testObj.createValue(mockValueFactory, resource, URI);
        verify(mockValueFactory).createValue("info:xyz", URI);
    }

    @Test(expected = ValueFormatException.class)
    public final void shouldMapRdfValuesToJcrPropertyValuesError()
            throws RepositoryException {

        // non-uri references - error
        final RDFNode n = createResource();
        testObj.createValue(mockValueFactory, n, REFERENCE);
    }

    @Test
    public void shouldAddReferencePropertyForDomainObject() throws RepositoryException {
        when(mockNodeType.getPropertyDefinitions()).thenReturn(new PropertyDefinition[]{});
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});

        when(mockValueFactory.createValue(anyString(), eq(STRING))).thenReturn(mockValue);
        when(mockValueFactory.createValue(mockNode, true)).thenReturn(mockReferenceValue);

        when(mockSession.getNode("/x")).thenReturn(mockNode);

        when(mockNode.setProperty(anyString(), any(Value[].class), anyInt())).thenReturn(mockProperty);

        when(mockNode.getIdentifier()).thenReturn(UUID.randomUUID().toString());

        testObj.addProperty(mockFedoraResource,
                createProperty("some:property"),
                testSubjects.toDomain("x"),
                Collections.<String,String>emptyMap());

        verify(mockNode).setProperty("some:property_ref", new Value[]{mockReferenceValue}, mockReferenceValue.getType
                ());
    }

    @Test
    public void shouldNotAddReferencePropertyForNonDomainObject() throws RepositoryException {
        when(mockNodeType.getPropertyDefinitions()).thenReturn(new PropertyDefinition[]{});
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});

        when(mockValueFactory.createValue(anyString(), eq(STRING))).thenReturn(mockValue);
        when(mockValueFactory.createValue(mockNode, true)).thenReturn(mockReferenceValue);

        when(mockSession.getNode("/x")).thenReturn(mockNode);

        when(mockNode.setProperty(anyString(), any(Value[].class), anyInt())).thenReturn(mockProperty);

        testObj.addProperty(mockFedoraResource,
                createProperty("some:property"),
                createResource("some:resource"),
                Collections.<String,String>emptyMap());

        verify(mockNode, never()).setProperty(eq("some:property_ref"), any(Value[].class), anyInt());
    }

    @Test
    public void testCreateValueForNode() throws RepositoryException {
        when(mockNode.getSession().getValueFactory()).thenReturn(mockValueFactory);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getPropertyDefinitions()).thenReturn(new PropertyDefinition[]{});
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});

        when(mockValueFactory.createValue(anyString(), eq(STRING))).thenReturn(mockValue);
        final RDFNode n = createPlainLiteral("x");
        testObj.createValue(mockNode, n, "some:property");
        verify(mockValueFactory).createValue("x" + FIELD_DELIMITER + XSDstring.getURI(), STRING);
    }

    @Test
    public final void shouldMapInternalJcrNamespaceToFcrepoNamespace() {
        assertEquals(REPOSITORY_NAMESPACE,
                getRDFNamespaceForJcrNamespace("http://www.jcp.org/jcr/1.0"));
    }

    @Test
    public final void shouldMapFcrepoNamespaceToJcrNamespace() {
        assertEquals("http://www.jcp.org/jcr/1.0",
                getJcrNamespaceForRDFNamespace(REPOSITORY_NAMESPACE));
    }

    @Test
    public final void shouldPassThroughOtherNamespaceValues() {
        assertEquals("some-namespace-uri",
                getJcrNamespaceForRDFNamespace("some-namespace-uri"));
        assertEquals("some-namespace-uri",
                getRDFNamespaceForJcrNamespace("some-namespace-uri"));
    }

    @Test
    public void shouldPassthroughValidStatements() throws RepositoryException {
        final Model m = createDefaultModel();
        final Statement x = m.createStatement(testSubjects.toDomain("/"),
                createProperty("info:x"),
                createPlainLiteral("x"));
        final Statement statement = testObj.skolemize(testSubjects, x, "info:fedora/");

        assertEquals(x, statement);
    }

    @Test
    public void shouldSkolemizeBlankNodeSubjects() throws RepositoryException {
        final Model m = createDefaultModel();
        final Resource resource = createResource();
        final Statement x = m.createStatement(resource,
                createProperty("info:x"),
                testSubjects.toDomain("/"));
        testObj.jcrTools = mock(JcrTools.class);
        when(testObj.jcrTools.findOrCreateNode(eq(mockSession), anyString(), eq(NT_FOLDER))).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn("/#/x");
        when(mockNode.getParent()).thenReturn(mockHashNode);
        when(mockHashNode.getParent()).thenReturn(mockChildNode);
        when(mockHashNode.isNew()).thenReturn(true);
        when(FedoraTypesUtils.getClosestExistingAncestor(mockSession, anyString())).thenReturn(mockHashNode);
        final Statement statement = testObj.skolemize(testSubjects, x, "info:fedora/");

        assertTrue("Doesn't match: " + statement.getSubject().toString(),
                statement.getSubject().toString().startsWith("info:fedora/#"));
        verify(mockNode).addMixin(FEDORA_RESOURCE);
    }

    @Test
    public void shouldSkolemizeBlankNodeObjects() throws RepositoryException {
        final Model m = createDefaultModel();
        final Statement x = m.createStatement(testSubjects.toDomain("/foo"),
                createProperty("info:x"),
                createResource());
        testObj.jcrTools = mock(JcrTools.class);
        when(testObj.jcrTools.findOrCreateNode(eq(mockSession), anyString(), eq(NT_FOLDER))).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn("/foo#abc");
        when(mockNode.getParent()).thenReturn(mockHashNode);
        when(mockHashNode.getParent()).thenReturn(mockChildNode);
        when(mockHashNode.isNew()).thenReturn(true);
        when(FedoraTypesUtils.getClosestExistingAncestor(mockSession, anyString())).thenReturn(mockHashNode);
        final Statement statement = testObj.skolemize(testSubjects, x, x.getSubject().toString());

        assertTrue(statement.getObject().toString().startsWith("info:fedora/foo#"));
        verify(mockNode).addMixin(FEDORA_RESOURCE);
        verify(mockNode.getParent()).addMixin(FEDORA_PAIRTREE);
    }

    @Test
    public void shouldSkolemizeBlankNodeSubjectsAndObjects() throws RepositoryException {
        final Model m = createDefaultModel();
        final Resource resource = createResource();
        final Statement x = m.createStatement(resource,
                createProperty("info:x"),
                resource);
        testObj.jcrTools = mock(JcrTools.class);
        when(testObj.jcrTools.findOrCreateNode(eq(mockSession), anyString(), eq(NT_FOLDER))).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn("/#/x");
        when(mockNode.getParent()).thenReturn(mockHashNode);
        when(mockHashNode.getParent()).thenReturn(mockChildNode);
        when(FedoraTypesUtils.getClosestExistingAncestor(mockSession, anyString())).thenReturn(mockHashNode);
        final Statement statement = testObj.skolemize(testSubjects, x, "info:fedora/");

        assertTrue(statement.getSubject().toString().startsWith("info:fedora/#"));
        assertTrue(statement.getObject().toString().startsWith("info:fedora/#"));
    }

    @Test
    public void shouldCreateHashUriSubjects() throws RepositoryException {
        final Model m = createDefaultModel();
        final Statement x = m.createStatement(testSubjects.toDomain("/some/#/abc"),
                createProperty("info:x"),
                testSubjects.toDomain("/"));
        testObj.jcrTools = mock(JcrTools.class);
        when(mockNode.getParent()).thenReturn(mockHashNode);
        when(mockHashNode.getParent()).thenReturn(mockChildNode);
        when(mockSession.nodeExists("/some")).thenReturn(true);
        when(mockSession.getNode("/some")).thenReturn(mockChildNode);
        when(mockChildNode.isNew()).thenReturn(false);
        when(testObj.jcrTools.findOrCreateNode(mockSession, "/some/#/abc", NT_FOLDER)).thenReturn(mockNode);
        when(mockHashNode.isNew()).thenReturn(true);
        final Statement statement = testObj.skolemize(testSubjects, x, "/some/#/abc");
        assertEquals(x, statement);
        verify(testObj.jcrTools).findOrCreateNode(mockSession, "/some/#/abc", NT_FOLDER);
        verify(mockNode).addMixin(FEDORA_RESOURCE);
        verify(mockHashNode).addMixin(FEDORA_PAIRTREE);
    }

    @Test
    public void shouldCreateHashUriSubjectsWithExistingHashUri() throws RepositoryException {
        final Model m = createDefaultModel();
        final Statement x = m.createStatement(testSubjects.toDomain("/some/#/abc"),
                createProperty("info:x"),
                testSubjects.toDomain("/"));
        testObj.jcrTools = mock(JcrTools.class);
        when(mockNode.getParent()).thenReturn(mockHashNode);
        when(mockHashNode.getParent()).thenReturn(mockChildNode);
        when(mockSession.nodeExists("/some")).thenReturn(true);
        when(mockSession.getNode("/some")).thenReturn(mockChildNode);
        when(mockChildNode.isNew()).thenReturn(false);
        when(mockChildNode.hasNode("#")).thenReturn(true);
        when(mockChildNode.getNode("#")).thenReturn(mockHashNode);
        when(mockHashNode.isNew()).thenReturn(false);
        when(testObj.jcrTools.findOrCreateNode(mockSession, "/some/#/abc", NT_FOLDER)).thenReturn(mockNode);
        final Statement statement = testObj.skolemize(testSubjects, x, "/some/#/abc");
        assertEquals(x, statement);
        verify(testObj.jcrTools).findOrCreateNode(mockSession, "/some/#/abc", NT_FOLDER);
        verify(mockNode).addMixin(FEDORA_RESOURCE);
    }

    @Test(expected = PathNotFoundException.class)
    public void shouldNotAllowHashUriSubjectsForResourcesThatDontExist() throws RepositoryException {
        final Model m = createDefaultModel();
        final Statement x = m.createStatement(testSubjects.toDomain("/some/#/abc"),
                createProperty("info:x"),
                testSubjects.toDomain("/"));
        testObj.jcrTools = mock(JcrTools.class);
        when(mockNode.getParent()).thenReturn(mockHashNode);
        when(mockHashNode.getParent()).thenReturn(mockChildNode);
        when(mockSession.nodeExists("/some")).thenReturn(false);
        when(testObj.jcrTools.findOrCreateNode(mockSession, "/some/#/abc", NT_FOLDER)).thenReturn(mockNode);
        when(FedoraTypesUtils.getClosestExistingAncestor(mockSession,"/some/#/abc"))
                .thenReturn(mockNode);
        testObj.skolemize(testSubjects, x, "/some");
    }

    @Test
    public void shouldCreateHashUriObjects() throws RepositoryException {
        final Model m = createDefaultModel();
        final Statement x = m.createStatement(
                testSubjects.toDomain("/"),
                createProperty("info:x"),
                testSubjects.toDomain("/some/#/abc"));
        testObj.jcrTools = mock(JcrTools.class);
        when(mockNode.getParent()).thenReturn(mockHashNode);
        when(mockHashNode.getParent()).thenReturn(mockChildNode);
        when(mockSession.nodeExists("/some")).thenReturn(true);
        when(mockSession.getNode("/some")).thenReturn(mockChildNode);
        when(testObj.jcrTools.findOrCreateNode(mockSession, "/some/#/abc", NT_FOLDER)).thenReturn(mockNode);
        final Statement statement = testObj.skolemize(testSubjects, x, "/");
        assertEquals(x, statement);
        verify(testObj.jcrTools).findOrCreateNode(mockSession, "/some/#/abc", NT_FOLDER);
        verify(mockNode).addMixin(FEDORA_RESOURCE);
    }

    @Test
    public void shouldIgnoreHashUrisOutsideTheRepositoryDomain() throws RepositoryException {
        final Model m = createDefaultModel();

        final Statement x = m.createStatement(
                testSubjects.toDomain("/"),
                createProperty("info:x"),
                createResource("info:x#abc"));
        final Statement statement = testObj.skolemize(testSubjects, x, "/");
        assertEquals(x, statement);
    }

    @Mock
    private Property mockPredicate;

    @Mock
    private Function<javax.jcr.Property, org.apache.jena.rdf.model.Property> mockPredicateFactoryFunc;

    @Mock
    private NodeIterator mockNodes;

    @Mock
    private NodeIterator mockNodes2;

    @Mock
    private NodeIterator mockNodes3;

    @Mock
    private Function<Node, ValueFactory> mockValueFactoryFunc;

    @Mock
    private Node mockNode;

    @Mock
    private Node mockNode1;

    @Mock
    private Node mockNode2;

    @Mock
    private Node mockNode3;

    @Mock
    private Node mockParent;

    @Mock
    private Resource mockSubject;

    @Mock
    private Resource mockResource;

    @Mock
    private Session mockSession;

    @Mock
    private NodeType mockNodeType;

    @Mock
    private javax.jcr.Property mockProperty;

    @Mock
    private PropertyDefinition mockPropertyDefinition;

    @Mock
    private Value mockValue;

    @Mock
    private Value mockReferenceValue;

    @Mock
    private Repository mockRepository;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private PropertyIterator mockProperties;

    @Mock
    private PropertyIterator mockProperties2;

    @Mock
    private PropertyIterator mockParentProperties;

    @Mock
    private Node mockContentNode;

    @Mock
    private Version mockVersion;

    @Mock
    private Node mockFrozenNode;

    @Mock
    private VersionManager mockVersionManager;

    @Mock
    private VersionIterator mockVersionIterator;

    @Mock
    private VersionHistory mockVersionHistory;

    @Mock
    private BinaryValue mockBinary;

    @Mock
    private CacheEntry mockCacheEntry;

    @Mock
    private QueryManager mockQueryManager;

    @Mock
    private Query mockQuery;

    @Mock
    private ValueFactory mockValueFactory;

    @Mock
    private QueryResult mockQueryResult;

    @Mock
    private RowIterator mockRowIterator;

    @Mock
    private MetricRegistry mockMetrics;

    @Mock
    private Node mockChildNode, mockChildNode2, mockChildNode3, mockChildNode4, mockChildNode5, mockFullChildNode,
            mockRootNode, mockHashNode;

    @Mock
    private Counter mockCounter;

    @Mock
    private NodeTypeManager mockNodeTypeManager;

    @Mock
    private NodeTypeIterator mockNodeTypeIterator;

    @Mock
    private NodeType mockMixinNodeType, mockPrimaryNodeType;

    @Mock
    private NamespaceRegistry mockNsRegistry;

    @Mock
    private FedoraResourceImpl mockFedoraResource;

}
