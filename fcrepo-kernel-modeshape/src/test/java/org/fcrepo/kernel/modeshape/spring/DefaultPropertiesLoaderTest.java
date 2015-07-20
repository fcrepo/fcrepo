/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.modeshape.spring;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * @author Andrew Woods
 *         Date: 10/20/13
 */
public class DefaultPropertiesLoaderTest {

    private DefaultPropertiesLoader loader;

    private static final String PROP_FLAG = "integration-test";
    private static final String PROP_TEST = "fcrepo.ispn.repo.cache";
    private static final String HOME_PROP = "fcrepo.home";

    @Before
    public void setUp() {
        loader = new DefaultPropertiesLoader();
    }

    @After
    public void tearDown() {
        clearProps();
    }

    private static void clearProps() {
        System.clearProperty(PROP_FLAG);
        System.clearProperty(PROP_TEST);
        System.clearProperty(HOME_PROP);
    }

    @Test
    public void testLoadSystemProperties() {
        System.setProperty(PROP_FLAG, "true");

        loader.loadSystemProperties();
        Assert.assertNotNull(System.getProperty(PROP_FLAG));
        Assert.assertNull(System.getProperty(PROP_TEST));
    }

    @Test
    public void testLoadSystemPropertiesProduction() {
        loader.loadSystemProperties();
        Assert.assertNull(System.getProperty(PROP_FLAG));
        Assert.assertNotNull(System.getProperty(PROP_TEST));
    }

    @Test
    public void testContentIsInWorkingDir() {
       loader.loadSystemProperties();
       Assert.assertTrue("Default directories are within working directory.",
               containsPath(System.getProperty(PROP_TEST),
                       System.getProperty("user.dir")));
    }

    @Test
    public void testCustomHomeDirWithRelativeSubdirs() {
        System.setProperty(HOME_PROP, asTempPath("test"));
        System.setProperty(PROP_TEST, "sub");

        loader.loadSystemProperties();

        final File home = new File(asTempPath("test"));
        Assert.assertTrue("Relative subdirs are within fcrepo.home directory.",
                containsPath(System.getProperty(PROP_TEST),
                        home.getAbsolutePath()));
        clearProps();
    }

    @Test
    public void testCustomHomeDirWithAbsoluteSubdirs() {
        System.setProperty(HOME_PROP, asTempPath("test"));
        System.setProperty(PROP_TEST, asTempPath("sub"));

        loader.loadSystemProperties();

        final File home = new File(asTempPath("test"));
        Assert.assertFalse("Absolute subdirs are idependent of fcrepo.home.",
                containsPath(System.getProperty(PROP_TEST),
                        home.getAbsolutePath()));
        clearProps();
    }

    @Test
    public void relativePathsWorkForFedoraHome() {
        System.setProperty(HOME_PROP, "test");
        System.setProperty(PROP_TEST, "sub");

        loader.loadSystemProperties();

        Assert.assertEquals(new File(new File("test"), "sub"),
                new File(System.getProperty(PROP_TEST)));
        clearProps();
    }

    private static boolean containsPath(final String path, final String parentPath) {
        final File parent = new File(parentPath);
        for (File f = new File(path) ; f.getParentFile() != null ; f = f.getParentFile()) {
            if (f.getParentFile().equals(parent)) {
                return true;
            }
        }
        return false;
    }

    private static String asTempPath(final String file) {
       return System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + file;
    }
}
