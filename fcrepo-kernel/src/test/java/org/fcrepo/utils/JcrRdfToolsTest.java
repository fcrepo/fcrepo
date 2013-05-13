package org.fcrepo.utils;

import com.google.common.base.Function;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.NamespaceRegistry;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import java.util.Arrays;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;


public class JcrRdfToolsTest {

    private Node mockNode;
    private Function<Node,NamespaceRegistry> mockNsProc;
    private NamespaceRegistry mockNsRegistry;
    private Function<Node, NamespaceRegistry> holdNsRegistry;
    private Session mockSession;

    @Before
    public void setUp() throws RepositoryException {

        mockSession = mock(Session.class);
        mockNode = mock(Node.class);
        when(mockNode.getSession()).thenReturn(mockSession);

        mockNsRegistry = mock(NamespaceRegistry.class);

        when(mockNsRegistry.isRegisteredUri("registered-uri#")).thenReturn(true);
        when(mockNsRegistry.isRegisteredUri("not-registered-uri#")).thenReturn(false);
        when(mockNsRegistry.isRegisteredUri("http://www.jcp.org/jcr/1.0")).thenReturn(true);
        when(mockNsRegistry.getPrefix("http://www.jcp.org/jcr/1.0")).thenReturn("jcr");
        when(mockNsRegistry.getPrefix("registered-uri#")).thenReturn("some-prefix");

        mockNsProc = mock(Function.class);
        when(mockNsProc.apply(mockNode)).thenReturn(mockNsRegistry);

        holdNsRegistry = NamespaceTools.getNamespaceRegistry;
        NamespaceTools.getNamespaceRegistry = mockNsProc;

    }

    @After
    public void cleanUp() {
        NamespaceTools.getNamespaceRegistry = holdNsRegistry;
    }

    @Test
    public void shouldMapInternalJcrNamespaceToFcrepoNamespace() {
        assertEquals("info:fedora/fedora-system:def/internal#", JcrRdfTools.getRDFNamespaceForJcrNamespace("http://www.jcp.org/jcr/1.0"));
    }

    @Test
    public void shouldMapFcrepoNamespaceToJcrNamespace() {
        assertEquals("http://www.jcp.org/jcr/1.0", JcrRdfTools.getJcrNamespaceForRDFNamespace("info:fedora/fedora-system:def/internal#"));
    }

    @Test
    public void shouldPassThroughOtherNamespaceValues() {
        assertEquals("some-namespace-uri", JcrRdfTools.getJcrNamespaceForRDFNamespace("some-namespace-uri"));
        assertEquals("some-namespace-uri", JcrRdfTools.getRDFNamespaceForJcrNamespace("some-namespace-uri"));
    }

    @Test
    public void shouldMapRdfPredicatesToJcrProperties() throws RepositoryException {

        Property p = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#", "uuid");
        assertEquals("jcr:uuid", JcrRdfTools.getPropertyNameFromPredicate(mockNode, p));

    }

    @Test
    public void shouldReuseRegisteredNamespaces() throws RepositoryException {
        Property p = ResourceFactory.createProperty("registered-uri#", "uuid");
        assertEquals("some-prefix:uuid", JcrRdfTools.getPropertyNameFromPredicate(mockNode, p));
    }

    @Test
    public void shouldRegisterUnknownUris() throws RepositoryException {
        when(mockNsRegistry.registerNamespace("not-registered-uri#")).thenReturn("ns001");
        Property p = ResourceFactory.createProperty("not-registered-uri#", "uuid");
        assertEquals("ns001:uuid", JcrRdfTools.getPropertyNameFromPredicate(mockNode, p));
    }

    @Test
    public void shouldMapRdfValuesToJcrPropertyValues() throws RepositoryException {
        ValueFactory mockValueFactory = mock(ValueFactory.class);

        Function<Node, ValueFactory> mockValueFactoryFunc = mock(Function.class);
        when(mockValueFactoryFunc.apply(mockNode)).thenReturn(mockValueFactory);

        Function<Node, ValueFactory> holdValueFactory = FedoraTypesUtils.getValueFactory;
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
            verify(mockValueFactory).createValue("info:fedora/abc", PropertyType.URI);

            // other random resources
            n = ResourceFactory.createResource();
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue(n.toString(), PropertyType.UNDEFINED);

            // undeclared types, but infer them from rdf types

            n = ResourceFactory.createTypedLiteral(true);
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue(true);

            n = ResourceFactory.createTypedLiteral((byte) 1);
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue((byte) 1);

            n = ResourceFactory.createTypedLiteral((double)2);
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue((double)2);

            n = ResourceFactory.createTypedLiteral((float)3);
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue((float)3);

            n = ResourceFactory.createTypedLiteral(4);
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue(4);

            n = ResourceFactory.createTypedLiteral((long)5);
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue((long)5);

            n = ResourceFactory.createTypedLiteral((short)6);
            JcrRdfTools.createValue(mockNode, n, 0);
            verify(mockValueFactory).createValue((short)6);

            Calendar calendar = Calendar.getInstance();
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
        javax.jcr.Property mockProperty = mock(javax.jcr.Property.class);
        Property mockPredicate = mock(Property.class);

        Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> mockPredicateFactoryFunc = mock(Function.class);
        when(mockPredicateFactoryFunc.apply(mockProperty)).thenReturn(mockPredicate);

        Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> holdPredicate = FedoraTypesUtils.getPredicateForProperty;
        FedoraTypesUtils.getPredicateForProperty = mockPredicateFactoryFunc;


        try {
            Resource mockSubject = mock(Resource.class);
            Model mockModel = mock(Model.class);

            Value mockValue = mock(Value.class);
            when(mockValue.getString()).thenReturn("");

            when(mockProperty.isMultiple()).thenReturn(false);
            when(mockProperty.getValue()).thenReturn(mockValue);

            JcrRdfTools.addPropertyToModel(mockSubject, mockModel, mockProperty);
            verify(mockModel).add(mockSubject, mockPredicate, "");

        } finally {
            FedoraTypesUtils.getPredicateForProperty = holdPredicate;
        }

    }

