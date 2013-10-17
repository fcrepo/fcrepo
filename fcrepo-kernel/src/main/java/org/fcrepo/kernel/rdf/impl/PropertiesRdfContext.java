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

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.ImmutableSet.builder;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.jcr.FedoraJcrTypes.ROOT;
import static org.fcrepo.kernel.RdfLexicon.HAS_CONTENT;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_CHECK_COUNT;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_ERROR_COUNT;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_REPAIRED_COUNT;
import static org.fcrepo.kernel.RdfLexicon.HAS_LOCATION;
import static org.fcrepo.kernel.RdfLexicon.HAS_NODE_TYPE;
import static org.fcrepo.kernel.RdfLexicon.HAS_OBJECT_COUNT;
import static org.fcrepo.kernel.RdfLexicon.HAS_OBJECT_SIZE;
import static org.fcrepo.kernel.RdfLexicon.IS_CONTENT_OF;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getRepositoryCount;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getRepositorySize;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isBinaryProperty;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.property2values;
import static org.fcrepo.metrics.RegistryService.getMetrics;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.rdf.NodeRdfContext;
import org.fcrepo.kernel.rdf.impl.mappings.PropertyToTriple;
import org.fcrepo.kernel.rdf.impl.mappings.ZippingIterator;
import org.fcrepo.kernel.services.LowLevelStorageService;
import org.fcrepo.kernel.services.functions.GetClusterConfiguration;
import org.fcrepo.kernel.utils.LowLevelCacheEntry;
import org.fcrepo.kernel.utils.iterators.PropertyIterator;
import org.modeshape.jcr.JcrRepository;
import org.slf4j.Logger;

import com.codahale.metrics.Counter;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 * {@link NodeRdfContext} for RDF that derives from JCR properties on a
 * {@link Node}.
 *
 * @author ajs6f
 * @date Oct 10, 2013
 */
public class PropertiesRdfContext extends NodeRdfContext {

    private PropertyToTriple property2triple;

    private final static Logger LOGGER = getLogger(PropertiesRdfContext.class);

    /**
     * Default constructor.
     *
     * @param node
     * @throws RepositoryException
     */

    public PropertiesRdfContext(final javax.jcr.Node node, final GraphSubjects graphSubjects,
        final LowLevelStorageService lowLevelStorageService) throws RepositoryException {
        super(node, graphSubjects, lowLevelStorageService);
        property2triple = new PropertyToTriple(graphSubjects);
        putPropertiesIntoContext();
    }

    private void putPropertiesIntoContext() throws RepositoryException {

        LOGGER.debug(
                "Pushing RDF triples into context for properties of node: {}",
                node());

        // this node's own properties
        if (node().hasProperties()) {
            concat(triplesFromProperties(node()));
        }

        // if there's a jcr:content node, include information about it
        if (node().hasNode(JCR_CONTENT)) {
            final javax.jcr.Node contentNode = node().getNode(JCR_CONTENT);
            final Node contentSubject =
                graphSubjects().getGraphSubject(contentNode).asNode();
            final Node subject =
                graphSubjects().getGraphSubject(node()).asNode();
            // add triples representing parent-to-content-child relationship
            concat(Iterators.forArray(new Triple[] {
                    create(subject, HAS_CONTENT.asNode(), contentSubject),
                    create(contentSubject, IS_CONTENT_OF.asNode(), subject)}));
            // add properties from content child
            concat(triplesFromProperties(node().getNode(JCR_CONTENT)));

            // add triples describing storage of content child
            lowLevelStorageService().setRepository(
                    node().getSession().getRepository());
            concat(transform(lowLevelStorageService().getLowLevelCacheEntries(
                    contentNode).iterator(),
                    new Function<LowLevelCacheEntry, Triple>() {

                        @Override
                        public Triple apply(final LowLevelCacheEntry llce) {
                            return create(contentSubject,
                                    HAS_LOCATION.asNode(), createLiteral(llce
                                            .getExternalIdentifier()));
                        }
                    }));

        }

        if (node().getPrimaryNodeType().getName().equals(ROOT)) {
            concat(triplesForRootNode());
        }

    }

