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

package org.fcrepo.kernel.utils;

import static com.google.common.collect.ImmutableSet.of;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDbyte;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDlong;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDshort;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static javax.jcr.PropertyType.BINARY;
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
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_OBJECT;
import static org.fcrepo.jcr.FedoraJcrTypes.ROOT;
import static org.fcrepo.kernel.RdfLexicon.HAS_CHILD;
import static org.fcrepo.kernel.RdfLexicon.HAS_COMPUTED_CHECKSUM;
import static org.fcrepo.kernel.RdfLexicon.HAS_COMPUTED_SIZE;
import static org.fcrepo.kernel.RdfLexicon.HAS_CONTENT;
import static org.fcrepo.kernel.RdfLexicon.HAS_LOCATION;
import static org.fcrepo.kernel.RdfLexicon.HAS_MEMBER_OF_RESULT;
import static org.fcrepo.kernel.RdfLexicon.HAS_NAMESPACE_PREFIX;
import static org.fcrepo.kernel.RdfLexicon.HAS_NAMESPACE_URI;
import static org.fcrepo.kernel.RdfLexicon.HAS_PARENT;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION_LABEL;
import static org.fcrepo.kernel.RdfLexicon.IS_FIXITY_RESULT_OF;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.RESTAPI_NAMESPACE;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getPredicateForProperty;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getValueFactory;
import static org.fcrepo.kernel.utils.FixityResult.FixityState.BAD_CHECKSUM;
import static org.fcrepo.kernel.utils.FixityResult.FixityState.BAD_SIZE;
import static org.fcrepo.kernel.utils.JcrRdfTools.getJcrNamespaceForRDFNamespace;
import static org.fcrepo.kernel.utils.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.fcrepo.kernel.utils.JcrRdfTools.setGetClusterConfiguration;
import static org.fcrepo.kernel.utils.NamespaceTools.getNamespaceRegistry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
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
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeDefinition;
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

import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.rdf.impl.DefaultGraphSubjects;
import org.fcrepo.kernel.services.LowLevelStorageService;
import org.fcrepo.kernel.services.functions.GetClusterConfiguration;
import org.fcrepo.kernel.testutilities.TestPropertyIterator;
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
import com.hp.hpl.jena.vocabulary.RDF;

public class JcrRdfToolsTest {

    private static final Logger LOGGER = getLogger(JcrRdfToolsTest.class);

