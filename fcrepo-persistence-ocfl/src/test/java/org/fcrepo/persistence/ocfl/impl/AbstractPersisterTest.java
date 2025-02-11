/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author dbernstein
 * @since 6.0.0
 */
@ExtendWith(MockitoExtension.class)
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
