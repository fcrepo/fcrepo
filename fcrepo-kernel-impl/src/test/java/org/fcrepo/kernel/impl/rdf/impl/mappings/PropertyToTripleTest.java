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
package org.fcrepo.kernel.impl.rdf.impl.mappings;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.STRING;
import static javax.jcr.PropertyType.URI;
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeToResource;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.impl.utils.JcrPropertyMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * <p>PropertyToTripleTest class.</p>
 *
 * @author ajs6f
 */
public class PropertyToTripleTest {
    private com.hp.hpl.jena.graph.Node testSubject;

    // for mocks and setup gear see after tests

    @Test
    public void testMultiValuedLiteralTriple() throws RepositoryException {

        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockProperty.getType()).thenReturn(STRING);
        when(mockProperty.getValues()).thenReturn(
                new Value[] {mockValue, mockValue2});
        when(mockValue.getString()).thenReturn(TEST_VALUE);
        when(mockValue.getType()).thenReturn(STRING);
        when(mockValue2.getString()).thenReturn(TEST_VALUE);
        when(mockValue2.getType()).thenReturn(STRING);
        final Iterator<Triple> ts = testPropertyToTriple.apply(mockProperty);
        final Triple t1 = ts.next();
        LOGGER.debug("Constructed triple: {}", t1);
        final Triple t2 = ts.next();
        LOGGER.debug("Constructed triple: {}", t2);

        assertEquals("Got wrong RDF object!", TEST_VALUE, t1.getObject()
                .getLiteralValue());
        assertEquals("Got wrong RDF predicate!", createProperty(
                TEST_PROPERTY_NAME).asNode(), t1.getPredicate());
        assertEquals("Got wrong RDF subject!", testSubject, t1
                .getSubject());

