/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/containmentIndexTest.xml")
public class ContainerImplTest {
    @Mock
    private PersistentStorageSessionManager sessionManager;
    @Mock
    private ResourceFactory resourceFactory;
    @Mock
    private Transaction transaction;

    private final static String TX_ID = "transacted";

    private FedoraId fedoraId;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(transaction.getId()).thenReturn(UUID.randomUUID().toString());
        when(transaction.isShortLived()).thenReturn(true);
        fedoraId = FedoraId.create(UUID.randomUUID().toString());
    }

    @Test
    public void getChildren_WithChildren() {
        final var child1 = mock(Container.class);
        final var child2 = mock(Binary.class);
        final var childrenStream = Stream.of(child1, child2);

        when(resourceFactory.getChildren(transaction, fedoraId)).thenReturn(childrenStream);

        final Container container = new ContainerImpl(fedoraId, transaction, sessionManager, resourceFactory, null);

        final var resultStream = container.getChildren();
        final var childrenList = resultStream.collect(Collectors.toList());
        assertEquals(2, childrenList.size());

        assertTrue(childrenList.stream().anyMatch(c -> c instanceof Container));
        assertTrue(childrenList.stream().anyMatch(c -> c instanceof Binary));
    }
}
