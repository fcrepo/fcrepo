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
package org.fcrepo.persistence.api.common;

import static java.util.Arrays.asList;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.persistence.api.common.ResourceHeaderSerializationUtils.deserializeHeaders;
import static org.fcrepo.persistence.api.common.ResourceHeaderSerializationUtils.serializeHeaders;
import static org.fcrepo.persistence.api.common.ResourceHeaderUtils.newResourceHeaders;
import static org.fcrepo.persistence.api.common.ResourceHeaderUtils.populateBinaryHeaders;
import static org.fcrepo.persistence.api.common.ResourceHeaderUtils.populateExternalBinaryHeaders;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.junit.Test;

/**
 * @author bbpennel
 *
 */
public class ResourceHeaderSerializationUtilsTest {

    private static final String PARENT_ID = "info:fedora/parent";

    private static final String RESOURCE_ID = "info:fedora/resource";

    private static final String USER_PRINCIPAL = "someUser";

    private static final String CREATED_DATE = "2019-11-22T19:10:04.534001Z";

    private static final String MODIFIED_DATE = "2019-11-22T19:40:33.697004Z";

    private static final String MIMETYPE = "text/plain";

    private static final String FILENAME = "file.txt";

    private static final Long FILESIZE = 3431l;

    private static final Collection<URI> DIGESTS = asList(URI.create("sha1:123456789"));

    private static final String EXTERNAL_URL = "http://example.com/file.txt";

    private static final String EXTERNAL_HANDLING = "proxy";

    @Test(expected = IllegalArgumentException.class)
    public void serializeHeaders_NullHeaders() {
        serializeHeaders(null);
    }

    @Test
    public void serializeHeaders_MinimalHeaders() throws Exception {
        final var headers = newResourceHeaders(PARENT_ID, RESOURCE_ID, BASIC_CONTAINER.toString());

        final var headerStream = serializeHeaders(headers);
        final var serialized = IOUtils.toString(headerStream, "UTF-8");

        assertTrue(serialized.contains("info:fedora/parent"));
        assertTrue(serialized.contains("info:fedora/resource"));
        assertTrue(serialized.contains("http://www.w3.org/ns/ldp#BasicContainer"));
    }

    @Test
    public void serializeHeaders_TimeHeaders() throws Exception {
        final var headers = ResourceHeaderUtils.newResourceHeaders(PARENT_ID, RESOURCE_ID, BASIC_CONTAINER.toString());
        headers.setLastModifiedDate(Instant.parse(MODIFIED_DATE));
        headers.setCreatedDate(Instant.parse(CREATED_DATE));

        final var headerStream = serializeHeaders(headers);

        final var serialized = IOUtils.toString(headerStream, "UTF-8");

        assertTrue(serialized.contains("\"createdDate\":\"" + CREATED_DATE + "\""));
        assertTrue(serialized.contains("\"lastModifiedDate\":\"" + MODIFIED_DATE + "\""));
    }

    @Test(expected = PersistentStorageException.class)
    public void deserializeHeaders_InvalidContent() throws Exception {
        final var contentStream = new ByteArrayInputStream("Totally not headers".getBytes());

        deserializeHeaders(contentStream);
    }

    @Test
    public void deserializeHeaders_ContainerHeaders() throws Exception {
        final var headers = newResourceHeaders(PARENT_ID, RESOURCE_ID, BASIC_CONTAINER.toString());

        headers.setLastModifiedBy(USER_PRINCIPAL);
        headers.setLastModifiedDate(Instant.parse(MODIFIED_DATE));
        headers.setCreatedBy(USER_PRINCIPAL);
        headers.setCreatedDate(Instant.parse(CREATED_DATE));

        final var headerStream = serializeHeaders(headers);

        final var resultHeaders = deserializeHeaders(headerStream);

        assertEquals(PARENT_ID, resultHeaders.getParent());
        assertEquals(RESOURCE_ID, resultHeaders.getId());
        assertEquals(BASIC_CONTAINER.toString(), resultHeaders.getInteractionModel());

        assertEquals(USER_PRINCIPAL, resultHeaders.getCreatedBy());
        assertEquals(USER_PRINCIPAL, resultHeaders.getLastModifiedBy());
        assertEquals(MODIFIED_DATE, resultHeaders.getLastModifiedDate().toString());
        assertEquals(CREATED_DATE, resultHeaders.getCreatedDate().toString());
    }

    @Test
    public void deserializeHeaders_BinaryHeaders() throws Exception {
        final var headers = newResourceHeaders(PARENT_ID, RESOURCE_ID, NON_RDF_SOURCE.toString());

        headers.setLastModifiedBy(USER_PRINCIPAL);
        headers.setLastModifiedDate(Instant.parse(MODIFIED_DATE));
        headers.setCreatedBy(USER_PRINCIPAL);
        headers.setCreatedDate(Instant.parse(CREATED_DATE));

        populateBinaryHeaders(headers, MIMETYPE, FILENAME, FILESIZE, DIGESTS);
        populateExternalBinaryHeaders(headers, EXTERNAL_URL, EXTERNAL_HANDLING);

        final var headerStream = serializeHeaders(headers);

        final var resultHeaders = deserializeHeaders(headerStream);

        assertEquals(PARENT_ID, resultHeaders.getParent());
        assertEquals(RESOURCE_ID, resultHeaders.getId());
        assertEquals(NON_RDF_SOURCE.toString(), resultHeaders.getInteractionModel());

        assertEquals(USER_PRINCIPAL, resultHeaders.getCreatedBy());
        assertEquals(USER_PRINCIPAL, resultHeaders.getLastModifiedBy());
        assertEquals(MODIFIED_DATE, resultHeaders.getLastModifiedDate().toString());
        assertEquals(CREATED_DATE, resultHeaders.getCreatedDate().toString());

        assertEquals(MIMETYPE, resultHeaders.getMimeType());
        assertEquals(FILENAME, resultHeaders.getFilename());
        assertEquals(FILESIZE, resultHeaders.getContentSize());
        assertTrue(DIGESTS.containsAll(resultHeaders.getDigests()));
        assertEquals(EXTERNAL_URL, resultHeaders.getExternalUrl());
        assertEquals(EXTERNAL_HANDLING, resultHeaders.getExternalHandling());

    }
}
