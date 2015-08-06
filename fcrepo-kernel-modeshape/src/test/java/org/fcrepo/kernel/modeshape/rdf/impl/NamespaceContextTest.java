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
package org.fcrepo.kernel.modeshape.rdf.impl;

import static com.google.common.collect.Iterators.any;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_NAMESPACE_PREFIX;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_NAMESPACE_URI;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.function.Predicate;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import org.fcrepo.kernel.api.RdfLexicon;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;

import com.hp.hpl.jena.graph.Triple;

/**
 * <p>NamespaceContextTest class.</p>
 *
 * @author ajs6f
 */
public class NamespaceContextTest {

    // for mocks and setup gear see after tests

    @SuppressWarnings("unused")
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
        assertTrue(any(new NamespaceRdfContext(mockSession), hasTestUriAsObject::test));
    }

    @Test
    public void testJcrUris() throws RepositoryException {
        when(mockNamespaceRegistry.getPrefixes()).thenReturn(new String[] {"jcr"});
        when(mockNamespaceRegistry.getURI("jcr")).thenReturn("http://www.jcp.org/jcr/1.0");
        assertTrue(!new NamespaceRdfContext(mockSession).asModel().contains(
                createResource(REPOSITORY_NAMESPACE), HAS_NAMESPACE_URI, REPOSITORY_NAMESPACE));
        assertTrue(!new NamespaceRdfContext(mockSession).asModel().contains(
                createResource("jcr"), HAS_NAMESPACE_URI, "http://www.jcp.org/jcr/1.0"));
    }

    @Test
    public final void testGetJcrNamespaceModel() throws Exception {

        final String mockUri = "http://example.com/";
        when(mockNamespaceRegistry.isRegisteredUri(mockUri)).thenReturn(true);
        when(mockNamespaceRegistry.isRegisteredUri("not-registered-uri#")).thenReturn(
                false);
        when(mockNamespaceRegistry.isRegisteredUri("http://www.jcp.org/jcr/1.0"))
                .thenReturn(true);
        when(mockNamespaceRegistry.getPrefix("http://www.jcp.org/jcr/1.0"))
                .thenReturn("jcr");
        when(mockNamespaceRegistry.getPrefix(mockUri)).thenReturn("some-prefix");
        when(mockNamespaceRegistry.getURI("jcr")).thenReturn(
                "http://www.jcp.org/jcr/1.0");
        when(mockNamespaceRegistry.getURI("some-prefix")).thenReturn(mockUri);
        when(mockNamespaceRegistry.getPrefixes()).thenReturn(
                new String[] {"jcr", "some-prefix"});

        final Model jcrNamespaceModel = new NamespaceRdfContext(mockSession).asModel();
        assertTrue(!jcrNamespaceModel.contains(
                createResource(REPOSITORY_NAMESPACE), HAS_NAMESPACE_PREFIX,
                "fedora"));
        assertTrue(!jcrNamespaceModel.contains(
                createResource("http://www.jcp.org/jcr/1.0"), HAS_NAMESPACE_PREFIX,
                "jcr"));

        final Resource nsSubject = createResource(mockUri);
        assertTrue(jcrNamespaceModel.contains(nsSubject, RDF.type,
                RdfLexicon.VOAF_VOCABULARY));
        assertTrue(jcrNamespaceModel.contains(nsSubject, HAS_NAMESPACE_PREFIX,
                "some-prefix"));

        assertTrue(jcrNamespaceModel.contains(nsSubject, HAS_NAMESPACE_URI,
                mockUri));
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

    private final static String prefix = "testprefix";

    private static Predicate<Triple> hasTestUriAsObject = p -> p.objectMatches(createLiteral(testUri));
}
