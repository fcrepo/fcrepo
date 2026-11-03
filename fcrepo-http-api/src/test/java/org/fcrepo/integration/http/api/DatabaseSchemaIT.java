/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration tests for database schema created by Flyway.
 * @author bbpennel
 */
public class DatabaseSchemaIT extends AbstractResourceIT {

    @Test
    public void testPostgresqlContainmentLikeIndex() throws Exception {
        final var dataSource = getBean(DataSource.class);
        try (final var connection = dataSource.getConnection()) {
            assumeTrue("PostgreSQL".equals(connection.getMetaData().getDatabaseProductName()));
        }

        final var jdbcTemplate = new JdbcTemplate(dataSource);
        final Boolean nonCCollation = jdbcTemplate.queryForObject(
                "SELECT datcollate <> 'C' FROM pg_database WHERE datname = current_database()", Boolean.class);
        final Boolean indexExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_indexes WHERE tablename = 'containment' " +
                "AND indexname = 'containment_idx5')", Boolean.class);

        assertEquals(nonCCollation, indexExists);
    }
}
