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

import static org.slf4j.LoggerFactory.getLogger;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.apache.jena.graph.Triple;

import org.slf4j.Logger;


/**
 * {@link org.fcrepo.kernel.api.RdfStream} that supplies {@link Triple}s concerning
 * the versions of a selected Node.
 *
 * @author ajs6f
 * @since Oct 15, 2013
 */
public class VersionsRdfContext extends DefaultRdfStream {

    private final FedoraResource resource;

    private static final Logger LOGGER = getLogger(VersionsRdfContext.class);

    /**
     * Ordinary constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     */
    public VersionsRdfContext(final FedoraResource resource,
                              final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        super(idTranslator.reverse().convert(resource).asNode());
        this.resource = resource;
        concat(versionTriples());
        LOGGER.warn("Review if this class can be removed after implementing Memento!");
    }

    @SuppressWarnings("unchecked")
    private Stream<Triple> versionTriples() {
        return null;
    }
}
