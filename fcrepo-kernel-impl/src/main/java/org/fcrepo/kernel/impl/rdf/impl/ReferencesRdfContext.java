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

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.impl.mappings.PropertyToTriple;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
        putReferencesIntoContext(resource, property2triple);
        putReferencesIntoContext(resource, property2proxyRefs);
    }

    private void putReferencesIntoContext(final FedoraResource resource, final Function f) throws RepositoryException {
        concat(Iterators.concat(Iterators.transform(resource.getNode().getWeakReferences(), f)));
        concat(Iterators.concat(Iterators.transform(resource.getNode().getReferences(), f)));
    }

    private Function<Property, Iterator<Triple>> property2proxyRefs = new Function<Property, Iterator<Triple>>() {
        @Override
        public Iterator<Triple> apply(final Property p) {
            final Set<Iterator<Triple>> triples = new HashSet<>();
            try {
                for ( final PropertyIterator it = p.getParent().getProperties(); it.hasNext(); ) {
                    final Property potentialProxy = it.nextProperty();
                    if (potentialProxy.isMultiple()) {
                        for ( Value v : potentialProxy.getValues() ) {
                            triples.add(findProxies(v));
                        }
                    } else {
                        triples.add(findProxies(potentialProxy.getValue()));
                    }
                }
            } catch (RepositoryException ex) {
                throw new RepositoryRuntimeException(ex);
            }
            return Iterators.concat(triples.iterator());
        }
    };
    private Iterator<Triple> findProxies(final Value v) throws RepositoryException {
        if (v.getType() == PATH || v.getType() == REFERENCE || v.getType() == WEAKREFERENCE) {
            return new LdpContainerRdfContext(nodeConverter.convert(nodeForValue(session(), v)), translator());
        } else {
            return Collections.<Triple>emptyIterator();
        }
    }
}
