/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import io.ocfl.api.MutableOcflRepository;
import org.apache.commons.io.FileUtils;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.RepositoryInitializationStatus;
import org.fcrepo.persistence.ocfl.RepositoryInitializer;
import org.flywaydb.core.Flyway;
import org.springframework.test.context.TestContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
        final var initializer = getBean(testContext, RepositoryInitializer.class);
        final var status = getBean(testContext, RepositoryInitializationStatus.class);

        // must wait for the initialization to finish
        int i = 0;
        while (!status.isInitializationComplete()) {
            if (++i > 6000) {
                throw new RuntimeException("Repository failed to initialize");
            }
            TimeUnit.MILLISECONDS.sleep(10);
        }

        flyway.clean();
        flyway.migrate();

        cleanDb(testContext);

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

        initializer.initialize();
    }
}
