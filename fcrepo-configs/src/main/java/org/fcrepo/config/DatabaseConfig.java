/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.config;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;

/**
 * @author pwinckles
 */
@EnableTransactionManagement
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class DatabaseConfig extends BasePropsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConfig.class);

    private static final String H2_FILE = "fcrepo-h2";

    @Value("${flyway.cleanDisabled:true}")
    private boolean flywayCleanDisabled;

    @Value("${fcrepo.db.url:#{'jdbc:h2:'" +
            " + fedoraPropsConfig.fedoraData.resolve('" + H2_FILE + "').toAbsolutePath().toString()" +
            " + ';FILE_LOCK=SOCKET'}}")
    private String dbUrl;

    @Value("${fcrepo.db.user:}")
    private String dbUser;

    @Value("${fcrepo.db.password:}")
    private String dbPassword;

    @Value("${fcrepo.db.max.pool.size:#{null}}")
    private Integer maxPoolSize;

    @Value("${fcrepo.db.connection.checkout.timeout:#{null}}")
    private Integer checkoutTimeout;

    @Value("${fcrepo.db.custom.properties:#{null}}")
    private String customDbProperties;

    private static final Map<String, String> DB_DRIVER_MAP = Map.of(
            "h2", "org.h2.Driver",
            "postgresql", "org.postgresql.Driver",
            "mariadb", "org.mariadb.jdbc.Driver",
            "mysql", "com.mysql.cj.jdbc.Driver"
    );

    @PostConstruct
    public void setup() {
        ((ConverterRegistry) DefaultConversionService.getSharedInstance())
                // Adds a converter for mapping local datetimes to instants. This is dubious and not supported
                // by default because you must make an assumption about the timezone
                .addConverter(new Converter<LocalDateTime, Instant>() {
                    @Override
                    public Instant convert(final LocalDateTime source) {
                        return source.toInstant(ZoneOffset.UTC);
                    }
                });

    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public DataSource dataSource(final MeterRegistry registry) throws Exception {
        final var driver = identifyDbDriver();

        LOGGER.info("JDBC URL: {}", dbUrl);
        LOGGER.info("JDBC User: {}", dbUser);
        LOGGER.info("JDBC Password length: {}", dbPassword == null ? 0 : dbPassword.length());
        LOGGER.info("Using database driver: {}", driver);

        final HikariConfig config;
        if (customDbProperties != null) {
            config = new HikariConfig(customDbProperties);
            LOGGER.info("Using an external configuration file for Hikari: {}", customDbProperties);
        } else {
            config = new HikariConfig();
        }
        if (checkoutTimeout != null) {
            if (config.getConnectionTimeout() != 30000) {
                LOGGER.warn(
                        "Overriding HikariCP connectionTimeout setting in file from {} to {} ",
                        config.getConnectionTimeout(),
                        checkoutTimeout
                );
            }
            config.setConnectionTimeout(checkoutTimeout);
        }
        if (maxPoolSize != null) {
            if (config.getMaximumPoolSize() != 10) {
                LOGGER.warn(
                        "Overriding HikariCP maximumPoolSize setting in file from {} to {} ",
                        config.getMaximumPoolSize(),
                        maxPoolSize
                );
            }
            config.setMaximumPoolSize(maxPoolSize);
        }
        if (driver.equalsIgnoreCase("mariadb")) {
            config.addDataSourceProperty("useServerPrepStmts", "false");
        }
        config.setDriverClassName(driver);
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        if (registry instanceof PrometheusMeterRegistry) {
            config.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(registry));
        }

        final var dataSource = new HikariDataSource(config);

        flyway(dataSource);

        return dataSource;
    }

    /**
     * Get the database type in use
     * @return database type from the connect url.
     */
    private String getDbType() {
        final var parts = dbUrl.split(":");

        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid DB url: " + dbUrl);
        }
        return parts[1].toLowerCase();
    }

    private String identifyDbDriver() {
        final var driver = DB_DRIVER_MAP.get(getDbType());

        if (driver == null) {
            throw new IllegalStateException("No database driver found for: " + dbUrl);
        }

        return driver;
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static DataSourceTransactionManager txManager(final DataSource dataSource) {
        final var txManager = new DataSourceTransactionManager();
        txManager.setDataSource(dataSource);
        return txManager;
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static TransactionTemplate txTemplate(final PlatformTransactionManager txManager) {
        final var txDefinition = new DefaultTransactionDefinition();
        txDefinition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return new TransactionTemplate(txManager, txDefinition);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public Flyway flyway(final DataSource source) throws Exception {
        LOGGER.debug("Instantiating a new flyway bean");
        return FlywayFactory.create().setDataSource(source).setDatabaseType(getDbType())
                .setCleanDisabled(flywayCleanDisabled).getObject();
    }

}
