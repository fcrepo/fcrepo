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

import static com.google.common.collect.ImmutableSet.builder;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.fcrepo.kernel.api.FedoraJcrTypes.ROOT;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_CHECK_COUNT;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_ERROR_COUNT;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_REPAIRED_COUNT;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.slf4j.LoggerFactory.getLogger;

import com.hp.hpl.jena.rdf.model.Resource;
import org.apache.commons.lang3.StringUtils;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.metrics.RegistryService;

import java.util.Map;
import javax.jcr.Repository;
import org.fcrepo.kernel.modeshape.services.functions.GetClusterConfiguration;
import org.modeshape.jcr.JcrRepository;
import org.slf4j.Logger;

import com.codahale.metrics.Counter;
import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.graph.Triple;

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
            concatRepositoryTriples();
        }
    }

    private void concatRepositoryTriples() {
        LOGGER.trace("Creating RDF triples for repository description");
        final Repository repository = session().getRepository();

        final ImmutableSet.Builder<Triple> b = builder();
        stream(repository.getDescriptorKeys()).forEach(key -> {
            final String descriptor = repository.getDescriptor(key);
            if (descriptor != null) {
                // Create a URI from the jcr.Repository constant values,
                // converting them from dot notation (identifier.stability)
                // to the camel case that is more common in RDF properties.
                final String uri = stream(key.split("\\."))
                        .map(StringUtils::capitalize).collect(joining("", REPOSITORY_NAMESPACE + "repository", ""));
                b.add(create(subject(), createURI(uri), createLiteral(descriptor)));
            }
        });

        /*
            FIXME: removing due to performance problems, esp. w/ many files on federated filesystem
            see: https://www.pivotaltracker.com/story/show/78647248

            b.add(create(subject(), HAS_OBJECT_COUNT.asNode(), createLiteral(String
                    .valueOf(getRepositoryCount(repository)))));
            b.add(create(subject(), HAS_OBJECT_SIZE.asNode(), createLiteral(String
                    .valueOf(getRepositorySize(repository)))));
        */

        // Get the cluster configuration, if available
        // this ugly test checks to see whether this is an ordinary JCR
        // repository or a ModeShape repo, which will possess the extra info
        if (JcrRepository.class.isAssignableFrom(repository.getClass())) {
            final Map<String, String> config = new GetClusterConfiguration().apply(repository);
            assert (config != null);
            config.forEach((k, v) -> b.add(create(subject(), createURI(REPOSITORY_NAMESPACE + k), createLiteral(v))));
        }

        // retrieve the metrics from the service
        final Map<String, Counter> counters = registryService.getMetrics().getCounters();
        // and add the repository metrics to the RDF model
        if (counters.containsKey(FIXITY_CHECK_COUNTER)) {
            b.add(create(subject(), HAS_FIXITY_CHECK_COUNT.asNode(),
                    createTypedLiteral(counters.get(PREFIX + FIXITY_CHECK_COUNTER).getCount()).asNode()));
        }

        if (counters.containsKey(FIXITY_ERROR_COUNTER)) {
            b.add(create(subject(), HAS_FIXITY_ERROR_COUNT.asNode(),
                    createTypedLiteral(counters.get(PREFIX + FIXITY_ERROR_COUNTER).getCount()).asNode()));
        }

        if (counters.containsKey(FIXITY_REPAIRED_COUNTER)) {
            b.add(create(subject(), HAS_FIXITY_REPAIRED_COUNT.asNode(),
                    createTypedLiteral(counters.get(PREFIX + FIXITY_REPAIRED_COUNTER).getCount()).asNode()));
        }

        // offer all these accumulated triples
        concat(b.build());
    }
}
