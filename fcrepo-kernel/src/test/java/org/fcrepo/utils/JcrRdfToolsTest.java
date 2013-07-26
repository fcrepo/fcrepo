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

import static com.google.common.collect.ImmutableSet.of;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDbyte;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDlong;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDshort;
import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static java.util.Arrays.asList;
import static javax.jcr.PropertyType.BOOLEAN;
import static javax.jcr.PropertyType.DATE;
import static javax.jcr.PropertyType.DECIMAL;
import static javax.jcr.PropertyType.DOUBLE;
import static javax.jcr.PropertyType.LONG;
import static javax.jcr.PropertyType.NAME;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.STRING;
import static javax.jcr.PropertyType.UNDEFINED;
import static javax.jcr.PropertyType.URI;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static javax.jcr.query.Query.JCR_SQL2;
import static org.fcrepo.RdfLexicon.HAS_COMPUTED_CHECKSUM;
import static org.fcrepo.RdfLexicon.HAS_COMPUTED_SIZE;
import static org.fcrepo.RdfLexicon.HAS_CONTENT;
import static org.fcrepo.RdfLexicon.HAS_LOCATION;
import static org.fcrepo.RdfLexicon.HAS_MEMBER_OF_RESULT;
import static org.fcrepo.RdfLexicon.HAS_NAMESPACE_PREFIX;
import static org.fcrepo.RdfLexicon.HAS_NAMESPACE_URI;
import static org.fcrepo.RdfLexicon.HAS_VERSION;
import static org.fcrepo.RdfLexicon.HAS_VERSION_LABEL;
import static org.fcrepo.RdfLexicon.IS_FIXITY_RESULT_OF;
import static org.fcrepo.utils.FedoraJcrTypes.ROOT;
import static org.fcrepo.utils.FedoraTypesUtils.getPredicateForProperty;
import static org.fcrepo.utils.FedoraTypesUtils.getValueFactory;
import static org.fcrepo.utils.FixityResult.FixityState.BAD_CHECKSUM;
import static org.fcrepo.utils.FixityResult.FixityState.BAD_SIZE;
import static org.fcrepo.utils.JcrRdfTools.getJcrNamespaceForRDFNamespace;
import static org.fcrepo.utils.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.fcrepo.utils.JcrRdfTools.setGetClusterConfiguration;
import static org.fcrepo.utils.JcrRdfTools.setLlstore;
import static org.fcrepo.utils.NamespaceTools.getNamespaceRegistry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.slf4j.LoggerFactory.getLogger;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.fcrepo.RdfLexicon;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.rdf.impl.DefaultGraphSubjects;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.services.functions.GetClusterConfiguration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.slf4j.Logger;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.vocabulary.RDF;

public class JcrRdfToolsTest {

    private static final Logger LOGGER = getLogger(JcrRdfToolsTest.class);

    private GraphSubjects testSubjects;

    @Mock
    private Property mockPredicate;

    @Mock
    private Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> mockPredicateFactoryFunc;

    @Mock
    private NodeIterator mockNodes;

    @Mock
    private Function<Node, ValueFactory> mockValueFactoryFunc;

    @Mock
    private Node mockNode;

    @Mock
    private Node mockParent;

    @Mock
    private NamespaceRegistry mockNsRegistry;

    @Mock
    private GraphSubjects mockFactory;

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
    private Value mockValue;

    @Mock
    private Repository mockRepository;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private PropertyIterator mockProperties;

    @Mock
    private PropertyIterator mockParentProperties;

    @Mock
    private LowLevelStorageService mockLowLevelStorageService;

    @Mock
    private Node mockNodeContent;

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
    private LowLevelCacheEntry mockCacheEntry;

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
    private Node mockContentNode;

    @Mock
    private Node mockFullChildNode;

    @Mock
    private Counter mockCounter;

    @Mock
    private GetClusterConfiguration mockGetClusterConfiguration;

    @Mock
    private NodeTypeManager mockNodeTypeManager;

