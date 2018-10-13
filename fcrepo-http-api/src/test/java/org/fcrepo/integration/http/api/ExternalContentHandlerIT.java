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
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LOCATION;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.BAD_GATEWAY;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.TEMPORARY_REDIRECT;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_CREATED;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.junit.Test;

/**
 * @author whikloj
 * @since 2018-07-10
 */
public class ExternalContentHandlerIT extends AbstractResourceIT {

    private static final String NON_RDF_SOURCE_LINK_HEADER = "<" + NON_RDF_SOURCE.getURI() + ">;rel=\"type\"";

    private static final String WANT_DIGEST = "Want-Digest";

    private static final String DIGEST = "Digest";

    private static final String TEST_BINARY_CONTENT = "01234567890123456789012345678901234567890123456789";

    private static final String TEST_SHA_DIGEST_HEADER_VALUE = "sha=9578f951955d37f20b601c26591e260c1e5389bf";

    private static final String TEST_MD5_DIGEST_HEADER_VALUE = "md5=baed005300234f3d1503c50a48ce8e6f";

    @Test
    public void testRemoteUriContentType() throws Exception {
        final HttpPost method = postObjMethod();
        method.addHeader(CONTENT_TYPE, "audio/ogg");
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        method.setEntity(new StringEntity("xyz"));
        final String external_location;
        final String final_location = getRandomUniqueId();

        // Make an external remote URI.
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals(SC_CREATED, getStatus(response));
            external_location = getLocation(response);
        }
        // Make an external content resource proxying the above URI.
        final HttpPut put = putObjMethod(final_location);
        put.addHeader(LINK, getExternalContentLinkHeader(external_location, "proxy", null));
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(SC_CREATED, getStatus(response));
        }
        // Get the external content proxy resource.
        try (final CloseableHttpResponse response = execute(getObjMethod(final_location))) {
            assertEquals(SC_OK, getStatus(response));
            assertEquals("audio/ogg", response.getFirstHeader(CONTENT_TYPE).getValue());
            assertEquals(external_location, response.getFirstHeader(CONTENT_LOCATION).getValue());
        }

    }

    @Test
    public void testExternalDatastreamProxyWithWantDigestForLocalFile() throws IOException {

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
    public void testExternalDatastreamCopyWithWantDigestForLocalFile() throws IOException {

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
    public void testExternalDatastreamProxyWithWantDigest() throws IOException {

        final String dsId = getRandomUniqueId();
        createDatastream(dsId, "x", TEST_BINARY_CONTENT);

        final String dsUrl = serverAddress + dsId + "/x";

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
    public void testExternalDatastreamCopyWithWantDigest() throws IOException {

        final String dsId = getRandomUniqueId();
        createDatastream(dsId, "x", TEST_BINARY_CONTENT);

        final String dsUrl = serverAddress + dsId + "/x";

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
    public void testExternalDatastreamProxyWithWantDigestMultipleForLocalFile() throws IOException {

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
            assertEquals(fileUri, getContentLocation(response));
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
            assertEquals(fileUri, getContentLocation(response));
            assertTrue(response.getHeaders(DIGEST).length > 0);

            final String digesterHeaderValue = response.getHeaders(DIGEST)[0].getValue();
            assertTrue("SHA-1 Fixity Checksum doesn't match",
                    digesterHeaderValue.contains(TEST_SHA_DIGEST_HEADER_VALUE));
            assertTrue("MD5 fixity checksum doesn't match",
                    digesterHeaderValue.contains(TEST_MD5_DIGEST_HEADER_VALUE));
        }
    }

    private File createExternalLocalFile(final String content) {
        final File externalFile;
        try {
            externalFile = File.createTempFile("binary", ".txt");
            externalFile.deleteOnExit();
            try (final FileWriter fw = new FileWriter(externalFile)) {
                fw.write(content);
            }
            return externalFile;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
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
    public void testHeadExternalDatastreamRedirect() throws IOException, ParseException {

        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.addHeader(LINK, getExternalContentLinkHeader("http://example.com/test", "redirect", "image/jpeg"));
        assertEquals(CREATED.getStatusCode(), getStatus(put));

        // Configure HEAD request to NOT follow redirects
        final HttpHead headObjMethod = headObjMethod(id);
        final RequestConfig.Builder requestConfig = RequestConfig.custom();
        requestConfig.setRedirectsEnabled(false);
        headObjMethod.setConfig(requestConfig.build());

        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            assertEquals(TEMPORARY_REDIRECT.getStatusCode(), response.getStatusLine().getStatusCode());
            assertEquals("http://example.com/test", getLocation(response));
            assertEquals("bytes", response.getFirstHeader("Accept-Ranges").getValue());
            assertEquals("0", response.getFirstHeader("Content-Length").getValue());
            final ContentDisposition disposition =
                    new ContentDisposition(response.getFirstHeader(CONTENT_DISPOSITION).getValue());
            assertEquals("attachment", disposition.getType());
        }
    }

    @Test
    public void testGetExternalDatastream() throws IOException, ParseException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.addHeader(LINK, getExternalContentLinkHeader("http://example.com/test", "redirect", "image/jpeg"));
        assertEquals(CREATED.getStatusCode(), getStatus(put));

        // Configure HEAD request to NOT follow redirects
        final HttpGet getObjMethod = getObjMethod(id);
        final RequestConfig.Builder requestConfig = RequestConfig.custom();
        requestConfig.setRedirectsEnabled(false);
        getObjMethod.setConfig(requestConfig.build());

        try (final CloseableHttpResponse response = execute(getObjMethod)) {
            assertEquals(TEMPORARY_REDIRECT.getStatusCode(), response.getStatusLine().getStatusCode());
            assertEquals("http://example.com/test", getLocation(response));
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
            assertEquals(dsUrl, getLocation(response));
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
    public void testRedirectWithWantDigest() throws IOException {

        final String dsId = getRandomUniqueId();
        createDatastream(dsId, "x", TEST_BINARY_CONTENT);

        final String dsUrl = serverAddress + dsId + "/x";

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
    public void testExternalMessageBodyRedirect() throws IOException {

        // we need a client that won't automatically follow redirects
        try (final CloseableHttpClient noFollowClient = HttpClientBuilder.create().disableRedirectHandling()
                .build()) {

            final String id = getRandomUniqueId();
            final HttpPut httpPut = putObjMethod(id);
            httpPut.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
            httpPut.addHeader(LINK, getExternalContentLinkHeader("http://www.example.com/file", "redirect", null));

            try (final CloseableHttpResponse response = execute(httpPut)) {
                assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
                final HttpGet get = new HttpGet(getLocation(response));
                try (final CloseableHttpResponse getResponse = noFollowClient.execute(get)) {
                    assertEquals(TEMPORARY_REDIRECT.getStatusCode(), getStatus(getResponse));
                    assertEquals("http://www.example.com/file", getLocation(getResponse));
                }
            }
        }
    }

    @Test
    public void testExternalMessageBodyCopyLocalFile() throws Exception {
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
                final String content = EntityUtils.toString(getResponse.getEntity());
                assertEquals("Entity Data doesn't match original object!", entityStr, content);
                assertEquals("Content-Type is different from expected on External COPY", "text/plain",
                        response.getFirstHeader(CONTENT_TYPE).getValue());
            }
        }
    }

    @Test
    public void testExternalMessageBodyCopy() throws IOException {
        // create a random binary object
        final String copyPid = getRandomUniqueId();
        final String entityStr = "Hello there, this is the original object speaking.";
        final String copyLocation = serverAddress + copyPid + "/binary";
        assertEquals(CREATED.getStatusCode(), getStatus(putDSMethod(copyPid, "binary", entityStr)));

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
                final String content = EntityUtils.toString(getResponse.getEntity());
                assertEquals("Entity Data doesn't match original object!", entityStr, content);
                assertEquals("Content-Type is different from expected on External COPY", "text/plain",
                        response.getFirstHeader(CONTENT_TYPE).getValue());
            }
        }
    }

    @Test
    public void testExternalMessageBodyProxy() throws IOException {
        // Create a resource
        final HttpPost method = postObjMethod();
        final String entityStr = "Hello there, this is the original object speaking.";
        method.setEntity(new StringEntity(entityStr));

        final String origLocation;
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            origLocation = response.getFirstHeader("Location").getValue();
        }

        final String id = getRandomUniqueId();
        final HttpPut httpPut = putObjMethod(id);
        httpPut.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        httpPut.addHeader(LINK, getExternalContentLinkHeader(origLocation, "proxy", null));

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            final HttpGet get = new HttpGet(getLocation(response));
            try (final CloseableHttpResponse getResponse = execute(get)) {
                assertEquals(OK.getStatusCode(), getStatus(getResponse));
                assertEquals(origLocation, getContentLocation(getResponse));
                final String content = EntityUtils.toString(getResponse.getEntity());
                assertEquals("Entity Data doesn't match original object!", entityStr, content);
            }
        }
    }

    @Test
    public void testPostExternalContentProxy() throws Exception {
        // Create a resource
        final HttpPost method = postObjMethod();
        final String entityStr = "Hello there, this is the original object speaking.";
        method.setEntity(new StringEntity(entityStr));

        final String origLocation;
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            origLocation = response.getFirstHeader("Location").getValue();
        }

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
                assertEquals(origLocation, getContentLocation(getResponse));

                final String contentType = getResponse.getFirstHeader("Content-Type").getValue();
                assertEquals("text/plain", contentType);

                final String content = EntityUtils.toString(getResponse.getEntity());
                assertEquals("Entity Data doesn't match original object!", entityStr, content);
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
        }
    }

    @Test
    public void testUnsupportedHandlingTypeInExternalMessagePOST() throws IOException {

        final String id = getRandomUniqueId();
        final HttpPost httpPost = postObjMethod(id);
        httpPost.addHeader(LINK, getExternalContentLinkHeader("http://example.com/junk", "junk", "image/jpeg"));

        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals("Didn't get a BAD_REQUEST response!", BAD_REQUEST.getStatusCode(),
                    getStatus(response));
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
            assertEquals("Didn't get a BAD_REQUEST response!", BAD_GATEWAY.getStatusCode(),
                    getStatus(response));
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
            assertEquals("Didn't get a BAD_GATEWAY response!", BAD_GATEWAY.getStatusCode(),
                    getStatus(response));
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
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(),
                    getStatus(response));

            final HttpGet get = new HttpGet(getLocation(response));
            try (final CloseableHttpResponse getResponse = execute(get)) {
                assertEquals(BAD_GATEWAY.getStatusCode(), getStatus(getResponse));
            }
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
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(),
                    getStatus(response));

            final HttpGet get = new HttpGet(getLocation(response));
            try (final CloseableHttpResponse getResponse = execute(get)) {
                assertEquals(BAD_GATEWAY.getStatusCode(), getStatus(getResponse));
            }
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
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(),
                    getStatus(response));

            final HttpGet get = new HttpGet(getLocation(response));
            try (final CloseableHttpClient noFollowClient =
                    HttpClientBuilder.create().disableRedirectHandling().build();
                    final CloseableHttpResponse getResponse = noFollowClient.execute(get)) {
                assertEquals(TEMPORARY_REDIRECT.getStatusCode(), getStatus(getResponse));
            }
        }
    }
}
