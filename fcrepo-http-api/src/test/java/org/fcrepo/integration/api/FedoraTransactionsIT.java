package org.fcrepo.integration.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.validation.constraints.AssertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.codehaus.jackson.map.ObjectMapper;
import org.fcrepo.Transaction;
import org.fcrepo.Transaction.State;
import org.junit.Test;

public class FedoraTransactionsIT extends AbstractResourceIT {
	@Test
	public void testCreateAndGetTransaction() throws Exception {
		/* create a tx */
		HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
		HttpResponse resp = execute(createTx);
		assertTrue(resp.getStatusLine().getStatusCode() == 200);
		ObjectMapper mapper = new ObjectMapper();
		Transaction tx = mapper.readValue(resp.getEntity().getContent(), Transaction.class);
		assertNotNull(tx);
		assertNotNull(tx.getId());
		assertNotNull(tx.getState());
		assertNotNull(tx.getCreated());
		assertTrue(tx.getState() == State.NEW);

		/* fetch the create dtx from the endpoint */
		HttpGet getTx = new HttpGet(serverAddress + "fcr:tx/" + tx.getId());
		resp = execute(getTx);
		Transaction fetched = mapper.readValue(resp.getEntity().getContent(), Transaction.class);
		assertEquals(tx.getId(), fetched.getId());
		assertEquals(tx.getState(), fetched.getState());
		assertEquals(tx.getCreated(), fetched.getCreated());
	}

	@Test
	public void testCreateAndCommitTransaction() throws Exception {
		/* create a tx */
		HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
		HttpResponse resp = execute(createTx);
		assertTrue(resp.getStatusLine().getStatusCode() == 200);
		ObjectMapper mapper = new ObjectMapper();
		Transaction tx = mapper.readValue(resp.getEntity().getContent(), Transaction.class);
		assertNotNull(tx);
		assertNotNull(tx.getId());
		assertNotNull(tx.getState());
		assertNotNull(tx.getCreated());
		assertTrue(tx.getState() == State.NEW);
		
		/* create a new object inside the tx */
		HttpPost postNew = new HttpPost(serverAddress + "fcr:tx/" + tx.getId() + "/objects/testobj1/fcr:newhack");
		resp = execute(postNew);
		assertTrue(resp.getStatusLine().getStatusCode() == 200);
				
		/* check if the object is already there, before the commit */
		HttpGet getObj = new HttpGet(serverAddress + "/objects/testobj1");
		resp = execute(getObj);
		assertTrue(resp.getStatusLine().toString(),resp.getStatusLine().getStatusCode() == 404);

		/* commit the tx */
		HttpPost commitTx = new HttpPost(serverAddress + "fcr:tx/" + tx.getId() + "/fcr:commit");
		resp = execute(commitTx);
		Transaction committed = mapper.readValue(resp.getEntity().getContent(), Transaction.class);
		assertEquals(committed.getState(), State.COMMITED);

		/* check if the object is there, after the commit */
		resp = execute(getObj);
		assertTrue(resp.getStatusLine().toString(),resp.getStatusLine().getStatusCode() == 200);
	}
	
	@Test
	public void testCreateAndRollbackTransaction() throws Exception {
		/* create a tx */
		HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
		HttpResponse resp = execute(createTx);
		assertTrue(resp.getStatusLine().getStatusCode() == 200);
		ObjectMapper mapper = new ObjectMapper();
		Transaction tx = mapper.readValue(resp.getEntity().getContent(), Transaction.class);
		assertNotNull(tx);
		assertNotNull(tx.getId());
		assertNotNull(tx.getState());
		assertNotNull(tx.getCreated());
		assertTrue(tx.getState() == State.NEW);

		/* create a new object inside the tx */
		HttpPost postNew = new HttpPost(serverAddress + "fcr:tx/" + tx.getId() + "/objects/testobj2/fcr:newhack");
		resp = execute(postNew);
		assertTrue(resp.getStatusLine().getStatusCode() == 200);
				
		/* check if the object is already there, before the commit */
		HttpGet getObj = new HttpGet(serverAddress + "/objects/testobj2");
		resp = execute(getObj);
		assertTrue(resp.getStatusLine().toString(),resp.getStatusLine().getStatusCode() == 404);

		/* and rollback */
		HttpPost rollbackTx = new HttpPost(serverAddress + "fcr:tx/" + tx.getId() + "/fcr:rollback");
		resp = execute(rollbackTx);
		Transaction rolledBack = mapper.readValue(resp.getEntity().getContent(), Transaction.class);
		assertEquals(rolledBack.getId(),tx.getId());
		assertTrue(rolledBack.getState() == State.ROLLED_BACK);
		
		/* check if the object is there, after the rollback */
		resp = execute(getObj);
		assertTrue(resp.getStatusLine().toString(),resp.getStatusLine().getStatusCode() == 404);
	}
}
