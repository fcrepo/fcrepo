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

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.http.api.ExternalContentHandlerFactory;
import org.fcrepo.http.api.ExternalContentPathValidator;
import org.fcrepo.kernel.api.auth.ACLHandle;
import org.fcrepo.kernel.api.rdf.RdfNamespaceRegistry;

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

/**
 * Spring config for the webapp
 *
 * @author pwinckles
 */
@Configuration
@EnableScheduling
public class WebappConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebappConfig.class);

    @Inject
    private FedoraPropsConfig fedoraPropsConfig;

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
     * @param propsConfig config
     * @return event bus
     */
    @Bean
    public EventBus eventBus(final FedoraPropsConfig propsConfig) {
        return new AsyncEventBus(eventBusExecutor(propsConfig));
    }

    /**
     * @param propsConfig config
     * @return executor intended to be used by the Guava event bus
     */
    @Bean
    public ExecutorService eventBusExecutor(final FedoraPropsConfig propsConfig) {
        LOGGER.debug("Event bus threads: {}", propsConfig);
        return Executors.newFixedThreadPool(propsConfig.getEventBusThreads());
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

    /**
     * Used to cache the effective ACL location and authorizations for a given resource.
     *
     * @return the cache
     */
    @Bean
    public Cache<String, Optional<ACLHandle>> authHandleCache() {
        return Caffeine.newBuilder().weakValues()
                .expireAfterAccess(fedoraPropsConfig.getWebacCacheTimeout(), TimeUnit.MINUTES)
                .maximumSize(fedoraPropsConfig.getWebacCacheSize()).build();
    }
}
