/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.impl.utils.iterators;

import static com.google.common.base.Predicates.not;
import static com.hp.hpl.jena.graph.NodeFactory.createAnon;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.RdfLexicon.HAS_CHILD;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.impl.rdf.ManagedRdf.isManagedMixin;
import static org.fcrepo.kernel.impl.rdf.ManagedRdf.isManagedTriple;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.graph.Triple;

/**
 * <p>ManagedRdfTest class.</p>
 *
 * @author ajs6f
 */
public class ManagedRdfTest {

    private final static Triple managedTriple = create(createAnon(), HAS_CHILD
            .asNode(), createAnon());

    private final static Triple unManagedTriple = create(createAnon(),
            createAnon(), createAnon());

    private RdfStream testStream;

    @Before
    public void setUp() {
        initMocks(this);
        testStream =
            new RdfStream(managedTriple, unManagedTriple)
                    .filter(not(isManagedTriple));
    }

    @Test
    public void testFiltering() {
        assertEquals("Didn't get unmanaged triple!", unManagedTriple,
                testStream.next());
        assertFalse("Failed to filter managed triple!", testStream.hasNext());
    }

    @Test
    public void testMixinFiltering() {
        assertTrue(isManagedMixin.apply(createResource(REPOSITORY_NAMESPACE
                + "thing")));
        assertFalse(isManagedMixin.apply(createResource("myNS:thing")));
    }

}
