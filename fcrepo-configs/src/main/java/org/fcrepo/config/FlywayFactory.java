/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.FactoryBean;

/**
 * Factory to generate a Flyway instance for Fedora.
 * @author whikloj
 */
public class FlywayFactory implements FactoryBean<Flyway> {

    private DataSource dataSource;

    private String databaseType;

    /**
     * Static constructor
     * @return a new FlywayFactory instance.
     */
    public static FlywayFactory create() {
        return new FlywayFactory();
    }

    @Override
    public Flyway getObject() throws Exception {
        if (dataSource == null) {
            throw new IllegalStateException("Cannot get flyway instance without a configured datasource.");
        }
        final var fly = Flyway.configure().dataSource(dataSource)
                .locations("classpath:sql/" + (databaseType == null ? "h2" : databaseType)).load();
        fly.migrate();
        return fly;
    }

    @Override
    public Class<?> getObjectType() {
        return Flyway.class;
    }

    /**
     * Set the datasource for use with Flyway.
     * @param source the data source.
     * @return this factory
     */
    public FlywayFactory setDataSource(final DataSource source) {
        dataSource = source;
        return this;
    }

    /**
     * Set the type of database to pick the correct schema files.
     * @param type database type
     * @return this factory
     */
    public FlywayFactory setDatabaseType(final String type) {
        databaseType = type;
        return this;
    }
}
