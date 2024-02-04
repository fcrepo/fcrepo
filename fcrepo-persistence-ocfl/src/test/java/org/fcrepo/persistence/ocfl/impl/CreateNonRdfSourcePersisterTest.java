/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.ResourceHeaders;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author dbernstein
 * @since 6.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CreateNonRdfSourcePersisterTest {

    private NonRdfSourceOperation nonRdfSourceOperation;

    @Mock
    private OcflObjectSession session;

    @Captor
    private ArgumentCaptor<InputStream> userContentCaptor;

    @Captor
    private ArgumentCaptor<ResourceHeaders> headersCaptor;

    private ResourceHeadersImpl headers;

    @Mock
    private OcflPersistentStorageSession psSession;

    @Mock
    private Transaction transaction;

    private TestOcflObjectIndex index;

    private static final FedoraId RESOURCE_ID = FedoraId.create("info:fedora/parent/child");

    private static final FedoraId ROOT_RESOURCE_ID = FedoraId.create("info:fedora/parent");

    private static final String USER_PRINCIPAL = "fedoraUser";

    private static final String CONTENT_BODY = "this is some example content";

    private static final String CONTENT_SHA1 = "75e72e503d19628d1fd9067b60e6e79e17fc1ef4";

    private static final URI CONTENT_SHA1_URI = URI.create("urn:sha1:" + CONTENT_SHA1);

    private static final String EXTERNAL_URL = "http://example.com/file.txt";

    private static final String EXTERNAL_HANDLING = "proxy";

    private static final long EXTERNAL_CONTENT_SIZE = 526632L;

    private CreateNonRdfSourcePersister persister;

    private static final String SESSION_ID = "SOME-SESSION-ID";

    @BeforeEach
    public void setUp() throws Exception {
        index = new TestOcflObjectIndex();

        index.addMapping(transaction, ROOT_RESOURCE_ID, FedoraId.getRepositoryRootId(),
                ROOT_RESOURCE_ID.getFullId());

        headers = new ResourceHeadersImpl();
        headers.setArchivalGroup(false);

        nonRdfSourceOperation = mock(NonRdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
        when(nonRdfSourceOperation.getUserPrincipal()).thenReturn(USER_PRINCIPAL);
        when(nonRdfSourceOperation.getResourceId()).thenReturn(RESOURCE_ID);
        when(nonRdfSourceOperation.getContentSize()).thenReturn(-1L);
        when(nonRdfSourceOperation.getType()).thenReturn(CREATE);
        when(((CreateResourceOperation)nonRdfSourceOperation).getParentId()).thenReturn(ROOT_RESOURCE_ID);

        when(psSession.getHeaders(((CreateResourceOperation) nonRdfSourceOperation).getParentId(), null))
                .thenReturn(headers);
        when(psSession.getId()).thenReturn(SESSION_ID);

        persister = new CreateNonRdfSourcePersister(index);

        when(psSession.findOrCreateSession(anyString())).thenReturn(session);
    }

    @Test
    public void testNonRdfNewResource() throws Exception {
        final InputStream content = IOUtils.toInputStream(CONTENT_BODY, "UTF-8");

        when(nonRdfSourceOperation.getContentStream()).thenReturn(content);
        when(((CreateResourceOperation) nonRdfSourceOperation).getInteractionModel())
                .thenReturn(NON_RDF_SOURCE.toString());

        persister.persist(psSession, nonRdfSourceOperation);

        verify(session).writeResource(headersCaptor.capture(), userContentCaptor.capture());

        final InputStream userContent = userContentCaptor.getValue();
        assertEquals(CONTENT_BODY, IOUtils.toString(userContent, StandardCharsets.UTF_8));

        final var resultHeaders = headersCaptor.getValue();
        assertEquals(NON_RDF_SOURCE.toString(), resultHeaders.getInteractionModel());
        assertModificationHeadersSet(resultHeaders);
    }

    @Test
    public void testHandle() {
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

        persister.persist(psSession, nonRdfSourceOperation);

        verify(session).writeResource(headersCaptor.capture(), userContentCaptor.capture());

        final var resultHeaders = headersCaptor.getValue();

        assertEquals(NON_RDF_SOURCE.toString(), resultHeaders.getInteractionModel());
        assertEquals(EXTERNAL_HANDLING, resultHeaders.getExternalHandling());
        assertEquals(EXTERNAL_URL, resultHeaders.getExternalUrl());
        assertEquals(EXTERNAL_CONTENT_SIZE, resultHeaders.getContentSize());

        assertModificationHeadersSet(resultHeaders);
    }

    @Test
    public void testInternalWithDigest() throws Exception {
        // During write, ensure that input stream is consumed
        mockSessionWriteConsumeStream();

        final InputStream content = IOUtils.toInputStream(CONTENT_BODY, UTF_8);

        when(nonRdfSourceOperation.getContentDigests()).thenReturn(List.of(CONTENT_SHA1_URI));

        when(nonRdfSourceOperation.getContentStream()).thenReturn(content);
        when(((CreateResourceOperation) nonRdfSourceOperation).getInteractionModel())
                .thenReturn(NON_RDF_SOURCE.toString());

        persister.persist(psSession, nonRdfSourceOperation);

        // verify resource headers
        final var resultHeaders = headersCaptor.getValue();

        assertEquals(NON_RDF_SOURCE.toString(), resultHeaders.getInteractionModel());

        assertModificationHeadersSet(resultHeaders);
        assertTrue(resultHeaders.getDigests().contains(CONTENT_SHA1_URI),
                "Headers did not contain the provided sha1 digest");
    }

    @Test
    public void testInternalWithInvalidDigest() throws Exception {
        // During write, ensure that input stream is consumed
        mockSessionWriteConsumeStream();

        final InputStream content = IOUtils.toInputStream(CONTENT_BODY, UTF_8);

        when(nonRdfSourceOperation.getContentDigests()).thenReturn(asList(
                CONTENT_SHA1_URI, URI.create("urn:md5:baaaaaad")));

        when(nonRdfSourceOperation.getContentStream()).thenReturn(content);
        when(((CreateResourceOperation) nonRdfSourceOperation).getInteractionModel())
                .thenReturn(NON_RDF_SOURCE.toString());

        assertThrows(InvalidChecksumException.class, () -> {
            persister.persist(psSession, nonRdfSourceOperation);
        });
    }

    private void mockSessionWriteConsumeStream() throws Exception {
        doAnswer(invocation -> {
            // Consume the input stream
            IOUtils.toString((InputStream) invocation.getArgument(1), UTF_8);
            return null;
        }).when(session).writeResource(headersCaptor.capture(), any(InputStream.class));
    }

    private void assertModificationHeadersSet(final ResourceHeaders headers) {
        assertEquals(USER_PRINCIPAL, headers.getCreatedBy());
        assertNotNull(headers.getCreatedDate());
        assertEquals(USER_PRINCIPAL, headers.getLastModifiedBy());
        assertNotNull(headers.getLastModifiedDate());
    }
}
