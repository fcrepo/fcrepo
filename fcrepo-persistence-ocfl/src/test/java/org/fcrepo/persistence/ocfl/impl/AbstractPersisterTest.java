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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
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

    private static final Set<ResourceOperationType> OPERATION_TYPES = new HashSet<>();

    static {
        OPERATION_TYPES.add(CREATE);
        OPERATION_TYPES.add(UPDATE);
    }

    @Test
    public void testHandle() {
        class MyPersister extends AbstractPersister<NonRdfSourceOperation> {
            MyPersister() {
                super(CREATE);
            }

            @Override
            public void persist(final OCFLObjectSession session,
                                final NonRdfSourceOperation operation,
                                final FedoraOCFLMapping mapping) {}
        }

        when(nonRdfSourceOperation.getType()).thenReturn(CREATE);
        final MyPersister mp = new MyPersister();
        assertTrue(mp.handle(nonRdfSourceOperation));
    }

    @Test
    public void testHandleMultiple() {
        class MyPersister extends AbstractPersister<NonRdfSourceOperation> {
            MyPersister() {
                super(OPERATION_TYPES);
            }

            @Override
            public void persist(final OCFLObjectSession session,
                                final NonRdfSourceOperation operation,
                                final FedoraOCFLMapping mapping) {
            }
        }
        when(nonRdfSourceOperation.getType()).thenReturn(CREATE);
        final MyPersister mp = new MyPersister();
        assertTrue(mp.handle(nonRdfSourceOperation));

        when(nonRdfSourceOperation.getType()).thenReturn(UPDATE);
        assertTrue(mp.handle(nonRdfSourceOperation));
    }
}
