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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.persistence.ocfl.RepositoryInitializer;

import org.apache.commons.io.FileUtils;
import org.flywaydb.core.Flyway;
import org.springframework.test.context.TestContext;

import edu.wisc.library.ocfl.api.MutableOcflRepository;

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
        final var ocflConfig = getBean(testContext, OcflPropsConfig.class);
        final var flyway = getBean(testContext, Flyway.class);

        flyway.clean();
        flyway.migrate();

        final var hasError = new AtomicBoolean(false);

        ocflRepo.listObjectIds().forEach(object -> {
            try {
                ocflRepo.purgeObject(object);
            } catch (final RuntimeException e) {
                // Recursive deletes don't behave well on Windows and it's possible for the above to error out.
                hasError.set(true);
            }
        });

        if (hasError.get()) {
            // If one of the purge operations failed, attempt to nuke everything. Maybe it'll work second time round?
            // We still need the purgeObject calls first so that objects are removed from the ocfl-java cache.
            try (final var files = Files.list(ocflConfig.getOcflRepoRoot())) {
                files.filter(Files::isDirectory).forEach(childDir -> {
                    try {
                        FileUtils.cleanDirectory(childDir.toFile());
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        }

        final var initializer = getBean(testContext, RepositoryInitializer.class);
        initializer.initialize();
    }
}
