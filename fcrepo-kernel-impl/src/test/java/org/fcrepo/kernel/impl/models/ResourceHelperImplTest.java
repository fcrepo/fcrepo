/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.models;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import jakarta.inject.Inject;

import java.util.UUID;

import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.ReadOnlyTransaction;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.impl.TestTransactionHelper;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentSessionClosedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test for ResourceHelper
 * @author whikloj
 * @since 6.0.0
 */
@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@ContextConfiguration("/containmentIndexTest.xml")
public class ResourceHelperImplTest {

    @Mock
    private PersistentStorageSessionManager sessionManager;

    @Mock
    private PersistentStorageSession psSession;

    private Transaction mockTx;

    @Inject
    private ContainmentIndex containmentIndex;

    @InjectMocks
    private ResourceHelperImpl resourceHelper;

    private String fedoraIdStr;

    private String sessionId;

    private final FedoraId rootId = FedoraId.getRepositoryRootId();

    private FedoraId fedoraId;

    private String fedoraMementoIdStr;

    private FedoraId fedoraMementoId;

    private Transaction readOnlyTx;

    @BeforeEach
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        fedoraIdStr = FEDORA_ID_PREFIX + "/" + UUID.randomUUID().toString();
        fedoraId = FedoraId.create(fedoraIdStr);
        fedoraMementoIdStr = fedoraIdStr + "/fcr:versions/20000102120000";
        fedoraMementoId = FedoraId.create(fedoraMementoIdStr);

        sessionId = UUID.randomUUID().toString();
        mockTx = TestTransactionHelper.mockTransaction(sessionId, false);

        resourceHelper = new ResourceHelperImpl();

        setField(resourceHelper, "persistentStorageSessionManager", sessionManager);
        setField(resourceHelper, "containmentIndex", containmentIndex);

        when(sessionManager.getSession(mockTx)).thenReturn(psSession);
        when(sessionManager.getReadOnlySession()).thenReturn(psSession);

