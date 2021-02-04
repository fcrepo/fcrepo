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