    @Test
    public void shouldAddMultivaluedPropertiesToModel() throws RepositoryException {
        javax.jcr.Property mockProperty = mock(javax.jcr.Property.class);
        Property mockPredicate = mock(Property.class);

        Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> mockPredicateFactoryFunc = mock(Function.class);
        when(mockPredicateFactoryFunc.apply(mockProperty)).thenReturn(mockPredicate);

        Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> holdPredicate = FedoraTypesUtils.getPredicateForProperty;
        FedoraTypesUtils.getPredicateForProperty = mockPredicateFactoryFunc;


        try {
            Resource mockSubject = mock(Resource.class);
            Model mockModel = mock(Model.class);

            Value mockValue = mock(Value.class);
            when(mockValue.getString()).thenReturn("1");


            Value mockValue2 = mock(Value.class);
            when(mockValue2.getString()).thenReturn("2");

            when(mockProperty.isMultiple()).thenReturn(true);
            when(mockProperty.getValues()).thenReturn(Arrays.asList(mockValue, mockValue2).toArray(new Value[2]));

            JcrRdfTools.addPropertyToModel(mockSubject, mockModel, mockProperty);
            verify(mockModel).add(mockSubject, mockPredicate, "1");
            verify(mockModel).add(mockSubject, mockPredicate, "2");

        } finally {
            FedoraTypesUtils.getPredicateForProperty = holdPredicate;
        }

    }

    @Test
    public void shouldMapJcrTypesToRdfDataTypes() throws RepositoryException {
        javax.jcr.Property mockProperty = mock(javax.jcr.Property.class);
        Property mockPredicate = mock(Property.class);

        Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> mockPredicateFactoryFunc = mock(Function.class);
        when(mockPredicateFactoryFunc.apply(mockProperty)).thenReturn(mockPredicate);

        Function<javax.jcr.Property, com.hp.hpl.jena.rdf.model.Property> holdPredicate = FedoraTypesUtils.getPredicateForProperty;
        FedoraTypesUtils.getPredicateForProperty = mockPredicateFactoryFunc;


        try {
            Resource mockSubject = mock(Resource.class);
            Model mockModel = mock(Model.class);


            Value mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.URI);
            when(mockValue.getString()).thenReturn("info:fedora");
            when(mockModel.createResource("info:fedora")).thenReturn(ResourceFactory.createResource("info:fedora"));

            JcrRdfTools.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);
            verify(mockModel).add(mockSubject, mockPredicate, ResourceFactory.createResource("info:fedora"));

            mockValue = mock(Value.class);
            when(mockProperty.getSession()).thenReturn(mockSession);
            when(mockSession.getNodeByIdentifier("uuid")).thenReturn(mockNode);
            when(mockNode.getPath()).thenReturn("/abc");
            when(mockModel.createResource("info:fedora/abc")).thenReturn(ResourceFactory.createResource("info:fedora/abc"));

            when(mockValue.getType()).thenReturn(PropertyType.REFERENCE);
            when(mockValue.getString()).thenReturn("uuid");
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);

        //    verify(mockModel).add(mockSubject, mockPredicate, ResourceFactory.createResource("info:fedora/abc"));

            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.WEAKREFERENCE);
            when(mockValue.getString()).thenReturn("uuid");
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);

            //    verify(mockModel).add(mockSubject, mockPredicate, ResourceFactory.createResource("info:fedora/abc"));


            mockValue = mock(Value.class);
            when(mockValue.getType()).thenReturn(PropertyType.PATH);
            when(mockValue.getString()).thenReturn("/abc");
            JcrRdfTools.addPropertyToModel(mockSubject, mockModel, mockProperty, mockValue);


            verify(mockModel, times(3)).add(mockSubject, mockPredicate, ResourceFactory.createResource("info:fedora/abc"));



        } finally {
            FedoraTypesUtils.getPredicateForProperty = holdPredicate;
        }

    }
}
