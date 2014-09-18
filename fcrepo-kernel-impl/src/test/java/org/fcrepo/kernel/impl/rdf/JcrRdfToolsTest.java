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
package org.fcrepo.kernel.impl.rdf;

import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDbyte;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDlong;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDshort;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static java.util.Arrays.asList;
import static javax.jcr.PropertyType.NAME;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.STRING;
import static javax.jcr.PropertyType.UNDEFINED;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_RESULT;
import static org.fcrepo.kernel.RdfLexicon.HAS_MEMBER_OF_RESULT;
import static org.fcrepo.kernel.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.RdfLexicon.HAS_NAMESPACE_PREFIX;
import static org.fcrepo.kernel.RdfLexicon.HAS_NAMESPACE_URI;
import static org.fcrepo.kernel.RdfLexicon.HAS_SIZE;
import static org.fcrepo.kernel.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.impl.rdf.JcrRdfTools.getJcrNamespaceForRDFNamespace;
import static org.fcrepo.kernel.impl.rdf.JcrRdfTools.getPredicateForProperty;
import static org.fcrepo.kernel.impl.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator.RESOURCE_NAMESPACE;
import static org.fcrepo.kernel.impl.utils.NodePropertiesTools.getReferencePropertyName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
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

import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.impl.testutilities.TestPropertyIterator;
import org.fcrepo.kernel.impl.utils.FixityResultImpl;
import org.fcrepo.kernel.impl.utils.JcrPropertyMock;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.utils.CacheEntry;
import org.fcrepo.kernel.utils.FixityResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.modeshape.jcr.value.BinaryValue;
import org.slf4j.Logger;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Function;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * <p>JcrRdfToolsTest class.</p>
 *
 * @author awoods
 */
public class JcrRdfToolsTest implements FedoraJcrTypes {

    private static final Logger LOGGER = getLogger(JcrRdfToolsTest.class);

    private IdentifierTranslator testSubjects;

    private JcrRdfTools testObj;

    private static final String mockPredicateName =
        "http://example.com#someProperty";

    private static final String mockUri = "http://example.com/";

    /*
     * Also see enormous list of mock fields at bottom.
     */

    @Before
    public final void setUp() throws RepositoryException {
        initMocks(this);
        testSubjects = new DefaultIdentifierTranslator();
        testObj = new JcrRdfTools(testSubjects, mockSession);
        buildMockNodeAndSurroundings();
    }

