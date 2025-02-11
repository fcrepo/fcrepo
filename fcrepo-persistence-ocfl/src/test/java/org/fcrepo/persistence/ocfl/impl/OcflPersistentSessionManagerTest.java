/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * Test class for {@link OcflPersistentSessionManager}
 *
 * @author dbernstein
 */
@ExtendWith(MockitoExtension.class)
public class OcflPersistentSessionManagerTest {
    @TempDir
    public Path tempFolder;

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

    @Mock
    private OcflPropsConfig ocflPropsConfig;

    @BeforeEach
    public void setUp() throws IOException {
        this.sessionManager = new OcflPersistentSessionManager();
        when(transaction.getId()).thenReturn(testSessionId);
        readWriteSession = this.sessionManager.getSession(transaction);
        setField(sessionManager, "objectSessionFactory", objectSessionFactory);
        setField(sessionManager, "ocflIndex", index);
        setField(sessionManager, "ocflPropsConfig", ocflPropsConfig);
        readOnlySession = this.sessionManager.getReadOnlySession();
    }

    @Test
    public void testUnsupportedOperationOnUnrecognizedOperation() throws Exception {
        assertThrows(UnsupportedOperationException.class, () -> {
            readWriteSession.persist(mockOperation);
        });
    }

    @Test
    public void testPersistNoSession() throws Exception {
        assertThrows(PersistentStorageException.class, () -> {
            readOnlySession.persist(mockOperation);
        });
    }

    @Test
    public void testNullSessionId() {
        assertThrows(IllegalArgumentException.class, () -> {
            this.sessionManager.getSession(null);
        });
    }

    @Test
    public void removeSession() {
        final var session = sessionManager.removeSession(testSessionId);
        assertSame(readWriteSession, session);
        assertNull(sessionManager.removeSession(testSessionId));
    }

    @Test
    public void clearAllSessions() throws Exception {
        final var stagingPath = tempFolder.resolve("staging");
        final var stagedPath = stagingPath.resolve("path/to/staged");
        Files.createDirectories(stagedPath);
        Files.createFile(stagedPath.resolve("file1.txt"));
        when(ocflPropsConfig.getFedoraOcflStaging()).thenReturn(stagingPath);

        sessionManager.clearAllSessions();

        verify(index).clearAllTransactions();
        try (DirectoryStream<Path> directory = Files.newDirectoryStream(stagingPath)) {
            assertFalse(directory.iterator().hasNext(), "Staging directory must be empty");
        }
    }

}
