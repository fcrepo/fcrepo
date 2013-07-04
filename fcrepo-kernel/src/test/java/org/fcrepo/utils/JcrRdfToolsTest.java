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

import static javax.jcr.query.Query.JCR_SQL2;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
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

import org.fcrepo.RdfLexicon;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.rdf.impl.DefaultGraphSubjects;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.services.functions.GetClusterConfiguration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.slf4j.Logger;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;

public class JcrRdfToolsTest {

    private static final Logger LOGGER = getLogger(JcrRdfToolsTest.class);

    private Node mockNode;

    private NamespaceRegistry mockNsRegistry;

    private GraphSubjects testSubjects;

    private Session mockSession;

    private Repository mockRepository;

    private Workspace mockWorkspace;

    @Before
    public void setUp() throws RepositoryException {

        mockSession = mock(Session.class);
        testSubjects = new DefaultGraphSubjects();
        mockNode = mock(Node.class);
        when(mockNode.getSession()).thenReturn(mockSession);

        mockWorkspace = mock(Workspace.class);

        mockRepository = mock(Repository.class);
        when(mockSession.getRepository()).thenReturn(mockRepository);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);

        mockNamespaceRegistry();
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNsRegistry);

    }

    @Test
    public void shouldMapInternalJcrNamespaceToFcrepoNamespace() {
        assertEquals("info:fedora/fedora-system:def/internal#", JcrRdfTools
                .getRDFNamespaceForJcrNamespace("http://www.jcp.org/jcr/1.0"));
    }

    @Test
    public void shouldMapFcrepoNamespaceToJcrNamespace() {
        assertEquals(
                "http://www.jcp.org/jcr/1.0",
                JcrRdfTools
                        .getJcrNamespaceForRDFNamespace("info:fedora/fedora-system:def/internal#"));
    }

    @Test
    public void shouldPassThroughOtherNamespaceValues() {
        assertEquals("some-namespace-uri", JcrRdfTools
                .getJcrNamespaceForRDFNamespace("some-namespace-uri"));
        assertEquals("some-namespace-uri", JcrRdfTools
                .getRDFNamespaceForJcrNamespace("some-namespace-uri"));
    }

    @Test
    public void shouldMapRdfPredicatesToJcrProperties()
            throws RepositoryException {

        final Property p =
                ResourceFactory.createProperty(
                        "info:fedora/fedora-system:def/internal#", "uuid");
        assertEquals("jcr:uuid", JcrRdfTools.getPropertyNameFromPredicate(
                mockNode, p));

    }

    @Test
    public void shouldReuseRegisteredNamespaces() throws RepositoryException {
        final Property p =
                ResourceFactory.createProperty("registered-uri#", "uuid");
        assertEquals("some-prefix:uuid", JcrRdfTools
                .getPropertyNameFromPredicate(mockNode, p));
    }

    @Test
    public void shouldRegisterUnknownUris() throws RepositoryException {
        when(mockNsRegistry.registerNamespace("not-registered-uri#"))
                .thenReturn("ns001");
        final Property p =
                ResourceFactory.createProperty("not-registered-uri#", "uuid");
        assertEquals("ns001:uuid", JcrRdfTools.getPropertyNameFromPredicate(
                mockNode, p));
    }

    @Test
    public void shouldMapJcrNodeNamestoRDFResources()
            throws RepositoryException {
        when(mockNode.getPath()).thenReturn("/abc");

        assertEquals("info:fedora/abc", JcrRdfTools.getGraphSubject(
                testSubjects, mockNode).toString());
    }

    @Test
    public void shouldMapJcrContentNodeNamestoRDFResourcesIntheFcrNamespace()
            throws RepositoryException {
        when(mockNode.getPath()).thenReturn("/abc/jcr:content");

        assertEquals("info:fedora/abc/fcr:content", JcrRdfTools
                .getGraphSubject(testSubjects, mockNode).toString());
    }

    @Test
    public void shouldMapRDFResourcesToJcrNodes() throws RepositoryException {
        when(mockSession.nodeExists("/abc")).thenReturn(true);
        when(mockSession.getNode("/abc")).thenReturn(mockNode);
        assertEquals(mockNode, JcrRdfTools.getNodeFromGraphSubject(
                testSubjects, mockSession, ResourceFactory
                        .createResource("info:fedora/abc")));
    }

    @Test
    public void shouldMapRDFContentResourcesToJcrContentNodes()
            throws RepositoryException {
        when(mockSession.nodeExists("/abc/jcr:content")).thenReturn(true);
        when(mockSession.getNode("/abc/jcr:content")).thenReturn(mockNode);
        assertEquals(mockNode, JcrRdfTools.getNodeFromGraphSubject(
                testSubjects, mockSession, ResourceFactory
                        .createResource("info:fedora/abc/fcr:content")));
    }

    @Test
    public void shouldReturnNullIfItFailstoMapRDFResourcesToJcrNodes()
            throws RepositoryException {
        when(mockSession.nodeExists("/does-not-exist")).thenReturn(false);
        assertNull(
                "should receive null for a non-JCR resource",
                JcrRdfTools
                        .getNodeFromGraphSubject(
                                testSubjects,
                                mockSession,
                                ResourceFactory
                                        .createResource("this-is-not-a-fedora-node/abc")));
        assertNull("should receive null a JCR node that isn't found",
                JcrRdfTools.getNodeFromGraphSubject(testSubjects, mockSession,
                        ResourceFactory
                                .createResource("info:fedora/does-not-exist")));
    }

    @Test
    public void shouldDetermineIfAGraphResourceIsAJcrNode()
            throws RepositoryException {
        final GraphSubjects mockFactory = mock(GraphSubjects.class);
        final Resource mockSubject = mock(Resource.class);
        when(mockFactory.isFedoraGraphSubject(mockSubject)).thenReturn(true);
        assertTrue(JcrRdfTools.isFedoraGraphSubject(mockFactory, mockSubject));

        verify(mockFactory).isFedoraGraphSubject(mockSubject);
    }

    @Test
    public void testGetPropertiesModel() throws RepositoryException {

        final Node mockParent = mock(Node.class);
        when(mockParent.getPath()).thenReturn("/test");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getParent()).thenReturn(mockParent);
        final NodeType nodeType = mock(NodeType.class);
        when(mockNode.getPrimaryNodeType()).thenReturn(nodeType);
        when(mockNode.getPrimaryNodeType().getName()).thenReturn("");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        final javax.jcr.NodeIterator mockNodes =
                mock(javax.jcr.NodeIterator.class);
        when(mockNode.getNodes()).thenReturn(mockNodes);

        final PropertyIterator mockProperties = mock(PropertyIterator.class);
        when(mockNode.getProperties()).thenReturn(mockProperties);

        final PropertyIterator mockParentProperties =
                mock(PropertyIterator.class);
        when(mockParent.getProperties()).thenReturn(mockParentProperties);
        when(mockProperties.hasNext()).thenReturn(true, false);
        when(mockParentProperties.hasNext()).thenReturn(true, false);
        final javax.jcr.Property mockProperty = mock(javax.jcr.Property.class);
        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockProperty.getName()).thenReturn("xyz");
        when(mockProperty.getType()).thenReturn(0);
        final Value mockValue = mock(Value.class);
        when(mockValue.getString()).thenReturn("abc");
        when(mockProperty.getValue()).thenReturn(mockValue);
        when(mockProperties.nextProperty()).thenReturn(mockProperty);
        when(mockParentProperties.nextProperty()).thenReturn(mockProperty);

        final Model actual =
                JcrRdfTools.getJcrPropertiesModel(testSubjects, mockNode);
        assertEquals("info:fedora/fedora-system:def/internal#", actual
                .getNsPrefixURI("fedora-internal"));
        assertTrue(actual.contains(testSubjects.getGraphSubject(mockNode),
                actual.getProperty("xyz"), actual.createLiteral("abc")));

    }

    @Test
    public void testGetPropertiesModelWithContent() throws RepositoryException {
        final LowLevelStorageService mockLowLevelStorageService =
                mock(LowLevelStorageService.class);
        JcrRdfTools.setLlstore(mockLowLevelStorageService);

        final Node mockParent = mock(Node.class);
        when(mockParent.getPath()).thenReturn("/test");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getParent()).thenReturn(mockParent);
        final NodeType nodeType = mock(NodeType.class);
        when(mockNode.getPrimaryNodeType()).thenReturn(nodeType);
        when(mockNode.getPrimaryNodeType().getName()).thenReturn("");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.hasNode(JcrConstants.JCR_CONTENT)).thenReturn(true);
        final Node mockNodeContent = mock(Node.class);
        when(mockNodeContent.getPath()).thenReturn("/test/jcr/jcr:content");
        final javax.jcr.Property mockData = mock(javax.jcr.Property.class);
        final BinaryValue mockBinary = mock(BinaryValue.class);
        when(mockBinary.getKey()).thenReturn(new BinaryKey("abc"));
        when(mockData.getBinary()).thenReturn(mockBinary);
        when(mockNodeContent.getProperty(JcrConstants.JCR_DATA)).thenReturn(
                mockData);
        final LowLevelCacheEntry mockCacheEntry =
                mock(LowLevelCacheEntry.class);
        when(mockCacheEntry.getExternalIdentifier()).thenReturn("xyz");
        when(
                mockLowLevelStorageService
                        .getLowLevelCacheEntries(mockNodeContent)).thenReturn(
                ImmutableSet.of(mockCacheEntry));
        when(mockNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(
                mockNodeContent);
        final javax.jcr.NodeIterator mockNodes =
                mock(javax.jcr.NodeIterator.class);
        when(mockNode.getNodes()).thenReturn(mockNodes);

        final PropertyIterator mockProperties = mock(PropertyIterator.class);
        when(mockNode.getProperties()).thenReturn(mockProperties);
        when(mockParent.getProperties()).thenReturn(mockProperties);
        when(mockNodeContent.getProperties()).thenReturn(mockProperties);
        when(mockProperties.hasNext()).thenReturn(false);

        final Model actual =
                JcrRdfTools.getJcrPropertiesModel(testSubjects, mockNode);
        assertEquals("info:fedora/fedora-system:def/internal#", actual
                .getNsPrefixURI("fedora-internal"));
        assertTrue(actual.contains(testSubjects.getGraphSubject(mockNode),
                RdfLexicon.HAS_CONTENT, testSubjects
                        .getGraphSubject(mockNodeContent)));
        assertTrue(actual.contains(testSubjects
                .getGraphSubject(mockNodeContent), RdfLexicon.HAS_LOCATION,
                actual.createLiteral("xyz")));
    }

    @Test
    public void testGetPropertiesModelForRootNode() throws RepositoryException {
        when(mockRepository.login()).thenReturn(mockSession);
        final QueryManager mockQueryManager = mock(QueryManager.class);
        final Query mockQuery = mock(Query.class);
        final QueryResult mockQueryResult = mock(QueryResult.class);
        final RowIterator mockRowIterator = mock(RowIterator.class);
        when(mockRowIterator.getSize()).thenReturn(0L);
        when(mockQueryResult.getRows()).thenReturn(mockRowIterator);
        when(mockQuery.execute()).thenReturn(mockQueryResult);
        when(mockQueryManager.createQuery(anyString(), eq(JCR_SQL2)))
                .thenReturn(mockQuery);
        when(mockWorkspace.getQueryManager()).thenReturn(mockQueryManager);
        final MetricRegistry mockMetrics = mock(MetricRegistry.class);
        final Counter mockCounter = mock(Counter.class);
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
        final GetClusterConfiguration mockGetClusterConfiguration =
                mock(GetClusterConfiguration.class);
        when(mockGetClusterConfiguration.apply(mockRepository)).thenReturn(
                ImmutableMap.of("a", "b"));
        JcrRdfTools.setGetClusterConfiguration(mockGetClusterConfiguration);

        when(mockNode.getPath()).thenReturn("/");
        final NodeType nodeType = mock(NodeType.class);
        when(mockNode.getPrimaryNodeType()).thenReturn(nodeType);
        when(mockNode.getPrimaryNodeType().getName()).thenReturn(
                FedoraJcrTypes.ROOT);
        when(mockNode.getPath()).thenReturn("/test/jcr");
        final javax.jcr.NodeIterator mockNodes =
                mock(javax.jcr.NodeIterator.class);
        when(mockNode.getNodes()).thenReturn(mockNodes);
        final PropertyIterator mockProperties = mock(PropertyIterator.class);
        when(mockNode.getProperties()).thenReturn(mockProperties);
        when(mockProperties.hasNext()).thenReturn(false);

        when(mockSession.getRepository()).thenReturn(mockRepository);
        when(mockRepository.getDescriptorKeys()).thenReturn(
                new String[] {"some-descriptor-key"});
        when(mockRepository.getDescriptor("some-descriptor-key")).thenReturn(
                "some-descriptor-value");
        final NodeTypeManager mockNodeTypeManager = mock(NodeTypeManager.class);
        final NodeTypeIterator mockNodeTypeIterator =
                mock(NodeTypeIterator.class);
        when(mockNodeTypeIterator.hasNext()).thenReturn(false);
        when(mockNodeTypeManager.getAllNodeTypes()).thenReturn(
                mockNodeTypeIterator);

        when(mockWorkspace.getNodeTypeManager())
                .thenReturn(mockNodeTypeManager);

        final Model actual =
                JcrRdfTools.getJcrPropertiesModel(testSubjects, mockNode);
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
        final Node mockParent = mock(Node.class);
        when(mockParent.getPath()).thenReturn("/test");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getParent()).thenReturn(mockParent);
        when(mockNode.getDepth()).thenReturn(2);
        final NodeType nodeType = mock(NodeType.class);
        when(mockNode.getPrimaryNodeType()).thenReturn(nodeType);
        when(mockNode.getPrimaryNodeType().getName()).thenReturn(
                "fedora:object");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        final PropertyIterator mockProperties = mock(PropertyIterator.class);
        when(mockNode.getProperties()).thenReturn(mockProperties);
        when(mockParent.getProperties()).thenReturn(mockProperties);
        when(mockProperties.hasNext()).thenReturn(true, false);
        final javax.jcr.Property mockProperty = mock(javax.jcr.Property.class);
        final Value mockValue = mock(Value.class);
        when(mockProperty.getValue()).thenReturn(mockValue);
        when(mockValue.getType()).thenReturn(PropertyType.BINARY);
        when(mockProperties.nextProperty()).thenReturn(mockProperty);

        final Model actual =
                JcrRdfTools.getJcrPropertiesModel(testSubjects, mockNode);
        assertEquals(0, actual.size());
    }

    @Test
    public void shouldIncludeParentNodeInformation() throws RepositoryException {
        final Node mockParent = mock(Node.class);
        when(mockParent.getPath()).thenReturn("/test");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getParent()).thenReturn(mockParent);

        final NodeIterator mockIterator = mock(NodeIterator.class);
        when(mockIterator.hasNext()).thenReturn(false);
        when(mockNode.getNodes()).thenReturn(mockIterator);
        final Model actual =
                JcrRdfTools.getJcrTreeModel(testSubjects, mockNode, 0, -1);
        assertEquals(1, actual.size());
    }

    @Test
    public void shouldIncludeChildNodeInformation() throws RepositoryException {
        final Node mockParent = mock(Node.class);
        when(mockParent.getPath()).thenReturn("/test");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getParent()).thenReturn(mockParent);

        final Node mockChildNode = mock(Node.class);
        when(mockChildNode.getName()).thenReturn("some-name");

        when(mockChildNode.getPath()).thenReturn("/test/jcr/1", "/test/jcr/2",
                "/test/jcr/3", "/test/jcr/4", "/test/jcr/5");
        final NodeIterator mockIterator = mock(NodeIterator.class);
        when(mockIterator.hasNext()).thenReturn(true, true, true, true, true,
                false);
        when(mockIterator.nextNode()).thenReturn(mockChildNode);

        when(mockNode.getNodes()).thenReturn(mockIterator);
        final Model actual =
                JcrRdfTools.getJcrTreeModel(testSubjects, mockNode, 0, 0);
        assertEquals(5 * 2 + 1, actual.size());
    }

    @Test
    public void shouldIncludeFullChildNodeInformationInsideWindow()
            throws RepositoryException {
        final Node mockParent = mock(Node.class);
        when(mockParent.getPath()).thenReturn("/test");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getParent()).thenReturn(mockParent);

        final Node mockChildNode = mock(Node.class);
        when(mockChildNode.getName()).thenReturn("some-name");
        when(mockChildNode.getPath()).thenReturn("/test/jcr/1", "/test/jcr/4",
                "/test/jcr/5");

        final Node mockFullChildNode = mock(Node.class);
        when(mockFullChildNode.getName()).thenReturn("some-other-name");
        when(mockFullChildNode.getPath()).thenReturn("/test/jcr/2",
                "/test/jcr/3");

        final PropertyIterator mockProperties = mock(PropertyIterator.class);
        when(mockFullChildNode.getProperties()).thenReturn(mockProperties);

        when(mockProperties.hasNext()).thenReturn(false);

        final NodeIterator mockIterator = mock(NodeIterator.class);
        when(mockIterator.hasNext()).thenReturn(true, true, true, true, true,
                false);
        when(mockIterator.nextNode()).thenReturn(mockChildNode,
                mockFullChildNode, mockFullChildNode, mockChildNode,
                mockChildNode);

        when(mockNode.getNodes()).thenReturn(mockIterator);
        final Model actual =
                JcrRdfTools.getJcrTreeModel(testSubjects, mockNode, 1, 2);
        assertEquals(5 * 2 + 1, actual.size());
        verify(mockChildNode, never()).getProperties();
    }

    @Test
    public void shouldMapRdfValuesToJcrPropertyValues()
            throws RepositoryException {
        final ValueFactory mockValueFactory = mock(ValueFactory.class);

        @SuppressWarnings("unchecked")
        final Function<Node, ValueFactory> mockValueFactoryFunc =
                mock(Function.class);
        when(mockValueFactoryFunc.apply(mockNode)).thenReturn(mockValueFactory);

        final Function<Node, ValueFactory> holdValueFactory =
                FedoraTypesUtils.getValueFactory;
        FedoraTypesUtils.getValueFactory = mockValueFactoryFunc;

        try {
            RDFNode n = ResourceFactory.createResource("info:fedora/abc");

            // node references
            when(mockSession.getNode("/abc")).thenReturn(mockNode);
            JcrRdfTools.createValue(mockNode, n, PropertyType.REFERENCE);
            JcrRdfTools.createValue(mockNode, n, PropertyType.WEAKREFERENCE);
            verify(mockValueFactory, times(2)).createValue(mockNode);

            // uris
            JcrRdfTools.createValue(mockNode, n, PropertyType.UNDEFINED);
            verify(mockValueFactory).createValue("info:fedora/abc",
                    PropertyType.URI);

            // other random resources
            n = ResourceFactory.createResource();
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue(n.toString(),
                    PropertyType.UNDEFINED);

            // undeclared types, but infer them from rdf types

            n = ResourceFactory.createTypedLiteral(true);
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue(true);

            n = ResourceFactory.createTypedLiteral("1", XSDDatatype.XSDbyte);
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue((byte) 1);

            n = ResourceFactory.createTypedLiteral((double) 2);
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue((double) 2);

            n = ResourceFactory.createTypedLiteral((float) 3);
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue((float) 3);

            n = ResourceFactory.createTypedLiteral(4);
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue(4);

            n = ResourceFactory.createTypedLiteral("5", XSDDatatype.XSDlong);
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue(5);

            n = ResourceFactory.createTypedLiteral("6", XSDDatatype.XSDshort);
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue((short) 6);

            final Calendar calendar = Calendar.getInstance();
            n = ResourceFactory.createTypedLiteral(calendar);
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue(any(Calendar.class));

            n = ResourceFactory.createTypedLiteral("string");
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue("string", PropertyType.STRING);

            n = ResourceFactory.createTypedLiteral("string");
            JcrRdfTools.createValue(mockNode, n, PropertyType.NAME);
            verify(mockValueFactory).createValue("string", PropertyType.NAME);

        } finally {
            FedoraTypesUtils.getValueFactory = holdValueFactory;
        }

    }

    @Test
    public void shouldAddPropertiesToModel() throws RepositoryException {
        final javax.jcr.Property mockProperty = mock(javax.jcr.Property.class);
        final Property mockPredicate = mock(Property.class);

        @SuppressWarnings("unchecked")
        final Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> mockPredicateFactoryFunc =
                mock(Function.class);
        when(mockPredicateFactoryFunc.apply(mockProperty)).thenReturn(
                mockPredicate);

        final Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> holdPredicate =
                FedoraTypesUtils.getPredicateForProperty;
        FedoraTypesUtils.getPredicateForProperty = mockPredicateFactoryFunc;

        try {
            final Resource mockSubject = mock(Resource.class);
            final Model mockModel = mock(Model.class);

            final Value mockValue = mock(Value.class);
            when(mockValue.getString()).thenReturn("");

            when(mockProperty.isMultiple()).thenReturn(false);
            when(mockProperty.getValue()).thenReturn(mockValue);

            JcrRdfTools
                    .addPropertyToModel(mockSubject, mockModel, mockProperty);
            verify(mockModel).add(mockSubject, mockPredicate, "");

        } finally {
            FedoraTypesUtils.getPredicateForProperty = holdPredicate;
        }

    }

    @Test
    public void shouldAddMultivaluedPropertiesToModel()
            throws RepositoryException {
        final javax.jcr.Property mockProperty = mock(javax.jcr.Property.class);
        final Property mockPredicate = mock(Property.class);

        @SuppressWarnings("unchecked")
        final Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> mockPredicateFactoryFunc =
                mock(Function.class);
        when(mockPredicateFactoryFunc.apply(mockProperty)).thenReturn(
                mockPredicate);

        final Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> holdPredicate =
                FedoraTypesUtils.getPredicateForProperty;
        FedoraTypesUtils.getPredicateForProperty = mockPredicateFactoryFunc;

        try {
            final Resource mockSubject = mock(Resource.class);
            final Model mockModel = mock(Model.class);

            final Value mockValue = mock(Value.class);
            when(mockValue.getString()).thenReturn("1");

            final Value mockValue2 = mock(Value.class);
            when(mockValue2.getString()).thenReturn("2");

            when(mockProperty.isMultiple()).thenReturn(true);
            when(mockProperty.getValues()).thenReturn(
                    Arrays.asList(mockValue, mockValue2).toArray(new Value[2]));

            JcrRdfTools
                    .addPropertyToModel(mockSubject, mockModel, mockProperty);
            verify(mockModel).add(mockSubject, mockPredicate, "1");
            verify(mockModel).add(mockSubject, mockPredicate, "2");

        } finally {
            FedoraTypesUtils.getPredicateForProperty = holdPredicate;
        }

    }

    @Test
    public void shouldMapJcrTypesToRdfDataTypes() throws RepositoryException {
        final javax.jcr.Property mockProperty = mock(javax.jcr.Property.class);
        final Resource mockSubject =
                ResourceFactory.createResource("some-resource-uri");
        final Model mockModel = ModelFactory.createDefaultModel();
        final Property mockPredicate =
                mockModel.createProperty("some-predicate-uri");

        @SuppressWarnings("unchecked")
        final Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> mockPredicateFactoryFunc =
                mock(Function.class);
        when(mockPredicateFactoryFunc.apply(mockProperty)).thenReturn(
                mockPredicate);

        final Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> holdPredicate =
                FedoraTypesUtils.getPredicateForProperty;
        FedoraTypesUtils.getPredicateForProperty = mockPredicateFactoryFunc;

        try {

            Value mockValue;

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.BOOLEAN);
            when(mockValue.getBoolean()).thenReturn(true);
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel,
                    mockProperty, mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    ResourceFactory.createTypedLiteral(true)));

            mockValue = mock(Value.class);
            final Calendar mockCalendar = Calendar.getInstance();
            when(mockValue.getType()).thenReturn(PropertyType.DATE);
            when(mockValue.getDate()).thenReturn(mockCalendar);
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel,
                    mockProperty, mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    ResourceFactory.createTypedLiteral(mockCalendar)));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.DECIMAL);
            when(mockValue.getDecimal()).thenReturn(BigDecimal.valueOf(0.0));
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel,
                    mockProperty, mockValue);
            assertTrue(mockModel
                    .contains(mockSubject, mockPredicate, ResourceFactory
                            .createTypedLiteral(BigDecimal.valueOf(0.0))));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.DOUBLE);
            when(mockValue.getDouble()).thenReturn((double) 0);
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel,
                    mockProperty, mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    ResourceFactory.createTypedLiteral((double) 0)));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.LONG);
            when(mockValue.getLong()).thenReturn(0L);
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel,
                    mockProperty, mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    ResourceFactory.createTypedLiteral(0L)));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.STRING);
            when(mockValue.getString()).thenReturn("XYZ");
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel,
                    mockProperty, mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    ResourceFactory.createTypedLiteral("XYZ")));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.URI);
            when(mockValue.getString()).thenReturn("info:fedora");

            JcrRdfTools.addPropertyToModel(mockSubject, mockModel,
                    mockProperty, mockValue);

            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    ResourceFactory.createResource("info:fedora")));

            mockValue = mock(Value.class);
            when(mockProperty.getSession()).thenReturn(mockSession);
            when(mockSession.getNodeByIdentifier("uuid")).thenReturn(mockNode);
            when(mockNode.getPath()).thenReturn("/abc");

            when(mockValue.getType()).thenReturn(PropertyType.REFERENCE);
            when(mockValue.getString()).thenReturn("uuid");
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel,
                    mockProperty, mockValue);

            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    ResourceFactory.createResource("info:fedora/abc")));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.WEAKREFERENCE);
            when(mockValue.getString()).thenReturn("uuid");
            when(mockNode.getPath()).thenReturn("/def");
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel,
                    mockProperty, mockValue);

            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    ResourceFactory.createResource("info:fedora/def")));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.PATH);
            when(mockValue.getString()).thenReturn("/ghi");
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel,
                    mockProperty, mockValue);

            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    ResourceFactory.createResource("info:fedora/ghi")));

        } finally {
            FedoraTypesUtils.getPredicateForProperty = holdPredicate;
        }

    }

    @Test
    @Ignore
    public void testJcrNodeContent() throws RepositoryException {

        final NodeType nodeType = mock(NodeType.class);
        when(mockNode.getPrimaryNodeType()).thenReturn(nodeType);
        when(mockNode.getPrimaryNodeType().getName()).thenReturn("");

        final PropertyIterator mockProperties = mock(PropertyIterator.class);
        when(mockProperties.hasNext()).thenReturn(false);
        when(mockNode.getProperties()).thenReturn(mockProperties);

        when(mockNode.getPath()).thenReturn("/path/to/node");
        final NodeIterator mockIterator = mock(NodeIterator.class);
        when(mockIterator.hasNext()).thenReturn(false);
        when(mockNode.getNodes()).thenReturn(mockIterator);

        final Node mockContent = mock(Node.class);
        when(mockContent.getPath()).thenReturn("/path/to/node/content");
        when(mockContent.getProperties()).thenReturn(mockProperties);
        when(mockContent.getSession()).thenReturn(mockSession);

        when(mockNode.hasNode(JcrConstants.JCR_CONTENT)).thenReturn(true);
        when(mockNode.getNode(JcrConstants.JCR_CONTENT))
                .thenReturn(mockContent);
        final Model model =
                JcrRdfTools.getJcrPropertiesModel(testSubjects, mockNode);

        assertTrue(model != null);
    }

    @Test
    public void testJcrNodeIteratorModel() throws RepositoryException {

        final Resource mockResource = mock(Resource.class);
        final org.fcrepo.utils.NodeIterator mockIterator =
                mock(org.fcrepo.utils.NodeIterator.class);
        when(mockIterator.hasNext()).thenReturn(false);
        final Model model =
                JcrRdfTools.getJcrNodeIteratorModel(testSubjects, mockIterator,
                        mockResource);
        assertTrue(model != null);
    }

    @Test
    public void testJcrNodeIteratorAddsPredicatesForEachNode()
            throws RepositoryException {
        final Resource mockResource =
                ResourceFactory.createResource("info:fedora/search/resource");
        final Node mockNode1 = mock(Node.class);
        final Node mockNode2 = mock(Node.class);
        final Node mockNode3 = mock(Node.class);
        final PropertyIterator mockProperties = mock(PropertyIterator.class);
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
                Arrays.asList(mockNode1, mockNode2, mockNode3).iterator();
        final Model model =
                JcrRdfTools.getJcrNodeIteratorModel(testSubjects, mockIterator,
                        mockResource);
        assertEquals(3, model.listObjectsOfProperty(
                RdfLexicon.HAS_MEMBER_OF_RESULT).toSet().size());
    }

    @Test
    public void testGetFixityResultsModel() throws RepositoryException,
            URISyntaxException {
        final LowLevelCacheEntry mockEntry = mock(LowLevelCacheEntry.class);
        when(mockEntry.getExternalIdentifier()).thenReturn("xyz");
        final FixityResult mockResult =
                new FixityResult(mockEntry, 123, new URI("abc"));
        mockResult.status.add(FixityResult.FixityState.BAD_CHECKSUM);
        mockResult.status.add(FixityResult.FixityState.BAD_SIZE);

        final List<FixityResult> mockBlobs = Arrays.asList(mockResult);
        when(mockNode.getPath()).thenReturn("/path/to/node");

        final PropertyIterator mockProperties = mock(PropertyIterator.class);
        when(mockProperties.hasNext()).thenReturn(false);
        when(mockNode.getProperties()).thenReturn(mockProperties);

        final Model fixityResultsModel =
                JcrRdfTools.getFixityResultsModel(testSubjects, mockNode,
                        mockBlobs);

        LOGGER.info("Got graph {}", fixityResultsModel);

        final GraphStore gs = GraphStoreFactory.create(fixityResultsModel);
        assertTrue(gs.contains(com.hp.hpl.jena.graph.Node.ANY,
                com.hp.hpl.jena.graph.Node.ANY, RdfLexicon.IS_FIXITY_RESULT_OF
                        .asNode(), ResourceFactory.createResource(
                        "info:fedora/path/to/node").asNode()));
        assertTrue(gs.contains(com.hp.hpl.jena.graph.Node.ANY,
                com.hp.hpl.jena.graph.Node.ANY,
                RdfLexicon.HAS_COMPUTED_CHECKSUM.asNode(), ResourceFactory
                        .createResource("abc").asNode()));
        assertTrue(gs.contains(com.hp.hpl.jena.graph.Node.ANY,
                com.hp.hpl.jena.graph.Node.ANY, RdfLexicon.HAS_COMPUTED_SIZE
                        .asNode(), ResourceFactory.createTypedLiteral(123)
                        .asNode()));

    }

    @Test
    public void testGetJcrNamespaceModel() throws Exception {
        final Model jcrNamespaceModel =
                JcrRdfTools.getJcrNamespaceModel(mockSession);
        assertTrue(jcrNamespaceModel.contains(ResourceFactory
                .createResource("info:fedora/fedora-system:def/internal#"),
                RdfLexicon.HAS_NAMESPACE_PREFIX, "fedora-internal"));
        assertTrue(jcrNamespaceModel.contains(ResourceFactory
                .createResource("registered-uri#"),
                RdfLexicon.HAS_NAMESPACE_PREFIX, "some-prefix"));
    }

    @Test
    public void testGetJcrVersionsModel() throws Exception {

        when(mockNode.getPath()).thenReturn("/test/jcr");

        final VersionManager mockVersionManager = mock(VersionManager.class);
        final VersionHistory mockVersionHistory = mock(VersionHistory.class);
        when(mockVersionManager.getVersionHistory(mockNode.getPath()))
                .thenReturn(mockVersionHistory);

        final VersionIterator mockVersionIterator = mock(VersionIterator.class);
        when(mockVersionIterator.hasNext()).thenReturn(true, false);
        final Version mockVersion = mock(Version.class);
        final Node mockFrozenNode = mock(Node.class);
        when(mockFrozenNode.getPath()).thenReturn(
                "/jcr:system/versions/test/jcr");
        when(mockVersion.getFrozenNode()).thenReturn(mockFrozenNode);
        when(mockVersionIterator.nextVersion()).thenReturn(mockVersion);
        when(mockVersionHistory.getAllVersions()).thenReturn(
                mockVersionIterator);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionHistory.getVersionLabels(mockVersion)).thenReturn(
                new String[] {"abc"});

        final PropertyIterator mockProperties = mock(PropertyIterator.class);
        when(mockProperties.hasNext()).thenReturn(false);
        when(mockFrozenNode.getProperties()).thenReturn(mockProperties);
        final Model actual =
                JcrRdfTools.getJcrVersionsModel(testSubjects, mockNode);

        assertTrue(actual.contains(testSubjects.getGraphSubject(mockNode),
                RdfLexicon.HAS_VERSION, testSubjects
                        .getGraphSubject(mockFrozenNode)));
        assertTrue(actual.contains(
                testSubjects.getGraphSubject(mockFrozenNode),
                RdfLexicon.HAS_VERSION_LABEL, actual.createLiteral("abc")));
    }

    private void mockNamespaceRegistry() throws RepositoryException {

        mockNsRegistry = mock(NamespaceRegistry.class);
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

        when(NamespaceTools.getNamespaceRegistry(mockSession)).thenReturn(
                mockNsRegistry);
        when(NamespaceTools.getNamespaceRegistry(mockNode)).thenReturn(
                mockNsRegistry);
    }
}
