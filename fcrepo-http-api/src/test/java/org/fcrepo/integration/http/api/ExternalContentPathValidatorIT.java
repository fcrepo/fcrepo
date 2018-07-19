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
import java.nio.file.Files;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * @author bbpennel
 */
public class ExternalContentPathValidatorIT extends AbstractResourceIT {

    private static final Logger LOGGER = getLogger(ExternalContentPathValidatorIT.class);

    private static final String NON_RDF_SOURCE_LINK_HEADER = "<" + NON_RDF_SOURCE.getURI() + ">;rel=\"type\"";

    static {
        try {
            final File allowedFile = File.createTempFile("allowed", "txt");
            allowedFile.deleteOnExit();
            addAllowedPath(allowedFile, serverAddress);

            System.setProperty("fcrepo.external.content.allowed", allowedFile.getAbsolutePath());
            LOGGER.warn("fcrepo.external.content.allowed = {}", allowedFile.getAbsolutePath());
        } catch (final Exception e) {
            LOGGER.error("Failed to setup allowed configuration file", e);
        }
    }

    private static void addAllowedPath(final File allowedFile, final String allowed) throws Exception {
        try (BufferedWriter writer = Files.newBufferedWriter(allowedFile.toPath(), APPEND)) {
            writer.write(allowed + System.lineSeparator());
        }
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
        final String uri = serverAddress + id;

        final HttpPut put = putObjMethod(uri);
        put.addHeader(LINK, getExternalContentLinkHeader(externalLocation, "proxy", null));
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(SC_BAD_REQUEST, getStatus(response));
        }
    }
}
