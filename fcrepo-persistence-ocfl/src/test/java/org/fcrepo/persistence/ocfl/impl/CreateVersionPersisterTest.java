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

import org.fcrepo.kernel.api.operations.CreateVersionResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.api.CommitOption;
import org.fcrepo.persistence.api.exceptions.PersistentItemConflictException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.api.OcflObjectSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CreateVersionPersisterTest {

    private CreateVersionPersister persister;

    private FedoraToOcflObjectIndex index;

    @Mock
    private OcflPersistentStorageSession session;

    @Before
    public void setup() {
        index = new TestOcflObjectIndex();
        persister = new CreateVersionPersister(index);
    }

    @Test
    public void setCommitToNewVersionWhenNoChildOfAg() throws PersistentStorageException {
        final var resourceId = "info:fedora/blah";
        final var ocflId = "blah";

        final var objectSession = addMapping(resourceId, ocflId);
        expectArchivalGroup(resourceId, false);
        objectSession.setCommitOption(CommitOption.NEW_VERSION);

        persister.persist(session, operation(resourceId));
    }

    @Test
    public void setCommitToNewVersionWhenAg() throws PersistentStorageException {
        final var resourceId = "info:fedora/blah";
        final var ocflId = "blah";

        final var objectSession = addMapping(resourceId, ocflId);
        expectArchivalGroup(resourceId, true);
        objectSession.setCommitOption(CommitOption.NEW_VERSION);

        persister.persist(session, operation(resourceId));
    }

    @Test(expected = PersistentItemConflictException.class)
    public void failVersionCreationWhenChildOfAg() throws PersistentStorageException {
        final var resourceId = "info:fedora/ag/blah";

        expectArchivalGroup(resourceId, false);
        expectArchivalGroup("info:fedora/ag", true);

        persister.persist(session, operation(resourceId));
    }

    @Test(expected = PersistentStorageException.class)
    public void failVersionCreationWhenNoOclfMapping() throws PersistentStorageException {
        final var resourceId = "info:fedora/bogus";
        final var ocflId = "blah";

        addMapping("info:fedora/blah", ocflId);
        expectArchivalGroup(resourceId, false);

        persister.persist(session, operation(resourceId));
    }

    private void expectArchivalGroup(final String resourceId, final boolean isAgChild)
            throws PersistentStorageException {
        final var headers = new ResourceHeadersImpl();
        headers.setArchivalGroup(isAgChild);
        when(session.getHeaders(resourceId, null)).thenReturn(headers);
    }

    private OcflObjectSession addMapping(final String resourceId, final String ocflId) {
        index.addMapping("not-used", resourceId, resourceId, ocflId);
        final var objectSession = mock(OcflObjectSession.class);
        when(session.findOrCreateSession(ocflId)).thenReturn(objectSession);
        return objectSession;
    }

    private CreateVersionResourceOperation operation(final String resourceId) {
        final var operation = mock(CreateVersionResourceOperation.class);
        when(operation.getType()).thenReturn(ResourceOperationType.UPDATE);
        when(operation.getResourceId()).thenReturn(resourceId);
        return operation;
    }

}
