/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.common.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Dan Field
 */
@ExtendWith(MockitoExtension.class)
public class DbPlatformTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Test
    public void testFromString() {
        assertSame(DbPlatform.POSTGRESQL, DbPlatform.fromString("PostgreSQL"));
        assertSame(DbPlatform.H2, DbPlatform.fromString("H2"));
        assertSame(DbPlatform.MYSQL, DbPlatform.fromString("MySQL"));
        assertSame(DbPlatform.MARIADB, DbPlatform.fromString("MariaDB"));
    }

    @Test
    public void testFromStringRejectsUnknownPlatform() {
        final var e = assertThrows(IllegalArgumentException.class, () -> DbPlatform.fromString("Oracle"));
        assertEquals("Unknown database platform: Oracle", e.getMessage());
    }

    @Test
    public void testGetName() {
        assertEquals("PostgreSQL", DbPlatform.POSTGRESQL.getName());
    }

    @Test
    public void testFromDataSourceClosesConnection() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        assertSame(DbPlatform.POSTGRESQL, DbPlatform.fromDataSource(dataSource));
        verify(connection).close();
    }

    @Test
    public void testFromDataSourceWrapsSqlException() throws SQLException {
        final var sqlException = new SQLException("no connection");
        when(dataSource.getConnection()).thenThrow(sqlException);

        final var e = assertThrows(RuntimeException.class, () -> DbPlatform.fromDataSource(dataSource));
        assertSame(sqlException, e.getCause());
    }
}
