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
import static org.fcrepo.kernel.api.operations.ResourceOperationType.DELETE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
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

    private static final Set<Class<? extends ResourceOperation>> RESOURCE_OPERATION_TYPES = new HashSet<>();

    private static final Set<ResourceOperationType> OPERATION_TYPES = new HashSet<>();

    static {
        RESOURCE_OPERATION_TYPES.add(RdfSourceOperation.class);
        RESOURCE_OPERATION_TYPES.add(NonRdfSourceOperation.class);
        OPERATION_TYPES.add(CREATE);
        OPERATION_TYPES.add(UPDATE);
    }

    @Test
    public void testHandleSingles() {
        class MyPersister extends AbstractPersister {
            MyPersister() {
                super(NonRdfSourceOperation.class, CREATE);
            }

            @Override
            public void persist(final PersistentStorageSession storageSession,
                                final OCFLObjectSession objectSession,
                                final ResourceOperation operation,
                                final FedoraOCFLMapping mapping) {}
        }

        when(nonRdfSourceOperation.getType()).thenReturn(CREATE);
        final MyPersister mp = new MyPersister();
        assertTrue(mp.handle(nonRdfSourceOperation));
    }

    @Test
    public void testHandleSinglesFailureOperation() {
        class MyPersister extends AbstractPersister {
            MyPersister() {
                super(RdfSourceOperation.class, CREATE);
            }

            @Override
            public void persist(final PersistentStorageSession storageSession,
                                final OCFLObjectSession session,
                                final ResourceOperation operation,
                                final FedoraOCFLMapping mapping) {
            }
        }

        final MyPersister mp = new MyPersister();
        assertFalse(mp.handle(nonRdfSourceOperation));
    }

    @Test
    public void testHandleSinglesFailureType() {
        class MyPersister extends AbstractPersister {
            MyPersister() {
                super(NonRdfSourceOperation.class, CREATE);
            }

            @Override
            public void persist(final PersistentStorageSession storageSession,
                                final OCFLObjectSession session,
                                final ResourceOperation operation,
                                final FedoraOCFLMapping mapping) {
            }
        }

        when(nonRdfSourceOperation.getType()).thenReturn(UPDATE);
        final MyPersister mp = new MyPersister();
        assertFalse(mp.handle(nonRdfSourceOperation));
    }


    @Test
    public void testHandleSingleTypeMultipleOperation() {
        class MyPersister extends AbstractPersister {
            MyPersister() {
                super(NonRdfSourceOperation.class, OPERATION_TYPES);
            }

            @Override
            public void persist(final PersistentStorageSession storageSession,
                                final OCFLObjectSession session,
                                final ResourceOperation operation,
                                final FedoraOCFLMapping mapping) {
            }
        }
        when(nonRdfSourceOperation.getType()).thenReturn(CREATE);
        final MyPersister mp = new MyPersister();
        assertTrue(mp.handle(nonRdfSourceOperation));

        when(nonRdfSourceOperation.getType()).thenReturn(UPDATE);
        assertTrue(mp.handle(nonRdfSourceOperation));
    }

    @Test
    public void testHandleMultipleTypeMultipleOperation() {
        class MyPersister extends AbstractPersister {
            MyPersister() {
                super(RESOURCE_OPERATION_TYPES, OPERATION_TYPES);
            }

            @Override
            public void persist(final PersistentStorageSession storageSession,
                                final OCFLObjectSession session,
                                final ResourceOperation operation,
                                final FedoraOCFLMapping mapping) {
            }
        }
        final MyPersister mp = new MyPersister();

        when(nonRdfSourceOperation.getType()).thenReturn(CREATE);
        assertTrue(mp.handle(nonRdfSourceOperation));

        when(rdfSourceOperation.getType()).thenReturn(UPDATE);
        assertTrue(mp.handle(rdfSourceOperation));

        when(nonRdfSourceOperation.getType()).thenReturn(UPDATE);
        assertTrue(mp.handle(nonRdfSourceOperation));

        when(rdfSourceOperation.getType()).thenReturn(CREATE);
        assertTrue(mp.handle(rdfSourceOperation));
    }

    @Test
    public void testHandleMultipleTypeMultipleOperationFailure() {
        class MyPersister extends AbstractPersister {
            MyPersister() {
                super(RESOURCE_OPERATION_TYPES, OPERATION_TYPES);
            }

            @Override
            public void persist(final PersistentStorageSession storageSession,
                                final OCFLObjectSession session,
                                final ResourceOperation operation,
                                final FedoraOCFLMapping mapping) {
            }
        }
        final MyPersister mp = new MyPersister();

        when(nonRdfSourceOperation.getType()).thenReturn(DELETE);
        assertFalse(mp.handle(nonRdfSourceOperation));

        when(rdfSourceOperation.getType()).thenReturn(DELETE);
        assertFalse(mp.handle(rdfSourceOperation));
    }
}
