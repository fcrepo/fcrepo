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

import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.impl.mappings.PropertyToTriple;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import java.util.Iterator;

/**
 * Accumulate inbound references to a given resource
 *
 * @author cabeer
 */
public class ReferencesRdfContext extends NodeRdfContext {

    private final PropertyToTriple property2triple;

    /**
     * Add the inbound references from other nodes to this resource to the stream
     *
     * @param resource
     * @param idTranslator
     * @throws javax.jcr.RepositoryException
     */

    public ReferencesRdfContext(final FedoraResource resource,
                                final IdentifierConverter<Resource, FedoraResource> idTranslator)
        throws RepositoryException {
        super(resource, idTranslator);
        property2triple = new PropertyToTriple(resource.getNode().getSession(), idTranslator);
        concat(putStrongReferencePropertiesIntoContext());
        concat(putWeakReferencePropertiesIntoContext());
    }

    private Iterator<Triple> putWeakReferencePropertiesIntoContext() throws RepositoryException {
        final Iterator<Property> properties = resource().getNode().getWeakReferences();

        return Iterators.concat(Iterators.transform(properties, property2triple));
    }

    private Iterator<Triple> putStrongReferencePropertiesIntoContext() throws RepositoryException {
        final Iterator<Property> properties = resource().getNode().getReferences();

        return Iterators.concat(Iterators.transform(properties, property2triple));

    }

}
