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
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.RdfLexicon.HAS_NAMESPACE_URI;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;
import com.google.common.base.Predicate;
import com.hp.hpl.jena.graph.Triple;

public class NamespaceContextTest {

    // for mocks and setup gear see after tests

    @Test(expected = NullPointerException.class)
    public void testBadNamespaceRegistry() throws RepositoryException {
        mockNamespaceRegistry = null;
        new NamespaceRdfContext(mockSession);
    }

    @Test
    public void testConstructor() throws RepositoryException {
        when(mockNamespaceRegistry.getPrefixes()).thenReturn(
                new String[] {prefix, ""});
        when(mockNamespaceRegistry.getURI("")).thenReturn(
                "GARBAGE URI FOR FAKE NAMESPACE, SHOULD NEVER BE PARSED");
        when(mockNamespaceRegistry.getURI(prefix)).thenReturn(testUri);
        assertTrue(any(new NamespaceRdfContext(mockSession), hasTestUriAsObject));
    }

    @Test
    public void testJcrUris() throws RepositoryException {
        when(mockNamespaceRegistry.getPrefixes()).thenReturn(new String[] {"jcr"});
        when(mockNamespaceRegistry.getURI("jcr")).thenReturn("http://www.jcp.org/jcr/1.0");
        assertTrue(new NamespaceRdfContext(mockSession).asModel().contains(createResource(REPOSITORY_NAMESPACE), HAS_NAMESPACE_URI, REPOSITORY_NAMESPACE));
    }

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(
                mockNamespaceRegistry);
    }

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Mock
    private Session mockSession;

    @Mock
    private Workspace mockWorkspace;

    private final static String testUri = "http://example.com";

    private final static String prefix = "jcr";

    private static Predicate<Triple> hasTestUriAsObject =
        new Predicate<Triple>() {

            @Override
            public boolean apply(final Triple t) {
                return t.objectMatches(createLiteral(testUri));
            }
        };
}
