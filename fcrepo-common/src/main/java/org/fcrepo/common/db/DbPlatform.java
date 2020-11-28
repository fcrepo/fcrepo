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
