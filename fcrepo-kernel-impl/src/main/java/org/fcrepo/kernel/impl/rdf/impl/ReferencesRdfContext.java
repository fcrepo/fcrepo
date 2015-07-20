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
import static java.util.Collections.addAll;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.WEAKREFERENCE;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.impl.mappings.PropertyToTriple;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

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
        putReferencesIntoContext(resource.getNode());
    }

    private void putReferencesIntoContext(final Node node) throws RepositoryException {
        concat(Iterators.concat(Iterators.transform(
            Iterators.concat(node.getReferences(), node.getWeakReferences()), property2triple)));
        concat(Iterators.concat(Iterators.transform(Iterators.concat(Iterators.transform(
            Iterators.concat(node.getReferences(), node.getWeakReferences()), potentialProxies)), triplesForValue)));
    }

    /* References from LDP indirect containers are generated dynamically by LdpContainerRdfContext, so they won't
       show up in getReferences()/getWeakReferences().  Instead, we should check referrers to see if they are
       members of an IndirectContainer and generate the appropriate inbound references. */
    private Function<Property, Iterator<Value>> potentialProxies = new Function<Property, Iterator<Value>>() {
        @Override
        public Iterator<Value> apply(final Property p) {
            final Set<Value> values = new HashSet<Value>();
            try {
                for ( final PropertyIterator it = p.getParent().getProperties(); it.hasNext(); ) {
                    final Property potentialProxy = it.nextProperty();
                    if (potentialProxy.isMultiple()) {
                        addAll(values, potentialProxy.getValues());
                    } else {
                        values.add(potentialProxy.getValue());
                    }
                }
            } catch (RepositoryException ex) {
                throw new RepositoryRuntimeException(ex);
            }
            return Iterators.filter(values.iterator(), isReference);
        }
    };
    private Function<Value, Iterator<Triple>> triplesForValue = new Function<Value, Iterator<Triple>>() {
        @Override
        public Iterator<Triple> apply(final Value v) {
            try {
                return new LdpContainerRdfContext(nodeConverter.convert(nodeForValue(session(), v)), translator());
            } catch (RepositoryException ex) {
                throw new RepositoryRuntimeException(ex);
            }
        }
    };
    private Predicate<Value> isReference = new Predicate<Value>() {
        @Override
        public boolean apply(final Value v) {
            return v.getType() == PATH || v.getType() == REFERENCE || v.getType() == WEAKREFERENCE;
        }
    };

}
