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

import org.fcrepo.http.commons.test.util.ContainerWrapper;
import org.fcrepo.persistence.ocfl.impl.ReindexService;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * A base class for hosting methods shared by other TestExecutionListeners.
 *
 * @author pwinckles
 */
public class BaseTestExecutionListener extends AbstractTestExecutionListener {

    protected void cleanDb(final TestContext testContext) {
        final ReindexService reindexService = getBean(testContext, ReindexService.class);
        reindexService.reset();
    }

    protected <T> T getBean(final TestContext testContext, final String name) {
        final var containerWrapper = testContext.getApplicationContext()
                .getBean(ContainerWrapper.class);
        return (T) containerWrapper.getSpringAppContext().getBean(name);
    }

    protected <T> T getBean(final TestContext testContext, final Class<T> clazz) {
        final var containerWrapper = testContext.getApplicationContext()
                .getBean(ContainerWrapper.class);
        return containerWrapper.getSpringAppContext().getBean(clazz);
    }


}
