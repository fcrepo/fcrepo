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

import org.springframework.core.Ordered;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * TestExecutionListener which marks the context as dirty before and after the test class.
 * Based on example from https://stackoverflow.com/questions/39277040/
 *
 * @author bbpennel
 */
public class DirtyContextBeforeAndAfterClassTestExecutionListener
        extends AbstractTestExecutionListener {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void beforeTestClass(final TestContext testContext) {
        testContext.markApplicationContextDirty(HierarchyMode.EXHAUSTIVE);
    }

    @Override
    public void afterTestClass(final TestContext testContext) {
        testContext.markApplicationContextDirty(HierarchyMode.EXHAUSTIVE);
    }

}
