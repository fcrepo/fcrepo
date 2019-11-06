package org.fcrepo.persistence.ocfl.impl;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.junit.Assert.assertTrue;


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
            public void persist(OCFLObjectSession session, NonRdfSourceOperation operation, String ocflSubPath) {

            }
        }

        MyPersister mp = new MyPersister();
        assertTrue(mp.handle(nonRdfSourceOperation));

    }
}
