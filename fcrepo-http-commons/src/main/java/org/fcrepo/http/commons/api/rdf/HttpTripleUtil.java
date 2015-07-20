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
package org.fcrepo.http.commons.api.rdf;

import static org.fcrepo.kernel.api.utils.iterators.RdfStream.fromModel;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import javax.ws.rs.core.UriInfo;

import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;

import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.hp.hpl.jena.rdf.model.Model;

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
     * @param idTranslator the id translator
     */
    public void addHttpComponentModelsForResourceToStream(final RdfStream rdfStream,
            final FedoraResource resource, final UriInfo uriInfo,
            final IdentifierConverter<Resource,FedoraResource> idTranslator) {

        LOGGER.debug("Adding additional HTTP context triples to stream");
        getUriAwareTripleFactories().forEach((bean, factory) -> {
            LOGGER.debug("Adding response information using: {}", bean);
            final Model m = factory.createModelForResource(resource, uriInfo, idTranslator);
            rdfStream.concat(fromModel(m));
        });
    }

    private Map<String, UriAwareResourceModelFactory> getUriAwareTripleFactories() {
        return applicationContext
                .getBeansOfType(UriAwareResourceModelFactory.class);
    }
}
