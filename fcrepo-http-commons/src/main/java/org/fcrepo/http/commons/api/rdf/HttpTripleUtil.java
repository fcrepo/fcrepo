/**
 * Copyright 2013 DuraSpace, Inc.
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

import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.FedoraResourceImpl;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * Utility for injecting HTTP-contextual data into a Dataset
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
     * @param dataset the source dataset we'll add named models to
     * @param resource the FedoraResourceImpl in question
     * @param uriInfo a JAX-RS UriInfo object to build URIs to resources
     * @param graphSubjects
     * @throws RepositoryException
     */
    public void addHttpComponentModelsForResource(final Dataset dataset,
            final FedoraResourceImpl resource, final UriInfo uriInfo,
            final GraphSubjects graphSubjects) throws RepositoryException {

        LOGGER.debug("Adding additional HTTP context triples to dataset");
        for (final Map.Entry<String, UriAwareResourceModelFactory> e : getUriAwareTripleFactories()
                .entrySet()) {
            final String beanName = e.getKey();
            final UriAwareResourceModelFactory uriAwareResourceModelFactory =
                    e.getValue();
            LOGGER.debug("Adding response information using {}", beanName);

            final Model m =
                    uriAwareResourceModelFactory.createModelForResource(
                            resource, uriInfo, graphSubjects);
            dataset.addNamedModel(beanName, m);
        }

    }

    /**
     * Add additional models to the RDF dataset for the given resource
     *
     * @param rdfStream the source stream we'll add named models to
     * @param resource the FedoraResourceImpl in question
     * @param uriInfo a JAX-RS UriInfo object to build URIs to resources
     * @param graphSubjects
     * @throws RepositoryException
     */
    public void addHttpComponentModelsForResourceToStream(final RdfStream rdfStream,
            final FedoraResource resource, final UriInfo uriInfo,
            final GraphSubjects graphSubjects) throws RepositoryException {

        LOGGER.debug("Adding additional HTTP context triples to dataset");
        for (final Map.Entry<String, UriAwareResourceModelFactory> e : getUriAwareTripleFactories()
                .entrySet()) {
            final String beanName = e.getKey();
            final UriAwareResourceModelFactory uriAwareResourceModelFactory =
                    e.getValue();
            LOGGER.debug("Adding response information using: {}", beanName);

            final Model m =
                    uriAwareResourceModelFactory.createModelForResource(
                            resource, uriInfo, graphSubjects);
            rdfStream.concat(RdfStream.fromModel(m));
        }

    }

    private Map<String, UriAwareResourceModelFactory> getUriAwareTripleFactories() {
        return applicationContext
                .getBeansOfType(UriAwareResourceModelFactory.class);
    }
}
