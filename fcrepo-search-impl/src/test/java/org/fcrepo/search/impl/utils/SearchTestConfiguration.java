/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.search.impl.utils;

import javax.sql.DataSource;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.config.FlywayFactory;
import org.fcrepo.kernel.api.RepositoryInitializationStatus;
import org.fcrepo.kernel.api.cache.UserTypesCache;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.impl.ContainmentIndexImpl;
import org.fcrepo.kernel.impl.RepositoryInitializationStatusImpl;
import org.fcrepo.kernel.impl.cache.UserTypesCacheImpl;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.search.api.SearchIndex;
import org.fcrepo.search.impl.DbSearchIndexImpl;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Spring configuration for the search tests.
 *
 * @author whikloj
 */
public class SearchTestConfiguration {

    @Bean
    @DependsOn("dataSource")
    public DataSourceTransactionManager getTxMgr(final DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public DriverManagerDataSource dataSource() {
        final var dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:index;DB_CLOSE_DELAY=-1");
        return dataSource;
    }

    @Bean
    @DependsOn({"fedoraPropsConfig", "dataSource"})
    public ContainmentIndexImpl containmentIndex() {
        return new ContainmentIndexImpl();
    }

    @Bean
    @DependsOn("dataSource")
    public FlywayFactory flywayFactory(final DataSource dataSource) {
        final var flywayFactory = new FlywayFactory();
        flywayFactory.setDataSource(dataSource);
        flywayFactory.setDatabaseType("h2");
        flywayFactory.setCleanDisabled(false);
        return flywayFactory;
    }

    @Bean
    @DependsOn("fedoraPropsConfig")
    public UserTypesCache userTypesCache(final FedoraPropsConfig config) {
        return new UserTypesCacheImpl(config);
    }

    @Bean
    public ResourceFactory resourceFactory() {
        return Mockito.mock(ResourceFactory.class);
    }

    @Bean
    public PersistentStorageSessionManager persistentStorageSessionManager() {
        return Mockito.mock(PersistentStorageSessionManager.class);
    }

    @Bean
    public SearchIndex searchIndex() {
        return new DbSearchIndexImpl();
    }

    @Bean
    public FedoraPropsConfig fedoraPropsConfig() {
        return new FedoraPropsConfig();
    }

    @Bean
    public RepositoryInitializationStatus initializationStatus() {
        return new RepositoryInitializationStatusImpl();
    }
}
