/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.junit.jupiter.api.Test;

/**
 * Unit test for ReferenceOperationBuilder
 *
 * @author bbpennel
 */
public class ReferenceOperationBuilderTest {

    @Test
    public void buildTest() {
        final Transaction tx = mock(Transaction.class);
        final FedoraId resourceId = FedoraId.create("info:fedora/test");
        final String userPrincipal = "testUser";

        final var builder = new ReferenceOperationBuilder(tx, resourceId);
        builder.userPrincipal(userPrincipal);

        final var operation = builder.build();

        assertNotNull(operation);
        assertEquals(resourceId, operation.getResourceId());
        assertEquals(tx, operation.getTransaction());
        assertEquals(userPrincipal, operation.getUserPrincipal());
    }
}