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
package org.fcrepo.http.commons.api.rdf;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import javax.ws.rs.core.UriInfo;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.utils.iterators.RdfStream;
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
     * @param idTranslator
     */
    public void addHttpComponentModelsForResourceToStream(final RdfStream rdfStream,
            final FedoraResource resource, final UriInfo uriInfo,
            final IdentifierConverter<Resource,FedoraResource> idTranslator) {

        LOGGER.debug("Adding additional HTTP context triples to stream");
        for (final Map.Entry<String, UriAwareResourceModelFactory> e : getUriAwareTripleFactories()
                .entrySet()) {
            final String beanName = e.getKey();
            final UriAwareResourceModelFactory uriAwareResourceModelFactory =
                    e.getValue();
            LOGGER.debug("Adding response information using: {}", beanName);

            final Model m =
                    uriAwareResourceModelFactory.createModelForResource(
                            resource, uriInfo, idTranslator);
            rdfStream.concat(RdfStream.fromModel(m));
        }

    }

    private Map<String, UriAwareResourceModelFactory> getUriAwareTripleFactories() {
        return applicationContext
                .getBeansOfType(UriAwareResourceModelFactory.class);
    }
}
