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
package org.fcrepo.persistence.common;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.utils.ContentDigest.DIGEST_ALGORITHM;
import org.junit.Test;

/**
 * @author bbpennel
 */
public class MultiDigestInputStreamWrapperTest {

    private static final String CONTENT = "Something to digest";

    private static final String MD5 = "7afbf05666feeebe7fbbf1c9071584e6";
    private static final URI MD5_URI = URI.create("urn:md5:" + MD5);

    private static final String SHA1 = "23d51c61a578a8cb00c5eec6b29c12b7da15c8de";
    private static final URI SHA1_URI = URI.create("urn:sha1:" + SHA1);

    private static final String SHA512 =
            "051c93c9cfd1b0a238c858267789fddb4fd1500c957d0ae609ec2fc2c96b3db9edddde5374be7" +
            "3b664056ed6281a842041aa43a87fd4e0fbc1b6890676cead6d";
    private static final URI SHA512_URI = URI.create("urn:sha-512:" + SHA512);

    private static final String SHA512256 = "dea93ec79abc429b0065e73995a4fe0ddb7a3ec65f2b14139e75360a8ab66efc";
    private static final URI SHA512256_URI = URI.create("urn:sha-512/256:" + SHA512256);

    private final InputStream contentStream = new ByteArrayInputStream(CONTENT.getBytes());

    @Test
    public void checkFixity_SingleDigests_Success() throws Exception {
        final var digests = asList(SHA1_URI);
        final var wrapper = new MultiDigestInputStreamWrapper(contentStream, digests, null);

        // Read the stream to allow digest calculation
        IOUtils.toString(wrapper.getInputStream(), UTF_8);

        // Expect no failures
        wrapper.checkFixity();
    }

    @Test
    public void checkFixity_MultiDigests_Success() throws Exception {
        final var digests = asList(MD5_URI, SHA1_URI, SHA512_URI, SHA512256_URI);
        final var wrapper = new MultiDigestInputStreamWrapper(contentStream, digests, null);

        // Read the stream to allow digest calculation
        IOUtils.toString(wrapper.getInputStream(), UTF_8);

        // Expect no failures
        wrapper.checkFixity();
    }

    @Test
    public void checkFixity_NoDigests() throws Exception {
        final Collection<URI> digests = emptyList();
        final var wrapper = new MultiDigestInputStreamWrapper(contentStream, digests, null);

        // Read the stream to allow digest calculation
        IOUtils.toString(wrapper.getInputStream(), UTF_8);

        // Expect no failures
        wrapper.checkFixity();
    }

