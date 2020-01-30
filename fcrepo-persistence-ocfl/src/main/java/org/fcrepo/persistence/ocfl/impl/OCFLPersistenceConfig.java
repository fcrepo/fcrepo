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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.fcrepo.persistence.ocfl.impl.OCFLConstants.OCFL_STORAGE_ROOT_DIR;
import static org.fcrepo.persistence.ocfl.impl.OCFLConstants.OCFL_WORK_DIR;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.createRepository;

/**
 * A Configuration for OCFL dependencies
 *
 * @author dbernstein
 * @since 6.0.0
 */

@Configuration
public class OCFLPersistenceConfig {

    private static final Logger log = LoggerFactory.getLogger(OCFLPersistenceConfig.class);

    /**
     * Create an OCFL Repository
     * @return the repository
     */
    @Bean
    public MutableOcflRepository repository() {
        return createRepository(OCFL_STORAGE_ROOT_DIR, OCFL_WORK_DIR);
    }
}