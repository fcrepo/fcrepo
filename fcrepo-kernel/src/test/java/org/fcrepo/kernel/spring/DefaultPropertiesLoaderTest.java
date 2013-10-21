/**
 * Copyright 2013 DuraSpace, Inc.
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
package org.fcrepo.kernel.spring;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrew Woods
 *         Date: 10/20/13
 */
public class DefaultPropertiesLoaderTest {

    private DefaultPropertiesLoader loader;

    private static final String PROP_FLAG = "integration-test";
    private static final String PROP_TEST = "fcrepo.ispn.repo.CacheDirPath";

    @Before
    public void setUp() throws Exception {
        loader = new DefaultPropertiesLoader();
    }

    @After
    public void tearDown() throws Exception {
        System.clearProperty(PROP_FLAG);
        System.clearProperty(PROP_TEST);
    }

    @Test
    public void testLoadSystemProperties() throws Exception {
        System.setProperty(PROP_FLAG, "true");

        loader.loadSystemProperties();
        Assert.assertNotNull(System.getProperty(PROP_FLAG));
        Assert.assertNull(System.getProperty(PROP_TEST));
    }

    @Test
    public void testLoadSystemPropertiesProduction() throws Exception {
        loader.loadSystemProperties();
        Assert.assertNull(System.getProperty(PROP_FLAG));
        Assert.assertNotNull(System.getProperty(PROP_TEST));
    }
}
