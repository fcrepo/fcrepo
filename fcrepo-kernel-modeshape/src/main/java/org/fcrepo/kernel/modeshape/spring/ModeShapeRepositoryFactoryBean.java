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
package org.fcrepo.kernel.modeshape.spring;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.modeshape.FedoraRepositoryImpl;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.NoSuchRepositoryException;
import org.modeshape.jcr.RepositoryConfiguration;
import org.slf4j.Logger;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.Resource;

import static org.fcrepo.kernel.api.RdfLexicon.SERVER_MANAGED_PROPERTIES_MODE;

/**
 * A Modeshape factory shim to make it play nice with our Spring-based
 * configuration
 *
 * @author Edwin Shin
 * @since Feb 7, 2013
 */
public class ModeShapeRepositoryFactoryBean implements
        FactoryBean<FedoraRepository> {

    private static final Logger LOGGER =
            getLogger(ModeShapeRepositoryFactoryBean.class);

    private DefaultPropertiesLoader propertiesLoader;

    @Inject
    private ModeShapeEngine modeShapeEngine;

    private Resource repositoryConfiguration;

    private JcrRepository repository;

    /**
     * Generate a JCR repository from the given configuration
     *
     */
    @PostConstruct
    public void buildRepository() {
        try {
            LOGGER.info("Using repo config (classpath): {}", repositoryConfiguration.getURL());
            getPropertiesLoader().loadSystemProperties();

            final RepositoryConfiguration config =
                    RepositoryConfiguration.read(repositoryConfiguration.getURL());
            repository = modeShapeEngine.deploy(config);

            // next line ensures that repository starts before the factory is used.
            final org.modeshape.common.collection.Problems problems =
                    repository.getStartupProblems();
            for (final org.modeshape.common.collection.Problem p : problems) {
                LOGGER.error("ModeShape Start Problem: {}", p.getMessageString());
                // TODO determine problems that should be runtime errors
            }
        } catch (final Exception e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Attempts to undeploy the repository and shutdown the ModeShape engine on
     * context destroy.
     *
     * @throws InterruptedException if interrupted exception occurred
     */
    @PreDestroy
    public void stopRepository() throws InterruptedException {
        LOGGER.info("Initiating shutdown of ModeShape");
        final String repoName = repository.getName();
        try {
            final Future<Boolean> futureUndeployRepo = modeShapeEngine.undeploy(repoName);
            if (futureUndeployRepo.get()) {
                LOGGER.info("ModeShape repository {} has undeployed.", repoName);
            } else {
                LOGGER.error("ModeShape repository {} undeploy failed without an exception, still deployed.", repoName);
            }
            LOGGER.info("Repository {} undeployed.", repoName);
        } catch (final NoSuchRepositoryException e) {
            LOGGER.error("Repository {} unknown, cannot undeploy.", repoName, e);
        } catch (final ExecutionException e) {
            LOGGER.error("Repository {} cannot undeploy.", repoName, e.getCause());
        }
        final Future<Boolean> futureShutdownEngine = modeShapeEngine.shutdown();
        try {
            if (futureShutdownEngine.get()) {
                LOGGER.info("ModeShape Engine has shutdown.");
            } else {
                LOGGER.error("ModeShape Engine shutdown failed without an exception, still running.");
            }
        } catch (final ExecutionException e) {
            LOGGER.error("ModeShape Engine shutdown failed.", e.getCause());
        }
    }

    @Override
    public FedoraRepository getObject() {
        return new FedoraRepositoryImpl(repository);
    }

    @Override
    public Class<?> getObjectType() {
        return FedoraRepository.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * Set the configuration to use for creating the repository
     *
     * @param repositoryConfiguration the repository configuration
     */
    public void setRepositoryConfiguration(
            final Resource repositoryConfiguration) {
        this.repositoryConfiguration = repositoryConfiguration;
    }

    private DefaultPropertiesLoader getPropertiesLoader() {
        if (null == propertiesLoader) {
            propertiesLoader = new DefaultPropertiesLoader();
        }
        return propertiesLoader;
    }

    /**
     * Set the mode for server managed properties.  The value "strict" (default)
     * will result in the classic behavior where certain provenance-related
     * properties cannot be set, while "relaxed" will allow for them to be
     * set in some circumstances.
     *
     * @param value the value (must be either "strict" or "relaxed")
     */
    public void setServerManagedPropertiesMode(final String value) {
        if ("strict".equals(value) || "relaxed".equals(value)) {
            final String propertyValue = System.getProperty(SERVER_MANAGED_PROPERTIES_MODE);
            if (propertyValue != null) {
                LOGGER.warn("The system property \"{}\" with the value \"{}\" is being used instead of the" +
                                "serverManagedPropertiesMode of \"{}\" specified in the Spring configuration!",
                        SERVER_MANAGED_PROPERTIES_MODE, propertyValue, value);
            } else {
                LOGGER.info("Set sytem property \"{}\" to \"{}\" to correspond to serverManagedPropertiesMode.",
                        SERVER_MANAGED_PROPERTIES_MODE, value);
                System.setProperty(SERVER_MANAGED_PROPERTIES_MODE, value);
            }
        } else {
            throw new IllegalArgumentException("Invalid value \"" + value
                    + "\" supplied for serverManagedPropertiesMode (expecting \"strict\" or \"relaxed\")!");
        }
    }
}
