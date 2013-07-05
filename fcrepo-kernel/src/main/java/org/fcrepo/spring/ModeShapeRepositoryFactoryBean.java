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

package org.fcrepo.spring;

import static java.util.Collections.singletonMap;
import static org.modeshape.jcr.api.RepositoryFactory.URL;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;

import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrRepositoryFactory;
import org.modeshape.jcr.api.Repository;
import org.slf4j.Logger;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * A Modeshape factory shim to make it play nice with our Spring-based
 * configuration
 * 
 * @author Edwin Shin
 * @date Feb 7, 2013
 */
public class ModeShapeRepositoryFactoryBean implements
        FactoryBean<JcrRepository> {

    private static final Logger LOGGER =
            getLogger(ModeShapeRepositoryFactoryBean.class);

    @Inject
    private JcrRepositoryFactory jcrRepositoryFactory;

    private Resource repositoryConfiguration;

    private JcrRepository repository;

    /**
     * Generate a JCR repository from the given configuration
     * 
     * @throws RepositoryException
     * @throws IOException
     */
    @PostConstruct
    public void buildRepository() throws RepositoryException, IOException {
        if (repositoryConfiguration instanceof ClassPathResource) {
            LOGGER.debug("Using repo config: {}",
                    ((ClassPathResource) repositoryConfiguration).getPath());
        }

        repository =
                (JcrRepository) jcrRepositoryFactory
                        .getRepository(singletonMap(URL,
                                repositoryConfiguration.getURL()));

    }

    @Override
    public JcrRepository getObject() throws RepositoryException, IOException {
        return repository;
    }

    @Override
    public Class<?> getObjectType() {
        return Repository.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * Set the configuration to use for creating the repository
     * 
     * @param repositoryConfiguration
     */
    public void setRepositoryConfiguration(
            final Resource repositoryConfiguration) {
        this.repositoryConfiguration = repositoryConfiguration;
    }

}
