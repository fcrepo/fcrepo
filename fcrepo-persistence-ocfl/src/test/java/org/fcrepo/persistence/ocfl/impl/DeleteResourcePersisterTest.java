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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.Persister;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Delete Persister tests.
 * @author whikloj
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class DeleteResourcePersisterTest {

    @Mock
    private FedoraOCFLMapping mapping;

    @Mock
    private PersistentStorageSession storageSession;

    @Mock
    private OCFLObjectSession session;

    @Mock
    private ResourceOperation operation;

    private final Persister persister = new DeleteResourcePersister();

    @Test
    public void testDeleteSubPath() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getParentFedoraResourceId()).thenReturn("info:fedora/an-ocfl-object");
        when(operation.getResourceId()).thenReturn("info:fedora/an-ocfl-object/some-subpath");
        persister.persist(storageSession, session, operation, mapping);
        verify(session).delete("some-subpath");
    }

    @Test
    public void testDeleteFullObject() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getParentFedoraResourceId()).thenReturn("info:fedora/an-ocfl-object");
        when(operation.getResourceId()).thenReturn("info:fedora/an-ocfl-object");
        persister.persist(storageSession, session, operation, mapping);
        verify(session).deleteObject();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotPartOfObject() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getParentFedoraResourceId()).thenReturn("info:fedora/some-wrong-object");
        when(operation.getResourceId()).thenReturn("info:fedora/an-ocfl-object");
        persister.persist(storageSession, session, operation, mapping);
    }
}
