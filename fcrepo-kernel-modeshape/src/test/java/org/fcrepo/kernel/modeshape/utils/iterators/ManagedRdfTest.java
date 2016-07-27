/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.utils.iterators;

import static java.util.stream.Stream.of;
import static org.apache.jena.graph.NodeFactory.createBlankNode;
import static org.apache.jena.graph.Triple.create;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_CHILD;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.modeshape.rdf.ManagedRdf.isManagedMixin;
import static org.fcrepo.kernel.modeshape.rdf.ManagedRdf.isManagedTriple;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Iterator;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import org.apache.jena.graph.Triple;

/**
 * <p>ManagedRdfTest class.</p>
 *
 * @author ajs6f
 */
public class ManagedRdfTest {

    private final static Triple managedTriple = create(createBlankNode(), HAS_CHILD
            .asNode(), createBlankNode());

    private final static Triple unManagedTriple = create(createBlankNode(),
            createBlankNode(), createBlankNode());

    private Stream<Triple> testStream;

    @Before
    public void setUp() {
        initMocks(this);
        testStream = of(managedTriple, unManagedTriple).filter(isManagedTriple.negate());
    }

    @Test
    public void testFiltering() {
        final Iterator<Triple> iter = testStream.iterator();
        assertEquals("Didn't get unmanaged triple!", unManagedTriple, iter.next());
        assertFalse("Failed to filter managed triple!", iter.hasNext());
    }

    @Test
    public void testMixinFiltering() {
        assertTrue(isManagedMixin.test(createResource(REPOSITORY_NAMESPACE
                + "thing")));
        assertFalse(isManagedMixin.test(createResource("myNS:thing")));
    }

}
