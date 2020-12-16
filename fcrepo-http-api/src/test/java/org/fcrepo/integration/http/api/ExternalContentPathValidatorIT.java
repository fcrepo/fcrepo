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

import static java.nio.file.StandardOpenOption.APPEND;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LOCATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@TestExecutionListeners(listeners = {
        DependencyInjectionTestExecutionListener.class,
        TestIsolationExecutionListener.class,
        DirtyContextBeforeAndAfterClassTestExecutionListener.class
}, mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
public class ExternalContentPathValidatorIT extends AbstractResourceIT {

    private static final Logger LOGGER = getLogger(ExternalContentPathValidatorIT.class);

    private static final String NON_RDF_SOURCE_LINK_HEADER = "<" + NON_RDF_SOURCE.getURI() + ">;rel=\"type\"";

    private static File disallowedDir;
    private static File allowedDir;

    static {
        try {
            final File allowedFile = File.createTempFile("allowed", ".txt");
            allowedFile.deleteOnExit();
            addAllowedPath(allowedFile, serverAddress);

            final Path disallowedPath = Files.createTempDirectory("disallowed");
            disallowedPath.toFile().deleteOnExit();
            disallowedDir = disallowedPath.toFile();
            allowedDir = Files.createTempDirectory(disallowedPath, "data").toFile();
            addAllowedPath(allowedFile, allowedDir.toURI().toString());

            System.setProperty("fcrepo.external.content.allowed", allowedFile.getAbsolutePath());
            LOGGER.warn("fcrepo.external.content.allowed = {}", allowedFile.getAbsolutePath());
        } catch (final Exception e) {
            LOGGER.error("Failed to setup allowed configuration file", e);
        }
    }

    private static void addAllowedPath(final File allowedFile, final String allowed) throws Exception {
        try (final BufferedWriter writer = Files.newBufferedWriter(allowedFile.toPath(), APPEND)) {
            writer.write(allowed + System.lineSeparator());
        }
    }

    @Before
    public void init() throws Exception {
        // Because of the dirtied context, need to wait for fedora to restart before testing
        int triesRemaining = 50;
        while (true) {
            final HttpGet get = new HttpGet(serverAddress);
            try (final CloseableHttpResponse response = execute(get)) {
                assertEquals(SC_OK, getStatus(response));
                break;
            } catch (final NoHttpResponseException | ConnectException e) {
                if (triesRemaining-- > 0) {
                    LOGGER.debug("Waiting for fedora to become available");
                    Thread.sleep(50);
                } else {
                    throw new Exception("Fedora instance did not become available in allowed time");
                }
            }
        }
        // Now that fedora has started, clear the property so it won't impact other tests
        System.clearProperty("fcrepo.external.content.allowed");
    }

    @Test
    public void testAllowedPath() throws Exception {
        final HttpPost method = postObjMethod();
        method.addHeader(CONTENT_TYPE, "text/plain");
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        method.setEntity(new StringEntity("xyz"));
        final String externalLocation;

        // Make an external remote URI.
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals(SC_CREATED, getStatus(response));
            externalLocation = getLocation(response);
        }

        final String id = getRandomUniqueId();

        final HttpPut put = putObjMethod(id);
        put.addHeader(LINK, getExternalContentLinkHeader(externalLocation, "proxy", null));
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(SC_CREATED, getStatus(response));
        }
        // Get the external content proxy resource.
        try (final CloseableHttpResponse response = execute(getObjMethod(id))) {
            assertEquals(SC_OK, getStatus(response));
            assertEquals("text/plain", response.getFirstHeader(CONTENT_TYPE).getValue());
            assertEquals(externalLocation, response.getFirstHeader(CONTENT_LOCATION).getValue());
        }
    }

    @Test
    public void testDisallowedPath() throws Exception {
        final String externalLocation = "http://example.com/";

        final String id = getRandomUniqueId();

        final HttpPut put = putObjMethod(id);
        put.addHeader(LINK, getExternalContentLinkHeader(externalLocation, "proxy", null));
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(SC_BAD_REQUEST, getStatus(response));
        }
    }

    @Test
    public void testAllowedFilePath() throws Exception {
        final String fileContent = "content";
        final File permittedFile = new File(allowedDir, "test.txt");
        FileUtils.writeStringToFile(permittedFile, fileContent, "UTF-8");
        final String fileUri = permittedFile.toURI().toString();

        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.addHeader(LINK, getExternalContentLinkHeader(fileUri, "proxy", "text/plain"));
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(SC_CREATED, getStatus(response));
        }
        // Get the external content proxy resource.
        try (final CloseableHttpResponse response = execute(getObjMethod(id))) {
            assertEquals(SC_OK, getStatus(response));
            assertEquals("text/plain", response.getFirstHeader(CONTENT_TYPE).getValue());
            assertEquals(fileUri, response.getFirstHeader(CONTENT_LOCATION).getValue());
            assertEquals(fileContent, IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
        }
    }

    @Test
    public void testAllowedCaseSensitiveFilePath() throws Exception {
        final String fileContent = "content";
        final File permittedFile = new File(allowedDir, "TEST.txt");
        FileUtils.writeStringToFile(permittedFile, fileContent, "UTF-8");
        final String fileUri = permittedFile.toURI().toString();

        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.addHeader(LINK, getExternalContentLinkHeader(fileUri, "proxy", "text/plain"));
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(SC_CREATED, getStatus(response));
        }
        // Get the external content proxy resource.
        try (final CloseableHttpResponse response = execute(getObjMethod(id))) {
            assertEquals(SC_OK, getStatus(response));
            assertEquals("text/plain", response.getFirstHeader(CONTENT_TYPE).getValue());
            assertEquals(fileUri, response.getFirstHeader(CONTENT_LOCATION).getValue());
            assertEquals(fileContent, IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
        }
    }

    @Test
    public void testDisallowedFilePath() throws Exception {
        final String fileContent = "content";
        final File disallowedFile = new File(disallowedDir, "test.txt");
        FileUtils.writeStringToFile(disallowedFile, fileContent, "UTF-8");
        final String fileUri = disallowedFile.toURI().toString();

        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.addHeader(LINK, getExternalContentLinkHeader(fileUri, "proxy", "text/plain"));
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(SC_BAD_REQUEST, getStatus(response));
        }
    }

    @Test
    public void testPathModifiers() throws Exception {
        // Creating file in disallowed path
        final String fileContent = "content";
        final File disallowedFile = new File(disallowedDir, "test.txt");
        FileUtils.writeStringToFile(disallowedFile, fileContent, "UTF-8");

        // Variations of path modifiers that should be rejected or fail to find file.
        final List<String> modifiers = Arrays.asList("../", "%2e%2e%2f", "%2e%2e/", "..%2f",
                "%252e%252e%255c", "%2e%2e%5c", "%2e%2e%5c%2f", "..%c0%af");

        for (final String modifier : modifiers) {
            // Attempt to address file with escaped uri modifiers
            final String externalLocation = allowedDir.toURI().toString() + modifier + "test.txt";

            final String id = getRandomUniqueId();
            final HttpPut put = putObjMethod(id);
            put.addHeader(LINK, getExternalContentLinkHeader(externalLocation, "proxy", "text/plain"));
            try (final CloseableHttpResponse response = execute(put)) {
                assertEquals("Path " + externalLocation + " must be rejected", SC_BAD_REQUEST, getStatus(response));
            }
        }
    }
}
