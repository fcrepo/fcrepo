/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.spring;

import java.io.IOException;
import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;

import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrRepositoryFactory;
import org.modeshape.jcr.api.Repository;
import org.modeshape.jcr.api.RepositoryFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @todo Add Documentation.
 * @author Edwin Shin
 * @date Feb 7, 2013
 */
public class ModeShapeRepositoryFactoryBean implements
        FactoryBean<JcrRepository> {

    private static final Logger LOGGER = getLogger(ModeShapeRepositoryFactoryBean.class);

    @Inject
    private JcrRepositoryFactory jcrRepositoryFactory;

    private Resource repositoryConfiguration;

    private JcrRepository repository;

    /**
     * @todo Add Documentation.
     */
    @PostConstruct
    public void buildRepository() throws RepositoryException, IOException {
        LOGGER.debug("Using repo config: {}",
                     ((ClassPathResource) repositoryConfiguration).getPath());

        repository =
                (JcrRepository) jcrRepositoryFactory.getRepository(Collections
                        .singletonMap(RepositoryFactory.URL,
                                repositoryConfiguration.getURL()));

    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public JcrRepository getObject() throws RepositoryException, IOException {
        return repository;
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public Class<?> getObjectType() {
        return Repository.class;
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * @todo Add Documentation.
     */
    public void setRepositoryConfiguration(
            final Resource repositoryConfiguration) {
        this.repositoryConfiguration = repositoryConfiguration;
    }

}
