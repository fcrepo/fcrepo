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
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.fcrepo.kernel.RdfLexicon.CONTENT_LOCATION_TYPE;
import static org.fcrepo.kernel.RdfLexicon.FIXITY_TYPE;
import static org.fcrepo.kernel.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.RdfLexicon.HAS_SIZE;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_RESULT;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_STATE;
import static org.fcrepo.kernel.RdfLexicon.HAS_CONTENT_LOCATION;
import static org.fcrepo.kernel.RdfLexicon.HAS_CONTENT_LOCATION_VALUE;

import java.util.Calendar;
import java.util.Iterator;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.hp.hpl.jena.vocabulary.RDF;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
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
    public FixityRdfContext(final Node node, final IdentifierTranslator graphSubjects,
            final LowLevelStorageService lowLevelStorageService,
            final Iterable<FixityResult> blobs) throws RepositoryException {
        super(node, graphSubjects, lowLevelStorageService);

        concat(Iterators.concat(Iterators.transform(blobs.iterator(),
                new Function<FixityResult, Iterator<Triple>>() {

                    @Override
                    public Iterator<Triple> apply(final FixityResult blob) {
                        final com.hp.hpl.jena.graph.Node resultSubject = getTransientFixitySubject();
                        final ImmutableSet.Builder<Triple> b = builder();
                        try {
                            b.add(create(resultSubject, RDF.type.asNode(), FIXITY_TYPE.asNode()));
                            b.add(create(graphSubjects.getSubject(node.getPath()).asNode(),
                                    HAS_FIXITY_RESULT.asNode(), resultSubject));
                            final String storeIdentifier = blob.getStoreIdentifier();
                            final com.hp.hpl.jena.graph.Node contentLocation = createResource(storeIdentifier)
                                                                     .asNode();
                            b.add(create(resultSubject, HAS_CONTENT_LOCATION.asNode(),
                                            contentLocation));
                            b.add(create(contentLocation,
                                            RDF.type.asNode(),
                                            CONTENT_LOCATION_TYPE.asNode()));
                            b.add(create(contentLocation,
                                            HAS_CONTENT_LOCATION_VALUE.asNode(),
                                            createLiteral(storeIdentifier)));

                            for (final FixityResult.FixityState state : blob.getStatus()) {
                                b.add(create(resultSubject, HAS_FIXITY_STATE
                                        .asNode(), createLiteral(state
                                        .toString())));
                            }
                            final String checksum =
                                    blob.getComputedChecksum().toString();
                            b.add(create(resultSubject, HAS_MESSAGE_DIGEST
                                    .asNode(), createURI(checksum)));
                            b.add(create(resultSubject, HAS_SIZE.asNode(),
                                    createTypedLiteral(
                                            blob.getComputedSize())
                                    .asNode()));
                            return b.build().iterator();
                        } catch (final RepositoryException e) {
                            throw propagate(e);
                        }
                    }
                })));
    }

    private com.hp.hpl.jena.graph.Node getTransientFixitySubject() {
        return createURI(subject().getURI() + "#fixity/" + Calendar.getInstance().getTimeInMillis());
    }
}
