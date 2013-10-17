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

package org.fcrepo.kernel.rdf.impl.mappings;

import static com.google.common.collect.Iterators.singletonIterator;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.STRING;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class PropertyToTripleTest {

    private PropertyToTriple testPropertyToTriple;

    @Mock
    private Session mockSession;

    @Mock
    private GraphSubjects mockGraphSubjects;

    @Mock
    private Property mockProperty;

    @Mock
    private Value mockValue, mockValue2;

    @Mock
    private javax.jcr.Node mockNode;

    private final static Logger LOGGER = getLogger(PropertyToTripleTest.class);

    private static final String TEST_NODE_PATH = "/test";

    private static final Resource TEST_NODE_SUBJECT = ResourceFactory
            .createResource("http:/" + TEST_NODE_PATH);

    private static final String TEST_VALUE = "test value";

    private static final String TEST_PROPERTY_NAME = "info:predicate";

    @Before
    public void setUp() throws ValueFormatException, RepositoryException {
        initMocks(this);
        testPropertyToTriple = new PropertyToTriple(mockGraphSubjects);
        when(mockProperty.getValue()).thenReturn(mockValue);
        when(mockProperty.getParent()).thenReturn(mockNode);
        when(mockProperty.getName()).thenReturn(TEST_PROPERTY_NAME);
        when(mockNode.getPath()).thenReturn(TEST_NODE_PATH);
        when(mockGraphSubjects.getGraphSubject(mockNode)).thenReturn(
                TEST_NODE_SUBJECT);
        when(mockNode.getNode(TEST_NODE_PATH)).thenReturn(mockNode);
    }

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
        final Function<Iterator<Value>, Iterator<Triple>> mapping =
            testPropertyToTriple.apply(mockProperty);
        final Iterator<Triple> ts =
            mapping.apply(twoValueIterator(mockValue, mockValue2));
        final Triple t1 = ts.next();
        LOGGER.debug("Constructed triple: {}", t1);
        final Triple t2 = ts.next();
        LOGGER.debug("Constructed triple: {}", t2);

        assertEquals("Got wrong RDF object!", t1.getObject().getLiteralValue(),
                TEST_VALUE);
        assertEquals("Got wrong RDF predicate!", t1.getPredicate(),
                createProperty(TEST_PROPERTY_NAME).asNode());
        assertEquals("Got wrong RDF subject!", t1.getSubject(),
                TEST_NODE_SUBJECT.asNode());

        assertEquals("Got wrong RDF object!", t2.getObject().getLiteralValue(),
                TEST_VALUE);
        assertEquals("Got wrong RDF predicate!", t2.getPredicate(),
                createProperty(TEST_PROPERTY_NAME).asNode());
        assertEquals("Got wrong RDF subject!", t2.getSubject(),
                TEST_NODE_SUBJECT.asNode());
    }

    @Test
    public void testSingleValuedResourceTriple() throws RepositoryException {

        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockProperty.getType()).thenReturn(PATH);
        when(mockProperty.getString()).thenReturn(TEST_VALUE);
        when(mockProperty.getNode()).thenReturn(mockNode);
        when(mockValue.getType()).thenReturn(PATH);
        final Function<Iterator<Value>, Iterator<Triple>> mapping =
            testPropertyToTriple.apply(mockProperty);
        final Triple t = mapping.apply(singletonIterator(mockValue)).next();
        LOGGER.debug("Constructed triple: {}", t);
        assertEquals("Got wrong RDF object!", t.getObject(), TEST_NODE_SUBJECT
                .asNode());
        assertEquals("Got wrong RDF predicate!", t.getPredicate(),
                createProperty(TEST_PROPERTY_NAME).asNode());
        assertEquals("Got wrong RDF subject!", t.getSubject(),
                TEST_NODE_SUBJECT.asNode());
    }

    @Test
    public void testSingleValuedLiteralTriple() throws RepositoryException {

        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockProperty.getType()).thenReturn(STRING);
        when(mockProperty.getValue()).thenReturn(mockValue);
        when(mockValue.getType()).thenReturn(STRING);
        when(mockValue.getString()).thenReturn(TEST_VALUE);
        final Function<Iterator<Value>, Iterator<Triple>> mapping =
            testPropertyToTriple.apply(mockProperty);
        final Triple t = mapping.apply(singletonIterator(mockValue)).next();
        LOGGER.debug("Constructed triple: {}", t);
        assertEquals("Got wrong RDF object!", t.getObject().getLiteralValue(),
                TEST_VALUE);
        assertEquals("Got wrong RDF predicate!", t.getPredicate(),
                createProperty(TEST_PROPERTY_NAME).asNode());
        assertEquals("Got wrong RDF subject!", t.getSubject(),
                TEST_NODE_SUBJECT.asNode());
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
        final Function<Iterator<Value>, Iterator<Triple>> mapping =
            testPropertyToTriple.apply(mockProperty);
        final Iterator<Triple> ts =
            mapping.apply(twoValueIterator(mockValue, mockValue2));
        final Triple t1 = ts.next();
        LOGGER.debug(
                "Constructed triple for testMultiValuedResourceTriple(): {}",
                t1);
        final Triple t2 = ts.next();
        LOGGER.debug(
                "Constructed triple for testMultiValuedResourceTriple(): {}",
                t2);

        assertEquals("Got wrong RDF object!", t1.getObject(), TEST_NODE_SUBJECT
                .asNode());
        assertEquals("Got wrong RDF predicate!", t1.getPredicate(),
                createProperty(TEST_PROPERTY_NAME).asNode());
        assertEquals("Got wrong RDF subject!", t1.getSubject(),
                TEST_NODE_SUBJECT.asNode());

        assertEquals("Got wrong RDF object!", t2.getObject(), TEST_NODE_SUBJECT
                .asNode());
        assertEquals("Got wrong RDF predicate!", t2.getPredicate(),
                createProperty(TEST_PROPERTY_NAME).asNode());
        assertEquals("Got wrong RDF subject!", t2.getSubject(),
                TEST_NODE_SUBJECT.asNode());
    }

    private <T> Iterator<T> twoValueIterator(final T t, final T t2) {
        return ImmutableList.of(t, t2).iterator();
    }

}