    private void buildMockNodeAndSurroundings() throws RepositoryException {
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getRepository()).thenReturn(mockRepository);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        mockNamespaceRegistry();
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNsRegistry);
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
        when(mockProperty.getName()).thenReturn(mockPredicateName);
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
    }

    @Test
    public final void shouldMapRdfValuesToJcrPropertyValues()
        throws RepositoryException {

        when(mockNode.getSession().getValueFactory()).thenReturn(
                mockValueFactory);

        RDFNode n = createResource(RESOURCE_NAMESPACE + "abc");

        // node references
        when(mockSession.getNode("/abc")).thenReturn(mockNode);
        when(mockSession.nodeExists("/abc")).thenReturn(true);
        testObj.createValue(mockNode, n, REFERENCE);
        verify(mockValueFactory).createValue(mockNode, false);
        testObj.createValue(mockNode, n, WEAKREFERENCE);
        verify(mockValueFactory).createValue(mockNode, true);

        // uris
        testObj.createValue(mockNode, n, UNDEFINED);
        verify(mockValueFactory).createValue(RESOURCE_NAMESPACE + "abc",
                PropertyType.URI);

        // other random resources
        n = createResource();
        testObj.createValue(mockNode, n, 0);
        verify(mockValueFactory).createValue(n.toString(), UNDEFINED);

        // undeclared types, but infer them from rdf types

        n = createTypedLiteral(true);
        testObj.createValue(mockNode, n, 0);
        verify(mockValueFactory).createValue(true);

        n = createTypedLiteral("1", XSDbyte);
        testObj.createValue(mockNode, n, 0);
        verify(mockValueFactory).createValue((byte) 1);

        n = createTypedLiteral((double) 2);
        testObj.createValue(mockNode, n, 0);
        verify(mockValueFactory).createValue((double) 2);

        n = createTypedLiteral((float) 3);
        testObj.createValue(mockNode, n, 0);
        verify(mockValueFactory).createValue((float) 3);

        n = createTypedLiteral(4);
        testObj.createValue(mockNode, n, 0);
        verify(mockValueFactory).createValue(4);

        n = createTypedLiteral("5", XSDlong);
        testObj.createValue(mockNode, n, 0);
        verify(mockValueFactory).createValue(5);

        n = createTypedLiteral("6", XSDshort);
        testObj.createValue(mockNode, n, 0);
        verify(mockValueFactory).createValue((short) 6);

        final Calendar calendar = Calendar.getInstance();
        n = createTypedLiteral(calendar);
        testObj.createValue(mockNode, n, 0);
        verify(mockValueFactory).createValue(any(Calendar.class));

        n = createTypedLiteral("string");
        testObj.createValue(mockNode, n, 0);
        verify(mockValueFactory).createValue("string", STRING);

        n = createTypedLiteral("string");
        testObj.createValue(mockNode, n, NAME);
        verify(mockValueFactory).createValue("string", NAME);

    }

    @Test(expected = ValueFormatException.class)
    public final void shouldMapRdfValuesToJcrPropertyValuesError()
            throws RepositoryException {
        when(mockNode.getSession().getValueFactory()).thenReturn(mockValueFactory);

        // non-uri references - error
        final RDFNode n = createResource();
        testObj.createValue(mockNode, n, REFERENCE);
    }

    @Test
    public final void testJcrNodeIteratorModel() throws RepositoryException {
        when(mockNodes.hasNext()).thenReturn(false);
        final Model model =
            testObj.getJcrPropertiesModel(
                    new org.fcrepo.kernel.utils.iterators.NodeIterator(
                            mockNodes), mockResource).asModel();
        assertNotNull(model);
    }

    @Test
    public final void testJcrNodeIteratorAddsPredicatesForEachNode()
        throws RepositoryException {
        final Resource mockResource =
            createResource(RESOURCE_NAMESPACE + "search/resource");
        when(mockProperties.hasNext()).thenReturn(false);
        when(mockNode1.getProperties()).thenReturn(mockProperties);
        when(mockNode1.getSession()).thenReturn(mockSession);
        when(mockNode2.getSession()).thenReturn(mockSession);
        when(mockNode3.getSession()).thenReturn(mockSession);
        when(mockNode1.getPath()).thenReturn("/path/to/first/node");
        when(mockNode2.getPath()).thenReturn("/second/path/to/node");
        when(mockNode3.getPath()).thenReturn("/third/path/to/node");
        when(mockNode1.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode2.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode3.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode1.getMixinNodeTypes()).thenReturn(emptyNodeTypes);
        when(mockNode2.getMixinNodeTypes()).thenReturn(emptyNodeTypes);
        when(mockNode3.getMixinNodeTypes()).thenReturn(emptyNodeTypes);

        when(mockNode1.getProperties()).thenReturn(mockProperties);
        when(mockNode2.getProperties()).thenReturn(mockProperties);
        when(mockNode3.getProperties()).thenReturn(mockProperties);

        final Iterator<Node> mockIterator =
            asList(mockNode1, mockNode2, mockNode3).iterator();
        final Model model =
            testObj.getJcrPropertiesModel(mockIterator, mockResource).asModel();
        assertEquals(3, model.listObjectsOfProperty(HAS_MEMBER_OF_RESULT)
                .toSet().size());
    }

    @Test
    public final void testGetFixityResultsModel() throws RepositoryException,
                                                 URISyntaxException,
                                                 IOException {
        when(mockCacheEntry.getExternalIdentifier()).thenReturn("http://xyz");
        final String testFixityUri = "http://abc";
        final FixityResult mockResult =
            new FixityResultImpl(mockCacheEntry, 123, new URI(testFixityUri));

        final List<FixityResult> mockBlobs = asList(mockResult);

        final Model fixityResultsModel =
            testObj.getJcrTriples(mockNode, mockBlobs, new URI(testFixityUri), 0L).asModel();

        logRDF(fixityResultsModel);
        assertTrue(fixityResultsModel.contains(createResource(RESOURCE_NAMESPACE + "test/jcr"),
                                                  HAS_FIXITY_RESULT,
                                                  (RDFNode)null));
        assertTrue(fixityResultsModel.contains(null, HAS_MESSAGE_DIGEST,
                createResource(testFixityUri)));
        assertTrue(fixityResultsModel.contains(null, HAS_SIZE,
                createTypedLiteral(123)));

    }

    @Test
    public final void testGetJcrNamespaceModel() throws Exception {
        final Model jcrNamespaceModel = testObj.getNamespaceTriples().asModel();
        assertTrue(jcrNamespaceModel.contains(
                createResource(REPOSITORY_NAMESPACE), HAS_NAMESPACE_PREFIX,
                "fcrepo"));

        final Resource nsSubject = createResource(mockUri);
        assertTrue(jcrNamespaceModel.contains(nsSubject, RDF.type,
                RdfLexicon.VOAF_VOCABULARY));
        assertTrue(jcrNamespaceModel.contains(nsSubject, HAS_NAMESPACE_PREFIX,
                "some-prefix"));

        assertTrue(jcrNamespaceModel.contains(nsSubject, HAS_NAMESPACE_URI,
                mockUri));
    }

    @Test
    public final void testIsInternalProperty() {
        assertTrue(testObj.isInternalProperty(mockNode, createProperty(
                REPOSITORY_NAMESPACE, "some-property")));
        assertTrue(testObj.isInternalProperty(mockNode, createProperty(
                "http://www.jcp.org/jcr/1.0", "some-property")));
        assertTrue(testObj.isInternalProperty(mockNode,
                createProperty(LDP_NAMESPACE + "some-property")));
        assertFalse(testObj
                .isInternalProperty(
                        mockNode,
                        createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#label")));
        assertFalse(testObj.isInternalProperty(mockNode, createProperty(
                "my-own-ns", "some-property")));
    }

    private void mockNamespaceRegistry() throws RepositoryException {

        when(mockNsRegistry.isRegisteredUri(mockUri)).thenReturn(true);
        when(mockNsRegistry.isRegisteredUri("not-registered-uri#")).thenReturn(
                false);
        when(mockNsRegistry.isRegisteredUri("http://www.jcp.org/jcr/1.0"))
                .thenReturn(true);
        when(mockNsRegistry.getPrefix("http://www.jcp.org/jcr/1.0"))
                .thenReturn("jcr");
        when(mockNsRegistry.getPrefix(mockUri)).thenReturn("some-prefix");
        when(mockNsRegistry.getURI("jcr")).thenReturn(
                "http://www.jcp.org/jcr/1.0");
        when(mockNsRegistry.getURI("some-prefix")).thenReturn(mockUri);
        when(mockNsRegistry.getPrefixes()).thenReturn(
                new String[] {"jcr", "some-prefix"});

        when(mockSession.getWorkspace().getNamespaceRegistry()).thenReturn(
                mockNsRegistry);
        when(mockNode.getSession().getWorkspace().getNamespaceRegistry())
                .thenReturn(mockNsRegistry);
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
    public final void
            shouldMapRdfPredicatesToJcrProperties() throws RepositoryException {

        final Property p = createProperty(REPOSITORY_NAMESPACE, "uuid");
        assertEquals("jcr:uuid", testObj.getPropertyNameFromPredicate(mockNode,
                p));

    }

    @Test
    public final void
            shouldReuseRegisteredNamespaces() throws RepositoryException {
        final Property p = createProperty(mockUri, "uuid");
        assertEquals("some-prefix:uuid", testObj.getPropertyNameFromPredicate(
                mockNode, p));
    }

    @Test
    public final void shouldRegisterUnknownUris() throws RepositoryException {
        when(mockNsRegistry.registerNamespace("not-registered-uri#"))
                .thenReturn("ns001");
        final Property p = createProperty("not-registered-uri#", "uuid");
        assertEquals("ns001:uuid", testObj.getPropertyNameFromPredicate(
                mockNode, p));
    }

    @Test
    public final void shouldMapInternalReferencePropertiesToPublicUris() throws RepositoryException {
        when(mockNamespacedProperty.getNamespaceURI()).thenReturn("info:xyz#");
        when(mockNamespacedProperty.getLocalName()).thenReturn(getReferencePropertyName("some_reference"));
        when(mockNamespacedProperty.getType()).thenReturn(REFERENCE);
        when(mockNamespacedProperty.getName()).thenReturn("xyz:" + getReferencePropertyName("some_reference"));
        final Property property = getPredicateForProperty.apply(mockNamespacedProperty);

        assert(property != null);
        assertEquals("some_reference", property.getLocalName());

    }

    private static void logRDF(final Model rdf) throws IOException {
        try (final Writer writer = new StringWriter()) {
            rdf.write(writer);
            LOGGER.debug("Found model: {}", writer);
        }
    }

    private static final NodeType[] emptyNodeTypes = new NodeType[] {};

    @Mock
    private Property mockPredicate;

    @Mock
    private Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> mockPredicateFactoryFunc;

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
    private NamespaceRegistry mockNsRegistry;

    @Mock
    private IdentifierTranslator mockFactory;

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
    private Node mockChildNode;

    @Mock
    private Node mockChildNode2;

    @Mock
    private Node mockChildNode3;

    @Mock
    private Node mockChildNode4;

    @Mock
    private Node mockChildNode5;

    @Mock
    private Node mockFullChildNode;

    @Mock
    private Counter mockCounter;

    @Mock
    private NodeTypeManager mockNodeTypeManager;

    @Mock
    private NodeTypeIterator mockNodeTypeIterator;

    @Mock
    private NodeType mockMixinNodeType;

    @Mock
    private NodeType mockPrimaryNodeType;

    @Mock
    private JcrPropertyMock mockNamespacedProperty;

}
