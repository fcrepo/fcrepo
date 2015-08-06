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
package org.fcrepo.kernel.modeshape.rdf.impl.mappings;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.modeshape.jcr.api.Namespaced;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;
import java.util.Iterator;
import java.util.function.Function;

import static com.google.common.base.Throwables.propagate;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.vocabulary.RDF.Property;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static com.hp.hpl.jena.vocabulary.RDFS.domain;
import static com.hp.hpl.jena.vocabulary.RDFS.label;
import static org.fcrepo.kernel.modeshape.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Utility for moving generic Item Definitions into RDFS triples
 * @author cbeer
 * @author ajs6f
 *
 * @since Oct 2013
 *
 * @param <T> the property of T
 */
public class ItemDefinitionToTriples<T extends ItemDefinition> implements Function<T, Iterator<Triple>> {

    private static final Logger LOGGER = getLogger(ItemDefinitionToTriples.class);

    private Node context;

    /**
     * Translate ItemDefinitions into triples. The definitions will hang off
     * the provided RDF Node
     * @param context the context
     */
    public ItemDefinitionToTriples(final Node context) {
        this.context = context;
    }

    @Override
    public Iterator<Triple> apply(final T input) {

        try {
            final Node propertyDefinitionNode = getResource(input).asNode();

            LOGGER.trace("Adding triples for nodeType: {} with child nodes: {}",
                         context.getURI(),
                         propertyDefinitionNode.getURI());

            return new RdfStream(
                    create(propertyDefinitionNode, type.asNode(), Property.asNode()),
                    create(propertyDefinitionNode, domain.asNode(), context),
                    create(propertyDefinitionNode, label.asNode(), createLiteral(input.getName())));
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    /**
     * Get a RDF {@link Resource} for a {@link Namespaced} JCR object.
     * {@link Namespaced} is a Modeshape API type which is implemented by types
     * that fulfill the JCR interfaces that represent definitions.
     *
     * @param namespacedObject the namespace object
     * @return a resource for the given Namespaced JCR object
     * @throws javax.jcr.RepositoryException if repository exception occurred
     */
    public static Resource getResource(final Namespaced namespacedObject)
        throws RepositoryException {
        // TODO find a better way to create an explicitly-namespaced resource
        // if Jena offers one, since this isn't actually a Property
        LOGGER.trace("Creating RDF resource for {}:{}",
                     namespacedObject.getNamespaceURI(),
                     namespacedObject.getLocalName());
        return createProperty(
                getRDFNamespaceForJcrNamespace(namespacedObject
                        .getNamespaceURI()), namespacedObject.getLocalName())
                .asResource();
    }

    /**
     * Get a RDF {@link Resource} for a {@link NodeType} JCR object.
     * {@link Namespaced} is a Modeshape API type which is implemented by types
     * that fulfill the JCR interfaces that represent definitions.
     *
     * @param nodeType the node type
     * @return a Resource for the given NodeType
     * @throws javax.jcr.RepositoryException if repository exception occurred
     */
    public static Resource getResource(final NodeType nodeType) throws RepositoryException {
        return getResource((Namespaced) nodeType);
    }

    /**
     * Get a RDF {@link Resource} for a {@link ItemDefinition} JCR object.
     * {@link Namespaced} is a Modeshape API type which is implemented by types
     * that fulfill the JCR interfaces that represent definitions.
     *
     * @param itemDefinition the given item definition
     * @return a resource for the given ItemDefinition
     * @throws javax.jcr.RepositoryException if repository exception occurred
     */
    public static Resource getResource(final ItemDefinition itemDefinition) throws RepositoryException {
        return getResource((Namespaced) itemDefinition);
    }
}
