package org.fcrepo.api.rdf;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.FedoraResource;
import org.fcrepo.rdf.GraphSubjects;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
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
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Add additional models to the RDF dataset for the given resource
     * @param dataset the source dataset we'll add named models to
     * @param resource the FedoraResource in question
     * @param uriInfo a JAX-RS UriInfo object to build URIs to resources
     * @param graphSubjects
     * @throws RepositoryException
     */
    public void addHttpComponentModelsForResource(Dataset dataset, FedoraResource resource, UriInfo uriInfo, GraphSubjects graphSubjects) throws RepositoryException {

        LOGGER.debug("Adding additional HTTP context triples to dataset");
        for (final Map.Entry<String, UriAwareResourceModelFactory> e : getUriAwareTripleFactories().entrySet()) {
            final String beanName = e.getKey();
            final UriAwareResourceModelFactory uriAwareResourceModelFactory = e.getValue();
            LOGGER.debug("Adding response information using {}", beanName);

            final Model m = uriAwareResourceModelFactory.createModelForResource(resource, uriInfo, graphSubjects);
            dataset.addNamedModel(beanName, m);
        }

    }

    private Map<String, UriAwareResourceModelFactory> getUriAwareTripleFactories() {
        return applicationContext.getBeansOfType(UriAwareResourceModelFactory.class);

    }
}
