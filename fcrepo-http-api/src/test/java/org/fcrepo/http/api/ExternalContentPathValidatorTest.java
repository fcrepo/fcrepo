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
package org.fcrepo.http.api;

import static java.nio.file.StandardOpenOption.APPEND;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.fcrepo.kernel.api.exception.ExternalMessageBodyException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author bbpennel
 */
public class ExternalContentPathValidatorTest {

    private ExternalContentPathValidator validator;

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private File dataDir;

    private String dataUri;

    private File allowListFile;

    private File goodFile;

    private String goodFileUri;

    @Before
    public void init() throws Exception {
        allowListFile = tmpDir.newFile();

        validator = new ExternalContentPathValidator();
        validator.setConfigPath(allowListFile.getAbsolutePath());

        dataDir = tmpDir.newFolder();
        dataUri = dataDir.toURI().toString();

        goodFile = new File(dataDir, "file.txt");
        goodFile.createNewFile();
        goodFileUri = goodFile.toURI().toString();
    }

    @After
    public void after() {
        validator.shutdown();
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testValidateWithNoAllowList() throws Exception {
        validator.setConfigPath(null);
        validator.init();

        validator.validate(goodFileUri);
    }

    @Test
    public void testValidFileUri() throws Exception {
        addAllowedPath(dataUri);

        validator.validate(goodFileUri);
    }

    @Test
    public void testValidHttpUri() throws Exception {
        final String goodPath = "http://example.com/";
        final String extPath = goodPath + "file.txt";

        addAllowedPath(goodPath);

        validator.validate(extPath);
    }

    @Test
    public void testExactFileUri() throws Exception {
        addAllowedPath(goodFileUri);

        validator.validate(goodFileUri);
    }

    @Test
    public void testMultipleMatches() throws Exception {
        new File(dataDir, "file.txt").createNewFile();
        final String extPath = goodFileUri;
        final String anotherPath = tmpDir.getRoot().toURI().toString();

        addAllowedPath(anotherPath);
        addAllowedPath(dataUri);

        validator.validate(extPath);
    }

    @Test
    public void testMultipleSchemes() throws Exception {
        final String httpPath = "http://example.com/";

        addAllowedPath(httpPath);
        addAllowedPath(dataUri);

        validator.validate(goodFileUri);
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testInvalidFileUri() throws Exception {
        final String extPath = tmpDir.newFile("file.txt").toURI().toString();

        addAllowedPath(dataUri);

        validator.validate(extPath);
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testMalformedUri() throws Exception {
        final String goodPath = "http://example.com/";
        final String extPath = ".bad://example.com/file.txt";

        addAllowedPath(goodPath);

        validator.validate(extPath);
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testNonabsoluteUri() throws Exception {
        final String goodPath = "http://example.com/";
        final String extPath = "/example.com/file.txt";

        addAllowedPath(goodPath);

        validator.validate(extPath);
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testInvalidHttpUri() throws Exception {
        final String goodPath = "http://good.example.com/";
        final String extPath = "http://bad.example.com/file.txt";

        addAllowedPath(goodPath);

        validator.validate(extPath);
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testHttpUriMissingSlash() throws Exception {
        // Slash after domain is required
        final String goodPath = "http://good.example.com";
        final String extPath = "http://good.example.com:8080/offlimits";

        addAllowedPath(goodPath);

        validator.validate(extPath);
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testRelativeModifier() throws Exception {
        final String extPath = dataUri + "../sneaky.txt";

        addAllowedPath(dataUri);

        validator.validate(extPath);
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testNoScheme() throws Exception {
        final String extPath = dataDir.getAbsolutePath() + "file.txt";

        addAllowedPath(dataUri);

        validator.validate(extPath);
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testEmptyPath() throws Exception {
        addAllowedPath(dataUri);

        validator.validate("");
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testNullPath() throws Exception {
        addAllowedPath(dataUri);

        validator.validate(null);
    }

    @Test(expected = IOException.class)
    public void testListFileDoesNotExist() throws Exception {
        allowListFile.delete();

        validator.init();
    }

    @Test
    public void testAllowAny() throws Exception {
        addAllowedPath("file:///");
        addAllowedPath("http://");
        addAllowedPath("https://");

        final String path1 = goodFileUri;
        validator.validate(path1);
        final String path2 = "http://example.com/file";
        validator.validate(path2);
        final String path3 = "https://example.com/file";
        validator.validate(path3);
    }

    @Test
    public void testCaseInsensitiveFileScheme() throws Exception {
        final String goodPath = "FILE://" + dataDir.toURI().getPath();
        final String extPath = dataUri + "file.txt";

        addAllowedPath(goodPath);

        validator.validate(extPath);
    }

    @Test
    public void testVaryingSensitiveFilePath() throws Exception {
        final File subfolder = tmpDir.newFolder("CAPSLOCK");
        final File file = new File(subfolder, "OoOoOooOOooo.txt");
        file.createNewFile();

        final String goodPath = subfolder.toURI().toString();
        final String extPath = file.toURI().toString();

        addAllowedPath(goodPath);

        validator.validate(extPath);
    }

    @Test
    public void testVaryingCaseHttpUri() throws Exception {
        final String goodPath = "http://sensitive.example.com/PATH/to/";
        final String extPath = "http://sensitive.example.com/PATH/to/stuff";

        addAllowedPath(goodPath);

        validator.validate(extPath);
    }

    @Test
    public void testFileSlashes() throws Exception {
        final File oneFolder = tmpDir.newFolder("one");
        final File twoFolder = tmpDir.newFolder("two");
        final File threeFolder = tmpDir.newFolder("three");
        final File manyFolder = tmpDir.newFolder("toomany");
        new File(oneFolder, "file").createNewFile();
        new File(twoFolder, "file").createNewFile();
        new File(threeFolder, "file").createNewFile();
        new File(manyFolder, "file").createNewFile();

        addAllowedPath("file:" + oneFolder.toURI().getPath() + "/");
        addAllowedPath("file:/" + twoFolder.toURI().getPath() + "/");
        addAllowedPath("file://" + threeFolder.toURI().getPath() + "/");
        addAllowedPath("file:///" + manyFolder.toURI().getPath() + "/");

        validator.validate("file:" + oneFolder.toURI().getPath() + "/file");
        validator.validate("file:/" + oneFolder.toURI().getPath() + "/file");
        validator.validate("file://" + oneFolder.toURI().getPath() + "/file");

        validator.validate("file:" + twoFolder.toURI().getPath() + "/file");
        validator.validate("file:/" + twoFolder.toURI().getPath() + "/file");
        validator.validate("file://" + twoFolder.toURI().getPath() + "/file");

        validator.validate("file:" + threeFolder.toURI().getPath() + "/file");
        validator.validate("file:/" + threeFolder.toURI().getPath() + "/file");
        validator.validate("file://" + threeFolder.toURI().getPath() + "/file");

        try {
            validator.validate("file:" + manyFolder.toURI().getPath() + "file");
            fail();
        } catch (final ExternalMessageBodyException e) {
            // expected
        }
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testDisallowDirectoryUriWithoutSlash() throws Exception {
        final String allowDir = dataUri.substring(0, dataUri.length() - 1);
        addAllowedPath(allowDir);

        final String extPath = dataUri + "file.txt";
        validator.validate(extPath);
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testDisallowFileUriWithSlash() throws Exception {
        new File(dataDir, "file").createNewFile();
        final String extPath = dataUri + "file/";
        final String allowExactPath = extPath + "/";

        addAllowedPath(allowExactPath);

        validator.validate(extPath);
    }

    /*
     * Test ignored because it takes around 10+ seconds to poll for events on MacOS:
     * https://bugs.openjdk.java.net/browse/JDK-7133447 Can be enabled for one off testing
     */
    @Ignore("Test is ignored due to file event timing")
    @Test
    public void testDetectModification() throws Exception {
        validator.setMonitorForChanges(true);

        final String anotherPath = tmpDir.newFolder("other").toURI().toString();
        addAllowedPath(anotherPath);

        final String path = dataUri + "file";
        try {
            validator.validate(path);
            fail();
        } catch (final ExternalMessageBodyException e) {
            // Expected
        }

        // Wait to ensure that the watch service is watching...
        Thread.sleep(5000);

        // Add a new allowed path
        try (final BufferedWriter writer = Files.newBufferedWriter(allowListFile.toPath(), APPEND)) {
            writer.write(dataUri + System.lineSeparator());
        }

        // Check that the new allowed path was detected
        boolean pass = false;
        // Polling to see if change occurred for 20 seconds
        final long endTimes = System.nanoTime() + 20000000000L;
        while (System.nanoTime() < endTimes) {
            Thread.sleep(50);
            try {
                validator.validate(path);
                pass = true;
                break;
            } catch (final ExternalMessageBodyException e) {
                // Still not passing, retry
            }
        }

        assertTrue("Validator did not update with new path", pass);
    }

    private void addAllowedPath(final String allowed) throws Exception {
        try (final BufferedWriter writer = Files.newBufferedWriter(allowListFile.toPath(), APPEND)) {
            writer.write(allowed + System.lineSeparator());
        }
        validator.init();
    }
}