    private GraphSubjects testSubjects;

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
        testSubjects = new DefaultGraphSubjects(mockSession);
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
        when(mockNodeType.getName()).thenReturn(FEDORA_OBJECT);
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
        when(mockValue.getString()).thenReturn("abc");
        when(mockParent.getProperties()).thenReturn(mockParentProperties);
        when(mockParentProperties.hasNext()).thenReturn(false);
    }

    private void addContentNode() throws RepositoryException {
        when(mockNode.hasNode(JCR_CONTENT)).thenReturn(true);
        when(mockNode.getNode(JCR_CONTENT)).thenReturn(mockContentNode);
        when(mockContentNode.getSession()).thenReturn(mockSession);
        when(mockContentNode.getPath()).thenReturn("/test/jcr/jcr:content");
        when(mockBinary.getKey()).thenReturn(new BinaryKey("abc"));
        when(mockBinaryProperty.getBinary()).thenReturn(mockBinary);
        when(mockContentNode.getProperty(JCR_DATA)).thenReturn(
                mockBinaryProperty);
        when(mockBinaryProperty.getName()).thenReturn(JCR_DATA);
        when(mockBinaryProperty.getNode()).thenReturn(mockContentNode);
        when(mockCacheEntry.getExternalIdentifier()).thenReturn("xyz");
        when(
                mockLowLevelStorageService
                        .getLowLevelCacheEntries(mockContentNode)).thenReturn(
                of(mockCacheEntry));

        when(mockContentNode.getProperties()).thenReturn(
                new TestPropertyIterator(mockBinaryProperty));
    }

    @Test
    public final void testGetPropertiesModel() throws RepositoryException,
                                              IOException {
        LOGGER.debug("Entering testGetPropertiesModel()...");
        when(mockNode.hasProperties()).thenReturn(true);
        final Model actual = testObj.getJcrPropertiesModel(mockNode);
        logRDF(actual);
        assertEquals(REPOSITORY_NAMESPACE, actual.getNsPrefixURI("fcrepo"));
        assertTrue("Didn't find appropriate triple!", actual.contains(
                testSubjects.getGraphSubject(mockNode), actual
                        .getProperty(mockPredicateName), actual
                        .createLiteral("abc")));

    }

    @Test
    public final void
            testGetPropertiesModelWithContent() throws RepositoryException {
        testObj.setLlstore(mockLowLevelStorageService);
        addContentNode();
        final Model actual = testObj.getJcrPropertiesModel(mockNode);
        assertEquals(REPOSITORY_NAMESPACE, actual.getNsPrefixURI("fcrepo"));
        assertTrue(actual.contains(testSubjects.getGraphSubject(mockNode),
                HAS_CONTENT, testSubjects.getGraphSubject(mockContentNode)));
        assertTrue(actual.contains(testSubjects
                .getGraphSubject(mockContentNode), HAS_LOCATION, actual
                .createLiteral("xyz")));
    }

    @Test
    public final void
            testGetPropertiesModelForRootNode() throws RepositoryException {

        LOGGER.debug("Entering testGetPropertiesModelForRootNode()...");
        when(mockRepository.login()).thenReturn(mockSession);
        when(mockRowIterator.getSize()).thenReturn(0L);
        when(mockQueryResult.getRows()).thenReturn(mockRowIterator);
        when(mockQuery.execute()).thenReturn(mockQueryResult);
        when(mockQueryManager.createQuery(anyString(), eq(JCR_SQL2)))
                .thenReturn(mockQuery);
        when(mockWorkspace.getQueryManager()).thenReturn(mockQueryManager);
        when(mockMetrics.getCounters()).thenReturn(
                ImmutableSortedMap.of(
                        "LowLevelStorageService.fixity-check-counter",
                        mockCounter,
                        "LowLevelStorageService.fixity-error-counter",
                        mockCounter,
                        "LowLevelStorageService.fixity-repaired-counter",
                        mockCounter

                ));
        when(mockGetClusterConfiguration.apply(mockRepository)).thenReturn(
                ImmutableMap.of("a", "b"));
        setGetClusterConfiguration(mockGetClusterConfiguration);

        when(mockNode.getPath()).thenReturn("/");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getPrimaryNodeType().getName()).thenReturn(ROOT);

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
        assertEquals(REPOSITORY_NAMESPACE, actual.getNsPrefixURI("fcrepo"));

        assertTrue(actual.contains(testSubjects.getGraphSubject(mockNode),
                actual.createProperty(REPOSITORY_NAMESPACE
                        + "repository/some-descriptor-key"), actual
                        .createLiteral("some-descriptor-value")));
    }

    @Test
    public final void
            shouldExcludeBinaryProperties() throws RepositoryException,
                                           IOException {
        when(mockNode.getDepth()).thenReturn(2);
        when(mockNode.getPrimaryNodeType().getName()).thenReturn(
                "fedora:object");
        reset(mockProperty, mockValue, mockNodes);
        when(mockProperty.getType()).thenReturn(BINARY);
        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockProperty.getName()).thenReturn(mockPredicateName);
        when(mockProperty.getBinary()).thenReturn(mockBinary);
        when(mockProperty.getValue()).thenReturn(mockValue);
        when(mockValue.getType()).thenReturn(BINARY);
        when(mockNodes.hasNext()).thenReturn(false);
        final Model actual = testObj.getJcrPropertiesModel(mockNode);
        logRDF(actual);
        assertFalse(
                "RDF contained a statement based on a binary property when it shouldn't have!",
                actual.contains(null, createProperty(mockPredicateName)));
    }

    @Test
    public final void
            shouldBeAbleToDisableResourceInlining() throws RepositoryException {

        final Model actual = testObj.getJcrTreeModel(mockNode, 0, -2);
        assertEquals(0, Iterators.size(actual.listObjectsOfProperty(actual
                .createProperty("http://www.w3.org/ns/ldp#inlinedResource"))));
        verify(mockParent, never()).getProperties();
        verify(mockNode, never()).getNodes();
    }

    @Test
    public
            void
            shouldIncludeLinkedDataPlatformContainerInformation()
                                                                 throws RepositoryException,
                                                                 IOException {
        LOGGER.debug("Entering shouldIncludeLinkedDataPlatformContainerInformation()...");
        final NodeType mockPrimaryNodeType = mock(NodeType.class);
        when(mockPrimaryNodeType.getChildNodeDefinitions()).thenReturn(
                new NodeDefinition[] {mock(NodeDefinition.class)});
        when(mockNode.getPrimaryNodeType()).thenReturn(mockPrimaryNodeType);

        when(mockProperties.hasNext()).thenReturn(false);

        when(mockNode.getDepth()).thenReturn(0);
        when(mockNodes.hasNext()).thenReturn(false);
        when(mockNode.getNodes()).thenReturn(mockNodes);
        final Model actual = testObj.getJcrTreeModel(mockNode, 0, -1);
        logRDF(actual);
        assertTrue(actual.contains(testSubjects.getContext(), type, actual
                .createProperty("http://www.w3.org/ns/ldp#Page")));
        assertTrue(actual.contains(testSubjects.getContext(), actual
                .createProperty("http://www.w3.org/ns/ldp#membersInlined"),
                actual.createLiteral(TRUE.toString())));

        final Resource graphSubject = testSubjects.getGraphSubject(mockNode);


        assertTrue(actual.contains(graphSubject, type, actual
                .createProperty("http://www.w3.org/ns/ldp#Container")));

        assertTrue(actual.contains(graphSubject, actual
                .createProperty("http://www.w3.org/ns/ldp#membershipSubject"),
                graphSubject));
        assertTrue(actual
                .contains(
                        graphSubject,
                        actual.createProperty("http://www.w3.org/ns/ldp#membershipPredicate"),
                        HAS_CHILD));
        assertTrue(actual
                .contains(
                        graphSubject,
                        actual.createProperty("http://www.w3.org/ns/ldp#membershipObject"),
                        actual.createResource("http://www.w3.org/ns/ldp#MemberSubject")));
        LOGGER.debug("Leaving shouldIncludeLinkedDataPlatformContainerInformation()...");

    }

    @Test
    public final
            void
            shouldIncludeContainerInfoWithMixinTypeContainer()
                                                              throws RepositoryException, IOException {

        final NodeType mockPrimaryNodeType = mock(NodeType.class);
        final NodeType mockMixinNodeType = mock(NodeType.class);
        when(mockPrimaryNodeType.getChildNodeDefinitions()).thenReturn(
                new NodeDefinition[] {});

        when(mockMixinNodeType.getChildNodeDefinitions()).thenReturn(
                new NodeDefinition[] {mock(NodeDefinition.class)});
        when(mockNode.getPrimaryNodeType()).thenReturn(mockPrimaryNodeType);
        when(mockNode.getMixinNodeTypes()).thenReturn(
                new NodeType[] {mockMixinNodeType});

        when(mockProperties.hasNext()).thenReturn(false);

        when(mockNode.getDepth()).thenReturn(0);
        when(mockNodes.hasNext()).thenReturn(false);
        final Model actual = testObj.getJcrTreeModel(mockNode, 0, -1);

        assertTrue(actual.contains(testSubjects.getContext(), type, actual
                .createProperty("http://www.w3.org/ns/ldp#Page")));
        assertTrue(actual.contains(testSubjects.getContext(), actual
                .createProperty("http://www.w3.org/ns/ldp#membersInlined"),
                actual.createLiteral(TRUE.toString())));

        final Resource graphSubject = testSubjects.getGraphSubject(mockNode);
        assertTrue(actual.contains(graphSubject, type, actual
                .createProperty("http://www.w3.org/ns/ldp#Container")));

        assertTrue(actual.contains(graphSubject, actual
                .createProperty("http://www.w3.org/ns/ldp#membershipSubject"),
                graphSubject));
        assertTrue(actual
                .contains(
                        graphSubject,
                        actual.createProperty("http://www.w3.org/ns/ldp#membershipPredicate"),
                        HAS_CHILD));
        assertTrue(actual
                .contains(
                        graphSubject,
                        actual.createProperty("http://www.w3.org/ns/ldp#membershipObject"),
                        actual.createResource("http://www.w3.org/ns/ldp#MemberSubject")));
    }

    @Test
    public final
            void
            shouldNotIncludeContainerInfoIfItIsntContainer()
                                                            throws RepositoryException {

        final NodeType mockPrimaryNodeType = mock(NodeType.class);
        final NodeType mockMixinNodeType = mock(NodeType.class);
        when(mockPrimaryNodeType.getChildNodeDefinitions()).thenReturn(
                new NodeDefinition[] {});

        when(mockMixinNodeType.getChildNodeDefinitions()).thenReturn(
                new NodeDefinition[] {});
        when(mockNode.getPrimaryNodeType()).thenReturn(mockPrimaryNodeType);
        when(mockNode.getMixinNodeTypes()).thenReturn(
                new NodeType[] {mockMixinNodeType});

        when(mockProperties.hasNext()).thenReturn(false);

        when(mockNode.getDepth()).thenReturn(0);
        when(mockNodes.hasNext()).thenReturn(false);
        final Model actual = testObj.getJcrTreeModel(mockNode, 0, -1);
        assertTrue(actual.contains(testSubjects.getContext(), RDF.type, actual
                .createProperty("http://www.w3.org/ns/ldp#Page")));
        assertFalse(actual.contains(testSubjects.getContext(), actual
                .createProperty("http://www.w3.org/ns/ldp#membersInlined"),
                actual.createTypedLiteral(true)));

        final Resource graphSubject = testSubjects.getGraphSubject(mockNode);
        assertFalse(actual.contains(graphSubject, RDF.type, actual
                .createProperty("http://www.w3.org/ns/ldp#Container")));

        assertFalse(actual.contains(graphSubject, actual
                .createProperty("http://www.w3.org/ns/ldp#membershipSubject"),
                graphSubject));
        assertFalse(actual
                .contains(
                        graphSubject,
                        actual.createProperty("http://www.w3.org/ns/ldp#membershipPredicate"),
                        HAS_CHILD));
        assertFalse(actual
                .contains(
                        graphSubject,
                        actual.createProperty("http://www.w3.org/ns/ldp#membershipObject"),
                        actual.createResource("http://www.w3.org/ns/ldp#MemberSubject")));

    }

    @Test
    public final void
            shouldIncludeParentNodeInformation() throws RepositoryException {

        final NodeType mockPrimaryNodeType = mock(NodeType.class);
        when(mockPrimaryNodeType.getChildNodeDefinitions()).thenReturn(
                new NodeDefinition[] {mock(NodeDefinition.class)});
        when(mockNode.getPrimaryNodeType()).thenReturn(mockPrimaryNodeType);
        when(mockProperties.hasNext()).thenReturn(false);
        when(mockNode.getDepth()).thenReturn(2);
        when(mockNodes.hasNext()).thenReturn(false);
        final Model actual = testObj.getJcrTreeModel(mockNode, 0, -1);
        assertEquals(1, Iterators.size(actual.listObjectsOfProperty(HAS_CHILD)));
    }

    @Test
    public void shouldIncludeChildNodeInformation() throws RepositoryException, IOException {
        reset(mockChildNode, mockNodes, mockNode);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getNodes()).thenReturn(mockNodes, mockNodes2, mockNodes3);
        when(mockNode.getName()).thenReturn("mockNode");
        when(mockNode.getProperties()).thenReturn(mockProperties);
        when(mockNode.getDepth()).thenReturn(0);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});

        when(mockChildNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockChildNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});
        when(mockChildNode2.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockChildNode2.getMixinNodeTypes()).thenReturn(new NodeType[]{});
        when(mockChildNode3.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockChildNode3.getMixinNodeTypes()).thenReturn(new NodeType[]{});
        when(mockChildNode4.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockChildNode4.getMixinNodeTypes()).thenReturn(new NodeType[]{});
        when(mockChildNode5.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockChildNode5.getMixinNodeTypes()).thenReturn(new NodeType[]{});

        when(mockChildNode.getProperties()).thenReturn(mockProperties);
        when(mockChildNode2.getProperties()).thenReturn(mockProperties);
        when(mockChildNode3.getProperties()).thenReturn(mockProperties);
        when(mockChildNode4.getProperties()).thenReturn(mockProperties);
        when(mockChildNode5.getProperties()).thenReturn(mockProperties);

        when(mockChildNode.getName()).thenReturn("mockChildNode");
        when(mockChildNode2.getName()).thenReturn("mockChildNode2");
        when(mockChildNode3.getName()).thenReturn("mockChildNode3");
        when(mockChildNode4.getName()).thenReturn("mockChildNode4");
        when(mockChildNode5.getName()).thenReturn("mockChildNode5");

        when(mockChildNode.getParent()).thenReturn(mockNode);
        when(mockChildNode2.getParent()).thenReturn(mockNode);
        when(mockChildNode3.getParent()).thenReturn(mockNode);
        when(mockChildNode4.getParent()).thenReturn(mockNode);
        when(mockChildNode5.getParent()).thenReturn(mockNode);

        when(mockChildNode.getPath()).thenReturn("/test/jcr/1");
        when(mockChildNode2.getPath()).thenReturn("/test/jcr/2");
        when(mockChildNode3.getPath()).thenReturn("/test/jcr/3");
        when(mockChildNode4.getPath()).thenReturn("/test/jcr/4");
        when(mockChildNode5.getPath()).thenReturn("/test/jcr/5");

        when(mockNodes.hasNext()).thenReturn(true, true, true, true, true,
                false);
        when(mockNode.hasNodes()).thenReturn(true);
        when(mockNodes.nextNode()).thenReturn(mockChildNode, mockChildNode2,
                mockChildNode3, mockChildNode4, mockChildNode5);
        final Model actual = testObj.getJcrTreeModel(mockNode, 0, 0);
        LOGGER.debug("Retrieved RDF for shouldIncludeChildNodeInformation() as follows: ");
        logRDF(actual);
        assertEquals(5, Iterators.size(actual.listObjectsOfProperty(HAS_CHILD)));
    }

    @Test
    @Ignore("Disabled until we shift from 'window' to iterated modality")
    public void shouldIncludeFullChildNodeInformationInsideWindow()
        throws RepositoryException {
        reset(mockChildNode, mockNodes, mockNode);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getNodes()).thenReturn(mockNodes);
        when(mockNode.getName()).thenReturn("mockNode");
        when(mockNode.getProperties()).thenReturn(mockProperties);
        when(mockNode.getDepth()).thenReturn(0);
        when(mockChildNode.getName()).thenReturn("some-name");
        when(mockChildNode.getPath()).thenReturn("/test/jcr/1", "/test/jcr/4",
                "/test/jcr/5");
        when(mockFullChildNode.getName()).thenReturn("some-other-name");
        when(mockFullChildNode.getPath()).thenReturn("/test/jcr/2",
                "/test/jcr/3");
        when(mockFullChildNode.getProperties()).thenReturn(mockProperties);
        when(mockProperties.hasNext()).thenReturn(false);
        when(mockNode.hasNodes()).thenReturn(true);
        final Model actual = testObj.getJcrTreeModel(mockNode, 1, 2);
        assertEquals(2, Iterators.size(actual
                .listSubjectsWithProperty(HAS_PARENT)));
        verify(mockChildNode, never()).getProperties();
    }

    @Test
    public final void
            shouldMapRdfValuesToJcrPropertyValues() throws RepositoryException {
        when(mockValueFactoryFunc.apply(mockNode)).thenReturn(mockValueFactory);
        final Function<Node, ValueFactory> holdValueFactory = getValueFactory;
        FedoraTypesUtils.getValueFactory = mockValueFactoryFunc;

        try {
            RDFNode n = createResource(RESTAPI_NAMESPACE + "/abc");

            // node references
            when(mockSession.getNode("/abc")).thenReturn(mockNode);
            when(mockSession.nodeExists("/abc")).thenReturn(true);
            testObj.createValue(mockNode, n, REFERENCE);
            verify(mockValueFactory).createValue(mockNode, false);
            testObj.createValue(mockNode, n, WEAKREFERENCE);
            verify(mockValueFactory).createValue(mockNode, true);

            // uris
            testObj.createValue(mockNode, n, UNDEFINED);
            verify(mockValueFactory).createValue(RESTAPI_NAMESPACE + "/abc",
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
    public final void shouldAddPropertiesToModel() throws RepositoryException {
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
    public final void
            shouldAddMultivaluedPropertiesToModel() throws RepositoryException {
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
    public final void
            shouldMapJcrTypesToRdfDataTypes() throws RepositoryException {
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
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty,
                    mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createTypedLiteral(true)));

            mockValue = mock(Value.class);
            final Calendar mockCalendar = Calendar.getInstance();
            when(mockValue.getType()).thenReturn(DATE);
            when(mockValue.getDate()).thenReturn(mockCalendar);
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty,
                    mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createTypedLiteral(mockCalendar)));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(DECIMAL);
            when(mockValue.getDecimal()).thenReturn(BigDecimal.valueOf(0.0));
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty,
                    mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createTypedLiteral(BigDecimal.valueOf(0.0))));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(DOUBLE);
            when(mockValue.getDouble()).thenReturn((double) 0);
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty,
                    mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createTypedLiteral((double) 0)));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(LONG);
            when(mockValue.getLong()).thenReturn(0L);
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty,
                    mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createTypedLiteral(0L)));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(STRING);
            when(mockValue.getString()).thenReturn("XYZ");
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty,
                    mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createTypedLiteral("XYZ")));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(URI);
            when(mockValue.getString()).thenReturn("info:fedora");

            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty,
                    mockValue);

            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createResource("info:fedora")));

            mockValue = mock(Value.class);
            when(mockProperty.getSession()).thenReturn(mockSession);
            when(mockSession.getNodeByIdentifier("uuid")).thenReturn(mockNode);
            when(mockNode.getPath()).thenReturn("/abc");

            when(mockValue.getType()).thenReturn(REFERENCE);
            when(mockValue.getString()).thenReturn("uuid");
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty,
                    mockValue);

            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createResource(RESTAPI_NAMESPACE + "/abc")));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(WEAKREFERENCE);
            when(mockValue.getString()).thenReturn("uuid");
            when(mockNode.getPath()).thenReturn("/def");
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty,
                    mockValue);

            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createResource(RESTAPI_NAMESPACE + "/def")));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PATH);
            when(mockValue.getString()).thenReturn("/ghi");
            testObj.addPropertyToModel(mockSubject, mockModel, mockProperty,
                    mockValue);

            assertTrue(mockModel.contains(mockSubject, mockPredicate,
                    createResource(RESTAPI_NAMESPACE + "/ghi")));

        } finally {
            getPredicateForProperty = holdPredicate;
        }

    }

    @Test
    @Ignore
    public final void testJcrNodeContent() throws RepositoryException {
        addContentNode();
        when(mockProperties.hasNext()).thenReturn(false);
        when(mockNodes.hasNext()).thenReturn(false);

        final Model model = testObj.getJcrPropertiesModel(mockNode);

        assertTrue(model != null);
    }

    @Test
    public final void testJcrNodeIteratorModel() throws RepositoryException {
        when(mockNodes.hasNext()).thenReturn(false);
        final Model model =
            testObj.getJcrPropertiesModel(
                    new org.fcrepo.kernel.utils.iterators.NodeIterator(
                            mockNodes), mockResource);
        assertTrue(model != null);
    }

    @Test
    public final
            void
            testJcrNodeIteratorAddsPredicatesForEachNode()
                                                          throws RepositoryException {
        final Resource mockResource =
            createResource(RESTAPI_NAMESPACE + "/search/resource");
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
        when(mockNode1.getProperties()).thenReturn(mockProperties);
        when(mockNode2.getProperties()).thenReturn(mockProperties);
        when(mockNode3.getProperties()).thenReturn(mockProperties);

        final Iterator<Node> mockIterator =
            asList(mockNode1, mockNode2, mockNode3).iterator();
        final Model model =
            testObj.getJcrPropertiesModel(mockIterator, mockResource);
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
            new FixityResult(mockCacheEntry, 123, new URI(testFixityUri));
        mockResult.status.add(BAD_CHECKSUM);
        mockResult.status.add(BAD_SIZE);

        final List<FixityResult> mockBlobs = asList(mockResult);

        final Model fixityResultsModel =
            testObj.getJcrPropertiesModel(mockNode, mockBlobs);

        logRDF(fixityResultsModel);
        assertTrue(fixityResultsModel.contains(null, IS_FIXITY_RESULT_OF,
                createResource(RESTAPI_NAMESPACE + "/test/jcr")));
        assertTrue(fixityResultsModel.contains(null, HAS_COMPUTED_CHECKSUM,
                createResource(testFixityUri)));
        assertTrue(fixityResultsModel.contains(null, HAS_COMPUTED_SIZE,
                createTypedLiteral(123)));

    }

    @Test
    public final void testGetJcrNamespaceModel() throws Exception {
        final Model jcrNamespaceModel = testObj.getJcrNamespaceModel();
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
    public final void testGetJcrVersionsModel() throws Exception {

        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockVersionManager.getVersionHistory(mockNode.getPath()))
                .thenReturn(mockVersionHistory);

        when(mockVersionIterator.hasNext()).thenReturn(true, false);
        when(mockFrozenNode.getPath()).thenReturn(
                "/jcr:system/versions/test/jcr");
        when(mockFrozenNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockVersion.getFrozenNode()).thenReturn(mockFrozenNode);
        when(mockVersionIterator.nextVersion()).thenReturn(mockVersion);
        when(mockVersionHistory.getAllVersions()).thenReturn(
                mockVersionIterator);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionHistory.getVersionLabels(mockVersion)).thenReturn(
                new String[] {"abc"});

        when(mockProperties.hasNext()).thenReturn(false);
        when(mockFrozenNode.getProperties()).thenReturn(mockProperties);
        final Model actual = testObj.getJcrVersionPropertiesModel(mockNode);

        assertTrue(actual.contains(testSubjects.getGraphSubject(mockNode),
                HAS_VERSION, testSubjects.getGraphSubject(mockFrozenNode)));
        assertTrue(actual.contains(
                testSubjects.getGraphSubject(mockFrozenNode),
                HAS_VERSION_LABEL, actual.createLiteral("abc")));
    }

    @Test
    public final void testIsInternalProperty() throws Exception {
        assertTrue(testObj.isInternalProperty(mockNode, createProperty(
                REPOSITORY_NAMESPACE, "some-property")));
        assertTrue(testObj.isInternalProperty(mockNode, createProperty(
                "http://www.jcp.org/jcr/1.0", "some-property")));
        assertTrue(testObj.isInternalProperty(mockNode,
                createProperty("http://www.w3.org/ns/ldp#some-property")));
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

        when(getNamespaceRegistry(mockSession)).thenReturn(mockNsRegistry);
        when(getNamespaceRegistry(mockNode)).thenReturn(mockNsRegistry);
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

    private void logRDF(final Model rdf) throws IOException {
        try (final Writer writer = new StringWriter()) {
            rdf.write(writer);
            LOGGER.debug("Found model: {}", writer);
        }
    }

    @Mock
    private Property mockPredicate;

    @Mock
    private Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> mockPredicateFactoryFunc;

    @Mock
    private NodeIterator mockNodes, mockNodes2, mockNodes3;

    @Mock
    private Function<Node, ValueFactory> mockValueFactoryFunc;

    @Mock
    private Node mockNode, mockNode1, mockNode2, mockNode3;

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
    private javax.jcr.Property mockProperty, mockBinaryProperty;

    @Mock
    private Value mockValue;

    @Mock
    private Repository mockRepository;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private PropertyIterator mockProperties, mockProperties2;

    @Mock
    private PropertyIterator mockParentProperties;

    @Mock
    private LowLevelStorageService mockLowLevelStorageService;

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
    private Node mockChildNode, mockChildNode2, mockChildNode3, mockChildNode4,
            mockChildNode5;

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
}
