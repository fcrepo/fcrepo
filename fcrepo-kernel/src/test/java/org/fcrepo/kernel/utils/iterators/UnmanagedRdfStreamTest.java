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

import static com.hp.hpl.jena.graph.NodeFactory.createAnon;
import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.kernel.RdfLexicon.HAS_CHILD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.hp.hpl.jena.graph.Triple;

public class UnmanagedRdfStreamTest {

    private final static Triple managedTriple = create(createAnon(), HAS_CHILD
            .asNode(), createAnon());

    private final static Triple unManagedTriple = create(createAnon(),
            createAnon(), createAnon());

    @Mock
    private Iterator<Triple> mockIterator;

    private UnmanagedRdfStream testStream;

    @Before
    public void setUp() {
        initMocks(this);
        testStream = new UnmanagedRdfStream(mockIterator);
    }

    @Test
    public void testFiltering() {
        when(mockIterator.hasNext()).thenReturn(true, true, false);
        when(mockIterator.next()).thenReturn(managedTriple, unManagedTriple);
        assertEquals("Didn't get unmanaged triple!", unManagedTriple,
                testStream.next());
        assertFalse("Failed to filter managed triple!", testStream.hasNext());
    }

}