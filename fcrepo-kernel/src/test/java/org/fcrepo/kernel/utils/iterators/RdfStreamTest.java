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

package org.fcrepo.kernel.utils.iterators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Iterator;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;

public class RdfStreamTest {

    private RdfStream testStream;

    @Mock
    private Iterator<Triple> mockIterator;

    @Mock
    private Triple triple1, triple2;

    private final static String prefix1 = "testNS";

    private final static String uri1 = "http://testNS";

    private final static String prefix2 = "testNS2";

    private final static String uri2 = "http://testNS2";

    private final Map<String, String> testNamespaces = ImmutableMap.of(prefix1,
            uri1, prefix2, uri2);

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Before
    public void setUp() {
        initMocks(this);
        testStream = new RdfStream(mockIterator);
    }

    @Test
    public void testHasNext() {
        when(mockIterator.hasNext()).thenReturn(true, false);
        assertTrue(testStream.hasNext());
        assertFalse(testStream.hasNext());
    }

    @Test
    public void testNext() {
        when(mockIterator.next()).thenReturn(triple1, triple2);
        assertEquals(triple1, testStream.next());
        assertEquals(triple2, testStream.next());
    }

    @Test
    public void testRemove() {
        testStream.remove();
        verify(mockIterator).remove();
    }

    @Test
    public void testIterator() {
        assertEquals(testStream, testStream.iterator());
    }

    @Test
    public void testDefaultConstructor() {
        assertFalse(new RdfStream().hasNext());
    }

    @Test
    public void testCollectionConstructor() {
        testStream = new RdfStream(ImmutableSet.of(triple1, triple2));
        assertEquals(triple1, testStream.next());
    }

    @Test
    public void testConcat() {
        when(mockIterator.next()).thenReturn(triple1, triple2);
        final RdfStream testStream2 = new RdfStream(ImmutableSet.of(triple1));
        testStream.concat(testStream2);
        assertEquals(triple1, Iterators.getLast(testStream));
    }

    @Test
    public void testCollectionConcat() {
        when(mockIterator.next()).thenReturn(triple1, triple2);
        testStream.concat(ImmutableSet.of(triple1));
        assertEquals(triple1, Iterators.getLast(testStream));
    }

    @Test
    public void testAddNamespace() {
        testStream.addNamespace(prefix1, uri1);
        assertTrue(testStream.namespaces().containsKey(prefix1));
        assertTrue(testStream.namespaces().containsValue(uri1));
    }

    @Test
    public void testAddNamespaces() {
        testStream.addNamespaces(testNamespaces);
        assertTrue(testStream.namespaces().containsKey(prefix1));
        assertTrue(testStream.namespaces().containsValue(uri1));
        assertTrue(testStream.namespaces().containsKey(prefix2));
        assertTrue(testStream.namespaces().containsValue(uri2));
    }

    @Test
    public void testAsModel() throws RepositoryException {
        testStream.addNamespaces(testNamespaces);
        final Model testModel = testStream.asModel();
        assertEquals(testModel.getNsPrefixMap(), testNamespaces);

    }
}
