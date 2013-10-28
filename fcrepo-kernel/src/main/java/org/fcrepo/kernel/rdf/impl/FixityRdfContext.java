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

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.ImmutableSet.builder;
import static com.hp.hpl.jena.graph.NodeFactory.createAnon;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.fcrepo.kernel.RdfLexicon.HAS_COMPUTED_CHECKSUM;
import static org.fcrepo.kernel.RdfLexicon.HAS_COMPUTED_SIZE;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_RESULT;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_STATE;
import static org.fcrepo.kernel.RdfLexicon.HAS_LOCATION;
import static org.fcrepo.kernel.RdfLexicon.IS_FIXITY_RESULT_OF;

import java.util.Iterator;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.services.LowLevelStorageService;
import org.fcrepo.kernel.utils.FixityResult;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;

/**
 * An {@link RdfStream} containing information about the fixity of a
 * {@link Node}.
 *
 * @author ajs6f
 * @date Oct 15, 2013
 */
public class FixityRdfContext extends NodeRdfContext {

    /**
     * Ordinary constructor.
     *
     * @param node
     * @param graphSubjects
     * @param lowLevelStorageService
     * @param blobs
     * @throws RepositoryException
     */
    public FixityRdfContext(final Node node, final GraphSubjects graphSubjects,
            final LowLevelStorageService lowLevelStorageService,
            final Iterable<FixityResult> blobs) throws RepositoryException {
        super(node, graphSubjects, lowLevelStorageService);

        concat(Iterators.concat(Iterators.transform(blobs.iterator(),
                new Function<FixityResult, Iterator<Triple>>() {

                    @Override
                    public Iterator<Triple> apply(final FixityResult blob) {
                        // fixity results are just blank nodes
                        final com.hp.hpl.jena.graph.Node resultSubject =
                            createAnon();
                        final ImmutableSet.Builder<Triple> b = builder();
                        try {
                            b.add(create(resultSubject, IS_FIXITY_RESULT_OF
                                    .asNode(), graphSubjects.getGraphSubject(
                                    node).asNode()));
                            b.add(create(graphSubjects.getGraphSubject(node)
                                    .asNode(), HAS_FIXITY_RESULT.asNode(),
                                    resultSubject));
                            b.add(create(resultSubject, HAS_LOCATION.asNode(),
                                    createResource(blob.getStoreIdentifier())
                                            .asNode()));

                            for (final FixityResult.FixityState state : blob.status) {
                                b.add(create(resultSubject, HAS_FIXITY_STATE
                                        .asNode(), createLiteral(state
                                        .toString())));
                            }
                            final String checksum =
                                blob.computedChecksum.toString();
                            b.add(create(resultSubject, HAS_COMPUTED_CHECKSUM
                                    .asNode(), createURI(checksum)));
                            b.add(create(resultSubject, HAS_COMPUTED_SIZE
                                    .asNode(), createTypedLiteral(blob.computedSize)
                                    .asNode()));
                            return b.build().iterator();
                        } catch (final RepositoryException e) {
                            throw propagate(e);
                        }
                    }
                })));
    }
}
