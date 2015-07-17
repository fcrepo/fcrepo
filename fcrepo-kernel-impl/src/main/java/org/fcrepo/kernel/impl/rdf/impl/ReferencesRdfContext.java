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
package org.fcrepo.kernel.impl.rdf.impl;

import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.impl.rdf.converters.ValueConverter.nodeForValue;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.WEAKREFERENCE;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.impl.mappings.PropertyToTriple;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import java.util.Iterator;

/**
 * Accumulate inbound references to a given resource
 *
 * @author cabeer
 * @author escowles
 */
public class ReferencesRdfContext extends NodeRdfContext {

    private final PropertyToTriple property2triple;

    /**
     * Add the inbound references from other nodes to this resource to the stream
     *
     * @param resource the resource
     * @param idTranslator the id translator
     * @throws RepositoryException if repository exception occurred
     */

    public ReferencesRdfContext(final FedoraResource resource,
                                final IdentifierConverter<Resource, FedoraResource> idTranslator)
        throws RepositoryException {
        super(resource, idTranslator);
        property2triple = new PropertyToTriple(resource.getNode().getSession(), idTranslator);
        putReferencesIntoContext(resource.getNode().getWeakReferences());
        putReferencesIntoContext(resource.getNode().getReferences());
    }

    private void putReferencesIntoContext(final Iterator<Property> properties) throws RepositoryException {
        while (properties.hasNext()) {
            final Property p = properties.next();
            concat(property2triple.apply(p));

            for ( final PropertyIterator it = p.getParent().getProperties(); it.hasNext(); ) {
                final Property potentialProxy = it.nextProperty();
                if (potentialProxy.isMultiple()) {
                    for ( Value v : potentialProxy.getValues() ) {
                        putProxyReferencesIntoContext(v);
                    }
                } else {
                    putProxyReferencesIntoContext(potentialProxy.getValue());
                }
            }
        }
    }
    private void putProxyReferencesIntoContext(final Value v) throws RepositoryException {
        if (v.getType() == PATH || v.getType() == REFERENCE || v.getType() == WEAKREFERENCE) {
            putProxyReferencesIntoContext(nodeForValue(session(), v));
        }
    }
    private void putProxyReferencesIntoContext(final Node n) throws RepositoryException {
        concat(new LdpContainerRdfContext(nodeConverter.convert(n), translator()));
    }
}
