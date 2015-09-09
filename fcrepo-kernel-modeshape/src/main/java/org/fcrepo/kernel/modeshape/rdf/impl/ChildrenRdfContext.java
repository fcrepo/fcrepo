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

import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;

import java.util.Iterator;
import java.util.function.Function;

import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @author ajs6f
 * @since 9/16/14
 */
public class ChildrenRdfContext extends NodeRdfContext {

    private static final Logger LOGGER = getLogger(ChildrenRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the idTranslator
     * @throws javax.jcr.RepositoryException if repository exception occurred
     */
    public ChildrenRdfContext(final FedoraResource resource,
                              final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);

        if (resource.getNode().hasNodes()) {
            LOGGER.trace("Found children of this resource: {}", resource.getPath());
            final Iterator<FedoraResource> niceChildren = resource().getChildren();
            concat(Iterators.transform(niceChildren, child2triple::apply));
        }
    }

    private final Function<FedoraResource, Triple> child2triple = child -> {

        final com.hp.hpl.jena.graph.Node childSubject = child instanceof NonRdfSourceDescription ?
                uriFor(((NonRdfSourceDescription) child).getDescribedResource()) : uriFor(child);

        LOGGER.trace("Creating triple for child node: {}", child);
        return create(subject(), CONTAINS.asNode(), childSubject);
    };
}
