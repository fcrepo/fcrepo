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
package org.fcrepo.kernel.modeshape.rdf.converters;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableBiMap;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.exception.FedoraInvalidNamespaceException;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.modeshape.jcr.api.Namespaced;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import java.util.Map;

import static com.google.common.base.Throwables.propagate;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static org.fcrepo.kernel.modeshape.rdf.JcrRdfTools.getJcrNamespaceForRDFNamespace;
import static org.fcrepo.kernel.modeshape.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getReferencePropertyOriginalName;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isInternalReferenceProperty;
import static org.fcrepo.kernel.api.utils.NamespaceTools.getNamespaceRegistry;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Convert between RDF properties and JCR properties
 * @author cabeer
 * @since 10/8/14
 */
public class PropertyConverter extends Converter<javax.jcr.Property, Property> {
    private static final Logger LOGGER = getLogger(PropertyConverter.class);

    @Override
    protected Property doForward(final javax.jcr.Property property) {
        LOGGER.trace("Creating predicate for property: {}",
                property);
        try {
            if (property instanceof Namespaced) {
                final Namespaced nsProperty = (Namespaced) property;
                final String uri = nsProperty.getNamespaceURI();
                final String localName = nsProperty.getLocalName();
                final String rdfLocalName;

                if (isInternalReferenceProperty.test(property)) {
                    rdfLocalName = getReferencePropertyOriginalName(localName);
                } else {
                    rdfLocalName = localName;
                }
                return createProperty(
                        getRDFNamespaceForJcrNamespace(uri),
                        rdfLocalName);
            }
            return createProperty(property.getName());
        } catch (final RepositoryException e) {
            throw propagate(e);
        }

    }

    @Override
    protected javax.jcr.Property doBackward(final Property property) {
        throw new UnsupportedOperationException();
    }

    /**
     * Given an RDF predicate value (namespace URI + local name), figure out
     * what JCR property to use
     *
     * @param node the JCR node we want a property for
     * @param predicate the predicate to map to a property name
     * @param namespaceMapping prefix to uri namespace mapping
     * @return the JCR property name
     * @throws RepositoryException if repository exception occurred
     */
    public static String getPropertyNameFromPredicate(final Node node,
                                                      final Resource predicate,
                                                      final Map<String, String> namespaceMapping)
            throws RepositoryException {
        final NamespaceRegistry namespaceRegistry = getNamespaceRegistry.apply(node);
        return getPropertyNameFromPredicate(namespaceRegistry,
                predicate, namespaceMapping);
    }

    /**
     * Get the JCR property name for an RDF predicate
     *
     * @param namespaceRegistry the namespace registry
     * @param predicate the predicate to map to a property name
     * @param namespaceMapping the namespace mapping
     * @return JCR property name for an RDF predicate
     * @throws RepositoryException if repository exception occurred
     */
    public static String getPropertyNameFromPredicate(final NamespaceRegistry namespaceRegistry,
                                                      final Resource predicate,
                                                      final Map<String, String> namespaceMapping)
            throws RepositoryException {

        // reject if update request contains any fcr namespaces
        if (namespaceMapping != null && namespaceMapping.containsKey("fcr")) {
            throw new FedoraInvalidNamespaceException("Invalid fcr namespace properties " + predicate + ".");
        }

        final String rdfNamespace = predicate.getNameSpace();

        // log warning if the user-supplied namespace doesn't match value from predicate.getNameSpace(),
        // e.g., if the Jena method returns "http://" for "http://myurl.org" (no terminating character).
        if (namespaceMapping != null && !namespaceMapping.containsValue(rdfNamespace)) {
            LOGGER.warn("The namespace of predicate: {} was possibly misinterpreted as: {}."
                    , predicate, rdfNamespace);
        }

        final String rdfLocalname = predicate.getLocalName();

        final String prefix;

        assert (namespaceRegistry != null);

        final String namespace = getJcrNamespaceForRDFNamespace(rdfNamespace);

        if (namespaceRegistry.isRegisteredUri(namespace)) {
            LOGGER.debug("Discovered namespace: {} in namespace registry.",namespace);
            prefix = namespaceRegistry.getPrefix(namespace);
        } else {
            LOGGER.debug("Didn't discover namespace: {} in namespace registry.",namespace);
            final ImmutableBiMap<String, String> nsMap =
                    ImmutableBiMap.copyOf(namespaceMapping);
            if (nsMap.containsValue(namespace)) {
                LOGGER.debug("Discovered namespace: {} in namespace map: {}.", namespace,
                        nsMap);
                prefix = nsMap.inverse().get(namespace);
                namespaceRegistry.registerNamespace(prefix, namespace);
            } else {
                prefix = namespaceRegistry.registerNamespace(namespace);
            }
        }

        final String propertyName = prefix + ":" + rdfLocalname;

        LOGGER.debug("Took RDF predicate {} and translated it to JCR property {}", namespace, propertyName);

        return propertyName;

    }

}
