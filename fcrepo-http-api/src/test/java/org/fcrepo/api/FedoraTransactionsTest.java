package org.fcrepo.api;

import static org.junit.Assert.*;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.Transaction;
import org.fcrepo.Transaction.State;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

public class FedoraTransactionsTest {

	FedoraTransactions resource;

	Session mockSession;

	@Before
	public void setup() throws Exception {
		resource = new FedoraTransactions();
		mockSession = TestHelpers.mockSession(resource);
	}

	@Test
	public void testCreateTx() throws Exception {
		Transaction tx = resource.createTransaction();
		
		assertNotNull(tx);
		assertNotNull(tx.getCreated());
		assertNotNull(tx.getId());
		assertTrue(tx.getState() == State.NEW);
	}

	@Test
	public void testGetTx() throws Exception {
		Transaction tx = resource.createTransaction();
		tx = resource.getTransaction(tx.getId());

		assertNotNull(tx);
		assertNotNull(tx.getCreated());
		assertNotNull(tx.getId());
		assertTrue(tx.getState() == State.NEW);
	}

	@Test
	public void testCommitTx() throws Exception {
		Transaction tx = resource.createTransaction();
		tx = resource.commit(tx.getId());

		assertNotNull(tx);
		assertNotNull(tx.getCreated());
		assertNotNull(tx.getId());
		assertTrue(tx.getState() == State.COMMITED);
		try{
			assertNull(resource.getTransaction(tx.getId()));
			fail("Transaction is available after commit");
		}catch(RepositoryException e){
			// just checking that the exception occurs
		}
			
	}

	@Test
	public void testRollbackTx() throws Exception {
		Transaction tx = resource.createTransaction();
		tx = resource.rollback(tx.getId());
		
		assertNotNull(tx);
		assertNotNull(tx.getCreated());
		assertNotNull(tx.getId());
		assertTrue(tx.getState() == State.ROLLED_BACK);
		try{
			assertNull(resource.getTransaction(tx.getId()));
			fail("Transaction is available after rollback");
		}catch(RepositoryException e){
			// just checking that the exception occurs
		}
	}

}
