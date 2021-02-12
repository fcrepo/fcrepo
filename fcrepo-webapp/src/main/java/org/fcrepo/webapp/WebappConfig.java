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

package org.fcrepo.webapp;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.http.api.ExternalContentHandlerFactory;
import org.fcrepo.http.api.ExternalContentPathValidator;
import org.fcrepo.kernel.api.rdf.RdfNamespaceRegistry;

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.google.common.eventbus.EventBus;

/**
 * Spring config for the webapp
 *
 * @author pwinckles
 */
@Configuration
public class WebappConfig {

    /**
     * Task scheduler used for cleaning up transactions
     *
     * @return scheduler
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        final var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ScheduledTask");
        return scheduler;
    }

    /**
     * HTTP connection manager
     *
     * @return connection manager
     */
    @Bean
    public HttpClientConnectionManager connectionManager() {
        return new PoolingHttpClientConnectionManager();
    }

    /**
     * Fedora's lightweight internal event bus. Currently memory-resident.
     *
     * @return event bus
     */
    @Bean
    public EventBus eventBus() {
        // TODO this should be an async event bus: https://jira.lyrasis.org/browse/FCREPO-3587
        return new EventBus();
    }

    /**
     * Configuration of namespace prefixes
     *
     * @param propsConfig config properties
     * @return rdf namespace registry
     */
    @Bean(initMethod = "init", destroyMethod = "shutdown")
    public RdfNamespaceRegistry rdfNamespaceRegistry(final FedoraPropsConfig propsConfig) {
        final var registry = new RdfNamespaceRegistry();
        registry.setConfigPath(propsConfig.getNamespaceRegistry());
        registry.setMonitorForChanges(true);
        return registry;
    }

    /**
     * External content configuration
     *
     * @param propsConfig config properties
     * @return external content path validator
     */
    @Bean(initMethod = "init", destroyMethod = "shutdown")
    public ExternalContentPathValidator externalContentPathValidator(final FedoraPropsConfig propsConfig) {
        final var validator = new ExternalContentPathValidator();
        validator.setConfigPath(propsConfig.getExternalContentAllowed());
        validator.setMonitorForChanges(true);
        return validator;
    }

    @Bean
    public ExternalContentHandlerFactory externalContentHandlerFactory(final ExternalContentPathValidator validator) {
        final var factory = new ExternalContentHandlerFactory();
        factory.setValidator(validator);
        return factory;
    }

}
