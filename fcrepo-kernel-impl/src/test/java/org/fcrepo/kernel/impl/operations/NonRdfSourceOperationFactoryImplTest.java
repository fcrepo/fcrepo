/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.apache.commons.io.IOUtils;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

/**
 * @author bseeger
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class NonRdfSourceOperationFactoryImplTest {

    private NonRdfSourceOperationFactory factory;

    private FedoraId randomId;

    @Mock
    private Transaction transaction;

    @BeforeEach
    public void setUp() throws Exception {
        when(transaction.getId()).thenReturn("tx-123");
        factory = new NonRdfSourceOperationFactoryImpl();
        randomId = FedoraId.create(UUID.randomUUID().toString());
    }

    @Test
    public void testCreateInternalBuilder() throws Exception {
        final InputStream stream = IOUtils.toInputStream("This is some test data", "UTF-8");
        final NonRdfSourceOperationBuilder builder = factory.createInternalBinaryBuilder(transaction, randomId, stream);
        assertEquals(CreateNonRdfSourceOperationBuilderImpl.class, builder.getClass());
    }

    @Test
    public void testCreateExternalBuilder() {
        final URI externalURI = URI.create("http://example.com/some/location");
        final NonRdfSourceOperationBuilder builder = factory.createExternalBinaryBuilder(transaction, randomId,
                "PROXY", externalURI);
        assertEquals(CreateNonRdfSourceOperationBuilderImpl.class, builder.getClass());
    }
}
