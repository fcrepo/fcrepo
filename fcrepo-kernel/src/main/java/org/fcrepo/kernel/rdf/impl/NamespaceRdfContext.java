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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.RdfLexicon.HAS_NAMESPACE_PREFIX;
import static org.fcrepo.kernel.RdfLexicon.HAS_NAMESPACE_URI;
import static org.fcrepo.kernel.RdfLexicon.VOAF_VOCABULARY;
import static org.fcrepo.kernel.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 * An {@link RdfStream} that holds the namespace mappings for serializations,
 * as well as {@link Triple}s describing those namespaces.
 *
 * @author ajs6f
 * @date Oct 9, 2013
 */
public class NamespaceRdfContext extends RdfStream {

    private static Logger LOGGER = getLogger(NamespaceRdfContext.class);

    /**
     * Default constructor. Loads context with RDF describing namespaces in
     * scope in the repository.
     *
     * @param session
     * @throws RepositoryException
     */
    public NamespaceRdfContext(final Session session) throws RepositoryException {
        super();
        final NamespaceRegistry namespaceRegistry =
            session.getWorkspace().getNamespaceRegistry();
        checkNotNull(namespaceRegistry,
                "Couldn't find namespace registry in repository!");

        final ImmutableMap.Builder<String, String> namespaces =
            ImmutableMap.builder();
        final ImmutableCollection.Builder<Triple> nsTriples =
            ImmutableSet.builder();
        for (String prefix : namespaceRegistry.getPrefixes()) {
            if (!prefix.isEmpty()) {
                final String nsURI = namespaceRegistry.getURI(prefix);
                if (prefix.equals("jcr")) {
                    prefix = "fcrepo";
                }
                LOGGER.trace(
                        "Discovered namespace prefix \"{}\" with URI \"{}\"",
                        prefix, nsURI);
                final String rdfNsUri = getRDFNamespaceForJcrNamespace(nsURI);
                // first, let's put the namespace in context
                namespaces.put(prefix, rdfNsUri);
                LOGGER.trace("Added namespace prefix \"{}\" with URI \"{}\"",
                        prefix, rdfNsUri);
                final Node nsSubject = createURI(rdfNsUri);
                // now, some triples describing this namespace
                nsTriples.add(create(nsSubject, type.asNode(), VOAF_VOCABULARY
                        .asNode()));
                nsTriples.add(create(nsSubject, HAS_NAMESPACE_PREFIX.asNode(),
                        createLiteral(prefix)));
                nsTriples.add(create(nsSubject, HAS_NAMESPACE_URI.asNode(),
                        createLiteral(nsURI)));
            }
        }
        concat(nsTriples.build()).namespaces(namespaces.build());
    }
}