    @Mock
    private NodeTypeIterator mockNodeTypeIterator;
    private JcrRdfTools testObj;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        testSubjects = new DefaultGraphSubjects();
        testObj = new JcrRdfTools(testSubjects, mockSession);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getRepository()).thenReturn(mockRepository);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        mockNamespaceRegistry();
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNsRegistry);

    }

    @Test
    public void shouldMapInternalJcrNamespaceToFcrepoNamespace() {
        assertEquals("info:fedora/fedora-system:def/internal#",
                getRDFNamespaceForJcrNamespace("http://www.jcp.org/jcr/1.0"));
    }

    @Test
    public void shouldMapFcrepoNamespaceToJcrNamespace() {
        assertEquals(
                "http://www.jcp.org/jcr/1.0",
                getJcrNamespaceForRDFNamespace("info:fedora/fedora-system:def/internal#"));
    }

    @Test
    public void shouldPassThroughOtherNamespaceValues() {
        assertEquals("some-namespace-uri",
                getJcrNamespaceForRDFNamespace("some-namespace-uri"));
        assertEquals("some-namespace-uri",
                getRDFNamespaceForJcrNamespace("some-namespace-uri"));
    }

    @Test
    public void shouldMapRdfPredicatesToJcrProperties()
            throws RepositoryException {

        final Property p =
                createProperty("info:fedora/fedora-system:def/internal#",
                        "uuid");
        assertEquals("jcr:uuid", testObj.getPropertyNameFromPredicate(mockNode, p));

    }

    @Test
    public void shouldReuseRegisteredNamespaces() throws RepositoryException {
        final Property p = createProperty("registered-uri#", "uuid");
        assertEquals("some-prefix:uuid", testObj.getPropertyNameFromPredicate(mockNode,
                p));
    }

    @Test
    public void shouldRegisterUnknownUris() throws RepositoryException {
        when(mockNsRegistry.registerNamespace("not-registered-uri#"))
                .thenReturn("ns001");
        final Property p = createProperty("not-registered-uri#", "uuid");
        assertEquals("ns001:uuid", testObj.getPropertyNameFromPredicate(mockNode, p));
    }

    @Test
    public void testGetPropertiesModel() throws RepositoryException {
        when(mockParent.getPath()).thenReturn("/test");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getParent()).thenReturn(mockParent);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getPrimaryNodeType().getName()).thenReturn("");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getNodes()).thenReturn(mockNodes);
        when(mockNode.getProperties()).thenReturn(mockProperties);
        when(mockParent.getProperties()).thenReturn(mockParentProperties);
        when(mockProperties.hasNext()).thenReturn(true, false);
        when(mockParentProperties.hasNext()).thenReturn(true, false);
        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockProperty.getName()).thenReturn("xyz");
        when(mockProperty.getType()).thenReturn(0);
        when(mockValue.getString()).thenReturn("abc");
        when(mockProperty.getValue()).thenReturn(mockValue);
        when(mockProperties.nextProperty()).thenReturn(mockProperty);
        when(mockParentProperties.nextProperty()).thenReturn(mockProperty);

        final Model actual = testObj.getJcrPropertiesModel(mockNode);
        assertEquals("info:fedora/fedora-system:def/internal#", actual
                .getNsPrefixURI("fedora-internal"));
        assertTrue(actual.contains(testSubjects.getGraphSubject(mockNode),
                actual.getProperty("xyz"), actual.createLiteral("abc")));

    }

    @Test
    public void testGetPropertiesModelWithContent() throws RepositoryException {
        setLlstore(mockLowLevelStorageService);
        when(mockParent.getPath()).thenReturn("/test");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getParent()).thenReturn(mockParent);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getPrimaryNodeType().getName()).thenReturn("");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.hasNode(JCR_CONTENT)).thenReturn(true);
        when(mockNodeContent.getPath()).thenReturn("/test/jcr/jcr:content");
        when(mockBinary.getKey()).thenReturn(new BinaryKey("abc"));
        when(mockProperty.getBinary()).thenReturn(mockBinary);
        when(mockNodeContent.getProperty(JCR_DATA)).thenReturn(mockProperty);
        when(mockCacheEntry.getExternalIdentifier()).thenReturn("xyz");
        when(
                mockLowLevelStorageService
                        .getLowLevelCacheEntries(mockNodeContent)).thenReturn(
                of(mockCacheEntry));
        when(mockNode.getNode(JCR_CONTENT)).thenReturn(mockNodeContent);
        when(mockNode.getNodes()).thenReturn(mockNodes);
        when(mockNode.getProperties()).thenReturn(mockProperties);
        when(mockParent.getProperties()).thenReturn(mockProperties);
        when(mockNodeContent.getProperties()).thenReturn(mockProperties);
        when(mockProperties.hasNext()).thenReturn(false);

        final Model actual = testObj.getJcrPropertiesModel(mockNode);
        assertEquals("info:fedora/fedora-system:def/internal#", actual
                .getNsPrefixURI("fedora-internal"));
        assertTrue(actual.contains(testSubjects.getGraphSubject(mockNode),
                HAS_CONTENT, testSubjects.getGraphSubject(mockNodeContent)));
        assertTrue(actual.contains(testSubjects
                .getGraphSubject(mockNodeContent), HAS_LOCATION, actual
                .createLiteral("xyz")));
    }

    @Test
    public void testGetPropertiesModelForRootNode() throws RepositoryException {
        when(mockRepository.login()).thenReturn(mockSession);
        when(mockRowIterator.getSize()).thenReturn(0L);
        when(mockQueryResult.getRows()).thenReturn(mockRowIterator);
        when(mockQuery.execute()).thenReturn(mockQueryResult);
        when(mockQueryManager.createQuery(anyString(), eq(JCR_SQL2)))
                .thenReturn(mockQuery);
        when(mockWorkspace.getQueryManager()).thenReturn(mockQueryManager);
        when(mockMetrics.getCounters())
                .thenReturn(
                        ImmutableSortedMap
                                .of("org.fcrepo.services.LowLevelStorageService.fixity-check-counter",
                                        mockCounter,
                                        "org.fcrepo.services.LowLevelStorageService.fixity-error-counter",
                                        mockCounter,
                                        "org.fcrepo.services.LowLevelStorageService.fixity-repaired-counter",
                                        mockCounter

                                ));
        when(mockGetClusterConfiguration.apply(mockRepository)).thenReturn(
                ImmutableMap.of("a", "b"));
        setGetClusterConfiguration(mockGetClusterConfiguration);

        when(mockNode.getPath()).thenReturn("/");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getPrimaryNodeType().getName()).thenReturn(ROOT);
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getNodes()).thenReturn(mockNodes);
        when(mockNode.getProperties()).thenReturn(mockProperties);
        when(mockProperties.hasNext()).thenReturn(false);

        when(mockSession.getRepository()).thenReturn(mockRepository);
        when(mockRepository.getDescriptorKeys()).thenReturn(
                new String[] {"some-descriptor-key"});
        when(mockRepository.getDescriptor("some-descriptor-key")).thenReturn(
                "some-descriptor-value");
        when(mockNodeTypeIterator.hasNext()).thenReturn(false);
        when(mockNodeTypeManager.getAllNodeTypes()).thenReturn(
                mockNodeTypeIterator);
        when(mockWorkspace.getNodeTypeManager())
                .thenReturn(mockNodeTypeManager);

        final Model actual = testObj.getJcrPropertiesModel(mockNode);
        assertEquals("info:fedora/fedora-system:def/internal#", actual
                .getNsPrefixURI("fedora-internal"));

        assertTrue(actual
                .contains(
                        testSubjects.getGraphSubject(mockNode),
                        actual.createProperty("info:fedora/fedora-system:def/internal#repository/some-descriptor-key"),
                        actual.createLiteral("some-descriptor-value")));
        assertTrue(actual
                .contains(
                        testSubjects.getGraphSubject(mockNode),
                        actual.createProperty("info:fedora/fedora-system:def/internal#a"),
                        actual.createLiteral("b")));

        // assertTrue(actual.contains(testSubjects.getGraphSubject(mockNode),
        // RdfLexicon.HAS_FIXITY_CHECK_COUNT));
        // assertTrue(actual.contains(testSubjects.getGraphSubject(mockNode),
        // RdfLexicon.HAS_FIXITY_ERROR_COUNT));
        // assertTrue(actual.contains(testSubjects.getGraphSubject(mockNode),
        // RdfLexicon.HAS_FIXITY_REPAIRED_COUNT));
    }

    @Test
    public void shouldExcludeBinaryProperties() throws RepositoryException {
        when(mockParent.getPath()).thenReturn("/test");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getParent()).thenReturn(mockParent);
        when(mockNode.getDepth()).thenReturn(2);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getPrimaryNodeType().getName()).thenReturn(
                "fedora:object");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getProperties()).thenReturn(mockProperties);
        when(mockParent.getProperties()).thenReturn(mockProperties);
        when(mockProperties.hasNext()).thenReturn(true, false);
        when(mockProperty.getValue()).thenReturn(mockValue);
        when(mockValue.getType()).thenReturn(PropertyType.BINARY);
        when(mockProperties.nextProperty()).thenReturn(mockProperty);

        final Model actual = testObj.getJcrPropertiesModel(mockNode);
        assertEquals(0, actual.size());
    }

    @Test
    public void shouldBeAbleToDisableResourceInlining() throws RepositoryException {
        when(mockNode.getPath()).thenReturn("/test/jcr");

        final Model actual = testObj.getJcrTreeModel(mockNode, 0, -2);
        assertEquals(0, Iterators.size(actual.listObjectsOfProperty(actual.createProperty("http://www.w3.org/ns/ldp#inlinedResource"))));
        verify(mockParent, never()).getProperties();
        verify(mockNode, never()).getNodes();
    }

    @Test
    public void shouldIncludeParentNodeInformation() throws RepositoryException {
        when(mockParent.getPath()).thenReturn("/test");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getParent()).thenReturn(mockParent);

        when(mockParent.getProperties()).thenReturn(mockProperties);
        when(mockProperties.hasNext()).thenReturn(false);

        when(mockNode.getDepth()).thenReturn(2);
        when(mockNodes.hasNext()).thenReturn(false);
        when(mockNode.getNodes()).thenReturn(mockNodes);
        final Model actual = testObj.getJcrTreeModel(mockNode, 0, -1);
        assertEquals(1, Iterators.size(actual.listObjectsOfProperty(RdfLexicon.HAS_CHILD)));
    }

    @Test
    public void shouldIncludeChildNodeInformation() throws RepositoryException {
        when(mockParent.getPath()).thenReturn("/test");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getParent()).thenReturn(mockParent);

        when(mockParent.getProperties()).thenReturn(mockProperties);
        when(mockProperties.hasNext()).thenReturn(false);

        when(mockNode.getDepth()).thenReturn(0);
        when(mockChildNode.getName()).thenReturn("some-name");
        when(mockChildNode.getPath()).thenReturn("/test/jcr/1", "/test/jcr/2",
                "/test/jcr/3", "/test/jcr/4", "/test/jcr/5");
        when(mockNodes.hasNext()).thenReturn(true, true, true, true, true, true,
                false);
        when(mockNodes.nextNode()).thenReturn(mockChildNode);

        when(mockNode.getNodes()).thenReturn(mockNodes);
        final Model actual = testObj.getJcrTreeModel(mockNode, 0, 0);
        assertEquals(5, Iterators.size(actual.listObjectsOfProperty(RdfLexicon.HAS_CHILD)));
    }

    @Test
    public void shouldIncludeFullChildNodeInformationInsideWindow()
            throws RepositoryException {
        when(mockParent.getPath()).thenReturn("/test");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getParent()).thenReturn(mockParent);
        when(mockNode.getDepth()).thenReturn(0);
        when(mockChildNode.getName()).thenReturn("some-name");
        when(mockChildNode.getPath()).thenReturn("/test/jcr/1", "/test/jcr/4",
                "/test/jcr/5");
        when(mockFullChildNode.getName()).thenReturn("some-other-name");
        when(mockFullChildNode.getPath()).thenReturn("/test/jcr/2",
                "/test/jcr/3");
        when(mockFullChildNode.getProperties()).thenReturn(mockProperties);
        when(mockProperties.hasNext()).thenReturn(false);
        when(mockNodes.hasNext()).thenReturn(true, true, true, true, true,
                false);
        when(mockNodes.nextNode()).thenReturn(mockChildNode, mockFullChildNode,
                mockFullChildNode, mockChildNode, mockChildNode);
        when(mockNode.getNodes()).thenReturn(mockNodes);
        final Model actual = testObj.getJcrTreeModel(mockNode, 1, 2);
        assertEquals(2, Iterators.size(actual.listSubjectsWithProperty(RdfLexicon.HAS_PARENT)));
        verify(mockChildNode, never()).getProperties();
    }

    @Test
    public void shouldMapRdfValuesToJcrPropertyValues()
            throws RepositoryException {
        when(mockValueFactoryFunc.apply(mockNode)).thenReturn(mockValueFactory);
        final Function<Node, ValueFactory> holdValueFactory = getValueFactory;
        FedoraTypesUtils.getValueFactory = mockValueFactoryFunc;

        try {
            RDFNode n = createResource("info:fedora/abc");

            // node references
            when(mockSession.getNode("/abc")).thenReturn(mockNode);
            when(mockSession.nodeExists("/abc")).thenReturn(true);
            testObj.createValue(mockNode, n, REFERENCE);
            verify(mockValueFactory).createValue(mockNode, false);
            testObj.createValue(mockNode, n, WEAKREFERENCE);
            verify(mockValueFactory).createValue(mockNode, true);

            // uris
            testObj.createValue(mockNode, n, UNDEFINED);
            verify(mockValueFactory).createValue("info:fedora/abc",
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

        } finally {
            getValueFactory = holdValueFactory;
        }

    }

    @Test
    public void shouldAddPropertiesToModel() throws RepositoryException {
        when(mockPredicateFactoryFunc.apply(mockProperty)).thenReturn(
                mockPredicate);
        final Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> holdPredicate =
                getPredicateForProperty;
        getPredicateForProperty = mockPredicateFactoryFunc;

        try {
            final Resource mockSubject = mock(Resource.class);
            final Model mockModel = mock(Model.class);

            final Value mockValue = mock(Value.class);
            when(mockValue.getString()).thenReturn("");

            when(mockProperty.isMultiple()).thenReturn(false);
            when(mockProperty.getValue()).thenReturn(mockValue);

            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty);
            verify(mockModel).add(mockSubject, mockPredicate, "");

        } finally {
            getPredicateForProperty = holdPredicate;
        }

    }

    @Test
    public void shouldAddMultivaluedPropertiesToModel()
            throws RepositoryException {
        when(mockPredicateFactoryFunc.apply(mockProperty)).thenReturn(
                mockPredicate);
        final Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> holdPredicate =
                getPredicateForProperty;
        getPredicateForProperty = mockPredicateFactoryFunc;

        try {
            final Resource mockSubject = mock(Resource.class);
            final Model mockModel = mock(Model.class);

            final Value mockValue = mock(Value.class);
            when(mockValue.getString()).thenReturn("1");

            final Value mockValue2 = mock(Value.class);
            when(mockValue2.getString()).thenReturn("2");

            when(mockProperty.isMultiple()).thenReturn(true);
            when(mockProperty.getValues()).thenReturn(
                    asList(mockValue, mockValue2).toArray(new Value[2]));

            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty);
            verify(mockModel).add(mockSubject, mockPredicate, "1");
            verify(mockModel).add(mockSubject, mockPredicate, "2");

        } finally {
            getPredicateForProperty = holdPredicate;
        }

    }

    @Test
    public void shouldMapJcrTypesToRdfDataTypes() throws RepositoryException {
        final Resource mockSubject = createResource("some-resource-uri");
        final Model mockModel = createDefaultModel();
        final Property mockPredicate =
                mockModel.createProperty("some-predicate-uri");
        when(mockPredicateFactoryFunc.apply(mockProperty)).thenReturn(
                mockPredicate);

        final Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> holdPredicate =
                getPredicateForProperty;
        getPredicateForProperty = mockPredicateFactoryFunc;

        try {
            when(mockValue.getType()).thenReturn(BOOLEAN);
            when(mockValue.getBoolean()).thenReturn(true);
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createTypedLiteral(true)));

            mockValue = mock(Value.class);
            final Calendar mockCalendar = Calendar.getInstance();
            when(mockValue.getType()).thenReturn(DATE);
            when(mockValue.getDate()).thenReturn(mockCalendar);
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createTypedLiteral(mockCalendar)));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(DECIMAL);
            when(mockValue.getDecimal()).thenReturn(BigDecimal.valueOf(0.0));
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createTypedLiteral(BigDecimal.valueOf(0.0))));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(DOUBLE);
            when(mockValue.getDouble()).thenReturn((double) 0);
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createTypedLiteral((double) 0)));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(LONG);
            when(mockValue.getLong()).thenReturn(0L);
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createTypedLiteral(0L)));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(STRING);
            when(mockValue.getString()).thenReturn("XYZ");
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createTypedLiteral("XYZ")));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(URI);
            when(mockValue.getString()).thenReturn("info:fedora");

            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);

            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createResource("info:fedora")));

            mockValue = mock(Value.class);
            when(mockProperty.getSession()).thenReturn(mockSession);
            when(mockSession.getNodeByIdentifier("uuid")).thenReturn(mockNode);
            when(mockNode.getPath()).thenReturn("/abc");

            when(mockValue.getType()).thenReturn(REFERENCE);
            when(mockValue.getString()).thenReturn("uuid");
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);

            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createResource("info:fedora/abc")));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(WEAKREFERENCE);
            when(mockValue.getString()).thenReturn("uuid");
            when(mockNode.getPath()).thenReturn("/def");
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);

            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createResource("info:fedora/def")));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PATH);
            when(mockValue.getString()).thenReturn("/ghi");
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);

            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createResource("info:fedora/ghi")));

        } finally {
            getPredicateForProperty = holdPredicate;
        }

    }

    @Test
    @Ignore
    public void testJcrNodeContent() throws RepositoryException {

        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getPrimaryNodeType().getName()).thenReturn("");
        when(mockProperties.hasNext()).thenReturn(false);
        when(mockNode.getProperties()).thenReturn(mockProperties);
        when(mockNode.getPath()).thenReturn("/path/to/node");
        when(mockNodes.hasNext()).thenReturn(false);
        when(mockNode.getNodes()).thenReturn(mockNodes);
        when(mockContentNode.getPath()).thenReturn("/path/to/node/content");
        when(mockContentNode.getProperties()).thenReturn(mockProperties);
        when(mockContentNode.getSession()).thenReturn(mockSession);
        when(mockNode.hasNode(JCR_CONTENT)).thenReturn(true);
        when(mockNode.getNode(JCR_CONTENT)).thenReturn(mockContentNode);

        final Model model = testObj.getJcrPropertiesModel(mockNode);

        assertTrue(model != null);
    }

    @Test
    public void testJcrNodeIteratorModel() throws RepositoryException {
        when(mockNodes.hasNext()).thenReturn(false);
        final Model model =
                testObj.getJcrPropertiesModel(
                                                 new org.fcrepo.utils.NodeIterator(mockNodes),
                                                 mockResource);
        assertTrue(model != null);
    }

    @Test
    public void testJcrNodeIteratorAddsPredicatesForEachNode()
            throws RepositoryException {
        final Resource mockResource =
                createResource("info:fedora/search/resource");
        final Node mockNode1 = mock(Node.class);
        final Node mockNode2 = mock(Node.class);
        final Node mockNode3 = mock(Node.class);
        when(mockProperties.hasNext()).thenReturn(false);
        when(mockNode1.getProperties()).thenReturn(mockProperties);
        when(mockNode1.getSession()).thenReturn(mockSession);

        when(mockNode1.getPath()).thenReturn("/path/to/first/node");
        when(mockNode2.getPath()).thenReturn("/second/path/to/node");
        when(mockNode3.getPath()).thenReturn("/third/path/to/node");
        when(mockNode1.getProperties()).thenReturn(mockProperties);
        when(mockNode2.getProperties()).thenReturn(mockProperties);
        when(mockNode3.getProperties()).thenReturn(mockProperties);

        final Iterator<Node> mockIterator =
                asList(mockNode1, mockNode2, mockNode3).iterator();
        final Model model =
                testObj.getJcrPropertiesModel(mockIterator,
                                                 mockResource);
        assertEquals(3, model.listObjectsOfProperty(HAS_MEMBER_OF_RESULT)
                .toSet().size());
    }

    @Test
    public void testGetFixityResultsModel() throws RepositoryException,
            URISyntaxException {
        when(mockCacheEntry.getExternalIdentifier()).thenReturn("xyz");
        final FixityResult mockResult =
                new FixityResult(mockCacheEntry, 123, new URI("abc"));
        mockResult.status.add(BAD_CHECKSUM);
        mockResult.status.add(BAD_SIZE);

        final List<FixityResult> mockBlobs = asList(mockResult);
        when(mockNode.getPath()).thenReturn("/path/to/node");
        when(mockProperties.hasNext()).thenReturn(false);
        when(mockNode.getProperties()).thenReturn(mockProperties);

        final Model fixityResultsModel =
                testObj.getJcrPropertiesModel(mockNode, mockBlobs);

        LOGGER.debug("Got graph {}", fixityResultsModel);

        final GraphStore gs = GraphStoreFactory.create(fixityResultsModel);
        assertTrue(gs.contains(ANY, ANY, IS_FIXITY_RESULT_OF.asNode(),
                createResource("info:fedora/path/to/node").asNode()));
        assertTrue(gs.contains(ANY, ANY, HAS_COMPUTED_CHECKSUM.asNode(),
                createResource("abc").asNode()));
        assertTrue(gs.contains(ANY, ANY, HAS_COMPUTED_SIZE.asNode(),
                createTypedLiteral(123).asNode()));

    }

    @Test
    public void testGetJcrNamespaceModel() throws Exception {
        final Model jcrNamespaceModel = testObj.getJcrNamespaceModel();
        assertTrue(jcrNamespaceModel.contains(
                createResource("info:fedora/fedora-system:def/internal#"),
                HAS_NAMESPACE_PREFIX, "fedora-internal"));

        final Resource nsSubject = createResource("registered-uri#");
        assertTrue(jcrNamespaceModel.contains(nsSubject,
                                                RDF.type,
                                                RdfLexicon.VOAF_VOCABULARY));
        assertTrue(jcrNamespaceModel.contains(nsSubject,
                                                 HAS_NAMESPACE_PREFIX,
                                                 "some-prefix"));

        assertTrue(jcrNamespaceModel.contains(nsSubject,
                                                 HAS_NAMESPACE_URI,
                                                 "registered-uri#"));
    }

    @Test
    public void testGetJcrVersionsModel() throws Exception {

        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockVersionManager.getVersionHistory(mockNode.getPath()))
                .thenReturn(mockVersionHistory);

        when(mockVersionIterator.hasNext()).thenReturn(true, false);
        when(mockFrozenNode.getPath()).thenReturn(
                "/jcr:system/versions/test/jcr");
        when(mockVersion.getFrozenNode()).thenReturn(mockFrozenNode);
        when(mockVersionIterator.nextVersion()).thenReturn(mockVersion);
        when(mockVersionHistory.getAllVersions()).thenReturn(
                mockVersionIterator);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionHistory.getVersionLabels(mockVersion)).thenReturn(
                new String[] {"abc"});

        when(mockProperties.hasNext()).thenReturn(false);
        when(mockFrozenNode.getProperties()).thenReturn(mockProperties);
        final Model actual = testObj.getJcrPropertiesModel(mockVersionHistory, testSubjects.getGraphSubject(mockNode));

        assertTrue(actual.contains(testSubjects.getGraphSubject(mockNode),
                HAS_VERSION, testSubjects.getGraphSubject(mockFrozenNode)));
        assertTrue(actual.contains(
                testSubjects.getGraphSubject(mockFrozenNode),
                HAS_VERSION_LABEL, actual.createLiteral("abc")));
    }

    @Test
    public void testIsInternalProperty() throws Exception {
        assertTrue(testObj.isInternalProperty(mockNode, ResourceFactory.createProperty(RdfLexicon.INTERNAL_NAMESPACE, "some-property")));
        assertTrue(testObj.isInternalProperty(mockNode, ResourceFactory.createProperty("http://www.jcp.org/jcr/1.0", "some-property")));
        assertTrue(testObj.isInternalProperty(mockNode, ResourceFactory.createProperty("http://www.w3.org/ns/ldp#some-property")));
        assertFalse(testObj.isInternalProperty(mockNode, ResourceFactory.createProperty("my-own-ns", "some-property")));
    }

    private void mockNamespaceRegistry() throws RepositoryException {

        when(mockNsRegistry.isRegisteredUri("registered-uri#"))
                .thenReturn(true);
        when(mockNsRegistry.isRegisteredUri("not-registered-uri#")).thenReturn(
                false);
        when(mockNsRegistry.isRegisteredUri("http://www.jcp.org/jcr/1.0"))
                .thenReturn(true);
        when(mockNsRegistry.getPrefix("http://www.jcp.org/jcr/1.0"))
                .thenReturn("jcr");
        when(mockNsRegistry.getPrefix("registered-uri#")).thenReturn(
                "some-prefix");
        when(mockNsRegistry.getURI("jcr")).thenReturn(
                "http://www.jcp.org/jcr/1.0");
        when(mockNsRegistry.getURI("some-prefix"))
                .thenReturn("registered-uri#");
        when(mockNsRegistry.getPrefixes()).thenReturn(
                new String[] {"jcr", "some-prefix"});

        when(getNamespaceRegistry(mockSession)).thenReturn(mockNsRegistry);
        when(getNamespaceRegistry(mockNode)).thenReturn(mockNsRegistry);
    }
}
