/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.integration.http.api;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.update.GraphStore;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.BasicHttpEntity;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.lang.Math.min;
import static java.lang.Thread.sleep;
import static java.util.UUID.randomUUID;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.fcrepo.kernel.RdfLexicon.DC_TITLE;
import static org.fcrepo.kernel.impl.TransactionImpl.DEFAULT_TIMEOUT;
import static org.fcrepo.kernel.impl.TransactionImpl.TIMEOUT_SYSTEM_PROPERTY;
import static org.fcrepo.kernel.impl.services.TransactionServiceImpl.REAP_INTERVAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * <p>FedoraTransactionsIT class.</p>
 *
 * @author awoods
 */
public class FedoraTransactionsIT extends AbstractResourceIT {

    @Test
    public void testCreateTransaction() throws Exception {
        final String location = createTransaction();

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
        final HttpResponse response = execute(createTx);
        assertEquals(410, response.getStatusLine().getStatusCode());

    }

    @Test
    public void testCreateAndTimeoutTransaction() throws Exception {

        /* create a short-lived tx */
        final long testTimeout = min(500, REAP_INTERVAL / 2);
        System.setProperty(TIMEOUT_SYSTEM_PROPERTY, Long.toString(testTimeout));

        /* create a tx */
        final String location = createTransaction();

        final HttpGet getWithinTx = new HttpGet(location);
        HttpResponse resp = execute(getWithinTx);
        IOUtils.toString(resp.getEntity().getContent());
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertTrue(Iterators.any(Iterators.forArray(resp.getHeaders("Link")), new Predicate<Header>() {
            @Override
            public boolean apply(final Header input) {
                return input.getValue().contains("<" + serverAddress + ">;rel=\"canonical\"");
            }
        }));

        int statusCode = 0;

        sleep(REAP_INTERVAL * 2);
        final HttpGet getWithExpiredTx = new HttpGet(location);
        resp = execute(getWithExpiredTx);
        IOUtils.toString(resp.getEntity().getContent());
        statusCode = resp.getStatusLine().getStatusCode();

        try {
            assertEquals("Transaction did not expire", 410, statusCode);
        } finally {
            System.setProperty(TIMEOUT_SYSTEM_PROPERTY, Long
                    .toString(DEFAULT_TIMEOUT));
            System.clearProperty("fcrepo.transactions.timeout");
        }
    }

