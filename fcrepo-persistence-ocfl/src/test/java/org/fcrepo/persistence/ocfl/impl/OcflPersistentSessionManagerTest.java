/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;

import java.io.IOException;

/**
 * Test class for {@link OcflPersistentSessionManager}
 *
 * @author dbernstein
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OcflPersistentSessionManagerTest {

    private OcflPersistentSessionManager sessionManager;

    private PersistentStorageSession readWriteSession;

    private PersistentStorageSession readOnlySession;

    private final String testSessionId = randomUUID().toString();

    @Mock
    private ResourceOperation mockOperation;

    @Mock
    private FedoraToOcflObjectIndex index;

    @Mock
    private OcflObjectSessionFactory objectSessionFactory;

    @Mock
    private Transaction transaction;

    @BeforeEach
    public void setUp() throws IOException {
        this.sessionManager = new OcflPersistentSessionManager();
        when(transaction.getId()).thenReturn(testSessionId);
        readWriteSession = this.sessionManager.getSession(transaction);
        setField(sessionManager, "objectSessionFactory", objectSessionFactory);
        setField(sessionManager, "ocflIndex", index);
        readOnlySession = this.sessionManager.getReadOnlySession();
    }

    @Test
    public void testUnsupportedOperationOnUnrecognizedOperation() throws Exception {
        assertThrows(UnsupportedOperationException.class, () -> readWriteSession.persist(mockOperation));
    }

    @Test
    public void testPersistNoSession() throws Exception {
        assertThrows(PersistentStorageException.class, () -> readOnlySession.persist(mockOperation));
    }

    @Test
    public void testNullSessionId() {
        assertThrows(IllegalArgumentException.class, () -> this.sessionManager.getSession(null));
    }

    @Test
    public void removeSession() {
        final var session = sessionManager.removeSession(testSessionId);
        assertSame(readWriteSession, session);
        assertNull(sessionManager.removeSession(testSessionId));
    }

}