    private Set<Triple> triplesForRootNode() throws RepositoryException {
        // a rdf description of the root node
        LOGGER.debug("Creating RDF triples for repository description");
        final Repository repository = node().getSession().getRepository();
        // retrieve the metrics from the service
        final SortedMap<String, Counter> counters = getMetrics().getCounters();
        final ImmutableSet.Builder<Triple> b = builder();
        final Node subject = graphSubjects().getGraphSubject(node()).asNode();
        for (final String key : repository.getDescriptorKeys()) {
            final String descriptor = repository.getDescriptor(key);
            if (descriptor != null) {
                final String uri = REPOSITORY_NAMESPACE + "repository/" + key;
                b.add(create(subject, createURI(uri), createLiteral(descriptor)));
            }
        }
        final NodeTypeManager nodeTypeManager =
            node().getSession().getWorkspace().getNodeTypeManager();

        final NodeTypeIterator nodeTypes = nodeTypeManager.getAllNodeTypes();
        while (nodeTypes.hasNext()) {
            final NodeType nodeType = nodeTypes.nextNodeType();
            b.add(create(subject, HAS_NODE_TYPE.asNode(),
                    createLiteral(nodeType.getName())));
        }

        b.add(create(subject, HAS_OBJECT_COUNT.asNode(), createLiteral(String
                .valueOf(getRepositoryCount(repository)))));
        b.add(create(subject, HAS_OBJECT_SIZE.asNode(), createLiteral(String
                .valueOf(getRepositorySize(repository)))));
        // Get the cluster configuration for the RDF response, if available
        // this ugly test checks to see whether this is an ordinary JCR
        // repository
        // or a ModeShape repo, which will possess the extra info
        if (JcrRepository.class.isAssignableFrom(repository.getClass())) {
            final Map<String, String> config =
                new GetClusterConfiguration().apply(repository);
            assert (config != null);

            for (final Map.Entry<String, String> entry : config.entrySet()) {
                b.add(create(subject, createURI(REPOSITORY_NAMESPACE
                        + entry.getKey()), createLiteral(entry.getValue())));
            }
        }

        // and add the repository metrics to the RDF model
        if (counters.containsKey("LowLevelStorageService.fixity-check-counter")) {
            b.add(create(subject, HAS_FIXITY_CHECK_COUNT.asNode(),
                    createLiteral(String.valueOf(counters.get(
                            "org.fcrepo.services." + "LowLevelStorageService."
                                    + "fixity-check-counter").getCount()))));
        }

        if (counters.containsKey("LowLevelStorageService.fixity-error-counter")) {
            b.add(create(subject, HAS_FIXITY_ERROR_COUNT.asNode(),
                    createLiteral(String.valueOf(counters.get(
                            "org.fcrepo.services." + "LowLevelStorageService."
                                    + "fixity-error-counter").getCount()))));
        }

        if (counters
                .containsKey("LowLevelStorageService.fixity-repaired-counter")) {
            b.add(create(subject, HAS_FIXITY_REPAIRED_COUNT.asNode(),
                    createLiteral(String.valueOf(counters.get(
                            "org.fcrepo.services." + "LowLevelStorageService."
                                    + "fixity-repaired-counter").getCount()))));
        }
        return b.build();
    }

    private Iterator<Triple> triplesFromProperties(final javax.jcr.Node n) throws RepositoryException {
        LOGGER.debug("Creating triples for node: {}", n);
        final UnmodifiableIterator<Property> nonBinaryProperties =
            filter(new PropertyIterator(n.getProperties()),
                    not(isBinaryProperty));

        final UnmodifiableIterator<Property> nonBinaryPropertiesCopy =
            filter(new PropertyIterator(n.getProperties()),
                    not(isBinaryProperty));

        return Iterators.concat(new ZippingIterator<>(
            transform(
                nonBinaryProperties, property2values),
            transform(
                nonBinaryPropertiesCopy, property2triple)));

    }

}
