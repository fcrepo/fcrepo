/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.common.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Detects the database platform from a datasource.
 *
 * @author pwinckles
 * @since 6.0.0
 */
public enum DbPlatform {

    POSTGRESQL("PostgreSQL"),
    H2("H2"),
    MYSQL("MySQL"),
    MARIADB("MariaDB");

    private static final Logger LOGGER = LoggerFactory.getLogger(DbPlatform.class);

    private final String name;

    DbPlatform(final String name) {
        this.name = name;
    }

    public static DbPlatform fromDataSource(final DataSource dataSource) {
        try (final var connection = dataSource.getConnection()) {
            final var name = connection.getMetaData().getDatabaseProductName();
            LOGGER.debug("Identified database as: {}", name);
            return fromString(name);
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static DbPlatform fromString(final String name) {
        for (final var platform : values()) {
            if (platform.name.equals(name)) {
                return platform;
            }
        }
        throw new IllegalArgumentException("Unknown database platform: " + name);
    }

    public String getName() {
        return name;
    }
}