    @Test
    public void testCreateDoStuffAndRollbackTransaction() throws Exception {
        /* create a tx */
        final HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");

        final HttpResponse response = execute(createTx);
        assertEquals(201, response.getStatusLine().getStatusCode());

        final String txLocation =
            response.getFirstHeader("Location").getValue();

        /* create a new object inside the tx */
        final HttpPost postNew =
            new HttpPost(txLocation);
        final String pid = getRandomUniquePid();
        postNew.addHeader("Slug", pid);
        HttpResponse resp = execute(postNew);
        assertEquals(201, resp.getStatusLine().getStatusCode());

        /* fetch the created tx from the endpoint */
        final HttpGet getTx =
            new HttpGet(txLocation + "/" + pid);
        final GraphStore graphStore = getGraphStore(getTx);
        logger.debug(graphStore.toString());

        assertTrue(graphStore.toDataset().asDatasetGraph().contains(Node.ANY,
                createURI(txLocation + "/" + pid), ANY, ANY));

        /* fetch the created tx from the endpoint */
        final HttpGet getObj =
            new HttpGet(serverAddress + "/" + pid);
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
    public void testTransactionKeepAlive() throws Exception {
        /* create a tx */
        final HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
        final HttpResponse response = execute(createTx);
        assertEquals(201, response.getStatusLine().getStatusCode());

        final String txLocation = response.getFirstHeader("Location").getValue();
        final HttpPost renewTx = new HttpPost(txLocation + "/fcr:tx");
        assertEquals(204, getStatus(renewTx));
    }

    @Test
    public void testCreateDoStuffAndCommitTransaction() throws Exception {
        /* create a tx */
        final String txLocation = createTransaction();

        /* create a new object inside the tx */
        final String objectInTxCommit = randomUUID().toString();
        final HttpPost postNew =
            new HttpPost(txLocation);
        postNew.addHeader("Slug", objectInTxCommit);
        HttpResponse resp = execute(postNew);
        assertEquals(201, resp.getStatusLine().getStatusCode());

        /* fetch the created tx from the endpoint */
        final HttpGet getTx = new HttpGet(txLocation + "/" + objectInTxCommit);
        GraphStore graphStore = getGraphStore(getTx);

        logger.debug(graphStore.toString());

        assertTrue(graphStore.toDataset().asDatasetGraph().contains(ANY,
                createURI(txLocation + "/" + objectInTxCommit), ANY, ANY));

        /* fetch the object-in-tx outside of the tx */
        final HttpGet getObj =
                new HttpGet(serverAddress + objectInTxCommit);
        resp = execute(getObj);
        assertEquals(
                "Expected to not find our object within the scope of the transaction",
                404, resp.getStatusLine().getStatusCode());

        /* and commit */
        final HttpPost commitTx =
            new HttpPost(txLocation + "/fcr:tx/fcr:commit");
        resp = execute(commitTx);

        assertEquals(204, resp.getStatusLine().getStatusCode());

        /* fetch the object-in-tx outside of the tx after it has been committed */
        final HttpGet getObjCommitted =
            new HttpGet(serverAddress + objectInTxCommit);
        graphStore = getGraphStore(getObjCommitted);

        assertTrue("Expected to  find our object after the transaction was committed",
                      graphStore.toDataset().asDatasetGraph()
                          .contains(ANY, createURI(serverAddress + objectInTxCommit), ANY, ANY));

    }

    @Test
    public void testCreateDoStuffAndCommitTransactionSeparateConnections() throws Exception {
        /* create a tx */
        final String txLocation = createTransaction();

        /* create a new object inside the tx */
        final String objectInTxCommit = randomUUID().toString();
        client = createClient();
        final HttpPost postNew =
                new HttpPost(txLocation);
        postNew.addHeader("Slug", objectInTxCommit);
        HttpResponse resp = execute(postNew);
        assertEquals(201, resp.getStatusLine().getStatusCode());

        /* fetch the created tx from the endpoint */
        client = createClient();
        final HttpGet getTx = new HttpGet(txLocation + "/" + objectInTxCommit);
        GraphStore graphStore = getGraphStore(getTx);

        logger.debug(graphStore.toString());

        assertTrue(graphStore.toDataset().asDatasetGraph().contains(ANY,
                                                                    createURI(txLocation + "/" + objectInTxCommit),
                                                                    ANY,
                                                                    ANY));

        /* fetch the object-in-tx outside of the tx */
        client = createClient();
        final HttpGet getObj =
                new HttpGet(serverAddress + objectInTxCommit);
        resp = execute(getObj);
        assertEquals(
                "Expected to not find our object within the scope of the transaction",
                404, resp.getStatusLine().getStatusCode());

        /* and commit */
        client = createClient();
        final HttpPost commitTx =
                new HttpPost(txLocation + "/fcr:tx/fcr:commit");
        resp = execute(commitTx);

        assertEquals(204, resp.getStatusLine().getStatusCode());

        /* fetch the object-in-tx outside of the tx after it has been committed */
        client = createClient();
        final HttpGet getObjCommitted =
                new HttpGet(serverAddress + objectInTxCommit);
        graphStore = getGraphStore(getObjCommitted);

        assertTrue("Expected to  find our object after the transaction was committed",
                   graphStore.toDataset().asDatasetGraph()
                           .contains(ANY, createURI(serverAddress + objectInTxCommit), ANY, ANY));

    }

    /**
     * Tests whether a Sparql update is visible within a transaction
     * and if the update is made persistent along with the commit.
     * @throws Exception
     */
    @Test
    public void testIngestNewWithSparqlPatchWithinTransaction() throws Exception {
        final String objectInTxCommit = randomUUID().toString();

        /* create new tx */
        final String txLocation = createTransaction();

        client = createClient();
        final HttpPost postNew =
                new HttpPost(txLocation);
        postNew.addHeader("Slug", objectInTxCommit);
        HttpResponse resp = execute(postNew);
        assertEquals(201, resp.getStatusLine().getStatusCode());
        final String newObjectLocation = resp.getFirstHeader("Location").getValue();

        /* update sparql */
        final HttpPatch method = new HttpPatch(newObjectLocation);
        method.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity entity = new BasicHttpEntity();
        final String title = "this is a new title";
        entity.setContent(new ByteArrayInputStream(
                ("INSERT { <> <http://purl.org/dc/elements/1.1/title> \"" + title + "\" } WHERE {}")
                        .getBytes()));
        method.setEntity(entity);
        final HttpResponse responseFromPatch = client.execute(method);
        final int status = responseFromPatch.getStatusLine().getStatusCode();
        assertEquals("Didn't get a 204 status! Got status:\n" + status,
                NO_CONTENT.getStatusCode(), status);

        /* make sure the change was made within the tx */
        final HttpGet httpGet = new HttpGet(newObjectLocation);
        final GraphStore graphStore = getGraphStore(httpGet);
        assertTrue("The sparql update did not succeed within a transaction",
                graphStore.contains(ANY, createResource(newObjectLocation).asNode(),
                DC_TITLE.asNode(), createPlainLiteral(title)
                .asNode()));

        /* commit */
        client = createClient();
        final HttpPost commitTx =
                new HttpPost(txLocation + "/fcr:tx/fcr:commit");
        resp = execute(commitTx);

        assertEquals(204, resp.getStatusLine().getStatusCode());

        /* it must exist after commit */
        client = createClient();
        final HttpGet getObjCommitted =
                new HttpGet(serverAddress + objectInTxCommit);
        final GraphStore graphStoreAfterCommit = getGraphStore(getObjCommitted);
        assertTrue("The inserted triple does not exist after the transaction has committed",
                graphStoreAfterCommit.contains(ANY, ANY,
                DC_TITLE.asNode(), createPlainLiteral(title)
                .asNode()));


    }

    @Test
    public void testGetNonExistingObject() throws Exception {
        final String txLocation = createTransaction();
        final String newObjectLocation = txLocation + "/idontexist";
        final HttpGet httpGet = new HttpGet(newObjectLocation);

        client = createClient();
        final HttpResponse responseFromGet = client.execute(httpGet);
        final int status = responseFromGet.getStatusLine().getStatusCode();
        assertEquals("Status should be 404", 404, status);
    }

    /**
     * Tests that transactions cannot be hijacked
     */
    @Test
    public void testTransactionHijackingNotPossible() throws Exception {

        /* "fedoraAdmin" creates a transaction */
        final HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
        final HttpResponse response = executeWithBasicAuth(createTx, "fedoraAdmin", "fedoraAdmin");
        assertEquals("Status should be 201 after creating a transaction with user fedoraAdmin",
                201, response.getStatusLine().getStatusCode());
        final String txLocation = response.getFirstHeader("Location").getValue();

        /* "fedoraUser" puts to "fedoraAdmin"'s transaction and fails */
        final HttpPut putFedoraUser = new HttpPut(txLocation);
        final HttpResponse responseFedoraUser = executeWithBasicAuth(putFedoraUser, "fedoraUser", "fedoraUser");
        assertEquals("Status should be 410 because putting on a transaction of a different user is not allowed",
                410, responseFedoraUser.getStatusLine().getStatusCode());

        /* anonymous user puts to "fedoraAdmin"'s transaction and fails */
        final HttpPut putTxAnon = new HttpPut(txLocation);
        final HttpResponse responseTxAnon = execute(putTxAnon);
        assertEquals("Status should be 410 because putting on a transaction of a different user is not allowed",
                410, responseTxAnon.getStatusLine().getStatusCode());

        /* transaction is still intact and "fedoraAdmin" - the owner - can successfully put to it */
        final String objectInTxCommit = randomUUID().toString();
        final HttpPut putToExistingTx = new HttpPut(txLocation + "/" + objectInTxCommit);
        final HttpResponse responseFromPutToTx = executeWithBasicAuth(putToExistingTx, "fedoraAdmin", "fedoraAdmin");
        assertEquals("Status should be 201 after putting", 201, responseFromPutToTx.getStatusLine().getStatusCode());

    }

    /**
     * Tests that transactions cannot be hijacked,
     * even if created by an anonymous user
     */
    @Test
    public void testTransactionHijackingNotPossibleAnoymous() throws Exception {

        /* anonymous user creates a transaction */
        final String txLocation = createTransaction();

        /* fedoraAdmin attempts to puts to anonymous transaction and fails */
        final HttpPut putFedoraAdmin = new HttpPut(txLocation);
        final HttpResponse responseFedoraAdmin = executeWithBasicAuth(putFedoraAdmin, "fedoraAdmin", "fedoraAdmin");
        assertEquals("Status should be 410 because putting on a transaction of a different user is not permitted",
                410, responseFedoraAdmin.getStatusLine().getStatusCode());

        /* fedoraUser attempts to put to anonymous transaction and fails */
        final HttpPut putFedoraUser = new HttpPut(txLocation);
        final HttpResponse responseFedoraUser = executeWithBasicAuth(putFedoraUser, "fedoraUser", "fedoraUser");
        assertEquals("Status should be 410 because putting on a transaction of a different user is not permitted",
                410, responseFedoraUser.getStatusLine().getStatusCode());

        /* transaction is still intact and any anonymous user can successfully put to it */
        final String objectInTxCommit = randomUUID().toString();
        final HttpPut putToExistingTx = new HttpPut(txLocation + "/" + objectInTxCommit);
        final HttpResponse responseFromPutToTx = execute(putToExistingTx);
        assertEquals("Status should be 201 after putting", 201, responseFromPutToTx.getStatusLine().getStatusCode());

    }

    /**
     * Tests that caching headers are disabled during transactions.
     * 
     * The Last-Modified date is only updated when Modeshape's
     * session.save() is invoked. Since this operation is not
     * invoked during a Fedora transaction, the Last-Modified
     * date never gets updated during a transaction and the
     * delivered content may be stale. Etag won't work either
     * because it is directly derived from Last-Modified.
     *
     * @throws Exception
     */
    @Test
    public void testNoCachingHeadersDuringTransaction() throws Exception {
        final String txLocation = createTransaction();
        final HttpPost post = new HttpPost(txLocation);
        final HttpResponse response = execute(post);

        assertFalse("Last-Modified header must not be present during a transaction",
                    response.containsHeader("Last-Modified"));
        assertFalse("ETag header must not be present during a transaction",
                    response.containsHeader("ETag"));

        // Assert Cache-Control headers are present to invalidate caches
        final String location = response.getFirstHeader("Location").getValue();
        final HttpGet get = new HttpGet(location);
        final HttpResponse responseFromGet = execute(get);

        final Header[] headers = responseFromGet.getHeaders(CACHE_CONTROL);
        assertEquals("Two cache control headers expected: ", 2, headers.length);
        assertEquals(CACHE_CONTROL + "expected", CACHE_CONTROL, headers[0].getName());
        assertEquals(CACHE_CONTROL + "expected", CACHE_CONTROL, headers[1].getName());
        assertEquals("must-revalidate expected", "must-revalidate", headers[0].getValue());
        assertEquals("max-age=0 expected", "max-age=0", headers[1].getValue());
    }

    /**
     * Tests that transactions are treated as atomic with regards to nodes.
     *
     * A common use case for applications written against fedora is that an
     * operation checks some property of a fedora object and acts on it
     * accordingly.  In order for this to work in a multi-client or
     * multi-threaded environment that comparison+action combination needs to
     * be atomic.
     *
     * Imagine a scenario where we have one process that deletes all objects
     * in the repository that don't have a "preserve" property set to the
     * literal "true", and we have any number of other clients that add such
     * a property.
     *
     * We want to ensure that there is no way for a client to successfully
     * add this property between when the "deleter" process has determined that
     * no such property exists and when it deletes the object.
     *
     * In other words, if there are only clients adding properties and the
     * "deleter" deleting objects it should not be possible for an object
     * to be deleted if a client has added a title and received a successful
     * http response code.
     */
    @Test
    @Ignore("Until we implement some kind of record level locking.")
    public void testTransactionAndConcurrentConflictingUpdate() throws Exception {
        final String preserveProperty = "preserve";
        final String preserveValue = "true";

        /* create the object in question */
        final String objPid = randomUUID().toString();
        createObject(objPid);

         /* create the deleter transaction */
        final String deleterTxLocation = createTransaction();
        final String deleterTxId = deleterTxLocation.substring(serverAddress.length());

        /* assert that the object is eligible for delete in the transaction */
        verifyProperty("No preserve property should be set!", objPid, deleterTxId,
                preserveProperty, preserveValue, false);

        /* delete that object in the transaction */
        final HttpDelete delete = new HttpDelete(deleterTxLocation + "/" + objPid);
        assertEquals(204, execute(delete).getStatusLine().getStatusCode());

        /* fetch the object-deleted-in-tx outside of the tx */
        final HttpGet getObj =
                new HttpGet(serverAddress + objPid);
        HttpResponse resp = execute(getObj);
        assertEquals("Expected to find our object outside the scope of the tx,"
                + " despite it being deleted in the uncommitted transaction.",
                200, resp.getStatusLine().getStatusCode());

        /* mark the object as not deletable outside the context of the transaction */
        setProperty(objPid, preserveProperty, preserveValue);

        /* commit that transaction */
        final HttpPost commitDeleteTx =
                new HttpPost(deleterTxLocation + "/fcr:tx/fcr:commit");
        resp = execute(commitDeleteTx);
        assertNotEquals("Transaction is not atomic with regards to the object!",
                204, resp.getStatusLine().getStatusCode());
    }

    private void verifyProperty(final String assertionMessage, final String pid, final String txId,
                                final String propertyUri, final String propertyValue,
                                final boolean shouldExist) throws IOException {
        client = createClient();
        final HttpGet getObjCommitted =
                new HttpGet(serverAddress + (txId != null ? txId + "/" : "") + pid);
        final GraphStore graphStore = getGraphStore(getObjCommitted);
        final boolean exists = graphStore.contains(ANY, createResource(serverAddress + pid).asNode(),
                ResourceFactory.createProperty(propertyUri).asNode(), createPlainLiteral(propertyValue)
                .asNode());
        if (shouldExist) {
            assertTrue(assertionMessage, exists);
        } else {
            assertFalse(assertionMessage, exists);
        }
    }

}
