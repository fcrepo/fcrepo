/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
