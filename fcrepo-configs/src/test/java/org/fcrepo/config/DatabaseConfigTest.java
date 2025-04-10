/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.zaxxer.hikari.HikariDataSource;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * @author bbpennel
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {DatabaseConfigTest.TestConfig.class})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class})
public class DatabaseConfigTest {

    @Configuration
    static class TestConfig {
        @Bean
        public FedoraPropsConfig fedoraPropsConfig() {
            return new FedoraPropsConfig();
        }

        @Bean
        public DatabaseConfig databaseConfig() {
            return new DatabaseConfig();
        }

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
    }

    @Autowired
    private DatabaseConfig databaseConfig;

    @TempDir
    public Path tmpDir;

    @BeforeEach
    @FlywayTest
    public void setUp() {
        setField(databaseConfig, "dbUrl", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        setField(databaseConfig, "flywayCleanDisabled", false);
        setField(databaseConfig, "customDbProperties", null);
    }

    @Test
    public void testDataSource() throws Exception {
        final DataSource dataSource = databaseConfig.dataSource();
        assertNotNull(dataSource);
        assertTrue(dataSource instanceof HikariDataSource);
        
        final HikariDataSource hikariDs = (HikariDataSource) dataSource;
        assertEquals("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", hikariDs.getJdbcUrl());
        assertEquals("org.h2.Driver", hikariDs.getDriverClassName());
    }
    
    @Test
    public void testTxManager() throws Exception {
        final DataSource dataSource = databaseConfig.dataSource();
        final PlatformTransactionManager txManager = databaseConfig.txManager(dataSource);
        assertNotNull(txManager);
    }
    
    @Test
    public void testTxTemplate() throws Exception {
        final DataSource dataSource = databaseConfig.dataSource();
        final PlatformTransactionManager txManager = databaseConfig.txManager(dataSource);
        final TransactionTemplate txTemplate = databaseConfig.txTemplate(txManager);
        assertNotNull(txTemplate);
    }
    
    @Test
    public void testFlyway() throws Exception {
        final DataSource dataSource = databaseConfig.dataSource();
        final Flyway flyway = databaseConfig.flyway(dataSource);
        assertNotNull(flyway);
    }

    @Test
    public void testDataSourceInvalidDbType() {
        setField(databaseConfig, "dbUrl", "failtime");
        assertThrows(IllegalArgumentException.class, () -> databaseConfig.dataSource());
    }

    @Test
    public void testDataSourceDriverNotFound() {
        setField(databaseConfig, "dbUrl", "oh:no");
        assertThrows(IllegalStateException.class, () -> databaseConfig.dataSource());
    }

    @Test
    public void testDataSourceWithCustomDbProperties() throws Exception {
        final var propertiesPath = tmpDir.resolve("db.properties");

        // Write basic properties to the file
        Properties props = new Properties();
        props.setProperty("driverClassName", "org.h2.Driver");
        props.setProperty("maximumPoolSize", "15");
        props.setProperty("connectionTimeout", "4000");

        try (final OutputStream out = Files.newOutputStream(propertiesPath)) {
            props.store(out, "Test hikari properties");
        }

        // Set the custom properties file path
        setField(databaseConfig, "customDbProperties", propertiesPath.toString());

        // Test the datasource creation with custom properties file
        final DataSource dataSource = databaseConfig.dataSource();
        assertNotNull(dataSource);
        assertTrue(dataSource instanceof HikariDataSource);

        final HikariDataSource hikariDs = (HikariDataSource) dataSource;
        assertEquals("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", hikariDs.getJdbcUrl());
        assertEquals("org.h2.Driver", hikariDs.getDriverClassName());
        assertEquals(15, hikariDs.getMaximumPoolSize());
        assertEquals(4000, hikariDs.getConnectionTimeout());
    }
    
    @Test
    public void testCustomPropertiesConfiguration() throws Exception {
        setField(databaseConfig, "maxPoolSize", 20);
        setField(databaseConfig, "checkoutTimeout", 5000);
        
        final DataSource dataSource = databaseConfig.dataSource();
        assertNotNull(dataSource);
        assertTrue(dataSource instanceof HikariDataSource);
        
        final HikariDataSource hikariDs = (HikariDataSource) dataSource;
        assertEquals(20, hikariDs.getMaximumPoolSize());
        assertEquals(5000, hikariDs.getConnectionTimeout());
    }
}