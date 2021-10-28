/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.fcrepo.common.db.DbTransactionExecutor;
import org.fcrepo.kernel.api.Transaction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Reindex manager tests.
 * @author whikloj
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ReindexManagerTest extends AbstractReindexerTest {

    @Mock
    private ReindexService reindexService;

    private ReindexManager reindexManager;

    @Before
    public void setup() throws Exception {
        super.setup();

        reindexManager = new ReindexManager(repository.listObjectIds(),
                reindexService, propsConfig, txManager, new DbTransactionExecutor());
    }

    @Test
    public void testProcessAnObject() throws Exception {
        final var session = persistentStorageSessionManager.getSession(transaction);

        createResource(session, resource1, true);
        createChildResourceNonRdf(session, resource1, resource2);

        session.prepare();
        session.commit();

        assertHasOcflId("resource1", resource1);
        assertHasOcflId("resource1", resource2);

        ocflIndex.reset();

        assertDoesNotHaveOcflId(resource1);
        assertDoesNotHaveOcflId(resource2);

        reindexManager.start();
        reindexManager.shutdown();

        verify(reindexService).indexOcflObject(any(Transaction.class), eq(FEDORA_ID_PREFIX + "/resource1"));
        verify(reindexService).indexMembership(any(Transaction.class));
    }
}
