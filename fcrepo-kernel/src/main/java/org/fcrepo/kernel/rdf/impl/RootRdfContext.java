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

import static com.google.common.collect.ImmutableSet.builder;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_CHECK_COUNT;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_ERROR_COUNT;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_REPAIRED_COUNT;
import static org.fcrepo.kernel.RdfLexicon.HAS_NODE_TYPE;
import static org.fcrepo.kernel.RdfLexicon.HAS_OBJECT_COUNT;
import static org.fcrepo.kernel.RdfLexicon.HAS_OBJECT_SIZE;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getRepositoryCount;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getRepositorySize;
import static org.fcrepo.metrics.RegistryService.getMetrics;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.SortedMap;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.services.LowLevelStorageService;
import org.fcrepo.kernel.services.functions.GetClusterConfiguration;
import org.modeshape.jcr.JcrRepository;
import org.slf4j.Logger;

import com.codahale.metrics.Counter;
import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.graph.Triple;

/**
 * Assemble {@link Triple}s derived from the root {@link Node} of a repository.
 *
 * @author ajs6f
 * @date Oct 18, 2013
 */
public class RootRdfContext extends NodeRdfContext {

    private final static Logger LOGGER = getLogger(RootRdfContext.class);

    /**
     * Ordinary constructor.
     *
     * @param node
     * @param graphSubjects
     * @param llsService
     * @throws RepositoryException
     */
    public RootRdfContext(final Node node, final GraphSubjects graphSubjects,
            final LowLevelStorageService llsService) throws RepositoryException {

        super(node, graphSubjects, llsService);

        LOGGER.trace("Creating RDF triples for repository description");
        final Repository repository = node().getSession().getRepository();

        final ImmutableSet.Builder<Triple> b = builder();

        for (final String key : repository.getDescriptorKeys()) {
            final String descriptor = repository.getDescriptor(key);
            if (descriptor != null) {
                final String uri = REPOSITORY_NAMESPACE + "repository/" + key;
                b.add(create(subject(), createURI(uri),
                        createLiteral(descriptor)));
            }
        }
        final NodeTypeManager nodeTypeManager =
            node().getSession().getWorkspace().getNodeTypeManager();

        final NodeTypeIterator nodeTypes = nodeTypeManager.getAllNodeTypes();
        while (nodeTypes.hasNext()) {
            final NodeType nodeType = nodeTypes.nextNodeType();
            b.add(create(subject(), HAS_NODE_TYPE.asNode(),
                    createLiteral(nodeType.getName())));
        }

        b.add(create(subject(), HAS_OBJECT_COUNT.asNode(), createLiteral(String
                .valueOf(getRepositoryCount(repository)))));
        b.add(create(subject(), HAS_OBJECT_SIZE.asNode(), createLiteral(String
                .valueOf(getRepositorySize(repository)))));

        // Get the cluster configuration, if available
        // this ugly test checks to see whether this is an ordinary JCR
        // repository or a ModeShape repo, which will possess the extra info
        if (JcrRepository.class.isAssignableFrom(repository.getClass())) {
            final Map<String, String> config =
                new GetClusterConfiguration().apply(repository);
            assert (config != null);

            for (final Map.Entry<String, String> entry : config.entrySet()) {
                b.add(create(subject(), createURI(REPOSITORY_NAMESPACE + entry.getKey()),
                        createLiteral(entry.getValue())));
            }
        }

        // retrieve the metrics from the service
        final SortedMap<String, Counter> counters = getMetrics().getCounters();
        // and add the repository metrics to the RDF model
        if (counters.containsKey("LowLevelStorageService.fixity-check-counter")) {
            b.add(create(subject(), HAS_FIXITY_CHECK_COUNT.asNode(),
                    createTypedLiteral(
                            counters.get(
                                    "org.fcrepo.services."
                                            + "LowLevelStorageService."
                                            + "fixity-check-counter")
                                    .getCount()).asNode()));
        }

        if (counters.containsKey("LowLevelStorageService.fixity-error-counter")) {
            b.add(create(subject(), HAS_FIXITY_ERROR_COUNT.asNode(),
                    createTypedLiteral(
                            counters.get(
                                    "org.fcrepo.services."
                                            + "LowLevelStorageService."
                                            + "fixity-error-counter")
                                    .getCount()).asNode()));
        }

        if (counters
                .containsKey("LowLevelStorageService.fixity-repaired-counter")) {
            b.add(create(subject(), HAS_FIXITY_REPAIRED_COUNT.asNode(),
                    createTypedLiteral(
                            counters.get(
                                    "org.fcrepo.services."
                                            + "LowLevelStorageService."
                                            + "fixity-repaired-counter")
                                    .getCount()).asNode()));
        }

        // offer all these accumulated triples
        concat(b.build());
    }

}
