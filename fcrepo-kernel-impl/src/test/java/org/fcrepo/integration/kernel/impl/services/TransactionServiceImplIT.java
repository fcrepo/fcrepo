/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.integration.kernel.impl.services;

import org.fcrepo.integration.kernel.impl.AbstractIT;
import org.fcrepo.kernel.Transaction;
import org.fcrepo.kernel.impl.TxAwareSession;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.services.TransactionService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author escowles
 * @since 2014-05-29
 */

@ContextConfiguration({"/spring-test/repo.xml"})
public class TransactionServiceImplIT extends AbstractIT {

    @Inject
    private Repository repository;

    @Inject
    NodeService nodeService;

    @Inject
    ObjectService objectService;

    @Inject
    TransactionService transactionService;

    @Test
    public void testGetTransaction() throws RepositoryException {
        final Session session = repository.login();
        final Transaction t1 = transactionService.beginTransaction( session, "fedoraAdmin" );
        assertNotNull(t1);
        assertTrue( transactionService.exists(t1.getId()) );

        final Transaction t2 = transactionService.getTransaction(session);
        assertNotNull(t2);
        assertEquals( t1.getId(), t2.getId() );
    }

    @Test
    public void testGetTransactionUser() throws RepositoryException {
        final Session session = repository.login();
        final Transaction t1 = transactionService.beginTransaction( session, "fedoraAdmin" );
        assertNotNull(t1);
        assertTrue( transactionService.exists(t1.getId()) );

        final Transaction t2 = transactionService.getTransaction(t1.getId(),"fedoraAdmin");
        assertNotNull(t2);
        assertEquals( t1.getId(), t2.getId() );
    }

    @Test
    public void testTransactionExpire() throws RepositoryException {
        final Session session = repository.login();
        final Transaction t = transactionService.beginTransaction( session, "fedoraAdmin" );
        final String pid = getTestObjIdentifier();
        final Session txSession = TxAwareSession.newInstance(session, t.getId());
        objectService.createObject(txSession, "/" + pid );

        // rollback and make sure the object doesn't exist
        t.expire();
        transactionService.removeAndRollbackExpired();

        final Session session2 = repository.login();
        assertFalse( objectService.exists(session2,"/" + pid) );
    }

    @Test
    public void testRollback() throws RepositoryException {
        final Session session = repository.login();
        final Transaction t = transactionService.beginTransaction( session, "fedoraAdmin" );
        final String pid = getTestObjIdentifier();
        final Session txSession = TxAwareSession.newInstance(session, t.getId());
        objectService.createObject(txSession, "/" + pid );

        // rollback and make sure the object doesn't exist
        transactionService.rollback( t.getId() );
        txSession.save();

        final Session session2 = repository.login();
        assertFalse( objectService.exists(session2,"/" + pid) );
    }

    @Test
    public void testCommit() throws RepositoryException {
        final Session session = repository.login();
        final Transaction t = transactionService.beginTransaction( session, "fedoraAdmin" );
        final String pid = getTestObjIdentifier();
        final Session txSession = TxAwareSession.newInstance(session, t.getId());
        objectService.createObject(txSession, "/" + pid );

        // rollback and make sure the object doesn't exist
        transactionService.commit( t.getId() );
        txSession.save();

        final Session session2 = repository.login();
        assertTrue( objectService.exists(session2,"/" + pid) );
    }

}
