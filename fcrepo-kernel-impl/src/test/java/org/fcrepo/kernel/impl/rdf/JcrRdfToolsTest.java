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
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static javax.jcr.PropertyType.NAME;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.STRING;
import static javax.jcr.PropertyType.UNDEFINED;
import static javax.jcr.PropertyType.URI;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.impl.rdf.JcrRdfTools.getJcrNamespaceForRDFNamespace;
import static org.fcrepo.kernel.impl.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Calendar;
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
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.impl.testutilities.TestPropertyIterator;
import org.fcrepo.kernel.utils.CacheEntry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.value.BinaryValue;
import org.slf4j.Logger;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Function;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * <p>JcrRdfToolsTest class.</p>
 *
 * @author awoods
 */
public class JcrRdfToolsTest implements FedoraJcrTypes {

    private static final Logger LOGGER = getLogger(JcrRdfToolsTest.class);

    private DefaultIdentifierTranslator testSubjects;

    private JcrRdfTools testObj;

    private static final String mockPredicateName =
        "http://example.com#someProperty";

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
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);
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
    public final void shouldMapUriValuesToJcrPropertyValues()
            throws RepositoryException {
        final RDFNode n = testSubjects.toDomain("/abc");

        when(mockValueFactory.createValue(n.asResource().getURI(), URI)).thenReturn(mockValue);
        // uris
        testObj.createValue(mockValueFactory, n, UNDEFINED);
        verify(mockValueFactory).createValue(n.asResource().getURI(),
                PropertyType.URI);
    }


    @Test
    public final void shouldMapBooleanValuesToJcrPropertyValues()
            throws RepositoryException {
        when(mockValueFactory.createValue(true)).thenReturn(mockValue);
        final RDFNode n = createTypedLiteral(true);

        testObj.createValue(mockValueFactory, n, 0);
        verify(mockValueFactory).createValue(true);
    }


    @Test
    public final void shouldMapByteValuesToJcrPropertyValues()
            throws RepositoryException {
        when(mockValueFactory.createValue((byte)1)).thenReturn(mockValue);
        final RDFNode n = createTypedLiteral("1", XSDbyte);

        testObj.createValue(mockValueFactory, n, 0);
        verify(mockValueFactory).createValue((byte) 1);
    }


    @Test
    public final void shouldMapDoubleValuesToJcrPropertyValues()
            throws RepositoryException {
        when(mockValueFactory.createValue((double)2)).thenReturn(mockValue);
        final RDFNode n = createTypedLiteral((double) 2);


        testObj.createValue(mockValueFactory, n, 0);
        verify(mockValueFactory).createValue((double) 2);
    }


    @Test
    public final void shouldMapFloatValuesToJcrPropertyValues()
            throws RepositoryException {
        when(mockValueFactory.createValue((float)3)).thenReturn(mockValue);
        final RDFNode n = createTypedLiteral((float) 3);


        testObj.createValue(mockValueFactory, n, 0);
        verify(mockValueFactory).createValue((float) 3);
    }


    @Test
    public final void shouldMapIntValuesToJcrPropertyValues()
            throws RepositoryException {
        when(mockValueFactory.createValue(4)).thenReturn(mockValue);
        final RDFNode n = createTypedLiteral(4);

        testObj.createValue(mockValueFactory, n, 0);
        verify(mockValueFactory).createValue(4);
    }


    @Test
    public final void shouldMapLongValuesToJcrPropertyValues()
            throws RepositoryException {
        when(mockValueFactory.createValue((long)5)).thenReturn(mockValue);
        final RDFNode n = createTypedLiteral("5", XSDlong);

        testObj.createValue(mockValueFactory, n, 0);
        verify(mockValueFactory).createValue(5);
    }


    @Test
    public final void shouldMapShortValuesToJcrPropertyValues()
            throws RepositoryException {

        when(mockValueFactory.createValue((short)6)).thenReturn(mockValue);
        final RDFNode n = createTypedLiteral("6", XSDshort);


        testObj.createValue(mockValueFactory, n, 0);
        verify(mockValueFactory).createValue((short) 6);
    }


    @Test
    public final void shouldMapCalendarValuesToJcrPropertyValues()
            throws RepositoryException {
        final Calendar calendar = Calendar.getInstance();
        final RDFNode n = createTypedLiteral(calendar);

        when(mockValueFactory.createValue(any(Calendar.class))).thenReturn(mockValue);

        testObj.createValue(mockValueFactory, n, 0);
        verify(mockValueFactory).createValue(any(Calendar.class));
    }


    @Test
    public final void shouldMapStringValuesToJcrPropertyValues()
            throws RepositoryException {
        when(mockValueFactory.createValue("string", STRING)).thenReturn(mockValue);
        final RDFNode n = createTypedLiteral("string");

        testObj.createValue(mockValueFactory, n, 0);
        verify(mockValueFactory).createValue("string", STRING);
    }


    @Test
    public final void shouldMapStringNameValuesToJcrPropertyValues()
            throws RepositoryException {
        when(mockValueFactory.createValue("string", NAME)).thenReturn(mockValue);
        final RDFNode n = createTypedLiteral("string");

        testObj.createValue(mockValueFactory, n, NAME);
        verify(mockValueFactory).createValue("string", NAME);

    }

    @Test(expected = ValueFormatException.class)
    public final void shouldMapRdfValuesToJcrPropertyValuesError()
            throws RepositoryException {

        // non-uri references - error
        final RDFNode n = createResource();
        testObj.createValue(mockValueFactory, n, REFERENCE);
    }

    @Test
    public void testCreateValueForNode() throws RepositoryException {
        when(mockNode.getSession().getValueFactory()).thenReturn(mockValueFactory);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getPropertyDefinitions()).thenReturn(new PropertyDefinition[]{});
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});

        when(mockValueFactory.createValue(true)).thenReturn(mockValue);
        final RDFNode n = createTypedLiteral(true);
        testObj.createValue(mockNode, n, "some:boolean");
        verify(mockValueFactory).createValue(true);
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


}