        assertEquals("Got wrong RDF object!", TEST_VALUE, t2.getObject()
                .getLiteralValue());
        assertEquals("Got wrong RDF predicate!", createProperty(
                TEST_PROPERTY_NAME).asNode(), t2.getPredicate());
        assertEquals("Got wrong RDF subject!", testSubject, t2
                .getSubject());
    }

    @Test
    public void testSingleValuedResourceTriple() throws RepositoryException {

        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockProperty.getType()).thenReturn(PATH);
        when(mockProperty.getString()).thenReturn(TEST_NODE_PATH);
        when(mockProperty.getValue()).thenReturn(mockValue);
        when(mockValue.getString()).thenReturn(TEST_NODE_PATH);
        when(mockValue.getType()).thenReturn(PATH);
        final Iterator<Triple> ts = testPropertyToTriple.apply(mockProperty);
        final Triple t = ts.next();
        LOGGER.debug("Constructed triple: {}", t);
        assertEquals("Got wrong RDF object!", testSubject, t
                .getObject());
        assertEquals("Got wrong RDF predicate!", createProperty(
                TEST_PROPERTY_NAME).asNode(), t.getPredicate());
        assertEquals("Got wrong RDF subject!", testSubject, t
                .getSubject());
    }

    @Test
    public void testSingleValuedStringLiteralTriple()
                                                     throws RepositoryException {

        when(mockProperty.getType()).thenReturn(STRING);
        when(mockValue.getType()).thenReturn(STRING);
        when(mockValue.getString()).thenReturn(TEST_VALUE);
        final Triple t = createSingleValuedLiteralTriple();
        assertEquals("Got wrong RDF object!", TEST_VALUE, t.getObject()
                .getLiteralValue());
        assertEquals("Got wrong RDF predicate!", createProperty(
                TEST_PROPERTY_NAME).asNode(), t.getPredicate());
        assertEquals("Got wrong RDF subject!", testSubject, t
                .getSubject());
    }

    @Test
    public void testSingleValuedUriLiteralTriple() throws RepositoryException {

        final String uri = "http://example.com/example-uri";

        when(mockProperty.getType()).thenReturn(URI);
        when(mockValue.getType()).thenReturn(URI);
        when(mockValue.getString()).thenReturn(uri);
        final Triple t = createSingleValuedLiteralTriple();
        assertEquals("Got wrong RDF object!", createResource(uri).asNode(), t
                .getObject());
        assertEquals("Got wrong RDF predicate!", createProperty(
                TEST_PROPERTY_NAME).asNode(), t.getPredicate());
        assertEquals("Got wrong RDF subject!", testSubject, t
                .getSubject());
    }

    @Test(expected = RuntimeException.class)
    public void testBadSingleValuedTriple() throws RepositoryException {
        when(mockProperty.getType()).thenReturn(URI);
        when(mockValue.getType()).thenReturn(URI);
        when(mockValue.getString()).thenThrow(
                new RepositoryException("Bad value!"));
        createSingleValuedLiteralTriple();
    }

    @Test
    public void testMultiValuedResourceTriple() throws RepositoryException {

        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockProperty.getType()).thenReturn(PATH);
        when(mockProperty.getValues()).thenReturn(
                new Value[] {mockValue, mockValue2});
        when(mockValue.getString()).thenReturn(TEST_NODE_PATH);
        when(mockValue.getType()).thenReturn(PATH);
        when(mockValue2.getString()).thenReturn(TEST_NODE_PATH);
        when(mockValue2.getType()).thenReturn(PATH);
        final Iterator<Triple> ts = testPropertyToTriple.apply(mockProperty);
        final Triple t1 = ts.next();
        LOGGER.debug(
                "Constructed triple for testMultiValuedResourceTriple(): {}",
                t1);
        final Triple t2 = ts.next();
        LOGGER.debug(
                "Constructed triple for testMultiValuedResourceTriple(): {}",
                t2);

        assertEquals("Got wrong RDF object!", testSubject, t1
                .getObject());
        assertEquals("Got wrong RDF predicate!", createProperty(
                TEST_PROPERTY_NAME).asNode(), t1.getPredicate());
        assertEquals("Got wrong RDF subject!", testSubject, t1
                .getSubject());

        assertEquals("Got wrong RDF object!", testSubject, t2
                .getObject());
        assertEquals("Got wrong RDF predicate!", createProperty(
                TEST_PROPERTY_NAME).asNode(), t2.getPredicate());
        assertEquals("Got wrong RDF subject!", testSubject, t2
                .getSubject());
    }

    @Test
    public
            void
            testMultiValuedResourceTripleWithReference()
                                                        throws RepositoryException {

        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockProperty.getType()).thenReturn(REFERENCE);
        when(mockProperty.getValues()).thenReturn(
                new Value[] {mockValue, mockValue2});
        when(mockValue.getString()).thenReturn(TEST_NODE_PATH);
        when(mockValue.getType()).thenReturn(REFERENCE);
        when(mockValue2.getString()).thenReturn(TEST_NODE_PATH);
        when(mockValue2.getType()).thenReturn(REFERENCE);
        when(mockSession.getNodeByIdentifier(TEST_NODE_PATH)).thenReturn(
                mockNode);

        final Iterator<Triple> ts = testPropertyToTriple.apply(mockProperty);
        final Triple t1 = ts.next();
        LOGGER.debug(
                "Constructed triple for testMultiValuedResourceTriple(): {}",
                t1);
        final Triple t2 = ts.next();
        LOGGER.debug(
                "Constructed triple for testMultiValuedResourceTriple(): {}",
                t2);

        assertEquals("Got wrong RDF object!", testSubject, t1
                .getObject());
        assertEquals("Got wrong RDF predicate!", createProperty(
                TEST_PROPERTY_NAME).asNode(), t1.getPredicate());
        assertEquals("Got wrong RDF subject!", testSubject, t1
                .getSubject());

        assertEquals("Got wrong RDF object!", testSubject, t2
                .getObject());
        assertEquals("Got wrong RDF predicate!", createProperty(
                TEST_PROPERTY_NAME).asNode(), t2.getPredicate());
        assertEquals("Got wrong RDF subject!", testSubject, t2
                .getSubject());
    }

    @Test(expected = RepositoryException.class)
    public void badProperty() throws AccessDeniedException,
                             ItemNotFoundException, RepositoryException {
        when(mockProperty.getParent()).thenThrow(
                new RepositoryException("Bad property!"));
        // we exhaust the mock of mockProperty.getParent() to replace it with an
        // exception
        mockProperty.getParent();
        createSingleValuedLiteralTriple();
    }

    private Triple createSingleValuedLiteralTriple() throws RepositoryException {

        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockProperty.getValue()).thenReturn(mockValue);
        final Iterator<Triple> ts = testPropertyToTriple.apply(mockProperty);
        final Triple t = ts.next();
        LOGGER.debug("Constructed triple: {}", t);
        return t;
    }

    @Before
    public void setUp() throws ValueFormatException, RepositoryException {
        initMocks(this);
        idTranslator = new DefaultIdentifierTranslator(mockSession);
        testPropertyToTriple = new PropertyToTriple(mockSession, idTranslator);
        when(mockProperty.getValue()).thenReturn(mockValue);
        when(mockProperty.getParent()).thenReturn(mockNode);
        when(mockProperty.getName()).thenReturn(TEST_PROPERTY_NAME);
        when(mockProperty.getNamespaceURI()).thenReturn("info:");
        when(mockProperty.getLocalName()).thenReturn("predicate");
        when(mockProperty.getSession()).thenReturn(mockSession);
        when(mockSession.getNode(TEST_NODE_PATH)).thenReturn(mockNode);
        when(mockNode.getNode(TEST_NODE_PATH)).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn(TEST_NODE_PATH);
        testSubject = nodeToResource(idTranslator).convert(mockNode).asNode();
    }

    private PropertyToTriple testPropertyToTriple;

    @Mock
    private Session mockSession;

    private IdentifierConverter<Resource, FedoraResource> idTranslator;

    @Mock
    private JcrPropertyMock mockProperty;

    @Mock
    private Value mockValue;

    @Mock
    private Value mockValue2;

    @Mock
    private javax.jcr.Node mockNode;

    private final static Logger LOGGER = getLogger(PropertyToTripleTest.class);

    private static final String TEST_NODE_PATH = "/test";

    private static final String TEST_VALUE = "test value";

    private static final String TEST_PROPERTY_NAME = "info:predicate";

}
