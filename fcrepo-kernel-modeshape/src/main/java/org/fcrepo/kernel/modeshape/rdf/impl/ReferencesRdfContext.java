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
package org.fcrepo.kernel.modeshape.rdf.impl;

import static org.fcrepo.kernel.api.utils.UncheckedFunction.uncheck;
import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.modeshape.rdf.converters.ValueConverter.nodeForValue;
import static java.util.Arrays.asList;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.WEAKREFERENCE;

import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.modeshape.rdf.impl.mappings.PropertyToTriple;
import org.fcrepo.kernel.modeshape.rdf.impl.mappings.PropertyValueIterator;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Accumulate inbound references to a given resource
 *
 * @author cabeer
 * @author escowles
 */
public class ReferencesRdfContext extends NodeRdfContext {

    private final PropertyToTriple property2triple;

    public static final List<Integer> REFERENCE_TYPES = asList(PATH, REFERENCE, WEAKREFERENCE);

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

    private final Predicate<Triple> INBOUND = t -> {
        return t.getObject().getURI().equals(uriFor(resource()).getURI());
    };

    @SuppressWarnings("unchecked")
    private void putReferencesIntoContext(final Node node) throws RepositoryException {
        Iterator<Property> references = node.getReferences();
        Iterator<Property> weakReferences = node.getWeakReferences();
        Iterator<Property> allReferences = Iterators.concat(references, weakReferences);
        concat(flatMap(allReferences, property2triple));

        references = node.getReferences();
        weakReferences = node.getWeakReferences();
        allReferences = Iterators.concat(references, weakReferences);
        concat(Iterators.filter(flatMap(flatMap( allReferences, potentialProxies), triplesForValue), INBOUND::test));
    }

    /* References from LDP indirect containers are generated dynamically by LdpContainerRdfContext, so they won't
       show up in getReferences()/getWeakReferences().  Instead, we should check referencers to see if they are
       members of an IndirectContainer and generate the appropriate inbound references. */
    @SuppressWarnings("unchecked")
    private final Function<Property, Iterator<Value>> potentialProxies = uncheck(p -> Iterators.filter(
            new PropertyValueIterator(p.getParent().getProperties()), v -> REFERENCE_TYPES.contains(v.getType())));

    private final Function<Value, Iterator<Triple>> triplesForValue = uncheck(v ->
        new LdpContainerRdfContext(nodeConverter.convert(nodeForValue(session(), v)), translator()));
}
