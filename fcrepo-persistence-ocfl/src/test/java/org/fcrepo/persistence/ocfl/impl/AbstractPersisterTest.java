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

import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author dbernstein
 * @since 6.0.0
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractPersisterTest {

    @Mock
    private NonRdfSourceOperation nonRdfSourceOperation;

    @Mock
    private RdfSourceOperation rdfSourceOperation;

    @Test
    public void testHandleSingles() {
        class MyPersister extends AbstractPersister {
            MyPersister() {
                super(NonRdfSourceOperation.class, CREATE, null);
            }

            @Override
            public void persist(final OcflPersistentStorageSession session,
                                final ResourceOperation operation) { }
        }

        when(nonRdfSourceOperation.getType()).thenReturn(CREATE);
        final MyPersister mp = new MyPersister();
        assertTrue(mp.handle(nonRdfSourceOperation));
    }

    @Test
    public void testHandleSinglesFailureOperation() {
        class MyPersister extends AbstractPersister {
            MyPersister() {
                super(RdfSourceOperation.class, CREATE,  null);
            }

            @Override
            public void persist(final OcflPersistentStorageSession session,
                                final ResourceOperation operation) { }
        }

        final MyPersister mp = new MyPersister();
        assertFalse(mp.handle(nonRdfSourceOperation));
    }

    @Test
    public void testHandleSinglesFailureType() {
        class MyPersister extends AbstractPersister {
            MyPersister() {
                super(NonRdfSourceOperation.class, CREATE, null);
            }

            @Override
            public void persist(final OcflPersistentStorageSession session,
                                final ResourceOperation operation) { }
        }

        when(nonRdfSourceOperation.getType()).thenReturn(UPDATE);
        final MyPersister mp = new MyPersister();
        assertFalse(mp.handle(nonRdfSourceOperation));
    }
}
