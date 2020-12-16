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

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.TEMPORARY_REDIRECT;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpStatus.SC_CREATED;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.fcrepo.kernel.api.FedoraTypes;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.test.context.TestExecutionListeners;

/**
 * @author whikloj
 * @since 2018-07-10
 */
@TestExecutionListeners(
        listeners = { TestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class ExternalContentHandlerIT extends AbstractResourceIT {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final String NON_RDF_SOURCE_LINK_HEADER = "<" + NON_RDF_SOURCE.getURI() + ">;rel=\"type\"";

    private static final String WANT_DIGEST = "Want-Digest";

    private static final String DIGEST = "Digest";

    private static final String TEST_BINARY_CONTENT = "01234567890123456789012345678901234567890123456789";

    private static final String TEST_SHA_DIGEST_HEADER_VALUE = "sha=9578f951955d37f20b601c26591e260c1e5389bf";

    private static final String TEST_MD5_DIGEST_HEADER_VALUE = "md5=baed005300234f3d1503c50a48ce8e6f";

    private static final CloseableHttpClient noFollowClient = HttpClientBuilder.create()
            .disableRedirectHandling().build();

    @Before
    public void setup() throws Exception {
        tempFolder.create();
    }

    @Test
    public void testProxyRemoteContentTypeForHttpUri() throws Exception {
        final var externalLocation = createHttpResource("audio/ogg", "xyz");
        final String finalLocation = getRandomUniqueId();

        // Make an external content resource proxying the above URI.
        final HttpPut put = putObjMethod(finalLocation);
        put.addHeader(LINK, getExternalContentLinkHeader(externalLocation, "proxy", null));
        assertEquals(CREATED.getStatusCode(), getStatus(put));

        // Get the external content proxy resource.
        try (final CloseableHttpResponse response = execute(getObjMethod(finalLocation))) {
            assertEquals(SC_OK, getStatus(response));
            assertContentType(response, "audio/ogg");
            assertContentLocation(response, externalLocation);
            assertContentLength(response, 3);
        }
    }

    @Test
    public void testProxyClientContentTypeOverridesRemoteForHttpUri() throws Exception {
        final var externalLocation = createHttpResource("audio/ogg", "vxyz");
        final String finalLocation = getRandomUniqueId();

        // Make an external content resource proxying the above URI.
        final HttpPut put = putObjMethod(finalLocation);
        put.addHeader(LINK, getExternalContentLinkHeader(externalLocation, "proxy", "audio/mp3"));
        assertEquals(CREATED.getStatusCode(), getStatus(put));

        // Get the external content proxy resource.
        try (final CloseableHttpResponse response = execute(getObjMethod(finalLocation))) {
            assertEquals(SC_OK, getStatus(response));
            assertContentType(response, "audio/mp3");
            assertContentLocation(response, externalLocation);
            assertContentLength(response, 4);
        }
    }

    @Test
    public void testCopyRemoteContentTypeForHttpUri() throws Exception {
        final var externalLocation = createHttpResource("audio/ogg", "xyz");
        final String finalLocation = getRandomUniqueId();

        // Make an external content resource proxying the above URI.
        final HttpPut put = putObjMethod(finalLocation);
        put.addHeader(LINK, getExternalContentLinkHeader(externalLocation, "copy", null));
        assertEquals(CREATED.getStatusCode(), getStatus(put));

        // Get the external content proxy resource.
        try (final CloseableHttpResponse response = execute(getObjMethod(finalLocation))) {
            assertEquals(SC_OK, getStatus(response));
            assertContentType(response, "audio/ogg");
            assertContentLength(response, 3);
        }
    }

    @Test
    public void testCopyClientContentTypeOverridesRemoteForHttpUri() throws Exception {
        final var externalLocation = createHttpResource("audio/ogg", "xyz");
        final String finalLocation = getRandomUniqueId();

        // Make an external content resource proxying the above URI.
        final HttpPut put = putObjMethod(finalLocation);
        put.addHeader(LINK, getExternalContentLinkHeader(externalLocation, "copy", "audio/mp3"));
        assertEquals(CREATED.getStatusCode(), getStatus(put));

        // Get the external content proxy resource.
        try (final CloseableHttpResponse response = execute(getObjMethod(finalLocation))) {
            assertEquals(SC_OK, getStatus(response));
            assertContentType(response, "audio/mp3");
            assertContentLength(response, 3);
        }
    }

    @Test
    public void testProxyWithWantDigestForLocalFile() throws IOException {

        final File externalFile = createExternalLocalFile(TEST_BINARY_CONTENT);

        final String fileUri = externalFile.toURI().toString();

        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.addHeader(LINK, getExternalContentLinkHeader(fileUri, "proxy", "text/plain"));
        assertEquals(CREATED.getStatusCode(), getStatus(put));

        final String expectedDigestHeaderValue = TEST_SHA_DIGEST_HEADER_VALUE;

        // HEAD request with Want-Digest
        final HttpHead headObjMethod = headObjMethod(id);
        headObjMethod.addHeader(WANT_DIGEST, "sha");
        checkExternalDataStreamResponseHeader(headObjMethod, fileUri, expectedDigestHeaderValue);

        // GET request with Want-Digest
        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod.addHeader(WANT_DIGEST, "sha");
        checkExternalDataStreamResponseHeader(getObjMethod, fileUri, expectedDigestHeaderValue);
    }

    @Test
    public void testCopyWithWantDigestForLocalFile() throws IOException {

        final File externalFile = createExternalLocalFile(TEST_BINARY_CONTENT);

        final String fileUri = externalFile.toURI().toString();

        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.addHeader(LINK, getExternalContentLinkHeader(fileUri, "copy", "text/plain"));
        assertEquals(CREATED.getStatusCode(), getStatus(put));

        final String expectedDigestHeaderValue = TEST_SHA_DIGEST_HEADER_VALUE;

        // HEAD request with Want-Digest
        final HttpHead headObjMethod = headObjMethod(id);
        headObjMethod.addHeader(WANT_DIGEST, "sha");
        checkExternalDataStreamResponseHeader(headObjMethod, null, expectedDigestHeaderValue);

        // GET request with Want-Digest
        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod.addHeader(WANT_DIGEST, "sha");
        checkExternalDataStreamResponseHeader(getObjMethod, null, expectedDigestHeaderValue);
    }

    @Test
    public void testProxyWithWantDigestForHttpUri() throws Exception {

        final String dsUrl = createHttpResource(TEST_BINARY_CONTENT);

        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.addHeader(LINK, getExternalContentLinkHeader(dsUrl, "proxy", "text/plain"));
        assertEquals(CREATED.getStatusCode(), getStatus(put));

        final String expectedDigestHeaderValue = TEST_SHA_DIGEST_HEADER_VALUE;

        // HEAD request with Want-Digest
        final HttpHead headObjMethod = headObjMethod(id);
        headObjMethod.addHeader(WANT_DIGEST, "sha");
        checkExternalDataStreamResponseHeader(headObjMethod, dsUrl, expectedDigestHeaderValue);

        // GET request with Want-Digest
        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod.addHeader(WANT_DIGEST, "sha");
        checkExternalDataStreamResponseHeader(getObjMethod, dsUrl, expectedDigestHeaderValue);
    }

    @Test
    public void testCopyWithWantDigestForHttpUri() throws Exception {

        final String dsUrl = createHttpResource(TEST_BINARY_CONTENT);

        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.addHeader(LINK, getExternalContentLinkHeader(dsUrl, "copy", "text/plain"));
        assertEquals(CREATED.getStatusCode(), getStatus(put));

        final String expectedDigestHeaderValue = TEST_SHA_DIGEST_HEADER_VALUE;

        // HEAD request with Want-Digest
        final HttpHead headObjMethod = headObjMethod(id);
        headObjMethod.addHeader(WANT_DIGEST, "sha");
        checkExternalDataStreamResponseHeader(headObjMethod, null, expectedDigestHeaderValue);

        // GET request with Want-Digest
        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod.addHeader(WANT_DIGEST, "sha");
        checkExternalDataStreamResponseHeader(getObjMethod, null, expectedDigestHeaderValue);
    }

    @Test
    public void testProxyWithWantDigestMultipleForLocalFile() throws IOException {

        final File externalFile = createExternalLocalFile(TEST_BINARY_CONTENT);

        final String fileUri = externalFile.toURI().toString();

        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.addHeader(LINK, getExternalContentLinkHeader(fileUri, "proxy", "text/plain"));
        assertEquals(CREATED.getStatusCode(), getStatus(put));

        // HEAD request with Want-Digest
        final HttpHead headObjMethod = headObjMethod(id);
        headObjMethod.addHeader(WANT_DIGEST, "sha, md5;q=0.3");
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            assertContentLocation(response, fileUri);
            assertTrue(response.getHeaders(DIGEST).length > 0);

            final String digesterHeaderValue = response.getHeaders(DIGEST)[0].getValue();
            assertTrue("SHA-1 Fixity Checksum doesn't match",
                    digesterHeaderValue.contains(TEST_SHA_DIGEST_HEADER_VALUE));
            assertTrue("MD5 fixity checksum doesn't match",
                    digesterHeaderValue.contains(TEST_MD5_DIGEST_HEADER_VALUE));
        }

        // GET request with Want-Digest
        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod.addHeader(WANT_DIGEST, "sha, md5;q=0.3");
        try (final CloseableHttpResponse response = execute(getObjMethod)) {
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            assertContentLocation(response, fileUri);
            assertTrue(response.getHeaders(DIGEST).length > 0);

            final String digesterHeaderValue = response.getHeaders(DIGEST)[0].getValue();
            assertTrue("SHA-1 Fixity Checksum doesn't match",
                    digesterHeaderValue.contains(TEST_SHA_DIGEST_HEADER_VALUE));
            assertTrue("MD5 fixity checksum doesn't match",
                    digesterHeaderValue.contains(TEST_MD5_DIGEST_HEADER_VALUE));
        }
    }

    private File createExternalLocalFile(final String content) throws IOException {
        final File externalFile = tempFolder.newFile();
        try (final FileWriter fw = new FileWriter(externalFile)) {
            fw.write(content);
        }
        return externalFile;
    }

    private void checkExternalDataStreamResponseHeader(final HttpUriRequest req, final String contenLocation,
            final String shaValue) throws IOException {
        try (final CloseableHttpResponse response = execute(req)) {
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            assertTrue(response.getHeaders(DIGEST).length > 0);
            if (StringUtils.isNoneBlank(contenLocation)) {
                assertEquals(contenLocation, getContentLocation(response));
            }
            final String digesterHeaderValue = response.getHeaders(DIGEST)[0].getValue();
            assertTrue("Fixity Checksum doesn't match",
                    digesterHeaderValue.equals(shaValue));
        }
    }

    @Test
    public void testHeadExternalDatastreamRedirectForHttpUri() throws Exception {

        final String externalLocation = createHttpResource(TEST_BINARY_CONTENT);

        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.addHeader(LINK, getExternalContentLinkHeader(externalLocation, "redirect", "image/jpeg"));
        assertEquals(CREATED.getStatusCode(), getStatus(put));

        // Configure HEAD request to NOT follow redirects
        final HttpHead headObjMethod = headObjMethod(id);
        final RequestConfig.Builder requestConfig = RequestConfig.custom();
        requestConfig.setRedirectsEnabled(false);
        headObjMethod.setConfig(requestConfig.build());

        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            assertEquals(TEMPORARY_REDIRECT.getStatusCode(), response.getStatusLine().getStatusCode());
            assertLocation(response, externalLocation);
            assertEquals("bytes", response.getFirstHeader("Accept-Ranges").getValue());
            assertContentLength(response, 0);
            assertContentType(response, "image/jpeg");
            final ContentDisposition disposition =
                    new ContentDisposition(response.getFirstHeader(CONTENT_DISPOSITION).getValue());
            assertEquals("attachment", disposition.getType());
        }
    }

    @Test
    public void testGetExternalDatastreamForHttpUri() throws Exception {

        final String externalLocation = createHttpResource(TEST_BINARY_CONTENT);

        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.addHeader(LINK, getExternalContentLinkHeader(externalLocation, "redirect", "image/jpeg"));
        assertEquals(CREATED.getStatusCode(), getStatus(put));

        // Configure HEAD request to NOT follow redirects
        final HttpGet getObjMethod = getObjMethod(id);
        final RequestConfig.Builder requestConfig = RequestConfig.custom();
        requestConfig.setRedirectsEnabled(false);
        getObjMethod.setConfig(requestConfig.build());

        try (final CloseableHttpResponse response = execute(getObjMethod)) {
            assertEquals(TEMPORARY_REDIRECT.getStatusCode(), response.getStatusLine().getStatusCode());
            assertLocation(response, externalLocation);
            assertContentType(response, "image/jpeg");
            assertEquals("bytes", response.getFirstHeader("Accept-Ranges").getValue());
            final ContentDisposition disposition =
                    new ContentDisposition(response.getFirstHeader(CONTENT_DISPOSITION).getValue());
            assertEquals("attachment", disposition.getType());
        }
    }

    private void checkRedirectWantDigestResult(final HttpRequestBase request, final String dsUrl, final String sha1,
            final String md5) throws IOException {
        try (final CloseableHttpResponse response = execute(request)) {
            assertEquals(TEMPORARY_REDIRECT.getStatusCode(), response.getStatusLine().getStatusCode());
            assertLocation(response, dsUrl);
            assertTrue(response.getHeaders(DIGEST).length > 0);

            final String digesterHeaderValue = response.getHeaders(DIGEST)[0].getValue();
            assertTrue("SHA-1 Fixity Checksum doesn't match",
                    digesterHeaderValue.contains(sha1));
            if (md5 != null) {
                assertTrue("MD5 fixity checksum doesn't match",
                        digesterHeaderValue.contains(md5));
            }
        }
    }

    @Test
    public void testRedirectWithWantDigest() throws Exception {

        final String dsUrl = createHttpResource(TEST_BINARY_CONTENT);

        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.addHeader(LINK, getExternalContentLinkHeader(dsUrl, "redirect", "image/jpeg"));
        assertEquals(CREATED.getStatusCode(), getStatus(put));

        // Configure request to NOT follow redirects
        final RequestConfig.Builder requestConfig = RequestConfig.custom();
        requestConfig.setRedirectsEnabled(false);

        // Verify HEAD request behavior with single Want-Digest
        final HttpHead headObjMethod = headObjMethod(id);
        headObjMethod.addHeader(WANT_DIGEST, "sha");
        headObjMethod.setConfig(requestConfig.build());

        checkRedirectWantDigestResult(headObjMethod, dsUrl,
                TEST_SHA_DIGEST_HEADER_VALUE, null);

        // Verify HEAD request behavior with multiple Want-Digest
        final HttpHead headObjMethodMulti = headObjMethod(id);
        headObjMethodMulti.addHeader(WANT_DIGEST, "sha, md5;q=0.3");
        headObjMethodMulti.setConfig(requestConfig.build());

        checkRedirectWantDigestResult(headObjMethodMulti, dsUrl,
                TEST_SHA_DIGEST_HEADER_VALUE, TEST_MD5_DIGEST_HEADER_VALUE);

        // Verify GET request behavior with Want-Digest
        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod.addHeader(WANT_DIGEST, "sha");
        getObjMethod.setConfig(requestConfig.build());

        checkRedirectWantDigestResult(getObjMethod, dsUrl,
                TEST_SHA_DIGEST_HEADER_VALUE, null);

        // Verify GET with multiple Want-Digest
        final HttpGet getObjMethodMulti = getObjMethod(id);
        getObjMethodMulti.addHeader(WANT_DIGEST, "sha, md5;q=0.3");
        getObjMethodMulti.setConfig(requestConfig.build());

        checkRedirectWantDigestResult(getObjMethodMulti, dsUrl,
                TEST_SHA_DIGEST_HEADER_VALUE, TEST_MD5_DIGEST_HEADER_VALUE);
    }

    @Test
    public void testRedirectForHttpUri() throws Exception {

        final String externalLocation = createHttpResource(TEST_BINARY_CONTENT);

        final String id = getRandomUniqueId();
        final HttpPut httpPut = putObjMethod(id);
        httpPut.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        httpPut.addHeader(LINK, getExternalContentLinkHeader(externalLocation, "redirect", null));

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            final HttpGet get = new HttpGet(getLocation(response));
            try (final CloseableHttpResponse getResponse = noFollowClient.execute(get)) {
                assertEquals(TEMPORARY_REDIRECT.getStatusCode(), getStatus(getResponse));
                assertLocation(getResponse, externalLocation);
            }
        }
    }

    @Test
    public void testProxyLocalFile() throws Exception {
        final File localFile = createExternalLocalFile(TEST_BINARY_CONTENT);

        final String id = getRandomUniqueId();
        final HttpPut httpPut = putObjMethod(id);
        httpPut.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        final String fileUri = localFile.toURI().toString();
        httpPut.addHeader(LINK, getExternalContentLinkHeader(fileUri, "proxy", "text/plain"));

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            final HttpGet get = new HttpGet(getLocation(response));
            try (final CloseableHttpResponse getResponse = execute(get)) {
                assertEquals(OK.getStatusCode(), getStatus(getResponse));
                assertContentLocation(getResponse, fileUri);
                assertContentLength(getResponse, TEST_BINARY_CONTENT.length());
                assertBodyMatches(getResponse, TEST_BINARY_CONTENT);
            }
        }
    }

    @Test
    public void testCopyLocalFile() throws Exception {
        final String entityStr = "Hello there, this is the original object speaking.";

        final File localFile = createExternalLocalFile(entityStr);

        final String id = getRandomUniqueId();
        final HttpPut httpPut = putObjMethod(id);
        httpPut.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        final String localPath = localFile.toURI().toString();
        httpPut.addHeader(LINK, getExternalContentLinkHeader(localPath, "copy", "text/plain"));

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));

            // fetch the copy of the object
            final HttpGet get = new HttpGet(getLocation(response));
            try (final CloseableHttpResponse getResponse = execute(get)) {
                assertEquals(OK.getStatusCode(), getStatus(getResponse));
                assertContentType(getResponse, "text/plain");
                assertBodyMatches(getResponse, entityStr);
            }
        }
    }

    @Test
    public void testCopyForHttpUri() throws Exception {
        // create a random binary object
        final var entityStr = "Hello there, this is the original object speaking.";
        final String copyLocation = createHttpResource(entityStr);

        // create a copy of it
        final String id = getRandomUniqueId();
        final HttpPut httpPut = putObjMethod(id);
        httpPut.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        httpPut.addHeader(LINK, getExternalContentLinkHeader(copyLocation, "copy", "text/plain"));

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));

            // fetch the copy of the object
            final HttpGet get = new HttpGet(getLocation(response));
            try (final CloseableHttpResponse getResponse = execute(get)) {
                assertEquals(OK.getStatusCode(), getStatus(getResponse));
                assertContentType(getResponse, "text/plain");
                assertBodyMatches(getResponse, entityStr);
            }
        }
    }

    @Test
    public void testProxyForHttpUri() throws Exception {
        // Create a resource
        final String entityStr = "Hello there, this is the original object speaking.";
        final String origLocation = createHttpResource(entityStr);

        final String id = getRandomUniqueId();
        final HttpPut httpPut = putObjMethod(id);
        httpPut.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        httpPut.addHeader(LINK, getExternalContentLinkHeader(origLocation, "proxy", null));

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            final HttpGet get = new HttpGet(getLocation(response));
            try (final CloseableHttpResponse getResponse = execute(get)) {
                assertEquals(OK.getStatusCode(), getStatus(getResponse));
                assertContentLocation(getResponse, origLocation);
                assertBodyMatches(getResponse, entityStr);
            }
        }
    }

    @Test
    public void testPostExternalContentProxyForHttpUri() throws Exception {
        // Create a resource
        final String entityStr = "Hello there, this is the original object speaking.";
        final String origLocation = createHttpResource(entityStr);

        final String id = getRandomUniqueId();
        final HttpPost httpPost = postObjMethod();
        httpPost.addHeader("Slug", id);
        httpPost.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        httpPost.addHeader(LINK, getExternalContentLinkHeader(origLocation, "proxy", "text/plain"));

        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            final HttpGet get = new HttpGet(getLocation(response));
            try (final CloseableHttpResponse getResponse = execute(get)) {
                assertEquals(OK.getStatusCode(), getStatus(getResponse));
                assertContentLocation(getResponse, origLocation);
                assertContentType(getResponse, "text/plain");
                assertBodyMatches(getResponse, entityStr);
            }
        }
    }

    @Test
    public void testUnsupportedHandlingTypeInExternalMessagePUT() throws IOException {

        final String id = getRandomUniqueId();
        final HttpPut httpPut = putObjMethod(id);
        httpPut.addHeader(LINK, getExternalContentLinkHeader("http://example.com/test", "junk", "image/jpeg"));

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Didn't get a BAD REQUEST error!", BAD_REQUEST.getStatusCode(),
                    getStatus(response));
            assertBodyContains(response, "External content link header url is malformed");
        }
    }

    @Test
    public void testUnsupportedHandlingTypeInExternalMessagePOST() throws IOException {
        final HttpPost httpPost = postObjMethod();
        httpPost.addHeader(LINK, getExternalContentLinkHeader("http://example.com/junk", "junk", "image/jpeg"));

        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals("Didn't get a BAD_REQUEST response!", BAD_REQUEST.getStatusCode(),
                    getStatus(response));
            assertBodyContains(response, "External content link header url is malformed");
        }
    }

    @Test
    public void testMissingHandlingTypeInExternalMessage() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut httpPut = putObjMethod(id);
        httpPut.addHeader(LINK, getExternalContentLinkHeader("http://example.com/junk", null, "image/jpeg"));

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Didn't get a BAD_REQUEST response!", BAD_REQUEST.getStatusCode(),
                    getStatus(response));
            assertBodyContains(response, "External content link header url is malformed");
        }
    }

    @Test
    public void testCopyNotFoundHttpContent() throws Exception {
        final String nonexistentPath = serverAddress + getRandomUniqueId();

        // create a copy of it
        final HttpPost httpPost = postObjMethod();
        httpPost.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        httpPost.addHeader(LINK, getExternalContentLinkHeader(nonexistentPath, "copy", null));

        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals("Didn't get a BAD_REQUEST response!", BAD_REQUEST.getStatusCode(),
                    getStatus(response));
            assertBodyContains(response, "Unable to access external binary");
        }
    }

    @Test
    public void testCopyUnreachableHttpContent() throws Exception {
        final String nonexistentPath = "http://" + getRandomUniqueId() + ".example.com/";

        // create a copy of it
        final HttpPost httpPost = postObjMethod();
        httpPost.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        httpPost.addHeader(LINK, getExternalContentLinkHeader(nonexistentPath, "copy", null));

        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals("Didn't get a BAD_REQUEST response!", BAD_REQUEST.getStatusCode(),
                    getStatus(response));
            assertBodyContains(response, "Unable to access external binary");
        }
    }

    @Test
    public void testProxyNotFoundHttpContent() throws Exception {
        final String nonexistentPath = serverAddress + getRandomUniqueId();

        // create a copy of it
        final HttpPost httpPost = postObjMethod();
        httpPost.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        httpPost.addHeader(LINK, getExternalContentLinkHeader(nonexistentPath, "proxy", null));

        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals("Expected failure on creation", BAD_REQUEST.getStatusCode(),
                    getStatus(response));
            assertBodyContains(response, "Unable to access external binary");
        }
    }

    @Test
    public void testProxyUnreachableHttpContent() throws Exception {
        final String nonexistentPath = "http://" + getRandomUniqueId() + ".example.com/";

        // create a copy of it
        final HttpPost httpPost = postObjMethod();
        httpPost.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        httpPost.addHeader(LINK, getExternalContentLinkHeader(nonexistentPath, "proxy", null));

        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals("Expected failure on creation", BAD_REQUEST.getStatusCode(),
                    getStatus(response));
            assertBodyContains(response, "Unable to access external binary");
        }
    }

    @Test
    public void testRedirectUnreachableHttpContent() throws Exception {
        final String nonexistentPath = "http://" + getRandomUniqueId() + ".example.com/";

        // create a copy of it
        final HttpPost httpPost = postObjMethod();
        httpPost.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        httpPost.addHeader(LINK, getExternalContentLinkHeader(nonexistentPath, "redirect", null));

        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals("Expected failure on creation", BAD_REQUEST.getStatusCode(),
                    getStatus(response));
            assertBodyContains(response, "Unable to access external binary");
        }
    }

    @Test
    public void testProxyNotFoundLocalFile() throws Exception {
        verifyNotFoundLocalFile("proxy");
    }

    @Test
    public void testRedirectNotFoundLocalFile() throws Exception {
        verifyNotFoundLocalFile("redirect");
    }

    @Test
    public void testCopyNotFoundLocalFile() throws Exception {
        verifyNotFoundLocalFile("copy");
    }

    private void verifyNotFoundLocalFile(final String handling) throws Exception {
        final File nonexistentFile = tempFolder.newFile();
        nonexistentFile.delete();
        final String nonexistentUri = nonexistentFile.toURI().toString();

        // create a copy of it
        final HttpPost httpPost = postObjMethod();
        httpPost.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        httpPost.addHeader(LINK, getExternalContentLinkHeader(nonexistentUri, handling, null));

        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals("Expected failure on creation", BAD_REQUEST.getStatusCode(),
                    getStatus(response));
            assertBodyContains(response, "Path did not match any allowed external content paths");
        }
    }

    @Test
    public void testCopyWithTransmissionFixityForLocalFile() throws Exception {
        final File localFile = createExternalLocalFile(TEST_BINARY_CONTENT);

        final String localPath = localFile.toURI().toString();
        final HttpPut httpPut = setupExternalContentPut(localPath, "copy", "text/plain");
        httpPut.addHeader("Digest", TEST_SHA_DIGEST_HEADER_VALUE);

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));

            // fetch the copy of the object
            final HttpGet get = new HttpGet(getLocation(response));
            try (final CloseableHttpResponse getResponse = execute(get)) {
                assertEquals(OK.getStatusCode(), getStatus(getResponse));
                assertContentType(getResponse, "text/plain");
                assertBodyMatches(getResponse, TEST_BINARY_CONTENT);
            }
        }
    }

    @Test
    public void testCopyWithInvalidTransmissionFixityForLocalFile() throws Exception {
        final String content = "Not the expected content";
        final File localFile = createExternalLocalFile(content);
        final String localPath = localFile.toURI().toString();

        final HttpPut httpPut = setupExternalContentPut(localPath, "copy", "text/plain");
        httpPut.addHeader("Digest", TEST_SHA_DIGEST_HEADER_VALUE);

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals(CONFLICT.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testCopyWithTransmissionFixityForHttpUri() throws Exception {
        final String externalLocation = createHttpResource("text/plain", TEST_BINARY_CONTENT);
        final HttpPut httpPut = setupExternalContentPut(externalLocation, "copy", "text/plain");
        httpPut.addHeader("Digest", TEST_SHA_DIGEST_HEADER_VALUE);

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));

            // fetch the copy of the object
            final HttpGet get = new HttpGet(getLocation(response));
            try (final CloseableHttpResponse getResponse = execute(get)) {
                assertEquals(OK.getStatusCode(), getStatus(getResponse));
                assertContentType(getResponse, "text/plain");
                assertBodyMatches(getResponse, TEST_BINARY_CONTENT);
            }
        }
    }

    @Test
    public void testProxyWithTransmissionFixityForLocalFile() throws Exception {
        final File localFile = createExternalLocalFile(TEST_BINARY_CONTENT);

        final String externalLocation = localFile.toURI().toString();
        final HttpPut httpPut = setupExternalContentPut(externalLocation, "proxy", "text/plain");
        httpPut.addHeader("Digest", TEST_SHA_DIGEST_HEADER_VALUE);

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            assertIsProxyBinary(getLocation(response), externalLocation, TEST_BINARY_CONTENT, "text/plain");
        }
    }

    @Test
    public void testProxyWithInvalidTransmissionFixityForLocalFile() throws Exception {
        final File localFile = createExternalLocalFile(TEST_BINARY_CONTENT);

        final String externalLocation = localFile.toURI().toString();
        final HttpPut httpPut = setupExternalContentPut(externalLocation, "proxy", "text/plain");
        httpPut.addHeader("Digest", "sha=12345678910");

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals(CONFLICT.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testRedirectWithTransmissionFixityForHttpUri() throws Exception {
        final String externalLocation = createHttpResource("text/plain", TEST_BINARY_CONTENT);
        final HttpPut httpPut = setupExternalContentPut(externalLocation, "redirect", "text/plain");
        httpPut.addHeader("Digest", TEST_SHA_DIGEST_HEADER_VALUE);

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            assertIsRedirectBinary(getLocation(response), externalLocation, TEST_BINARY_CONTENT, "text/plain");
        }
    }

    @Test
    public void testRedirectWithInvalidTransmissionFixityForHttpUri() throws Exception {
        final String externalLocation = createHttpResource("text/plain", "bad content");
        final HttpPut httpPut = setupExternalContentPut(externalLocation, "redirect", "text/plain");
        httpPut.addHeader("Digest", TEST_SHA_DIGEST_HEADER_VALUE);

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals(CONFLICT.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testProxyPostWithTransmissionFixityForHttpUri() throws Exception {
        final String externalLocation = createHttpResource("text/plain", TEST_BINARY_CONTENT);
        final HttpPost httpPost = postObjMethod();
        httpPost.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        httpPost.addHeader(LINK, getExternalContentLinkHeader(externalLocation, "proxy", "text/plain"));
        httpPost.addHeader("Digest", TEST_SHA_DIGEST_HEADER_VALUE);

        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            assertIsProxyBinary(getLocation(response), externalLocation, TEST_BINARY_CONTENT, "text/plain");
        }
    }

    @Test
    public void testProxyPostWithInvalidTransmissionFixityForHttpUri() throws Exception {
        final String externalLocation = createHttpResource("text/plain", "bad content");
        final HttpPost httpPost = postObjMethod();
        httpPost.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        httpPost.addHeader(LINK, getExternalContentLinkHeader(externalLocation, "proxy", "text/plain"));
        httpPost.addHeader("Digest", TEST_SHA_DIGEST_HEADER_VALUE);

        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals(CONFLICT.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testRedirectPostWithTransmissionFixityForHttpUri() throws Exception {
        final String externalLocation = createHttpResource("text/plain", TEST_BINARY_CONTENT);
        final HttpPost httpPost = postObjMethod();
        httpPost.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        httpPost.addHeader(LINK, getExternalContentLinkHeader(externalLocation, "redirect", "text/plain"));
        httpPost.addHeader("Digest", TEST_SHA_DIGEST_HEADER_VALUE);

        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            assertIsRedirectBinary(getLocation(response), externalLocation, TEST_BINARY_CONTENT, "text/plain");
        }
    }

    @Test
    public void testRedirectPostWithInvalidTransmissionFixityForHttpUri() throws Exception {
        final String externalLocation = createHttpResource("text/plain", "bad content");
        final HttpPost httpPost = postObjMethod();
        httpPost.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        httpPost.addHeader(LINK, getExternalContentLinkHeader(externalLocation, "redirect", "text/plain"));
        httpPost.addHeader("Digest", TEST_SHA_DIGEST_HEADER_VALUE);

        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals(CONFLICT.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testProxyPutWithTransmissionFixityForHttpUri() throws Exception {
        final String externalLocation = createHttpResource("text/plain", TEST_BINARY_CONTENT);

        final HttpPut httpPut = setupExternalContentPut(externalLocation, "proxy", "text/plain");
        httpPut.addHeader("Digest", TEST_SHA_DIGEST_HEADER_VALUE);

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            assertIsProxyBinary(getLocation(response), externalLocation, TEST_BINARY_CONTENT, "text/plain");
        }
    }

    @Test
    public void testUpdateProxyHttpUri() throws Exception {
        final String content2 = "<doc>some more content</doc>";
        final String externalLocation1 = createHttpResource("text/plain", TEST_BINARY_CONTENT);
        final String externalLocation2 = createHttpResource("text/xml", content2);

        final String rescLoc = createExternalContentResource(externalLocation1, "proxy", null);
        assertIsProxyBinary(rescLoc, externalLocation1, TEST_BINARY_CONTENT, "text/plain");

        updateExternalContentResource(rescLoc, externalLocation2, "proxy", null);
        assertIsProxyBinary(rescLoc, externalLocation2, content2, "text/xml");
    }

    @Test
    public void testUpdateProxyLocalFile() throws Exception {
        final String content2 = "<doc>some more content</doc>";
        final File localFile1 = createExternalLocalFile(TEST_BINARY_CONTENT);
        final File localFile2 = createExternalLocalFile(content2);
        final String externalLocation1 = localFile1.toURI().toString();
        final String externalLocation2 = localFile2.toURI().toString();

        final String rescLoc = createExternalContentResource(externalLocation1, "proxy", null);
        assertIsProxyBinary(rescLoc, externalLocation1, TEST_BINARY_CONTENT, "application/octet-stream");

        updateExternalContentResource(rescLoc, externalLocation2, "proxy", "text/xml");
        assertIsProxyBinary(rescLoc, externalLocation2, content2, "text/xml");
    }

    @Test
    public void testUpdateRedirectHttpUri() throws Exception {
        final String content2 = "<doc>some more content</doc>";
        final String externalLocation1 = createHttpResource("text/plain", TEST_BINARY_CONTENT);
        final String externalLocation2 = createHttpResource(content2);

        final String rescLoc = createExternalContentResource(externalLocation1, "redirect", null);
        assertIsRedirectBinary(rescLoc, externalLocation1, TEST_BINARY_CONTENT, "text/plain");

        updateExternalContentResource(rescLoc, externalLocation2, "redirect", "text/xml");
        assertIsRedirectBinary(rescLoc, externalLocation2, content2, "text/xml");
    }

    @Test
    public void testUpdateProxyToRedirectForHttpUri() throws Exception {
        // Create a resource
        final String externalLocation = createHttpResource(TEST_BINARY_CONTENT);

        final String rescLoc = createExternalContentResource(externalLocation, "proxy", null);
        assertIsProxyBinary(rescLoc, externalLocation, TEST_BINARY_CONTENT, null);

        updateExternalContentResource(rescLoc, externalLocation, "redirect", null);
        assertIsRedirectBinary(rescLoc, externalLocation, TEST_BINARY_CONTENT, null);
    }

    @Test
    public void testUpdateRedirectToProxyForHttpUri() throws Exception {
        final String externalLocation = createHttpResource(TEST_BINARY_CONTENT);

        final String rescLoc = createExternalContentResource(externalLocation, "redirect", null);
        assertIsRedirectBinary(rescLoc, externalLocation, TEST_BINARY_CONTENT, null);

        updateExternalContentResource(rescLoc, externalLocation, "proxy", null);
        assertIsProxyBinary(rescLoc, externalLocation, TEST_BINARY_CONTENT, null);
    }

    @Test
    public void testUpdateProxyToRedirectForLocalFile() throws Exception {
        final String content2 = "<doc>some more content</doc>";
        final File localFile1 = createExternalLocalFile(TEST_BINARY_CONTENT);
        final File localFile2 = createExternalLocalFile(content2);
        final String externalLocation1 = localFile1.toURI().toString();
        final String externalLocation2 = localFile2.toURI().toString();

        final String rescLoc = createExternalContentResource(externalLocation1, "proxy", "text/plain");
        assertIsProxyBinary(rescLoc, externalLocation1, TEST_BINARY_CONTENT, "text/plain");

        updateExternalContentResource(rescLoc, externalLocation2, "redirect", "text/xml");
        // Not checking on the content, since following a redirect on a file is unlikely to work
        assertIsRedirectBinary(rescLoc, externalLocation2, null, "text/xml");
    }

    @Test
    public void testUpdateHttpUriToLocalFile() throws Exception {
        final String content2 = "some more content";
        final String externalLocation1 = createHttpResource(TEST_BINARY_CONTENT);
        final File localFile2 = createExternalLocalFile(content2);
        final String externalLocation2 = localFile2.toURI().toString();

        final String rescLoc = createExternalContentResource(externalLocation1, "proxy", null);
        assertIsProxyBinary(rescLoc, externalLocation1, TEST_BINARY_CONTENT, "text/plain");

        updateExternalContentResource(rescLoc, externalLocation2, "proxy", null);
        assertIsProxyBinary(rescLoc, externalLocation2, content2, "application/octet-stream");
    }

    @Test
    public void testUpdateInternalToLocalFile() throws Exception {
        final String id = getRandomUniqueId();
        final String rescLoc;
        try (final CloseableHttpResponse response = execute(putDSMethod(id, "x", TEST_BINARY_CONTENT))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            rescLoc = getLocation(response);
        }

        final String content2 = "<doc>some more content</doc>";
        final File localFile = createExternalLocalFile(content2);
        final String externalLocation = localFile.toURI().toString();

        updateExternalContentResource(rescLoc, externalLocation, "proxy", "text/xml");
        assertIsProxyBinary(rescLoc, externalLocation, content2, "text/xml");
    }

    @Test
    public void testUpdateLocalFileToInternal() throws Exception {
        final File localFile = createExternalLocalFile(TEST_BINARY_CONTENT);
        final String externalLocation = localFile.toURI().toString();

        final String rescLoc = createExternalContentResource(externalLocation, "proxy", "text/plain");
        assertIsProxyBinary(rescLoc, externalLocation, TEST_BINARY_CONTENT, "text/plain");

        final String content2 = "<doc>some more content</doc>";
        final HttpPut put = new HttpPut(rescLoc);
        put.setEntity(new StringEntity(content2));
        put.setHeader(CONTENT_TYPE, "text/xml");
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(put));

        final HttpGet get = new HttpGet(rescLoc);
        try (final CloseableHttpResponse resp = execute(get)) {
            assertEquals(OK.getStatusCode(), getStatus(resp));
            assertNull(resp.getFirstHeader("Content-Location"));
            assertContentLength(resp, content2.length());
            assertBodyMatches(resp, content2);
            assertContentType(resp, "text/xml");
        }
    }

    @Test
    public void testUpdateInternalToHttpUri() throws Exception {
        final String id = getRandomUniqueId();
        final String rescLoc;
        try (final CloseableHttpResponse response = execute(putDSMethod(id, "x", TEST_BINARY_CONTENT))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            rescLoc = getLocation(response);
        }

        final String content2 = "<doc>some more content</doc>";
        final String externalLocation = createHttpResource(content2);

        updateExternalContentResource(rescLoc, externalLocation, "redirect", "text/xml");
        assertIsRedirectBinary(rescLoc, externalLocation, content2, "text/xml");
    }

    @Test
    public void testUpdateHttpUriToInternal() throws Exception {
        final String externalLocation = createHttpResource(TEST_BINARY_CONTENT);

        final String rescLoc = createExternalContentResource(externalLocation, "proxy", null);
        assertIsProxyBinary(rescLoc, externalLocation, TEST_BINARY_CONTENT, "text/plain");

        final String content2 = "<doc>some more content</doc>";
        final HttpPut put = new HttpPut(rescLoc);
        put.setEntity(new StringEntity(content2));
        put.setHeader(CONTENT_TYPE, "text/xml");
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(put));

        final HttpGet get = new HttpGet(rescLoc);
        try (final CloseableHttpResponse resp = execute(get)) {
            assertEquals(OK.getStatusCode(), getStatus(resp));
            assertNull(resp.getFirstHeader("Content-Location"));
            assertContentLength(resp, content2.length());
            assertBodyMatches(resp, content2);
            assertContentType(resp, "text/xml");
        }
    }

    @Test
    public void testLocalFileNotDeleted() throws Exception {
        final File localFile = createExternalLocalFile(TEST_BINARY_CONTENT);
        final String externalLocation = localFile.toURI().toString();

        final String rescLoc = createExternalContentResource(externalLocation, "proxy", "text/plain");
        final String id = StringUtils.substringAfterLast(rescLoc, "/");
        assertIsProxyBinary(rescLoc, externalLocation, TEST_BINARY_CONTENT, "text/plain");

        final HttpDelete delete = new HttpDelete(rescLoc);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(delete));
        assertDeleted(id);
        assertTrue("External binary must exist after resource deletion", localFile.exists());

        final HttpDelete deleteTomb = new HttpDelete(rescLoc + "/" + FedoraTypes.FCR_TOMBSTONE);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteTomb));
        assertTrue("External binary must exist after deleting tombstone", localFile.exists());
    }

    private HttpPut setupExternalContentPut(final String externalLocation, final String handling,
            final String contentType) {
        final String id = getRandomUniqueId();
        final HttpPut httpPut = putObjMethod(id);
        httpPut.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        httpPut.addHeader(LINK, getExternalContentLinkHeader(externalLocation, handling, contentType));
        return httpPut;
    }

    private String createExternalContentResource(final String externalLocation, final String handling,
            final String contentType) throws IOException {
        final HttpPut httpPut = setupExternalContentPut(externalLocation, handling, contentType);

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));

            return getLocation(response);
        }
    }

    private void updateExternalContentResource(final String rescLoc, final String externalLocation,
            final String handling, final String contentType) throws IOException {
        final HttpPut httpPut = new HttpPut(rescLoc);
        httpPut.addHeader(LINK, getExternalContentLinkHeader(externalLocation, handling, contentType));
        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }
    }

    private void assertIsProxyBinary(final String rescLocation, final String expectedLocation,
            final String expectedContent, final String expectedType) throws IOException {
        final HttpGet get = new HttpGet(rescLocation);
        try (final CloseableHttpResponse resp = execute(get)) {
            assertEquals(OK.getStatusCode(), getStatus(resp));
            assertContentLocation(resp, expectedLocation);
            assertContentLength(resp, expectedContent.length());
            assertBodyMatches(resp, expectedContent);
            if (expectedType != null) {
                assertContentType(resp, expectedType);
            }
        }
    }

    private void assertIsRedirectBinary(final String rescLocation, final String expectedLocation,
            final String expectedContent, final String expectedType) throws IOException {
        final HttpGet get = new HttpGet(rescLocation);
        try (final CloseableHttpResponse resp = noFollowClient.execute(get)) {
            assertEquals(TEMPORARY_REDIRECT.getStatusCode(), getStatus(resp));
            assertLocation(resp, expectedLocation);
            if (expectedType != null) {
                assertContentType(resp, expectedType);
            }
        }

        if (expectedContent != null) {
            // Follow redirect to the content
            try (final CloseableHttpResponse resp = execute(get)) {
                assertBodyMatches(resp, expectedContent);
                assertContentLength(resp, expectedContent.length());
            }
        }
    }

    private String createHttpResource(final String content) throws Exception {
        return createHttpResource("text/plain", content);
    }

    private String createHttpResource(final String contentType, final String content) throws Exception {
        final HttpPost method = postObjMethod();
        method.addHeader(CONTENT_TYPE, contentType);
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        method.setEntity(new StringEntity(content));

        // Make an external remote URI.
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals(SC_CREATED, getStatus(response));
            return getLocation(response);
        }
    }

    private void assertBodyContains(final CloseableHttpResponse response, final String expected) throws IOException {
        final String body = IOUtils.toString(response.getEntity().getContent(), UTF_8);
        assertTrue("Expected response to contain '" + expected + "' but was '" + body + "'",
                body.contains(expected));
    }

    private void assertBodyMatches(final CloseableHttpResponse response, final String expected) throws IOException {
        final String body = IOUtils.toString(response.getEntity().getContent(), UTF_8);
        assertEquals("Response body did not match the expected value", expected, body);
    }

    private void assertContentLength(final CloseableHttpResponse response, final long expectedLength) {
        assertEquals("Content-length header did not match", expectedLength, Long.parseLong(response
                .getFirstHeader(CONTENT_LENGTH).getValue()));
    }

    private void assertContentType(final CloseableHttpResponse response, final String expected) {
        assertEquals("Content-type header did not match", expected, response.getFirstHeader(CONTENT_TYPE).getValue());
    }

    private void assertContentLocation(final CloseableHttpResponse response, final String expectedLoc) {
        assertEquals("Content location header did not match", expectedLoc, getContentLocation(response));
    }

    private void assertLocation(final CloseableHttpResponse response, final String expectedLoc) {
        assertEquals("Location header did not match", expectedLoc, getLocation(response));
    }
}
