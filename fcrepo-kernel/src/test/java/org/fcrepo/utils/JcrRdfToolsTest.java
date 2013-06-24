
package org.fcrepo.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
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

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import org.fcrepo.RdfLexicon;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.rdf.impl.DefaultGraphSubjects;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.powermock.api.mockito.PowerMockito;

import com.google.common.base.Function;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;

public class JcrRdfToolsTest {

    private static final Logger LOGGER = getLogger(JcrRdfToolsTest.class);

    private Node mockNode;

    private NamespaceRegistry mockNsRegistry;

    private GraphSubjects testSubjects;

    private Session mockSession;
    private Repository mockRepository;

    @Before
    public void setUp() throws RepositoryException {

        mockSession = mock(Session.class);
        testSubjects = new DefaultGraphSubjects();
        mockNode = mock(Node.class);
        when(mockNode.getSession()).thenReturn(mockSession);

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
        when(mockNsRegistry.getURI("jcr")).thenReturn("http://www.jcp.org/jcr/1.0");
        when(mockNsRegistry.getURI("some-prefix")).thenReturn("registered-uri#");
        when(mockNsRegistry.getPrefixes()).thenReturn(new String[] { "jcr", "some-prefix"});

        final Workspace mockWorkspace = mock(Workspace.class);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNsRegistry);

