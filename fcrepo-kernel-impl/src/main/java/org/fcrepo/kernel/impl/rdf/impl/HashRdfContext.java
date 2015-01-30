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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import java.util.Iterator;

import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeConverter;

/**
 * @author cabeer
 * @since 10/9/14
 */
public class HashRdfContext extends NodeRdfContext {


    /**
     * Default constructor.
     *
     * @param resource
     * @param idTranslator
     * @throws javax.jcr.RepositoryException
     */
    public HashRdfContext(final FedoraResource resource,
                          final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);

        final Node node = resource().getNode();
        if (node.hasNode("#")) {
            final Iterator<Node> hashChildrenNodes = node.getNode("#").getNodes();
            concat(Iterators.concat(Iterators.transform(hashChildrenNodes,
                    new Function<Node, Iterator<Triple>>() {
                        @Override
                        public Iterator<Triple> apply(final Node input) {
                            final FedoraResource resource = nodeConverter.convert(input);

                            return resource.getTriples(idTranslator, ImmutableList.of(TypeRdfContext.class,
                                    PropertiesRdfContext.class,
                                    BlankNodeRdfContext.class));
                        }
                    })));
        }
    }
}