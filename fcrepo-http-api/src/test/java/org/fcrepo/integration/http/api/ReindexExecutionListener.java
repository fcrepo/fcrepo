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

import org.apache.commons.io.FileUtils;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.TestContext;

import java.nio.file.Path;

/**
 * Isolate ReindexITs from the rest of the IT contexts as well as tests within ReindexIT.
 *
 * @author dbernstein
 */
public class ReindexExecutionListener extends BaseTestExecutionListener {

    @Override
    public void afterTestMethod(final TestContext testContext) throws Exception {
        FileUtils.deleteDirectory(Path.of("target/fcrepo-home/data/ocfl-root").toFile());
        cleanDb(testContext);
        testContext.markApplicationContextDirty(HierarchyMode.EXHAUSTIVE);
    }
}
