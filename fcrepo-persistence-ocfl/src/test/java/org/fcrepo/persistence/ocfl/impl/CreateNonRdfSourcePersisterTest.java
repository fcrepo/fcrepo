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
package org.fcrepo.persistence.ocfl.impl;

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.utils.ContentDigest.DIGEST_ALGORITHM;
import org.fcrepo.persistence.api.WriteOutcome;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.FileWriteOutcome;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.api.OcflObjectSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.RESOURCE_HEADER_EXTENSION;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.deserializeHeaders;
import static org.fcrepo.persistence.ocfl.impl.OcflPersistentStorageUtils.getInternalFedoraDirectory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * @author dbernstein
 * @since 6.0.0
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class CreateNonRdfSourcePersisterTest {

    private NonRdfSourceOperation nonRdfSourceOperation;

    @Mock
    private OcflObjectSession session;

    @Mock
    private FedoraOcflMapping mapping;

    @Mock
    private FedoraToOcflObjectIndex index;

    @Mock
    private FileWriteOutcome writeOutcome;

    @Captor
    private ArgumentCaptor<InputStream> userContentCaptor;

    @Captor
    private ArgumentCaptor<InputStream> headersIsCaptor;

    @Mock
    private ResourceHeaders headers;

    @Mock
    private OcflPersistentStorageSession psSession;

    private static final FedoraId RESOURCE_ID = FedoraId.create("info:fedora/parent/child");

    private static final FedoraId ROOT_RESOURCE_ID = FedoraId.create("info:fedora/parent");

    private static final String USER_PRINCIPAL = "fedoraUser";

    private static final String CONTENT_BODY = "this is some example content";

    private static final String CONTENT_SHA1 = "75e72e503d19628d1fd9067b60e6e79e17fc1ef4";

    private static final URI CONTENT_SHA1_URI = URI.create("urn:sha1:" + CONTENT_SHA1);

    private static final Long LOCAL_CONTENT_SIZE = Long.valueOf(CONTENT_BODY.length());

    private static final String EXTERNAL_URL = "http://example.com/file.txt";

    private static final String EXTERNAL_HANDLING = "proxy";

    private static final Long EXTERNAL_CONTENT_SIZE = 526632L;

    private CreateNonRdfSourcePersister persister;

    private static final String SESSION_ID = "SOME-SESSION-ID";

    @Before
    public void setUp() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("object-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(ROOT_RESOURCE_ID);

        when(session.write(anyString(), any(InputStream.class))).thenReturn(writeOutcome);
        when(session.getObjectDigestAlgorithm()).thenReturn(DIGEST_ALGORITHM.SHA1);

        nonRdfSourceOperation = mock(NonRdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
        when(nonRdfSourceOperation.getUserPrincipal()).thenReturn(USER_PRINCIPAL);
        when(nonRdfSourceOperation.getResourceId()).thenReturn(RESOURCE_ID);
        when(nonRdfSourceOperation.getContentSize()).thenReturn(null);
        when(nonRdfSourceOperation.getType()).thenReturn(CREATE);
        when(((CreateResourceOperation)nonRdfSourceOperation).getParentId()).thenReturn(ROOT_RESOURCE_ID);

        when(psSession.getHeaders(((CreateResourceOperation) nonRdfSourceOperation).getParentId(), null))
                .thenReturn(headers);
        when(psSession.getId()).thenReturn(SESSION_ID);

        when(writeOutcome.getContentSize()).thenReturn(LOCAL_CONTENT_SIZE);

        persister = new CreateNonRdfSourcePersister(index);

        when(psSession.findOrCreateSession(anyString())).thenReturn(session);
        when(index.getMapping(eq(SESSION_ID), any())).thenReturn(mapping);

    }

    @Test
    public void testNonRdfNewResource() throws Exception {

        final InputStream content = IOUtils.toInputStream(CONTENT_BODY, "UTF-8");

        when(nonRdfSourceOperation.getContentStream()).thenReturn(content);
        when(((CreateResourceOperation) nonRdfSourceOperation).getInteractionModel())
                .thenReturn(NON_RDF_SOURCE.toString());
        when(headers.isArchivalGroup()).thenReturn(false);

        persister.persist(psSession, nonRdfSourceOperation);

        // verify user content
        verify(session).write(eq("child"), userContentCaptor.capture());
        final InputStream userContent = userContentCaptor.getValue();
        assertEquals(CONTENT_BODY, IOUtils.toString(userContent, StandardCharsets.UTF_8));

        // verify resource headers
        final var resultHeaders = retrievePersistedHeaders("child");

        assertEquals(NON_RDF_SOURCE.toString(), resultHeaders.getInteractionModel());
        assertEquals(LOCAL_CONTENT_SIZE, resultHeaders.getContentSize());

        assertModificationHeadersSet(resultHeaders);
    }

    @Test
    public void testHandle(){
        assertTrue(this.persister.handle(this.nonRdfSourceOperation));
        final ResourceOperation badOperation = mock(ResourceOperation.class);
        when(badOperation.getType()).thenReturn(CREATE);
        assertFalse(this.persister.handle(badOperation));
    }

    @Test
    public void testNonRdfNewExternalBinary() throws Exception {

        when(nonRdfSourceOperation.getContentUri()).thenReturn(URI.create(EXTERNAL_URL));
        when(nonRdfSourceOperation.getExternalHandling()).thenReturn(EXTERNAL_HANDLING);
        when(nonRdfSourceOperation.getContentSize()).thenReturn(EXTERNAL_CONTENT_SIZE);
        when(((CreateResourceOperation) nonRdfSourceOperation).getInteractionModel()).thenReturn(NON_RDF_SOURCE
                .toString());
        when(headers.isArchivalGroup()).thenReturn(false);

        persister.persist(psSession, nonRdfSourceOperation);

        // verify resource headers
        final var resultHeaders = retrievePersistedHeaders("child");

        assertEquals(NON_RDF_SOURCE.toString(), resultHeaders.getInteractionModel());
        assertEquals(EXTERNAL_HANDLING, resultHeaders.getExternalHandling());
        assertEquals(EXTERNAL_URL, resultHeaders.getExternalUrl());
        assertEquals(EXTERNAL_CONTENT_SIZE, resultHeaders.getContentSize());

        assertModificationHeadersSet(resultHeaders);
    }

    @Test(expected = PersistentStorageException.class)
    public void testNonRdfContentSizeMismatch() throws Exception {

        final InputStream content = IOUtils.toInputStream(CONTENT_BODY, "UTF-8");

        when(nonRdfSourceOperation.getContentStream()).thenReturn(content);
        when(nonRdfSourceOperation.getContentSize()).thenReturn(99L);
        when(((CreateResourceOperation) nonRdfSourceOperation).getInteractionModel())
                .thenReturn(NON_RDF_SOURCE.toString());
        when(headers.isArchivalGroup()).thenReturn(false);

        persister.persist(psSession, nonRdfSourceOperation);
    }

    @Test
    public void testInternalWithDigest() throws Exception {
        // During write, ensure that input stream is consumed
        mockSessionWriteConsumeStream();

        final InputStream content = IOUtils.toInputStream(CONTENT_BODY, UTF_8);

        when(nonRdfSourceOperation.getContentDigests()).thenReturn(asList(CONTENT_SHA1_URI));
        when(writeOutcome.getDigests()).thenReturn(asList(CONTENT_SHA1_URI));

        when(nonRdfSourceOperation.getContentStream()).thenReturn(content);
        when(((CreateResourceOperation) nonRdfSourceOperation).getInteractionModel())
                .thenReturn(NON_RDF_SOURCE.toString());

        persister.persist(psSession, nonRdfSourceOperation);

        // verify content was written
        verify(session).write(eq("child"), any(InputStream.class));

        // verify resource headers
        final var resultHeaders = retrievePersistedHeaders("child");

        assertEquals(NON_RDF_SOURCE.toString(), resultHeaders.getInteractionModel());
        assertEquals(LOCAL_CONTENT_SIZE, resultHeaders.getContentSize());

        assertModificationHeadersSet(resultHeaders);
        assertTrue("Headers did not contain the provided sha1 digest",
                resultHeaders.getDigests().contains(CONTENT_SHA1_URI));
    }

    @Test(expected = InvalidChecksumException.class)
    public void testInternalWithInvalidDigest() throws Exception {
        // During write, ensure that input stream is consumed
        mockSessionWriteConsumeStream();

        final InputStream content = IOUtils.toInputStream(CONTENT_BODY, UTF_8);

        when(nonRdfSourceOperation.getContentDigests()).thenReturn(asList(
                CONTENT_SHA1_URI, URI.create("urn:md5:baaaaaad")));

        when(nonRdfSourceOperation.getContentStream()).thenReturn(content);
        when(((CreateResourceOperation) nonRdfSourceOperation).getInteractionModel())
                .thenReturn(NON_RDF_SOURCE.toString());

        persister.persist(psSession, nonRdfSourceOperation);
    }

    private void mockSessionWriteConsumeStream() throws Exception {
        when(session.write(eq("child"), any(InputStream.class))).thenAnswer(new Answer<WriteOutcome>() {
            @Override
            public WriteOutcome answer(final InvocationOnMock invocation) throws Throwable {
                // Consume the input stream
                IOUtils.toString((InputStream) invocation.getArgument(1), UTF_8);
                return writeOutcome;
            }
        });
    }

    private ResourceHeaders retrievePersistedHeaders(final String subpath) throws Exception {
        verify(session).write(eq(getInternalFedoraDirectory() + subpath + RESOURCE_HEADER_EXTENSION),
                headersIsCaptor.capture());
        final var headersIs = headersIsCaptor.getValue();
        return deserializeHeaders(headersIs);
    }

    private void assertModificationHeadersSet(final ResourceHeaders headers) {
        assertEquals(USER_PRINCIPAL, headers.getCreatedBy());
        assertNotNull(headers.getCreatedDate());
        assertEquals(USER_PRINCIPAL, headers.getLastModifiedBy());
        assertNotNull(headers.getLastModifiedDate());
    }
}
