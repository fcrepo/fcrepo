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
package org.fcrepo.kernel.impl.models;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import javax.inject.Inject;

import java.util.UUID;

import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentSessionClosedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test for ResourceHelper
 * @author whikloj
 * @since 6.0.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/containmentIndexTest.xml")
public class ResourceHelperImplTest {

    @Mock
    private PersistentStorageSessionManager sessionManager;

    @Mock
    private PersistentStorageSession psSession;

    @Mock
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

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        fedoraIdStr = FEDORA_ID_PREFIX + "/" + UUID.randomUUID().toString();
        fedoraId = FedoraId.create(fedoraIdStr);
        fedoraMementoIdStr = fedoraIdStr + "/fcr:versions/20000102120000";
        fedoraMementoId = FedoraId.create(fedoraMementoIdStr);

        sessionId = UUID.randomUUID().toString();
        when(mockTx.getId()).thenReturn(sessionId);

        resourceHelper = new ResourceHelperImpl();

        setField(resourceHelper, "persistentStorageSessionManager", sessionManager);
        setField(resourceHelper, "containmentIndex", containmentIndex);

        when(sessionManager.getSession(sessionId)).thenReturn(psSession);
        when(sessionManager.getReadOnlySession()).thenReturn(psSession);
    }

    @Test
    public void doesResourceExist_Exists_WithSession() throws Exception {
        containmentIndex.addContainedBy(mockTx.getId(), rootId, fedoraId);
        final boolean answerIn = resourceHelper.doesResourceExist(mockTx, fedoraId, false);
        assertTrue(answerIn);
        final boolean answerOut = resourceHelper.doesResourceExist(null, fedoraId, false);
        assertFalse(answerOut);
    }

    @Test
    public void doesResourceExist_Exists_Description_WithSession() {
        containmentIndex.addContainedBy(mockTx.getId(), rootId, fedoraId);
        final FedoraId descId = fedoraId.asDescription();
        final boolean answerIn = resourceHelper.doesResourceExist(mockTx, descId, false);
        assertTrue(answerIn);
        final boolean answerOut = resourceHelper.doesResourceExist(null, descId, false);
        assertFalse(answerOut);
    }

    @Test
    public void doesResourceExist_Exists_WithoutSession() throws Exception {
        containmentIndex.addContainedBy(mockTx.getId(), rootId, fedoraId);
        containmentIndex.commitTransaction(mockTx.getId());
        final boolean answer = resourceHelper.doesResourceExist(null, fedoraId, false);
        assertTrue(answer);
    }

    @Test
    public void doesResourceExist_Exists_Description_WithoutSession() {
        containmentIndex.addContainedBy(mockTx.getId(), rootId, fedoraId);
        containmentIndex.commitTransaction(mockTx.getId());
        final FedoraId descId = fedoraId.asDescription();
        final boolean answer = resourceHelper.doesResourceExist(null, descId, false);
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
        final boolean answer = resourceHelper.doesResourceExist(null, fedoraId, false);
        assertFalse(answer);
    }

    @Test
    public void doesResourceExist_DoesntExists_Description_WithoutSession() {
        final FedoraId descId = fedoraId.asDescription();
        final boolean answer = resourceHelper.doesResourceExist(null, descId, false);
        assertFalse(answer);
    }

    /**
     * Only Mementos go to the persistence layer.
     */
    @Test(expected = RepositoryRuntimeException.class)
    public void doesResourceExist_Exception_WithSession() throws Exception {
        when(psSession.getHeaders(fedoraMementoId, fedoraMementoId.getMementoInstant()))
                .thenThrow(PersistentSessionClosedException.class);
        resourceHelper.doesResourceExist(mockTx, fedoraMementoId, false);
    }

    /**
     * Only Mementos go to the persistence layer.
     */
    @Test(expected = RepositoryRuntimeException.class)
    public void doesResourceExist_Exception_WithoutSession() throws Exception {
        when(psSession.getHeaders(fedoraMementoId, fedoraMementoId.getMementoInstant()))
                .thenThrow(PersistentSessionClosedException.class);
        resourceHelper.doesResourceExist(null, fedoraMementoId, false);
    }

    /**
     * Test an item is not a ghost node because it exists.
     */
    @Test
    public void testGhostNodeFailure() {
        containmentIndex.addContainedBy(mockTx.getId(), rootId, fedoraId);
        // Inside the transaction the resource exists, so its not a ghost node.
        assertTrue(resourceHelper.doesResourceExist(mockTx, fedoraId, false));
        assertFalse(resourceHelper.isGhostNode(mockTx, fedoraId));
        // Outside the transaction the resource does not exist.
        assertFalse(resourceHelper.doesResourceExist(null, fedoraId, false));
        // Because there are no other items it is not a ghost node.
        assertFalse(resourceHelper.isGhostNode(null, fedoraId));

        containmentIndex.commitTransaction(mockTx.getId());

        // Now it exists outside the transaction.
        assertTrue(resourceHelper.doesResourceExist(null, fedoraId, false));
        // So it can't be a ghost node.
        assertFalse(resourceHelper.isGhostNode(null, fedoraId));
    }

    /**
     * Test that when the resource that does exist shares the ID of a resource that does not, then we have a ghost node.
     */
    @Test
    public void testGhostNodeSuccess() {
        final var resourceId = fedoraId.resolve("the/child/path");
        containmentIndex.addContainedBy(mockTx.getId(), rootId, resourceId);
        assertTrue(resourceHelper.doesResourceExist(mockTx, resourceId, false));
        assertFalse(resourceHelper.doesResourceExist(mockTx, fedoraId, false));
        assertTrue(resourceHelper.isGhostNode(mockTx, fedoraId));
        assertFalse(resourceHelper.doesResourceExist(null, resourceId, false));
        assertFalse(resourceHelper.doesResourceExist(null, fedoraId, false));
        assertFalse(resourceHelper.isGhostNode(null, fedoraId));

        containmentIndex.commitTransaction(mockTx.getId());

        assertTrue(resourceHelper.doesResourceExist(null, resourceId, false));
        assertFalse(resourceHelper.doesResourceExist(null, fedoraId,false));
        assertTrue(resourceHelper.isGhostNode(null, fedoraId));
    }
}
