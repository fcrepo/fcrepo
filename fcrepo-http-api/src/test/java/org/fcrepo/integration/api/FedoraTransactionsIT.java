
package org.fcrepo.integration.api;

import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.fcrepo.Transaction;
import org.fcrepo.services.TransactionService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.update.GraphStore;

public class FedoraTransactionsIT extends AbstractResourceIT {

    @Test
    public void testCreateTransaction() throws Exception {
        /* create a tx */
        final HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
        HttpResponse response = execute(createTx);
        assertEquals(201, response.getStatusLine().getStatusCode());

        final String location = response.getFirstHeader("Location").getValue();

        logger.info("Got location {}", location);
        assertTrue(
                "Expected Location header to send us to root node path within the transaction",
                compile("tx:[0-9a-f-]+$").matcher(location).find());

    }

    @Test
    public void testRequestsInTransactionThatDoestExist() throws Exception {
        /* create a tx */
        final HttpPost createTx =
                new HttpPost(serverAddress + "tx:123/objects");
        HttpResponse response = execute(createTx);
        assertEquals(410, response.getStatusLine().getStatusCode());

    }

    @Test
    public void testCreateAndTimeoutTransaction() throws Exception {

        /* create a short-lived tx */
        final long testTimeout =
                Math.min(500, TransactionService.REAP_INTERVAL / 2);
        System.setProperty(Transaction.TIMEOUT_SYSTEM_PROPERTY, Long
                .toString(testTimeout));

        /* create a tx */
        final HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
        HttpResponse response = execute(createTx);
        assertEquals(201, response.getStatusLine().getStatusCode());

        final String location = response.getFirstHeader("Location").getValue();

        final HttpGet getWithinTx = new HttpGet(location);
        HttpResponse resp = execute(getWithinTx);
        IOUtils.toString(resp.getEntity().getContent());
        assertEquals(200, resp.getStatusLine().getStatusCode());

        int statusCode = 0;

        Thread.sleep(TransactionService.REAP_INTERVAL * 2);
        final HttpGet getWithExpiredTx = new HttpGet(location);
        resp = execute(getWithExpiredTx);
        IOUtils.toString(resp.getEntity().getContent());
        statusCode = resp.getStatusLine().getStatusCode();

        try {
            assertEquals("Transaction did not expire", 410, statusCode);
        } finally {
            System.setProperty(Transaction.TIMEOUT_SYSTEM_PROPERTY, Long
                    .toString(Transaction.DEFAULT_TIMEOUT));
            System.clearProperty("fcrepo4.tx.timeout");
        }
    }

    @Test
    public void testCreateDoStuffAndRollbackTransaction() throws Exception {
        /* create a tx */
        final HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");

        HttpResponse response = execute(createTx);
        assertEquals(201, response.getStatusLine().getStatusCode());

        final String txLocation =
                response.getFirstHeader("Location").getValue();

        /* create a new object inside the tx */
        final HttpPost postNew =
                new HttpPost(txLocation + "/object-in-tx-rollback");
        HttpResponse resp = execute(postNew);
        assertEquals(201, resp.getStatusLine().getStatusCode());

        /* fetch the created tx from the endpoint */
        final HttpGet getTx =
                new HttpGet(txLocation + "/object-in-tx-rollback");
        resp = execute(getTx);
        assertEquals(
                "Expected to find our object within the scope of the transaction",
                200, resp.getStatusLine().getStatusCode());

        final GraphStore graphStore =
                TestHelpers.parseTriples(resp.getEntity().getContent());
        logger.debug(graphStore.toString());

        assertTrue(graphStore.toDataset().asDatasetGraph().contains(Node.ANY,
                Node.createURI(txLocation + "/object-in-tx-rollback"),
                Node.ANY, Node.ANY));

        /* fetch the created tx from the endpoint */
        final HttpGet getObj =
                new HttpGet(serverAddress + "/object-in-tx-rollback");
        resp = execute(getObj);
        assertEquals(
                "Expected to not find our object within the scope of the transaction",
                404, resp.getStatusLine().getStatusCode());

        /* and rollback */
        final HttpPost rollbackTx =
                new HttpPost(txLocation + "/fcr:tx/fcr:rollback");
        resp = execute(rollbackTx);

        assertEquals(204, resp.getStatusLine().getStatusCode());

    }

    @Test
    public void testCreateDoStuffAndCommitTransaction() throws Exception {
        /* create a tx */
        final HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");

        HttpResponse response = execute(createTx);
        assertEquals(201, response.getStatusLine().getStatusCode());

        final String txLocation =
                response.getFirstHeader("Location").getValue();

        /* create a new object inside the tx */
        final HttpPost postNew =
                new HttpPost(txLocation + "/object-in-tx-commit");
        HttpResponse resp = execute(postNew);
        assertEquals(201, resp.getStatusLine().getStatusCode());

        /* fetch the created tx from the endpoint */
        final HttpGet getTx = new HttpGet(txLocation + "/object-in-tx-commit");
        resp = execute(getTx);
        assertEquals(
                "Expected to find our object within the scope of the transaction",
                200, resp.getStatusLine().getStatusCode());

        final GraphStore graphStore =
                TestHelpers.parseTriples(resp.getEntity().getContent());
        logger.debug(graphStore.toString());

        assertTrue(graphStore.toDataset().asDatasetGraph().contains(Node.ANY,
                Node.createURI(txLocation + "/object-in-tx-commit"),
                Node.ANY,
                Node.ANY));

        /* fetch the object-in-tx outside of the tx */
        final HttpGet getObj =
                new HttpGet(serverAddress + "/object-in-tx-commit");
        resp = execute(getObj);
        assertEquals(
                "Expected to not find our object within the scope of the transaction",
                404, resp.getStatusLine().getStatusCode());

        /* and rollback */
        final HttpPost commitTx =
                new HttpPost(txLocation + "/fcr:tx/fcr:commit");
        resp = execute(commitTx);

        assertEquals(204, resp.getStatusLine().getStatusCode());

        /* fetch the object-in-tx outside of the tx after it has been committed */
        final HttpGet getObjCommitted =
                new HttpGet(serverAddress + "/object-in-tx-commit");
        resp = execute(getObjCommitted);
        assertEquals(
                "Expected to  find our object after the transaction was committed",
                200, resp.getStatusLine().getStatusCode());

    }

}
