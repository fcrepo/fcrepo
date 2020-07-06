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

import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.http.commons.test.util.ContainerWrapper;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import javax.sql.DataSource;

/**
 * Isolate RebuildIT from the rest of the IT contexts.
 *
 * @author pwinckles
 */
public class RebuildTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public void beforeTestClass(final TestContext testContext) throws Exception {
        cleanDb(testContext);
        System.setProperty(OcflPropsConfig.FCREPO_OCFL_ROOT, "target/test-classes/test-rebuild-ocfl/ocfl-root");
        testContext.markApplicationContextDirty(HierarchyMode.EXHAUSTIVE);
    }

    @Override
    public void afterTestClass(final TestContext testContext) throws Exception {
        cleanDb(testContext);
        System.clearProperty(OcflPropsConfig.FCREPO_OCFL_ROOT);
        testContext.markApplicationContextDirty(HierarchyMode.EXHAUSTIVE);
    }

    private void cleanDb(final TestContext testContext) throws Exception {
        final var dataSource = getBean(testContext, DataSource.class);

        try (var conn = dataSource.getConnection()) {
            try (var queryStmt = conn.prepareStatement(
                    "SELECT DISTINCT table_name FROM information_schema.tables WHERE table_schema = 'PUBLIC'")) {
                try (var resultSet = queryStmt.executeQuery()) {
                    while (resultSet.next()) {
                        try (var truncStmt = conn.prepareStatement(
                                "TRUNCATE TABLE " + resultSet.getString(1))) {
                            truncStmt.executeUpdate();
                        }
                    }
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
