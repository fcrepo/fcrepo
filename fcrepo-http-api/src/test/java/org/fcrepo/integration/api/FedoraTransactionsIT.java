
package org.fcrepo.integration.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.codehaus.jackson.map.ObjectMapper;
import org.fcrepo.Transaction;
import org.fcrepo.Transaction.State;
import org.fcrepo.api.FedoraTransactions;
import org.junit.Test;

public class FedoraTransactionsIT extends AbstractResourceIT {

    @Test
    public void testCreateAndGetTransaction() throws Exception {
        /* create a tx */
        HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
        HttpResponse resp = execute(createTx);
        assertTrue(resp.getStatusLine().getStatusCode() == 200);
        ObjectMapper mapper = new ObjectMapper();
        Transaction tx =
                mapper.readValue(resp.getEntity().getContent(),
                        Transaction.class);
        createTx.releaseConnection();
        assertNotNull(tx);
        assertNotNull(tx.getId());
        assertNotNull(tx.getState());
        assertNotNull(tx.getCreated());
        assertTrue(tx.getState() == State.NEW);

        /* fetch the created tx from the endpoint */
        HttpGet getTx = new HttpGet(serverAddress + "fcr:tx/" + tx.getId());
        getTx.setHeader("Accept", "application/json");
        resp = execute(getTx);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        Transaction fetched =
                mapper.readValue(resp.getEntity().getContent(),
                        Transaction.class);
        getTx.releaseConnection();
        assertEquals(tx.getId(), fetched.getId());
        assertEquals(tx.getState(), fetched.getState());
        assertEquals(tx.getCreated(), fetched.getCreated());
    }

    @Test
    public void testCreateAndTimeoutTransaction() throws Exception {

        long start = System.currentTimeMillis();
        /* create a tx that will timeout in 10 ms */
        long testTimeout = 10;
    	System.setProperty(Transaction.TIMEOUT_SYSTEM_PROPERTY, Long.toString(testTimeout));
        HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
        HttpResponse resp = execute(createTx);
        assertTrue(resp.getStatusLine().getStatusCode() == 200);
        ObjectMapper mapper = new ObjectMapper();
        Transaction tx =
                mapper.readValue(resp.getEntity().getContent(),
                        Transaction.class);
        assertNotNull(tx);
        assertNotNull(tx.getId());
        assertNotNull(tx.getState());
        assertNotNull(tx.getCreated());
        assertTrue(tx.getState() == State.NEW);
        createTx.releaseConnection();

        boolean expired = false;
        long diff = 0;
        // the loop should be able to run for at least the tx reaping interval
        while (!expired &&
        		(diff = (System.currentTimeMillis() - start)) < (2* FedoraTransactions.REAP_INTERVAL)) {
            /* check that the tx is removed */
            HttpGet getTx = new HttpGet(serverAddress + "fcr:tx/" + tx.getId());
            resp = execute(getTx);
            getTx.releaseConnection();
            expired = (404 == resp.getStatusLine().getStatusCode());
        }

        try {
        	assertTrue("Transaction did not expire", expired);
        	assertTrue(diff >= testTimeout);
        } finally {
        	System.setProperty(Transaction.TIMEOUT_SYSTEM_PROPERTY,
        			Long.toString(Transaction.DEFAULT_TIMEOUT));
        }
        System.clearProperty("fcrepo4.tx.timeout");
    }

    @Test
    public void testCreateAndCommitTransaction() throws Exception {
        /* create a tx */
        HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
        HttpResponse resp = execute(createTx);
        assertTrue(resp.getStatusLine().getStatusCode() == 200);
        ObjectMapper mapper = new ObjectMapper();
        Transaction tx =
                mapper.readValue(resp.getEntity().getContent(),
                        Transaction.class);
        createTx.releaseConnection();
        assertNotNull(tx);
        assertNotNull(tx.getId());
        assertNotNull(tx.getState());
        assertNotNull(tx.getCreated());
        assertTrue(tx.getState() == State.NEW);

        /* create a new object inside the tx */
        HttpPost postNew =
                new HttpPost(serverAddress + "fcr:tx/" + tx.getId() +
                        "/objects/testobj1/fcr:newhack");
        resp = execute(postNew);
        postNew.releaseConnection();
        assertTrue(resp.getStatusLine().getStatusCode() == 200);

        /* check if the object is already there, before the commit */
        HttpGet getObj = new HttpGet(serverAddress + "/objects/testobj1");
        resp = execute(getObj);
        getObj.releaseConnection();
        assertTrue(resp.getStatusLine().toString(), resp.getStatusLine()
                .getStatusCode() == 404);

        /* commit the tx */
        HttpPost commitTx =
                new HttpPost(serverAddress + "fcr:tx/" + tx.getId() +
                        "/fcr:commit");
        resp = execute(commitTx);
        assertTrue(resp.getStatusLine().getStatusCode() == 200);
        Transaction committed =
                mapper.readValue(resp.getEntity().getContent(),
                        Transaction.class);
        commitTx.releaseConnection();
        assertEquals(committed.getState(), State.COMMITED);

        /* check if the object is there, after the commit */
        resp = execute(getObj);
        assertTrue(resp.getStatusLine().toString(), resp.getStatusLine()
                .getStatusCode() == 200);
        getObj.releaseConnection();
    }

    @Test
    public void testCreateAndRollbackTransaction() throws Exception {
        /* create a tx */
        HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
        HttpResponse resp = execute(createTx);
        assertTrue(resp.getStatusLine().getStatusCode() == 200);
        ObjectMapper mapper = new ObjectMapper();
        Transaction tx =
                mapper.readValue(resp.getEntity().getContent(),
                        Transaction.class);
        createTx.releaseConnection();
        assertNotNull(tx);
        assertNotNull(tx.getId());
        assertNotNull(tx.getState());
        assertNotNull(tx.getCreated());
        assertTrue(tx.getState() == State.NEW);

        /* create a new object inside the tx */
        HttpPost postNew =
                new HttpPost(serverAddress + "fcr:tx/" + tx.getId() +
                        "/objects/testobj2/fcr:newhack");
        resp = execute(postNew);
        assertTrue(resp.getStatusLine().getStatusCode() == 200);
        postNew.releaseConnection();

        /* check if the object is already there, before the commit */
        HttpGet getObj = new HttpGet(serverAddress + "/objects/testobj2");
        resp = execute(getObj);
        getObj.releaseConnection();
        assertTrue(resp.getStatusLine().toString(), resp.getStatusLine()
                .getStatusCode() == 404);

        /* and rollback */
        HttpPost rollbackTx =
                new HttpPost(serverAddress + "fcr:tx/" + tx.getId() +
                        "/fcr:rollback");
        resp = execute(rollbackTx);
        Transaction rolledBack =
                mapper.readValue(resp.getEntity().getContent(),
                        Transaction.class);
        rollbackTx.releaseConnection();
        assertEquals(rolledBack.getId(), tx.getId());
        assertTrue(rolledBack.getState() == State.ROLLED_BACK);

        /* check if the object is there, after the rollback */
        resp = execute(getObj);
        getObj.releaseConnection();
        assertTrue(resp.getStatusLine().toString(), resp.getStatusLine()
                .getStatusCode() == 404);
    }
}
