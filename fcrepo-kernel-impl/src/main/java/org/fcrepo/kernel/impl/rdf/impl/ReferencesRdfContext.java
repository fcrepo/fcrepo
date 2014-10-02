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

import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.impl.rdf.impl.mappings.PropertyToTriple;
import org.fcrepo.kernel.utils.iterators.PropertyIterator;
import org.fcrepo.kernel.utils.iterators.RdfStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import java.util.Iterator;

/**
 * Accumulate inbound references to a given node
 *
 * @author cabeer
 */
public class ReferencesRdfContext extends RdfStream {

    private final Node node;
    private PropertyToTriple property2triple;

    /**
     * Add the inbound references from other nodes to this node to the {@link RdfStream}
     *
     * @param node
     * @param graphSubjects
     * @throws javax.jcr.RepositoryException
     */

    public ReferencesRdfContext(final javax.jcr.Node node, final IdentifierTranslator graphSubjects)
        throws RepositoryException {
        super();
        this.node = node;
        property2triple = new PropertyToTriple(graphSubjects);
        concat(putStrongReferencePropertiesIntoContext());
        concat(putWeakReferencePropertiesIntoContext());
    }

    private Iterator<Triple> putWeakReferencePropertiesIntoContext() throws RepositoryException {
        final Iterator<Property> properties = new PropertyIterator(node.getWeakReferences());

        return Iterators.concat(Iterators.transform(properties, property2triple));
    }

    private Iterator<Triple> putStrongReferencePropertiesIntoContext() throws RepositoryException {
        final Iterator<Property> properties = new PropertyIterator(node.getReferences());

        return Iterators.concat(Iterators.transform(properties, property2triple));

    }

}
