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
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import javax.sql.DataSource;

/**
 * Clears out the h2 db so that it doesn't affect other tests
 *
 * @author pwinckles
 */
public class ClearDbTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public void afterTestClass(final TestContext testContext) throws Exception {
        final var containerWrapper = testContext.getApplicationContext()
                .getBean(ContainerWrapper.class);
        final var dataSource = containerWrapper.getSpringAppContext().getBean(DataSource.class);

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
}
