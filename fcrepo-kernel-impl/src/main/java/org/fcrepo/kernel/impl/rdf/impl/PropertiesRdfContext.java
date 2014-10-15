/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.impl.rdf.impl;

import static com.google.common.base.Predicates.not;
import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.jcr.FedoraJcrTypes.ROOT;
import static org.fcrepo.kernel.RdfLexicon.HAS_CONTENT;
import static org.fcrepo.kernel.RdfLexicon.IS_CONTENT_OF;
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isInternalProperty;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.impl.mappings.PropertyToTriple;
import org.fcrepo.kernel.utils.iterators.PropertyIterator;
import org.slf4j.Logger;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 * {@link NodeRdfContext} for RDF that derives from JCR properties on a
 * {@link Node}.
 *
 * @author ajs6f
 * @since Oct 10, 2013
 */
public class PropertiesRdfContext extends NodeRdfContext {

    private PropertyToTriple property2triple;

    private static final Logger LOGGER = getLogger(PropertiesRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource
     * @throws RepositoryException
     */

    public PropertiesRdfContext(final FedoraResource resource,
                                final IdentifierConverter<Resource, FedoraResource> graphSubjects)
        throws RepositoryException {
        super(resource, graphSubjects);
        property2triple = new PropertyToTriple(resource.getNode().getSession(), graphSubjects);
        putPropertiesIntoContext();
    }

    /**
     * Constructor with only a node
     * @param node
     * @param graphSubjects
     * @throws RepositoryException
     */
    public PropertiesRdfContext(final javax.jcr.Node node,
                                final IdentifierConverter<Resource, FedoraResource> graphSubjects)
            throws RepositoryException {
        this(nodeConverter.convert(node), graphSubjects);
    }

    private void putPropertiesIntoContext() throws RepositoryException {

        LOGGER.trace(
                "Pushing RDF triples into context for properties of node: {}",
                resource());

        // this node's own properties
        if (resource().getNode().hasProperties()) {
            concat(triplesFromProperties(resource()));
        }

        // if there's an accessible jcr:content node, include information about
        // it
        if (resource() instanceof Datastream) {
            final FedoraResource contentNode = ((Datastream) resource()).getBinary();
            final Node contentSubject = graphSubjects().reverse().convert(contentNode).asNode();
            final Node subject = graphSubjects().reverse().convert(resource()).asNode();
            // add triples representing parent-to-content-child relationship
            concat(new Triple[] {
                    create(subject, HAS_CONTENT.asNode(), contentSubject),
                    create(contentSubject, IS_CONTENT_OF.asNode(), subject)});
            // add properties from content child
            concat(new PropertiesRdfContext(contentNode,
                    graphSubjects()));

        }

        if (resource().hasType(ROOT)) {
            concat(new RootRdfContext(resource(), graphSubjects()));
        }

        concat(new HashRdfContext(resource(), graphSubjects()));

    }

    private Iterator<Triple> triplesFromProperties(final FedoraResource n)
        throws RepositoryException {
        LOGGER.trace("Creating triples for node: {}", n);
        final UnmodifiableIterator<Property> properties =
            Iterators.filter(new PropertyIterator(n.getNode().getProperties()), not(isInternalProperty));

        return Iterators.concat(Iterators.transform(properties, property2triple));

    }

}
