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
package org.fcrepo.integration.auth.webac;

import edu.wisc.library.ocfl.api.MutableOcflRepository;
import org.fcrepo.http.commons.test.util.ContainerWrapper;
import org.fcrepo.persistence.ocfl.RepositoryInitializer;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.util.concurrent.TimeUnit;

/**
 * Listener that baselines the DB and OCFL repo between every test.
 *
 * @author pwinckles
 */
public class TestIsolationExecutionListener extends AbstractTestExecutionListener {

    private static final int ATTEMPTS = 5;

    @Override
    public void beforeTestMethod(final TestContext testContext) throws InterruptedException {
        final var ocflRepo = getBean(testContext, MutableOcflRepository.class);
        final var initializer = getBean(testContext, RepositoryInitializer.class);

        ocflRepo.listObjectIds().forEach(ocflRepo::purgeObject);

        // Retry initialize if it fails
        for (int i = 0; i < ATTEMPTS; i++) {
            try {
                initializer.initialize();
                break;
            } catch (RuntimeException e) {
                // Windows queues files for deletion and returns before they are deleted.
                // This sometimes creates a problem when the root OCFL object is deleted and then recreated here,
                // because an exception is thrown it attempts to create the object directories while they are
                // still pending deletion.

                if (i + 1 != ATTEMPTS) {
                    TimeUnit.MILLISECONDS.sleep(10);
                } else {
                    throw e;
                }
            }
        }
    }

    private <T> T getBean(final TestContext testContext, final Class<T> clazz) {
        final var containerWrapper = testContext.getApplicationContext()
                .getBean(ContainerWrapper.class);
        return containerWrapper.getSpringAppContext().getBean(clazz);
    }

}
