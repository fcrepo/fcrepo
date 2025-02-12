/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE_HEADERS;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.newResourceHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchCreationHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchModificationHeaders;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.UpdateNonRdfSourceHeadersOperation;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;

/**
 * @author mikejritter
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UpdateNonRdfSourceHeadersPersisterTest {
    private static final FedoraId RESOURCE_ID = FedoraId.create("info:fedora/parent/child");
    private static final FedoraId ROOT_RESOURCE_ID = FedoraId.create("info:fedora/parent");
    private static final String USER_PRINCIPAL = "fedoraUser";

    @Mock
    private UpdateNonRdfSourceHeadersOperation operation;

    @Mock
    private OcflObjectSession session;

    @Mock
    private FedoraOcflMapping mapping;

    @Mock
    private FedoraToOcflObjectIndex index;

    @Mock
    private OcflPersistentStorageSession psSession;

    @Captor
    private ArgumentCaptor<ResourceHeaders> headersCaptor;

    private UpdateNonRdfSourceHeadersPersister persister;

    @Mock
    private Transaction transaction;

    @BeforeEach
    public void setup() throws Exception {
        when(psSession.findOrCreateSession(anyString())).thenReturn(session);
        when(index.getMapping(eq(transaction), any())).thenReturn(mapping);
        when(operation.getType()).thenReturn(UPDATE_HEADERS);
        when(operation.getTransaction()).thenReturn(transaction);

        persister = new UpdateNonRdfSourceHeadersPersister(index);
    }

    @Test
    public void testHandle() {
        assertTrue(persister.handle(operation));
        final NonRdfSourceOperation badOperation = mock(NonRdfSourceOperation.class);
        assertFalse(persister.handle(badOperation));
    }

    @Test
    public void testPersistHeaders() {
        final var now = Instant.now();
        final var user = "some-user";
        final var objectId = "object-id";

        when(operation.getResourceId()).thenReturn(RESOURCE_ID);
        when(mapping.getOcflObjectId()).thenReturn(objectId);

        // setup headers
        final var headers = newResourceHeaders(ROOT_RESOURCE_ID, RESOURCE_ID, BASIC_CONTAINER.toString());
        touchCreationHeaders(headers, USER_PRINCIPAL);
        touchModificationHeaders(headers, USER_PRINCIPAL);
        when(session.readHeaders(anyString())).thenReturn(new ResourceHeadersAdapter(headers).asStorageHeaders());

        when(operation.getCreatedDate()).thenReturn(now);
        when(operation.getLastModifiedDate()).thenReturn(now);
        when(operation.getCreatedBy()).thenReturn(user);
        when(operation.getLastModifiedBy()).thenReturn(user);

        persister.persist(psSession, operation);

        verify(session).writeHeaders(headersCaptor.capture());

        final var resultHeaders = headersCaptor.getValue();
        assertEquals(now, resultHeaders.getCreatedDate());
        assertEquals(now, resultHeaders.getLastModifiedDate());
        assertEquals(user, resultHeaders.getCreatedBy());
        assertEquals(user, resultHeaders.getLastModifiedBy());
    }

}