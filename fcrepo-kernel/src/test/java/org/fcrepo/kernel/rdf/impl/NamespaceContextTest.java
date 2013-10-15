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

package org.fcrepo.kernel.rdf.impl;

import static com.google.common.collect.Iterators.any;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.fcrepo.kernel.rdf.impl.NamespaceContext;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;
import com.google.common.base.Predicate;
import com.hp.hpl.jena.graph.Triple;

public class NamespaceContextTest {

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Mock
    private Session mockSession;

    @Mock
    Workspace mockWorkspace;

    private final static String testUri = "http://example.com";

    private final static String prefix = "jcr";

    @Test
    public void testConstructor() throws RepositoryException {
        initMocks(this);
        when(mockNamespaceRegistry.getPrefixes()).thenReturn(
                new String[] {prefix});
        when(mockNamespaceRegistry.getURI(prefix)).thenReturn(testUri);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(
                mockNamespaceRegistry);
        assertTrue(any(new NamespaceContext(mockSession), hasTestUriAsObject));
    }

    private static Predicate<Triple> hasTestUriAsObject =
        new Predicate<Triple>() {

            @Override
            public boolean apply(final Triple t) {
                return t.objectMatches(createLiteral(testUri));
            }
        };
}
