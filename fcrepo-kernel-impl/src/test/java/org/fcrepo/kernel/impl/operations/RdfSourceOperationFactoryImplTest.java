/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

/**
 * @author bseeger
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RdfSourceOperationFactoryImplTest {

    private RdfSourceOperationFactory factory;
    private FedoraId randomId;

    @Mock
    private Transaction tx;

    @BeforeEach
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
