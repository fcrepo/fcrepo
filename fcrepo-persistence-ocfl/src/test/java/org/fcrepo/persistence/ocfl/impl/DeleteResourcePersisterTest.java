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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndex;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory;
import org.junit.Before;
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

    @Mock
    private FedoraToOCFLObjectIndex index;

    @Mock
    private OCFLObjectSessionFactory sessionFactory;

    @Mock
    private OCFLPersistentStorageSession psSession;


    private DeleteResourcePersister persister;

    @Before
    public void setup() throws Exception {
        operation = mock(RdfSourceOperation.class);
        persister = new DeleteResourcePersister(this.sessionFactory, this.index);

        when(psSession.findOrCreateSession(anyString())).thenReturn(session);
        when(index.getMapping(anyString())).thenReturn(mapping);
    }


    @Test
    public void testDeleteSubPath() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn("info:fedora/an-ocfl-object");
        when(operation.getResourceId()).thenReturn("info:fedora/an-ocfl-object/some-subpath");

        persister.persist(psSession, operation);
        verify(session).delete("some-subpath");
    }

    @Test
    public void testDeleteFullObject() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn("info:fedora/an-ocfl-object");
        when(operation.getResourceId()).thenReturn("info:fedora/an-ocfl-object");
        persister.persist(psSession, operation);
        verify(session).deleteObject();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotPartOfObject() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn("info:fedora/some-wrong-object");
        when(operation.getResourceId()).thenReturn("info:fedora/an-ocfl-object");
        persister.persist(psSession, operation);
    }
}
