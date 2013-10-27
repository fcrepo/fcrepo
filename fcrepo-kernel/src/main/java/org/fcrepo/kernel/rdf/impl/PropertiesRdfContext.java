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
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.jcr.FedoraJcrTypes.ROOT;
import static org.fcrepo.kernel.RdfLexicon.HAS_CONTENT;
import static org.fcrepo.kernel.RdfLexicon.HAS_LOCATION;
import static org.fcrepo.kernel.RdfLexicon.IS_CONTENT_OF;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isBinaryProperty;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.property2values;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.security.AccessControlException;
import java.util.Iterator;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.rdf.impl.mappings.PropertyToTriple;
import org.fcrepo.kernel.rdf.impl.mappings.ZippingIterator;
import org.fcrepo.kernel.services.LowLevelStorageService;
import org.fcrepo.kernel.utils.LowLevelCacheEntry;
import org.fcrepo.kernel.utils.iterators.PropertyIterator;
import org.slf4j.Logger;

import com.google.common.base.Function;
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

        // if there's an accessible jcr:content node, include information about
        // it
        javax.jcr.Node contentNode = null;
        try {
            if (node().hasNode(JCR_CONTENT)) {
                contentNode = node().getNode(JCR_CONTENT);
            }
        } catch (final AccessControlException e) {
            LOGGER.debug("Access denied to content node", e);
        }
        if (contentNode != null) {
            final Node contentSubject =
                graphSubjects().getGraphSubject(contentNode).asNode();
            final Node subject =
                graphSubjects().getGraphSubject(node()).asNode();
            // add triples representing parent-to-content-child relationship
            concat(Iterators.forArray(new Triple[] {
                    create(subject, HAS_CONTENT.asNode(), contentSubject),
                    create(contentSubject, IS_CONTENT_OF.asNode(), subject)}));
            // add properties from content child
            concat(new PropertiesRdfContext(node().getNode(JCR_CONTENT),
                    graphSubjects(), lowLevelStorageService()));

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
            concat(new RootRdfContext(node(), graphSubjects(), lowLevelStorageService()));
        }

    }

    private Iterator<Triple> triplesFromProperties(final javax.jcr.Node n)
        throws RepositoryException {
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
