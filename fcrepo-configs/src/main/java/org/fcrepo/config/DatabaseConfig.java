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

package org.fcrepo.config;

import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * @author pwinckles
 */
@EnableTransactionManagement
@Configuration
public class DatabaseConfig extends BasePropsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConfig.class);

    private static final String H2_FILE = "fcrepo-h2";

    @Value("${fcrepo.db.url:#{'jdbc:h2:'" +
            " + fedoraPropsConfig.fedoraData.resolve('" + H2_FILE + "').toAbsolutePath().toString()" +
            " + ';FILE_LOCK=SOCKET'}}")
    private String dbUrl;

    @Value("${fcrepo.db.user:}")
    private String dbUser;

    @Value("${fcrepo.db.password:}")
    private String dbPassword;

    @Value("${fcrepo.db.max.pool.size:15}")
    private Integer maxPoolSize;

    @Value("${fcrepo.db.connection.checkout.timeout:10000}")
    private Integer checkoutTimeout;

    @Value("${fcrepo.db.connection.idle.test.period:300}")
    private Integer idleConnectionTestPeriod;

    @Value("${fcrepo.db.connection.test.on.checkout:true}")
    private boolean testConnectionOnCheckout;

    private static final Map<String, String> DB_DRIVER_MAP = Map.of(
            "h2", "org.h2.Driver",
            "postgresql", "org.postgresql.Driver",
            "mariadb", "org.mariadb.jdbc.Driver",
            "mysql", "com.mysql.cj.jdbc.Driver"
    );

    @Bean
    public DataSource dataSource() throws Exception {
        final var driver = identifyDbDriver();

        LOGGER.debug("JDBC URL: {}", dbUrl);
        LOGGER.debug("Using database driver: {}", driver);

        final var dataSource = new ComboPooledDataSource();
        dataSource.setDriverClass(driver);
        dataSource.setJdbcUrl(dbUrl);
        dataSource.setUser(dbUser);
        dataSource.setPassword(dbPassword);
        dataSource.setCheckoutTimeout(checkoutTimeout);
        dataSource.setMaxPoolSize(maxPoolSize);
        dataSource.setIdleConnectionTestPeriod(idleConnectionTestPeriod);
        dataSource.setTestConnectionOnCheckout(testConnectionOnCheckout);

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
    public DataSourceTransactionManager txManager(final DataSource dataSource) {
        final var txManager = new DataSourceTransactionManager();
        txManager.setDataSource(dataSource);
        return txManager;
    }

    @Bean
    public Flyway flyway(final DataSource source) throws Exception {
        LOGGER.debug("Instantiating a new flyway bean");
        return FlywayFactory.create().setDataSource(source).setDatabaseType(getDbType()).getObject();
    }

}
