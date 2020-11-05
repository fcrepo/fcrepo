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

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateVersionResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.api.exceptions.PersistentItemConflictException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.CommitType;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author pwinckles
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class CreateVersionPersisterTest {

    private CreateVersionPersister persister;

    private FedoraToOcflObjectIndex index;

    @Mock
    private OcflPersistentStorageSession session;

    @Captor
    private ArgumentCaptor<ResourceHeaders> headersCaptor;

    @Before
    public void setup() {
        index = new TestOcflObjectIndex();
        persister = new CreateVersionPersister(index);
    }

    @Test
    public void setCommitToNewVersionWhenNoChildOfAg() throws PersistentStorageException {
        final var resourceId = FedoraId.create("info:fedora/blah");
        final var ocflId = "blah";

        final var objectSession = addMapping(resourceId, ocflId);
        expectArchivalGroup(resourceId, false);

        persister.persist(session, operation(resourceId));

        verify(objectSession).commitType(CommitType.NEW_VERSION);
        verifyHeaders(objectSession);
    }

    @Test
    public void setCommitToNewVersionWhenAg() throws PersistentStorageException {
        final var resourceId = FedoraId.create("info:fedora/blah");
        final var ocflId = "blah";

        final var objectSession = addMapping(resourceId, ocflId);
        expectArchivalGroup(resourceId, true);

        persister.persist(session, operation(resourceId));

        verify(objectSession).commitType(CommitType.NEW_VERSION);
        verifyHeaders(objectSession);
    }

    @Test(expected = PersistentItemConflictException.class)
    public void forbidVersionCreationWhenChildOfAg() throws PersistentStorageException {
        final var resourceId = FedoraId.create("info:fedora/ag/blah");
        final var ocflId = "blah";

        final var objectSession = addMapping(resourceId, ocflId);
        expectArchivalGroup(resourceId, false);
        expectArchivalGroup(FedoraId.create("info:fedora/ag"), true);

        persister.persist(session, operation(resourceId));
    }

    @Test(expected = PersistentStorageException.class)
    public void failVersionCreationWhenNoOclfMapping() throws PersistentStorageException {
        final var resourceId = FedoraId.create("info:fedora/bogus");
        final var ocflId = "blah";

        addMapping(FedoraId.create("info:fedora/blah"), ocflId);
        expectArchivalGroup(resourceId, false);

        persister.persist(session, operation(resourceId));
    }

    private void expectArchivalGroup(final FedoraId resourceId, final boolean isAgChild)
            throws PersistentStorageException {
        final var headers = new ResourceHeadersImpl();
        headers.setArchivalGroup(isAgChild);
        when(session.getHeaders(resourceId, null)).thenReturn(headers);
    }

    private OcflObjectSession addMapping(final FedoraId resourceId, final String ocflId) {
        index.addMapping("not-used", resourceId, resourceId, ocflId);
        final var objectSession = mock(OcflObjectSession.class);
        when(session.findOrCreateSession(ocflId)).thenReturn(objectSession);
        doReturn(ResourceHeaders.builder().build()).when(objectSession)
                .readHeaders(resourceId.getResourceId());
        return objectSession;
    }

    private CreateVersionResourceOperation operation(final FedoraId resourceId) {
        final var operation = mock(CreateVersionResourceOperation.class);
        when(operation.getType()).thenReturn(ResourceOperationType.UPDATE);
        when(operation.getResourceId()).thenReturn(resourceId);
        return operation;
    }

    private void verifyHeaders(final OcflObjectSession objectSession) {
        verify(objectSession).writeHeaders(headersCaptor.capture());
        final var actualHeaders = headersCaptor.getValue();
        assertNotNull(actualHeaders.getStateToken());
        assertTrue("Timestamp should be within 1 second of now",
                Duration.between(actualHeaders.getLastModifiedDate(), Instant.now())
                        .compareTo(Duration.ofSeconds(1)) < 0);
    }

}
