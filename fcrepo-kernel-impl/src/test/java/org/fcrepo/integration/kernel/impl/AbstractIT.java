/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.integration.kernel.impl;

import static java.lang.Thread.currentThread;
import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author ajs6f
 * @since 2013
 */
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractIT {

    protected Logger logger;

    @Before
    public void setLogger() {
        logger = getLogger(this.getClass());
    }

    public String getGeneratedIdentifier(final String suffix) {
        final Thread currentThread = currentThread();
        final String threadName = currentThread.getName();
        final String methodName = currentThread.getStackTrace()[2].getMethodName();
        return methodName + "-" + threadName + "-" + randomUUID() + "-" + suffix;
    }

    public String getTestObjIdentifier() {
        return getGeneratedIdentifier("-object");
    }

    public String getTestDsIdentifier() {
        return getGeneratedIdentifier("-datastream");
    }

    public String getTestObjIdentifier(final String suffix) {
        return getGeneratedIdentifier("-object" + suffix);
    }

    public String getTestDsIdentifier(final String suffix) {
        return getGeneratedIdentifier("-datastream" + suffix);
    }

}
