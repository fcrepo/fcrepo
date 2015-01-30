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

import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeToResource;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;

import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;

import com.google.common.base.Converter;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * {@link RdfStream} that holds contexts related to a specific {@link Node}.
 *
 * @author ajs6f
 * @since Oct 10, 2013
 */
public class NodeRdfContext extends RdfStream {

    private final FedoraResource resource;

    private final IdentifierConverter<Resource, FedoraResource> idTranslator;

    private final com.hp.hpl.jena.graph.Node subject;

    private static final Logger LOGGER = getLogger(NodeRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource
     * @param idTranslator
     */
    public NodeRdfContext(final FedoraResource resource,
                          final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        super();
        this.resource = resource;
        this.idTranslator = idTranslator;
        this.subject = idTranslator.reverse().convert(resource).asNode();
    }

    /**
     * @return The {@link Node} in question
     */
    public FedoraResource resource() {
        return resource;
    }

    /**
     * @return local {@link org.fcrepo.kernel.identifiers.IdentifierConverter}
     */
    public IdentifierConverter<Resource, FedoraResource> translator() {
        return idTranslator;
    }

    /**
     * @return local {@link org.fcrepo.kernel.identifiers.IdentifierConverter}
     */
    public Converter<Node, Resource> nodeConverter() {
        return nodeToResource(idTranslator);
    }

    /**
     * @return the RDF subject at the center of this context
     */
    public com.hp.hpl.jena.graph.Node subject() {
        return subject;
    }
}