        mockRepository = mock(Repository.class);
        when(mockSession.getRepository()).thenReturn(mockRepository);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);

        when(NamespaceTools.getNamespaceRegistry(mockSession)).thenReturn(mockNsRegistry);

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

        assertEquals("info:fedora/abc",
                JcrRdfTools.getGraphSubject(testSubjects, mockNode)
                .toString());
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
        assertEquals(mockNode,
                JcrRdfTools.getNodeFromGraphSubject(testSubjects, mockSession,
                ResourceFactory.createResource("info:fedora/abc")));
    }

    @Test
    public void shouldMapRDFContentResourcesToJcrContentNodes()
            throws RepositoryException {
        when(mockSession.nodeExists("/abc/jcr:content")).thenReturn(true);
        when(mockSession.getNode("/abc/jcr:content")).thenReturn(mockNode);
        assertEquals(mockNode,
                JcrRdfTools.getNodeFromGraphSubject(testSubjects, mockSession,
                ResourceFactory.createResource("info:fedora/abc/fcr:content")));
    }

    @Test
    public void shouldReturnNullIfItFailstoMapRDFResourcesToJcrNodes()
            throws RepositoryException {
        when(mockSession.nodeExists("/does-not-exist")).thenReturn(false);
        assertNull("should receive null for a non-JCR resource",
                JcrRdfTools.getNodeFromGraphSubject(
                        testSubjects, mockSession, ResourceFactory
                        .createResource("this-is-not-a-fedora-node/abc")));
        assertNull("should receive null a JCR node that isn't found",
                JcrRdfTools.getNodeFromGraphSubject(testSubjects, mockSession,
                        ResourceFactory
                                .createResource("info:fedora/does-not-exist")));
    }

    @Test
    public void shouldDetermineIfAGraphResourceIsAJcrNode() throws RepositoryException {
        GraphSubjects mockFactory = mock(GraphSubjects.class);
        Resource mockSubject = mock(Resource.class);
        when(mockFactory.isFedoraGraphSubject(mockSubject)).thenReturn(true);
        assertTrue(JcrRdfTools.isFedoraGraphSubject(mockFactory, mockSubject));

        verify(mockFactory).isFedoraGraphSubject(mockSubject);
    }

    @Test
    public void testGetPropertiesModel() throws RepositoryException {
        final String fakeInternalUri = "info:fedora/test/jcr";
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
        PowerMockito.mockStatic(NamespaceTools.class);
        final NamespaceRegistry mockNames = mock(NamespaceRegistry.class);
        final String[] testPrefixes = new String[] {"jcr"};
        when(mockNames.getPrefixes()).thenReturn(testPrefixes);
        when(mockNames.getURI("jcr")).thenReturn(fakeInternalUri);
        when(NamespaceTools.getNamespaceRegistry(mockNode)).thenReturn(
                mockNames);
        final PropertyIterator mockProperties =
                mock(PropertyIterator.class);
        when(mockNode.getProperties()).thenReturn(mockProperties);
        when(mockParent.getProperties()).thenReturn(mockProperties);
        when(mockProperties.hasNext()).thenReturn(false);

        final Model actual = JcrRdfTools.getJcrPropertiesModel(testSubjects, mockNode);
        assertEquals(fakeInternalUri, actual.getNsPrefixURI("fedora-internal"));

        //TODO exercise the jcr:content child node logic
        //TODO exercise non-empty PropertyIterator
    }


    @Test
    public void shouldExcludeBinaryProperties() throws RepositoryException {
        String fakeInternalUri = "info:fedora/test/jcr";
        Node mockParent = mock(Node.class);
        when(mockParent.getPath()).thenReturn("/test");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getParent()).thenReturn(mockParent);
        when(mockNode.getDepth()).thenReturn(2);
        final NodeType nodeType = mock(NodeType.class);
        when(mockNode.getPrimaryNodeType()).thenReturn(nodeType);
        when(mockNode.getPrimaryNodeType().getName()).thenReturn("fedora:object");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        PowerMockito.mockStatic(NamespaceTools.class);
        NamespaceRegistry mockNames = mock(NamespaceRegistry.class);
        String[] testPrefixes = new String[]{"jcr"};
        when(mockNames.getPrefixes()).thenReturn(testPrefixes);
        when(mockNames.getURI("jcr")).thenReturn(fakeInternalUri);
        when(NamespaceTools.getNamespaceRegistry(mockNode)).thenReturn(mockNames);
        PropertyIterator mockProperties = mock(PropertyIterator.class);
        when(mockNode.getProperties()).thenReturn(mockProperties);
        when(mockParent.getProperties()).thenReturn(mockProperties);
        when(mockProperties.hasNext()).thenReturn(true, false);
        javax.jcr.Property mockProperty = mock(javax.jcr.Property.class);
        Value mockValue = mock(Value.class);
        when(mockProperty.getValue()).thenReturn(mockValue);
        when(mockValue.getType()).thenReturn(PropertyType.BINARY);
        when(mockProperties.nextProperty()).thenReturn(mockProperty);

        Model actual = JcrRdfTools.getJcrPropertiesModel(testSubjects, mockNode);
        assertEquals(0, actual.size());
    }


    @Test
    public void shouldIncludeParentNodeInformation() throws RepositoryException {
        Node mockParent = mock(Node.class);
        when(mockParent.getPath()).thenReturn("/test");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getParent()).thenReturn(mockParent);

        NodeIterator mockIterator = mock(NodeIterator.class);
        when(mockIterator.hasNext()).thenReturn(false);
        when(mockNode.getNodes()).thenReturn(mockIterator);
        Model actual = JcrRdfTools.getJcrTreeModel(testSubjects, mockNode, 0, -1);
        assertEquals(1, actual.size());
    }

    @Test
    public void shouldIncludeChildNodeInformation() throws RepositoryException {
        Node mockParent = mock(Node.class);
        when(mockParent.getPath()).thenReturn("/test");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getParent()).thenReturn(mockParent);

        Node mockChildNode = mock(Node.class);
        when(mockChildNode.getName()).thenReturn("some-name");

        when(mockChildNode.getPath()).thenReturn("/test/jcr/1","/test/jcr/2","/test/jcr/3","/test/jcr/4","/test/jcr/5");
        NodeIterator mockIterator = mock(NodeIterator.class);
        when(mockIterator.hasNext()).thenReturn(true, true, true, true, true, false);
        when(mockIterator.nextNode()).thenReturn(mockChildNode);


        when(mockNode.getNodes()).thenReturn(mockIterator);
        Model actual = JcrRdfTools.getJcrTreeModel(testSubjects, mockNode, 0, 0);
        assertEquals(5*2 + 1, actual.size());
    }

    @Test
    public void shouldIncludeFullChildNodeInformationInsideWindow() throws RepositoryException {
        Node mockParent = mock(Node.class);
        when(mockParent.getPath()).thenReturn("/test");
        when(mockNode.getPath()).thenReturn("/test/jcr");
        when(mockNode.getParent()).thenReturn(mockParent);

        Node mockChildNode = mock(Node.class);
        when(mockChildNode.getName()).thenReturn("some-name");
        when(mockChildNode.getPath()).thenReturn("/test/jcr/1","/test/jcr/4","/test/jcr/5");

        Node mockFullChildNode = mock(Node.class);
        when(mockFullChildNode.getName()).thenReturn("some-other-name");
        when(mockFullChildNode.getPath()).thenReturn("/test/jcr/2", "/test/jcr/3");

        PropertyIterator mockProperties = mock(PropertyIterator.class);
        when(mockFullChildNode.getProperties()).thenReturn(mockProperties);

        when(mockProperties.hasNext()).thenReturn(false);

        NodeIterator mockIterator = mock(NodeIterator.class);
        when(mockIterator.hasNext()).thenReturn(true, true, true, true, true, false);
        when(mockIterator.nextNode()).thenReturn(mockChildNode, mockFullChildNode, mockFullChildNode,mockChildNode, mockChildNode);


        when(mockNode.getNodes()).thenReturn(mockIterator);
        Model actual = JcrRdfTools.getJcrTreeModel(testSubjects, mockNode, 1, 2);
        assertEquals(5*2 + 1, actual.size());
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
        final Resource mockSubject = ResourceFactory.createResource("some-resource-uri");
        final Model mockModel = ModelFactory.createDefaultModel();
        final Property mockPredicate = mockModel.createProperty("some-predicate-uri");

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
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate, ResourceFactory.createTypedLiteral(true)));

            mockValue = mock(Value.class);
            Calendar mockCalendar = Calendar.getInstance();
            when(mockValue.getType()).thenReturn(PropertyType.DATE);
            when(mockValue.getDate()).thenReturn(mockCalendar);
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate, ResourceFactory.createTypedLiteral(mockCalendar)));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.DECIMAL);
            when(mockValue.getDecimal()).thenReturn(BigDecimal.valueOf(0.0));
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate, ResourceFactory.createTypedLiteral(BigDecimal.valueOf(0.0))));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.DOUBLE);
            when(mockValue.getDouble()).thenReturn((double)0);
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate, ResourceFactory.createTypedLiteral((double)0)));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.LONG);
            when(mockValue.getLong()).thenReturn(0L);
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate, ResourceFactory.createTypedLiteral(0L)));


            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.STRING);
            when(mockValue.getString()).thenReturn("XYZ");
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);
            assertTrue(mockModel.contains(mockSubject, mockPredicate, ResourceFactory.createTypedLiteral("XYZ")));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.URI);
            when(mockValue.getString()).thenReturn("info:fedora");

            JcrRdfTools.addPropertyToModel(mockSubject, mockModel,
                                                  mockProperty, mockValue);

            assertTrue(mockModel.contains(mockSubject, mockPredicate, ResourceFactory.createResource("info:fedora")));

            mockValue = mock(Value.class);
            when(mockProperty.getSession()).thenReturn(mockSession);
            when(mockSession.getNodeByIdentifier("uuid")).thenReturn(mockNode);
            when(mockNode.getPath()).thenReturn("/abc");

            when(mockValue.getType()).thenReturn(PropertyType.REFERENCE);
            when(mockValue.getString()).thenReturn("uuid");
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel,
                    mockProperty, mockValue);


            assertTrue(mockModel.contains(mockSubject, mockPredicate, ResourceFactory.createResource("info:fedora/abc")));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.WEAKREFERENCE);
            when(mockValue.getString()).thenReturn("uuid");
            when(mockNode.getPath()).thenReturn("/def");
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel,
                    mockProperty, mockValue);


            assertTrue(mockModel.contains(mockSubject, mockPredicate, ResourceFactory.createResource("info:fedora/def")));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.PATH);
            when(mockValue.getString()).thenReturn("/ghi");
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel,
                    mockProperty, mockValue);

            assertTrue(mockModel.contains(mockSubject, mockPredicate, ResourceFactory.createResource("info:fedora/ghi")));


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

        PropertyIterator mockProperties = mock(PropertyIterator.class);
        when(mockProperties.hasNext()).thenReturn(false);
        when(mockNode.getProperties()).thenReturn(mockProperties);

        when(mockNode.getPath()).thenReturn("/path/to/node");
        NodeIterator mockIterator = mock(NodeIterator.class);
        when(mockIterator.hasNext()).thenReturn(false);
        when(mockNode.getNodes()).thenReturn(mockIterator);

        Node mockContent = mock(Node.class);
        when(mockContent.getPath()).thenReturn("/path/to/node/content");
        when(mockContent.getProperties()).thenReturn(mockProperties);
        when(mockContent.getSession()).thenReturn(mockSession);

        when(mockNode.hasNode(JcrConstants.JCR_CONTENT)).thenReturn(true);
        when(mockNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(mockContent);
        Model model = JcrRdfTools.getJcrPropertiesModel(testSubjects, mockNode);

        assertTrue(model != null);
    }

    @Test
    public void testJcrNodeIteratorModel() throws RepositoryException {

        Resource mockResource = mock(Resource.class);
        NodeIterator mockIterator = mock(NodeIterator.class);
        when(mockIterator.hasNext()).thenReturn(false);
        final Model model = JcrRdfTools.getJcrNodeIteratorModel(testSubjects, mockIterator, mockResource);
        assertTrue(model != null);
    }

    @Test
    public void testJcrNodeIteratorAddsPredicatesForEachNode() throws RepositoryException {
        Resource mockResource = ResourceFactory.createResource("info:fedora/search/resource");
        Node mockNode1 = mock(Node.class);
        Node mockNode2 = mock(Node.class);
        Node mockNode3 = mock(Node.class);
        PropertyIterator mockProperties = mock(PropertyIterator.class);
        when(mockProperties.hasNext()).thenReturn(false);
        when(mockNode1.getProperties()).thenReturn(mockProperties);
        when(mockNode1.getSession()).thenReturn(mockSession);

        when(mockNode1.getPath()).thenReturn("/path/to/first/node");
        when(mockNode2.getPath()).thenReturn("/second/path/to/node");
        when(mockNode3.getPath()).thenReturn("/third/path/to/node");
        when(mockNode1.getProperties()).thenReturn(mockProperties);
        when(mockNode2.getProperties()).thenReturn(mockProperties);
        when(mockNode3.getProperties()).thenReturn(mockProperties);

        Iterator<Node> mockIterator = Arrays.asList(mockNode1, mockNode2, mockNode3).iterator();
        final Model model = JcrRdfTools.getJcrNodeIteratorModel(testSubjects, mockIterator, mockResource);
        assertEquals(3, model.listObjectsOfProperty(RdfLexicon.HAS_MEMBER_OF_RESULT).toSet().size());
    }

    @Test
    public void testGetFixityResultsModel() throws RepositoryException, URISyntaxException {
        LowLevelCacheEntry mockEntry = mock(LowLevelCacheEntry.class);
        when(mockEntry.getExternalIdentifier()).thenReturn("xyz");
        final FixityResult mockResult = new FixityResult(mockEntry, 123, new URI("abc"));
        mockResult.status.add(FixityResult.FixityState.BAD_CHECKSUM);
        mockResult.status.add(FixityResult.FixityState.BAD_SIZE);

        final List<FixityResult> mockBlobs = Arrays.asList(mockResult);
        when(mockNode.getPath()).thenReturn("/path/to/node");

        PropertyIterator mockProperties = mock(PropertyIterator.class);
        when(mockProperties.hasNext()).thenReturn(false);
        when(mockNode.getProperties()).thenReturn(mockProperties);

        final Model fixityResultsModel = JcrRdfTools.getFixityResultsModel(testSubjects, mockNode, mockBlobs);

        LOGGER.info("Got graph {}", fixityResultsModel);

        GraphStore gs =  GraphStoreFactory.create(fixityResultsModel);
        assertTrue(gs.contains(com.hp.hpl.jena.graph.Node.ANY,
                                      com.hp.hpl.jena.graph.Node.ANY,
                                      RdfLexicon.IS_FIXITY_RESULT_OF.asNode(),
                                      ResourceFactory.createResource("info:fedora/path/to/node").asNode()));
        assertTrue(gs.contains(com.hp.hpl.jena.graph.Node.ANY,
                                      com.hp.hpl.jena.graph.Node.ANY,
                                      RdfLexicon.HAS_COMPUTED_CHECKSUM.asNode(),
                                      ResourceFactory.createResource("abc").asNode()));
        assertTrue(gs.contains(com.hp.hpl.jena.graph.Node.ANY,
                                      com.hp.hpl.jena.graph.Node.ANY,
                                      RdfLexicon.HAS_COMPUTED_SIZE.asNode(),
                                      ResourceFactory.createTypedLiteral(123).asNode()));

    }

    @Test
    public void testGetJcrNamespaceModel() throws Exception {
        final Model jcrNamespaceModel = JcrRdfTools.getJcrNamespaceModel(mockSession);
        assertTrue(jcrNamespaceModel.contains(ResourceFactory.createResource("info:fedora/fedora-system:def/internal#"), RdfLexicon.HAS_NAMESPACE_PREFIX, "fedora-internal"));
        assertTrue(jcrNamespaceModel.contains(ResourceFactory.createResource("registered-uri#"), RdfLexicon.HAS_NAMESPACE_PREFIX, "some-prefix"));
    }
}
