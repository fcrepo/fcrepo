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
package org.fcrepo.integration.http.api;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;

/**
 * @author Andrew Woods
 *         Date: Aug 31, 2013
 */
// TODO: Reinstate this test with the fix of: https://www.pivotaltracker.com/story/show/57997564
@Ignore
public class FedoraBackupFSIT extends FedoraBackupIT {

    private static final String CONFIG_FILE = "fcrepo.modeshape.configuration";

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(CONFIG_FILE, "test_repository_fs.json");
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty(CONFIG_FILE);
    }

}

