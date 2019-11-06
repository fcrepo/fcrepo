package org.fcrepo.persistence.ocfl.impl;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;


import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
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


    @Test
    public void testHandle() {
        class MyPersister extends AbstractPersister<NonRdfSourceOperation> {
           MyPersister(){
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
}
