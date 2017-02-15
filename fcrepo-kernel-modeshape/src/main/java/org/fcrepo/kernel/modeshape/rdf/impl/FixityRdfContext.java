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
package org.fcrepo.kernel.modeshape.rdf.impl;

import static java.time.Instant.now;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.graph.Triple.create;
import static org.apache.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.FIXITY_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST_ALGORITHM;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SIZE;
import static org.fcrepo.kernel.api.RdfLexicon.EVENT_OUTCOME_INFORMATION;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_RESULT;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_STATE;
import static org.fcrepo.kernel.modeshape.utils.UncheckedFunction.uncheck;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.utils.FixityResult;
import org.apache.jena.graph.Triple;

/**
 * An {@link org.fcrepo.kernel.api.RdfStream} containing information about the fixity of a
 * {@link org.fcrepo.kernel.api.models.FedoraBinary}.
 *
 * @author ajs6f
 * @since Oct 15, 2013
 */
public class FixityRdfContext extends NodeRdfContext {

    /**
     * Ordinary constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     * @param blobs the blobs
     * @param digest the digest uri
     * @param size the size
     */
    public FixityRdfContext(final FedoraResource resource,
                            final IdentifierConverter<Resource, FedoraResource> idTranslator,
                            final Iterable<FixityResult> blobs,
                            final URI digest,
                            final long size) {
        super(resource, idTranslator);

        concat(StreamSupport.stream(blobs.spliterator(), false).flatMap(uncheck(blob -> {
            final org.apache.jena.graph.Node resultSubject =
                    createURI(subject().getURI() + "#fixity/" + now().toEpochMilli());
            final List<Triple> b = new ArrayList<>();

            b.add(create(subject(), HAS_FIXITY_RESULT.asNode(), resultSubject));
            b.add(create(resultSubject, type.asNode(), FIXITY_TYPE.asNode()));
            b.add(create(resultSubject, type.asNode(), EVENT_OUTCOME_INFORMATION.asNode()));

            blob.getStatus(size, digest).stream().map(state -> createLiteral(state.toString()))
                    .map(state -> create(resultSubject, HAS_FIXITY_STATE.asNode(), state)).forEach(b::add);

            b.add(create(resultSubject, HAS_MESSAGE_DIGEST_ALGORITHM.asNode(),
                    createTypedLiteral(blob.getUsedAlgorithm()).asNode()));
            b.add(create(resultSubject, HAS_MESSAGE_DIGEST.asNode(), createURI(blob.getComputedChecksum().toString())));
            b.add(create(resultSubject, HAS_SIZE.asNode(), createTypedLiteral(blob.getComputedSize()).asNode()));

            return b.stream();
        })));
    }

}