        readOnlyTx = ReadOnlyTransaction.INSTANCE;
    }

    @Test
    public void doesResourceExist_Exists_WithSession() throws Exception {
        containmentIndex.addContainedBy(mockTx, rootId, fedoraId);
        final boolean answerIn = resourceHelper.doesResourceExist(mockTx, fedoraId, false);
        assertTrue(answerIn);
        final boolean answerOut = resourceHelper.doesResourceExist(readOnlyTx, fedoraId, false);
        assertFalse(answerOut);
    }

    @Test
    public void doesResourceExist_Exists_Description_WithSession() {
        containmentIndex.addContainedBy(mockTx, rootId, fedoraId);
        final FedoraId descId = fedoraId.asDescription();
        final boolean answerIn = resourceHelper.doesResourceExist(mockTx, descId, false);
        assertTrue(answerIn);
        final boolean answerOut = resourceHelper.doesResourceExist(readOnlyTx, descId, false);
        assertFalse(answerOut);
    }

    @Test
    public void doesResourceExist_Exists_WithoutSession() throws Exception {
        containmentIndex.addContainedBy(mockTx, rootId, fedoraId);
        containmentIndex.commitTransaction(mockTx);
        final boolean answer = resourceHelper.doesResourceExist(readOnlyTx, fedoraId, false);
        assertTrue(answer);
    }

    @Test
    public void doesResourceExist_Exists_Description_WithoutSession() {
        containmentIndex.addContainedBy(mockTx, rootId, fedoraId);
        containmentIndex.commitTransaction(mockTx);
        final FedoraId descId = fedoraId.asDescription();
        final boolean answer = resourceHelper.doesResourceExist(readOnlyTx, descId, false);
        assertTrue(answer);
    }

    @Test
    public void doesResourceExist_DoesntExist_WithSession() throws Exception {
        final boolean answer = resourceHelper.doesResourceExist(mockTx, fedoraId, false);
        assertFalse(answer);
    }

    @Test
    public void doesResourceExist_DoesntExists_Description_WithSession() {
        final FedoraId descId = fedoraId.asDescription();
        final boolean answer = resourceHelper.doesResourceExist(mockTx, descId, false);
        assertFalse(answer);
    }

    @Test
    public void doesResourceExist_DoesntExist_WithoutSession() throws Exception {
        final boolean answer = resourceHelper.doesResourceExist(readOnlyTx, fedoraId, false);
        assertFalse(answer);
    }

    @Test
    public void doesResourceExist_DoesntExists_Description_WithoutSession() {
        final FedoraId descId = fedoraId.asDescription();
        final boolean answer = resourceHelper.doesResourceExist(readOnlyTx, descId, false);
        assertFalse(answer);
    }

    /**
     * Only Mementos go to the persistence layer.
     */
    @Test
    public void doesResourceExist_Exception_WithSession() throws Exception {
        when(psSession.getHeaders(fedoraMementoId, fedoraMementoId.getMementoInstant()))
                .thenThrow(PersistentSessionClosedException.class);
        assertThrows(RepositoryRuntimeException.class,
                () -> resourceHelper.doesResourceExist(mockTx, fedoraMementoId, false)
        );
    }

    /**
     * Only Mementos go to the persistence layer.
     */
    @Test
    public void doesResourceExist_Exception_WithoutSession() throws Exception {
        when(psSession.getHeaders(fedoraMementoId, fedoraMementoId.getMementoInstant()))
                .thenThrow(PersistentSessionClosedException.class);
        assertThrows(RepositoryRuntimeException.class,
                () -> resourceHelper.doesResourceExist(readOnlyTx, fedoraMementoId, false)
        );
    }

    /**
     * Test an item is not a ghost node because it exists.
     */
    @Test
    public void testGhostNodeFailure() {
        containmentIndex.addContainedBy(mockTx, rootId, fedoraId);
        // Inside the transaction the resource exists, so its not a ghost node.
        assertTrue(resourceHelper.doesResourceExist(mockTx, fedoraId, false));
        assertFalse(resourceHelper.isGhostNode(mockTx, fedoraId));
        // Outside the transaction the resource does not exist.
        assertFalse(resourceHelper.doesResourceExist(readOnlyTx, fedoraId, false));
        // Because there are no other items it is not a ghost node.
        assertFalse(resourceHelper.isGhostNode(readOnlyTx, fedoraId));

        containmentIndex.commitTransaction(mockTx);

        // Now it exists outside the transaction.
        assertTrue(resourceHelper.doesResourceExist(readOnlyTx, fedoraId, false));
        // So it can't be a ghost node.
        assertFalse(resourceHelper.isGhostNode(readOnlyTx, fedoraId));
    }

    /**
     * Test that when the resource that does exist shares the ID of a resource that does not, then we have a ghost node.
     */
    @Test
    public void testGhostNodeSuccess() {
        final var resourceId = fedoraId.resolve("the/child/path");
        containmentIndex.addContainedBy(mockTx, rootId, resourceId);
        assertTrue(resourceHelper.doesResourceExist(mockTx, resourceId, false));
        assertFalse(resourceHelper.doesResourceExist(mockTx, fedoraId, false));
        assertTrue(resourceHelper.isGhostNode(mockTx, fedoraId));
        assertFalse(resourceHelper.doesResourceExist(readOnlyTx, resourceId, false));
        assertFalse(resourceHelper.doesResourceExist(readOnlyTx, fedoraId, false));
        assertFalse(resourceHelper.isGhostNode(readOnlyTx, fedoraId));

        containmentIndex.commitTransaction(mockTx);

        assertTrue(resourceHelper.doesResourceExist(readOnlyTx, resourceId, false));
        assertFalse(resourceHelper.doesResourceExist(readOnlyTx, fedoraId,false));
        assertTrue(resourceHelper.isGhostNode(readOnlyTx, fedoraId));
    }
}
