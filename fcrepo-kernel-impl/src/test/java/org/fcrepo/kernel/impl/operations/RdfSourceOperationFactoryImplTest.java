/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import static org.junit.Assert.assertEquals;

import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

/**
 * @author bseeger
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class RdfSourceOperationFactoryImplTest {

    private RdfSourceOperationFactory factory;
    private FedoraId randomId;

    @Mock
    private Transaction tx;

    @Before
    public void setUp() {
        factory = new RdfSourceOperationFactoryImpl();
        randomId = FedoraId.create(UUID.randomUUID().toString());
    }

    @Test
    public void testCreateBuilder() {
        final String model = "some-interaction-model";
        final RdfSourceOperationBuilder builder = factory.createBuilder(tx, randomId, model,
                ServerManagedPropsMode.RELAXED);
        assertEquals(CreateRdfSourceOperationBuilderImpl.class, builder.getClass());
    }
}
