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
package org.fcrepo.integration.http.api;

import edu.wisc.library.ocfl.api.MutableOcflRepository;
import org.fcrepo.persistence.ocfl.RepositoryInitializer;
import org.springframework.test.context.TestContext;

/**
 * Listener that baselines the DB and OCFL repo between every test.
 * It does not baseline on Windows due to difficulties associated with deleting and immediately recreating directories.
 *
 * @author pwinckles
 */
public class TestIsolationExecutionListener extends BaseTestExecutionListener {

    @Override
    public void beforeTestMethod(final TestContext testContext) throws Exception {
        final var ocflRepo = getBean(testContext, MutableOcflRepository.class);
        ocflRepo.listObjectIds().forEach(ocflRepo::purgeObject);
        final var initializer = getBean(testContext, RepositoryInitializer.class);
        initializer.initialize();
    }
}
