/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.webapp;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.servlet.Filter;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.http.api.ExternalContentHandlerFactory;
import org.fcrepo.http.api.ExternalContentPathValidator;
import org.fcrepo.kernel.api.auth.ACLHandle;
import org.fcrepo.kernel.api.rdf.RdfNamespaceRegistry;

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.fcrepo.persistence.ocfl.RepositoryInitializationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.scheduling.annotation.EnableAsync;
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
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@EnableAsync
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
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static EventBus eventBus(final FedoraPropsConfig propsConfig) {
        return new AsyncEventBus(eventBusExecutor(propsConfig));
    }

    /**
     * @param propsConfig config
     * @return executor intended to be used by the Guava event bus
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static ExecutorService eventBusExecutor(final FedoraPropsConfig propsConfig) {
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
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public Cache<String, Optional<ACLHandle>> authHandleCache() {
        return Caffeine.newBuilder().weakValues()
                .expireAfterAccess(fedoraPropsConfig.getWebacCacheTimeout(), TimeUnit.MINUTES)
                .maximumSize(fedoraPropsConfig.getWebacCacheSize()).build();
    }

    /**
     * Filter to prevent http requests during repo init
     *
     * @return the filter
     */
    @Bean
    public Filter repositoryInitializationFilter() {
        return new RepositoryInitializationFilter();
    }

}
