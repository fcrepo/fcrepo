/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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

    protected <T> T getBean(final TestContext testContext, final Class<T> clazz) {
        final var containerWrapper = testContext.getApplicationContext()
                .getBean(ContainerWrapper.class);
        return containerWrapper.getSpringAppContext().getBean(clazz);
    }


}
