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
package org.fcrepo.persistence.ocfl.impl;

import edu.wisc.library.ocfl.api.MutableOcflRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import java.io.File;

import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.createRepository;

/**
 * A Configuration for OCFL dependencies
 *
 * @author dbernstein
 * @since 6.0.0
 */

@Configuration
public class OCFLPersistenceConfig {

    /**
     * Create an OCFL Repository
     * @return the repository
     */
    @Bean
    public MutableOcflRepository repository() {
        final OCFLConstants constants = new OCFLConstants();
        return createRepository(constants.getStorageRootDir(), constants.getWorkDir());
    }

    @Bean
    public OCFLConstants ocflConstants(){
        return new OCFLConstants();
    }

    @Bean
    public DataSource dataSource(final OCFLConstants ocflConstants) {
        final var dataSource = new DriverManagerDataSource();
        final var workDir = ocflConstants.getWorkDir().getAbsolutePath();
        dataSource.setUrl("jdbc:h2:" + workDir + File.separator + "containment.idx;FILE_LOCK=SOCKET");
        dataSource.setDriverClassName("org.h2.jdbcx.JdbcDataSource");
        return dataSource;
    }

    @Bean
    public DataSourceTransactionManager txManager(final DataSource dataSource) {
        final var txManager = new DataSourceTransactionManager();
        txManager.setDataSource(dataSource);
        return txManager;
    }
}