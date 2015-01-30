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
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.models.NonRdfSourceDescription;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;

import java.util.Iterator;

import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.kernel.RdfLexicon.CONTAINS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 9/16/14
 */
public class ChildrenRdfContext extends NodeRdfContext {

    private static final Logger LOGGER = getLogger(ChildrenRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource
     * @param idTranslator
     * @throws javax.jcr.RepositoryException
     */
    public ChildrenRdfContext(final FedoraResource resource,
                              final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);

        if (resource.getNode().hasNodes()) {
            LOGGER.trace("Found children of this resource.");
            concat(childrenContext());
        }
    }


    private Iterator<Triple> childrenContext() {

        final Iterator<FedoraResource> niceChildren = resource().getChildren();

        return Iterators.concat(Iterators.transform(niceChildren, child2triples()));
    }

    private Function<FedoraResource, Iterator<Triple>> child2triples() {
        return new Function<FedoraResource, Iterator<Triple>>() {

            @Override
            public Iterator<Triple> apply(final FedoraResource child) {

                final com.hp.hpl.jena.graph.Node childSubject;

                if (child instanceof NonRdfSourceDescription) {
                    childSubject = translator().reverse()
                            .convert(((NonRdfSourceDescription) child).getDescribedResource())
                            .asNode();
                } else {
                    childSubject = translator().reverse().convert(child).asNode();
                }
                LOGGER.trace("Creating triples for child node: {}", child);
                final RdfStream childStream = new RdfStream();

                childStream.concat(create(subject(), CONTAINS.asNode(), childSubject));

                return childStream;

            }
        };
    }

}
