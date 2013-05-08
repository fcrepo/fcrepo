package org.fcrepo.integration.api;

import static org.junit.Assert.*;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.codehaus.jettison.json.JSONObject;
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
		String data = IOUtils.toString(resp.getEntity().getContent());
		assertTrue(data.length() > 0);
		/* parse the tx using JSON */
		JSONObject json = new JSONObject(data);
		String txid = json.getString("id");
		Transaction.State state = State.valueOf(json.getString("state"));
		String created = json.getString("created-date");
		assertNotNull(txid);
		assertNotNull(state);
		assertNotNull(created);
		assertTrue(txid.length() > 0);
		/* fetch the create dtx from the endpoint */
		HttpGet getTx = new HttpGet(serverAddress + "fcr:tx/" + txid);
		resp = execute(getTx);
		/* and parse that one using JSON */
		data = IOUtils.toString(resp.getEntity().getContent());
		assertTrue(data.length() > 0);
		json = new JSONObject(data);
		String fetchedTxid = json.getString("id");
		State fetchedState = State.valueOf(json.getString("state"));
		String fetchedCreated = json.getString("created-date");
		assertEquals(txid, fetchedTxid);
		assertEquals(state, fetchedState);
		assertEquals(created, fetchedCreated);
	}

	@Test
	public void testCreateAndCommitTransaction() throws Exception {
		/* create a tx */
		HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
		HttpResponse resp = execute(createTx);
		assertTrue(resp.getStatusLine().getStatusCode() == 200);
		String data = IOUtils.toString(resp.getEntity().getContent());
		assertTrue(data.length() > 0);
		/* parse the tx using JSON */
		JSONObject json = new JSONObject(data);
		String txid = json.getString("id");
		Transaction.State state = State.valueOf(json.getString("state"));
		String created = json.getString("created-date");
		assertNotNull(txid);
		assertNotNull(state);
		assertNotNull(created);
		assertTrue(txid.length() > 0);

		/* fetch the create dtx from the endpoint */
		HttpPost commitTx = new HttpPost(serverAddress + "fcr:tx/" + txid + "/fcr:commit");
		resp = execute(commitTx);
		/* and parse that one using JSON */
		data = IOUtils.toString(resp.getEntity().getContent());
		assertTrue(data.length() > 0);
		json = new JSONObject(data);
		State fetchedState = State.valueOf(json.getString("state"));
		assertEquals(fetchedState, State.COMMITED);
	}
	
	@Test
	public void testCreateAndRollbackTransaction() throws Exception {
		/* create a tx */
		HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
		HttpResponse resp = execute(createTx);
		assertTrue(resp.getStatusLine().getStatusCode() == 200);
		String data = IOUtils.toString(resp.getEntity().getContent());
		assertTrue(data.length() > 0);
		/* parse the tx using JSON */
		JSONObject json = new JSONObject(data);
		String txid = json.getString("id");
		Transaction.State state = State.valueOf(json.getString("state"));
		String created = json.getString("created-date");
		assertNotNull(txid);
		assertNotNull(state);
		assertNotNull(created);
		assertTrue(txid.length() > 0);

		/* fetch the create dtx from the endpoint */
		HttpPost commitTx = new HttpPost(serverAddress + "fcr:tx/" + txid + "/fcr:rollback");
		resp = execute(commitTx);
		/* and parse that one using JSON */
		data = IOUtils.toString(resp.getEntity().getContent());
		assertTrue(data.length() > 0);
		json = new JSONObject(data);
		State fetchedState = State.valueOf(json.getString("state"));
		assertEquals(fetchedState, State.ROLLED_BACK);
	}
}
