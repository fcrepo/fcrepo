/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeToResource;

import javax.jcr.Node;

import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;

import com.google.common.base.Converter;
import org.apache.jena.rdf.model.Resource;

/**
 * {@link org.fcrepo.kernel.api.RdfStream} that holds contexts related to a specific {@link Node}.
 *
 * @author ajs6f
 * @since Oct 10, 2013
 */
public class NodeRdfContext extends DefaultRdfStream {

    private final FedoraResource resource;

    private final IdentifierConverter<Resource, FedoraResource> idTranslator;

    private final org.apache.jena.graph.Node subject;

    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     */
    public NodeRdfContext(final FedoraResource resource,
                          final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        super(idTranslator.reverse().convert(resource).asNode());
        this.resource = resource;
        this.idTranslator = idTranslator;
        this.subject = uriFor(resource);
    }

    /**
     * @return The {@link Node} in question
     */
    public FedoraResource resource() {
        return resource;
    }

    /**
     * @return local {@link org.fcrepo.kernel.api.identifiers.IdentifierConverter}
     */
    public IdentifierConverter<Resource, FedoraResource> translator() {
        return idTranslator;
    }

    /**
     * @param resource a Fedora model instance that must be identified by an URI
     * @return a translated URI for that resource
     */
    protected org.apache.jena.graph.Node uriFor(final FedoraResource resource) {
        return translator().reverse().convert(resource).asNode();
    }

    /**
     * @return local {@link org.fcrepo.kernel.api.identifiers.IdentifierConverter}
     */
    public Converter<Node, Resource> nodeConverter() {
        return nodeToResource(idTranslator);
    }

    /**
     * @return the RDF subject at the center of this context
     */
    public org.apache.jena.graph.Node subject() {
        return subject;
    }
}