    @Test(expected = InvalidChecksumException.class)
    public void checkFixity_InvalidDigest() throws Exception {
        final var digests = asList(MD5_URI, URI.create("urn:sha1:totallybusted"), SHA512_URI);
        final var wrapper = new MultiDigestInputStreamWrapper(contentStream, digests, null);

        // Read the stream to allow digest calculation
        IOUtils.toString(wrapper.getInputStream(), UTF_8);

        // Expect no failures
        wrapper.checkFixity();
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void unsupportedDigestAlgorithm() throws Exception {
        final var digests = asList(URI.create("urn:yum:123456"), SHA512_URI);
        final var wrapper = new MultiDigestInputStreamWrapper(contentStream, digests, null);

        wrapper.getInputStream();
    }

    @Test
    public void checkFixity_BeforeRead() throws Exception {
        final var digests = asList(MD5_URI);
        final var wrapper = new MultiDigestInputStreamWrapper(contentStream, digests, null);

        // Expect no failures
        wrapper.checkFixity();
    }

    @Test
    public void checkFixity_OnlyWantDigest() throws Exception {
        final var wantDigests = asList(DIGEST_ALGORITHM.SHA1);
        final var wrapper = new MultiDigestInputStreamWrapper(contentStream, null, wantDigests);

        // Read the stream to allow digest calculation
        IOUtils.toString(wrapper.getInputStream(), UTF_8);

        // Expect no failures
        wrapper.checkFixity();
    }

    @Test
    public void checkFixity_OverlappingWantDigestAndProvided() throws Exception {
        final var digests = asList(SHA1_URI);
        final var wantDigests = asList(DIGEST_ALGORITHM.SHA1);
        final var wrapper = new MultiDigestInputStreamWrapper(contentStream, digests, wantDigests);

        // Read the stream to allow digest calculation
        IOUtils.toString(wrapper.getInputStream(), UTF_8);

        // Expect no failures
        wrapper.checkFixity();
    }

    @Test(expected = InvalidChecksumException.class)
    public void checkFixity_OverlappingWantDigestAndProvided_InvalidDigest() throws Exception {
        final var digests = asList(URI.create("urn:sha1:totallybusted"));
        final var wantDigests = asList(DIGEST_ALGORITHM.SHA1);
        final var wrapper = new MultiDigestInputStreamWrapper(contentStream, digests, wantDigests);

        // Read the stream to allow digest calculation
        IOUtils.toString(wrapper.getInputStream(), UTF_8);

        // Expect no failures
        wrapper.checkFixity();
    }

    @Test
    public void checkFixity_DifferentWantDigestAndProvided() throws Exception {
        final var digests = asList(SHA1_URI);
        final var wantDigests = asList(DIGEST_ALGORITHM.MD5);
        final var wrapper = new MultiDigestInputStreamWrapper(contentStream, digests, wantDigests);

        // Read the stream to allow digest calculation
        IOUtils.toString(wrapper.getInputStream(), UTF_8);

        // Expect no failures
        wrapper.checkFixity();
    }

    @Test
    public void getDigests_FromWantDigests() throws Exception {
        final var wantDigests = asList(DIGEST_ALGORITHM.SHA1, DIGEST_ALGORITHM.SHA512);
        final var wrapper = new MultiDigestInputStreamWrapper(contentStream, null, wantDigests);

        final var computed = wrapper.getDigests();
        assertTrue(computed.contains(SHA1_URI));
        assertTrue(computed.contains(SHA512_URI));
    }

    @Test
    public void getDigests_FromProvidedDigests() throws Exception {
        final var digests = asList(SHA1_URI, MD5_URI);
        final var wrapper = new MultiDigestInputStreamWrapper(contentStream, digests, null);

        final var computed = wrapper.getDigests();
        assertTrue(computed.contains(SHA1_URI));
        assertTrue(computed.contains(MD5_URI));
    }

    @Test
    public void getDigests_FromWantDigestsAndProvided() throws Exception {
        final var wantDigests = asList(DIGEST_ALGORITHM.SHA1, DIGEST_ALGORITHM.SHA512);
        final var digests = asList(SHA1_URI, MD5_URI);
        final var wrapper = new MultiDigestInputStreamWrapper(contentStream, digests, wantDigests);

        final var computed = wrapper.getDigests();
        assertTrue(computed.contains(SHA1_URI));
        assertTrue(computed.contains(SHA512_URI));
        assertTrue(computed.contains(MD5_URI));
    }

    @Test
    public void getDigests_AfterCheckFixity() throws Exception {
        final var digests = asList(SHA1_URI);
        final var wrapper = new MultiDigestInputStreamWrapper(contentStream, digests, null);

        IOUtils.toString(wrapper.getInputStream(), UTF_8);

        wrapper.checkFixity();

        final var computed = wrapper.getDigests();
        assertTrue(computed.contains(SHA1_URI));
    }

    @Test
    public void getDigests_FromConsumedStream() throws Exception {
        final var wantDigests = asList(DIGEST_ALGORITHM.SHA512);
        final var wrapper = new MultiDigestInputStreamWrapper(contentStream, null, wantDigests);

        IOUtils.toString(wrapper.getInputStream(), UTF_8);

        final var computed = wrapper.getDigests();
        assertTrue(computed.contains(SHA512_URI));
    }
}
