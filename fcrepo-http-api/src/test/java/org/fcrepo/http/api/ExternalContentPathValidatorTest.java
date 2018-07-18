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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.fcrepo.kernel.api.exception.ExternalMessageBodyException;
import org.junit.Before;
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

    private File allowListFile;

    @Before
    public void init() throws Exception {
        allowListFile = tmpDir.newFile();

        validator = new ExternalContentPathValidator();
        validator.setAllowListPath(allowListFile.getAbsolutePath());
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testValidateWithNoAllowList() throws Exception {
        validator.setAllowListPath(null);
        validator.init();

        final String extPath = "file:///this/path/file.txt";
        validator.validate(extPath);
    }

    @Test
    public void testValidFileUri() throws Exception {
        final String goodPath = "file:///this/path/is/good/";
        final String extPath = goodPath + "file.txt";

        addAllowedPath(goodPath);

        validator.validate(extPath);
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
        final String goodPath = "file:///this/path/is/good/file.txt";

        addAllowedPath(goodPath);

        validator.validate(goodPath);
    }

    @Test
    public void testMultipleMatches() throws Exception {
        final String goodPath = "file:///this/path/is/good/";
        final String extPath = goodPath + "file.txt";
        final String anotherPath = "file:///this/path/";

        addAllowedPath(anotherPath);
        addAllowedPath(goodPath);

        validator.validate(extPath);
    }

    @Test
    public void testMultipleSchemes() throws Exception {
        final String httpPath = "http://example.com/";
        final String filePath = "file:///this/path/is/good/";
        final String extPath = filePath + "file.txt";

        addAllowedPath(httpPath);
        addAllowedPath(filePath);

        validator.validate(extPath);
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testInvalidFileUri() throws Exception {
        final String goodPath = "file:///this/path/is/good/";
        final String extPath = "file:///a/different/path/file.txt";

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
    public void testRelativeModifier() throws Exception {
        final String goodPath = "file:///this/path/";
        final String extPath = goodPath + "../sneaky/file.txt";

        addAllowedPath(goodPath);

        validator.validate(extPath);
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testNoScheme() throws Exception {
        final String goodPath = "file:///this/path/is/good/";
        final String extPath = "/this/path/is/good/file.txt";

        addAllowedPath(goodPath);

        validator.validate(extPath);
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testEmptyPath() throws Exception {
        final String goodPath = "file:///this/path/is/good/";
        addAllowedPath(goodPath);

        validator.validate("");
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testNullPath() throws Exception {
        final String goodPath = "file:///this/path/is/good/";
        addAllowedPath(goodPath);

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

        final String path1 = "file:///this/path/is/good/file";
        validator.validate(path1);
        final String path2 = "http://example.com/file";
        validator.validate(path2);
        final String path3 = "https://example.com/file";
        validator.validate(path3);
    }

    @Test
    public void testCaseInsensitive() throws Exception {
        final String goodPath = "FILE:///this/path/";
        final String extPath = "file:///this/path/file.txt";

        addAllowedPath(goodPath);

        validator.validate(extPath);
    }

    private void addAllowedPath(final String allowed) throws Exception {
        try (BufferedWriter writer = Files.newBufferedWriter(allowListFile.toPath(), APPEND)) {
            writer.write(allowed + System.lineSeparator());
        }
        validator.init();
    }
}
