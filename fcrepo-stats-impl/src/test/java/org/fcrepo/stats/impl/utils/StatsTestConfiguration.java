/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.impl.utils;

import org.fcrepo.config.FlywayFactory;
import org.fcrepo.stats.api.RepositoryStats;
import org.fcrepo.stats.impl.DbRepositoryStatsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * @author bbpennel
 */
public class StatsTestConfiguration {
    @Bean
    public DriverManagerDataSource dataSource() {
        final var dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:index;DB_CLOSE_DELAY=-1");
        return dataSource;
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
    @Autowired
    public RepositoryStats repositoryStats() {
        return new DbRepositoryStatsImpl();
    }
}
