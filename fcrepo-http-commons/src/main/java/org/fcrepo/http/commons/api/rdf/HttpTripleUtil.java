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
package org.fcrepo.http.commons.api.rdf;

import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Stream.concat;
import static java.util.stream.StreamSupport.stream;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.apache.jena.rdf.model.Statement;

import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.RdfStream;

import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Utility for injecting HTTP-contextual data into an RdfStream
 *
 * @author awoods
 */
@Component
public class HttpTripleUtil implements ApplicationContextAware {

    private static final Logger LOGGER = getLogger(HttpTripleUtil.class);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Add additional models to the RDF dataset for the given resource
     *
     * @param rdfStream the source stream we'll add named models to
     * @param resource the FedoraResourceImpl in question
     * @param uriInfo a JAX-RS UriInfo object to build URIs to resources
     * @return an RdfStream with the added triples
     */
    public RdfStream addHttpComponentModelsForResourceToStream(final RdfStream rdfStream,
            final FedoraResource resource, final UriInfo uriInfo) {

        LOGGER.debug("Adding additional HTTP context triples to stream");
        return new DefaultRdfStream(rdfStream.topic(), concat(rdfStream, getUriAwareTripleFactories().entrySet()
                    .stream().flatMap(e -> {
            LOGGER.debug("Adding response information using: {}", e.getKey());
            return stream(spliteratorUnknownSize(e.getValue().createModelForResource(resource, uriInfo)
                    .listStatements(), IMMUTABLE), false).map(Statement::asTriple);
        })));
    }

    private Map<String, UriAwareResourceModelFactory> getUriAwareTripleFactories() {
        return applicationContext
                .getBeansOfType(UriAwareResourceModelFactory.class);
    }
}
