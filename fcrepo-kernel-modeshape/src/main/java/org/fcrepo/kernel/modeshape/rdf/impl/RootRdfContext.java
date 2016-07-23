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

import static java.util.stream.Stream.of;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.graph.Triple.create;
import static org.apache.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_CHECK_COUNT;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_ERROR_COUNT;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_REPAIRED_COUNT;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.ROOT;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.metrics.RegistryService;

import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;

import com.codahale.metrics.Counter;
import org.apache.jena.graph.Triple;

/**
 * Assemble {@link Triple}s derived from the root of a repository.
 *
 * @author ajs6f
 * @since Oct 18, 2013
 */
public class RootRdfContext extends NodeRdfContext {

    private static final String PREFIX = "org.fcrepo.services.";
    private static final String FIXITY_REPAIRED_COUNTER = "LowLevelStorageService.fixity-repaired-counter";
    private static final String FIXITY_ERROR_COUNTER = "LowLevelStorageService.fixity-error-counter";
    private static final String FIXITY_CHECK_COUNTER = "LowLevelStorageService.fixity-check-counter";
    private static final Logger LOGGER = getLogger(RootRdfContext.class);
    static final RegistryService registryService = RegistryService.getInstance();

    /**
     * Ordinary constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     */
    public RootRdfContext(final FedoraResource resource,
                          final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        super(resource, idTranslator);

        if (resource().hasType(ROOT)) {
            concat(getRepositoryTriples());
        }
    }

    private Stream<Triple> getRepositoryTriples() {
        LOGGER.trace("Creating RDF triples for repository description");

        final Stream.Builder<Triple> b = Stream.builder();

        of("RepositoryRoot", "Resource", "Container").forEach(x ->
            b.accept(create(subject(), type.asNode(), createURI(REPOSITORY_NAMESPACE + x))));

        /*
            FIXME: removing due to performance problems, esp. w/ many files on federated filesystem
            see: https://www.pivotaltracker.com/story/show/78647248

            final Repository repository = session().getRepository();

            b.add(create(subject(), HAS_OBJECT_COUNT.asNode(), createLiteral(String
                    .valueOf(getRepositoryCount(repository)))));
            b.add(create(subject(), HAS_OBJECT_SIZE.asNode(), createLiteral(String
                    .valueOf(getRepositorySize(repository)))));
        */

        // retrieve the metrics from the service
        final Map<String, Counter> counters = registryService.getMetrics().getCounters();
        // and add the repository metrics to the RDF model
        if (counters.containsKey(FIXITY_CHECK_COUNTER)) {
            b.accept(create(subject(), HAS_FIXITY_CHECK_COUNT.asNode(),
                    createTypedLiteral(counters.get(PREFIX + FIXITY_CHECK_COUNTER).getCount()).asNode()));
        }

        if (counters.containsKey(FIXITY_ERROR_COUNTER)) {
            b.accept(create(subject(), HAS_FIXITY_ERROR_COUNT.asNode(),
                    createTypedLiteral(counters.get(PREFIX + FIXITY_ERROR_COUNTER).getCount()).asNode()));
        }

        if (counters.containsKey(FIXITY_REPAIRED_COUNTER)) {
            b.accept(create(subject(), HAS_FIXITY_REPAIRED_COUNT.asNode(),
                    createTypedLiteral(counters.get(PREFIX + FIXITY_REPAIRED_COUNTER).getCount()).asNode()));
        }

        // offer all these accumulated triples
        return b.build();
    }
}
