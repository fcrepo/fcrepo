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

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static java.lang.Math.min;
import static java.lang.Thread.sleep;
import static java.util.Arrays.stream;
import static java.util.UUID.randomUUID;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.fcrepo.kernel.RdfLexicon.DC_TITLE;
import static org.fcrepo.kernel.impl.TransactionImpl.DEFAULT_TIMEOUT;
import static org.fcrepo.kernel.impl.TransactionImpl.TIMEOUT_SYSTEM_PROPERTY;
import static org.fcrepo.kernel.impl.services.TransactionServiceImpl.REAP_INTERVAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.fcrepo.http.commons.test.util.CloseableGraphStore;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.junit.Ignore;
import org.junit.Test;

/**
 * <p>FedoraTransactionsIT class.</p>
 *
 * @author awoods
 */
public class FedoraTransactionsIT extends AbstractResourceIT {

    @Test
    public void testCreateTransaction() throws IOException {
        final String location = createTransaction();
        logger.info("Got location {}", location);
        assertTrue("Expected Location header to send us to root node path within the transaction",
                compile("tx:[0-9a-f-]+$").matcher(location).find());
    }

    @Test
    public void testRequestsInTransactionThatDoestExist() {
        /* create a tx */
        assertEquals(GONE.getStatusCode(), getStatus(new HttpPost(serverAddress + "tx:123/objects")));
    }

    @Test
    public void testCreateAndTimeoutTransaction() throws IOException, InterruptedException {

        /* create a short-lived tx */
        final long testTimeout = min(500, REAP_INTERVAL / 2);
        System.setProperty(TIMEOUT_SYSTEM_PROPERTY, Long.toString(testTimeout));

        /* create a tx */
        final String location = createTransaction();

        try (CloseableHttpResponse resp = execute(new HttpGet(location))) {
            assertEquals(OK.getStatusCode(), getStatus(resp));
            assertTrue(stream(resp.getHeaders("Link")).anyMatch(
                    i -> i.getValue().contains("<" + serverAddress + ">;rel=\"canonical\"")));
        }

        sleep(REAP_INTERVAL * 2);
        try {
            assertEquals("Transaction did not expire", GONE.getStatusCode(), getStatus(new HttpGet(location)));
        } finally {
            System.setProperty(TIMEOUT_SYSTEM_PROPERTY, Long.toString(DEFAULT_TIMEOUT));
            System.clearProperty("fcrepo.transactions.timeout");
        }
    }

    @Test
    public void testCreateDoStuffAndRollbackTransaction() throws IOException {
        /* create a tx */
        final HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");

        final String txLocation;
        try (final CloseableHttpResponse response = execute(createTx)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            txLocation = getLocation(response);
        }

        /* create a new object inside the tx */
        final HttpPost postNew = new HttpPost(txLocation);
        final String id = getRandomUniqueId();
        postNew.addHeader("Slug", id);
        try (CloseableHttpResponse resp = execute(postNew)) {
            assertEquals(CREATED.getStatusCode(), getStatus(resp));
        }
        /* fetch the created tx from the endpoint */
        try (final CloseableGraphStore graphStore = getGraphStore(new HttpGet(txLocation + "/" + id))) {
            assertTrue(graphStore.contains(ANY, createURI(txLocation + "/" + id), ANY, ANY));
        }
        /* fetch the created tx from the endpoint */
        assertEquals("Expected to not find our object within the scope of the transaction",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(serverAddress + "/" + id)));

        /* and rollback */
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPost(txLocation + "/fcr:tx/fcr:rollback")));
    }

    @Test
    public void testTransactionKeepAlive() throws IOException {
        /* create a tx */
        try (final CloseableHttpResponse response = execute(new HttpPost(serverAddress + "fcr:tx"))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPost(getLocation(response) + "/fcr:tx")));
        }
    }

    @Test
    public void testCreateDoStuffAndCommitTransaction() throws IOException {
        /* create a tx */
        final String txLocation = createTransaction();
        /* create a new object inside the tx */
        final String objectInTxCommit = getRandomUniqueId();
        final HttpPost postNew = new HttpPost(txLocation);
        postNew.addHeader("Slug", objectInTxCommit);
        assertEquals(CREATED.getStatusCode(), getStatus(postNew));

        /* fetch the created tx from the endpoint */
        try (CloseableGraphStore graphStore = getGraphStore(new HttpGet(txLocation + "/" + objectInTxCommit))) {
            assertTrue(graphStore.contains(ANY, createURI(txLocation + "/" + objectInTxCommit), ANY, ANY));
        }
        /* fetch the object-in-tx outside of the tx */
        assertEquals("Expected to not find our object within the scope of the transaction",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(serverAddress + objectInTxCommit)));
        /* and commit */
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPost(txLocation + "/fcr:tx/fcr:commit")));

        /* fetch the object-in-tx outside of the tx after it has been committed */
        try (CloseableGraphStore graphStore = getGraphStore(new HttpGet(serverAddress + objectInTxCommit))) {
            assertTrue("Expected to  find our object after the transaction was committed",
                    graphStore.contains(ANY, createURI(serverAddress + objectInTxCommit), ANY, ANY));
        }
    }

    @Test
    public void testCreateDoStuffAndCommitTransactionSeparateConnections() throws IOException {
        /* create a tx */
        final String txLocation = createTransaction();

        /* create a new object inside the tx */
        final String objectInTxCommit = randomUUID().toString();
        final HttpPost postNew = new HttpPost(txLocation);
        postNew.addHeader("Slug", objectInTxCommit);
        assertEquals(CREATED.getStatusCode(), getStatus(postNew));

        /* fetch the created tx from the endpoint */
        try (CloseableGraphStore graphStore = getGraphStore(new HttpGet(txLocation + "/" + objectInTxCommit))) {
            assertTrue(graphStore.contains(ANY, createURI(txLocation + "/" + objectInTxCommit), ANY, ANY));
        }
        /* fetch the object-in-tx outside of the tx */
        assertEquals("Expected to not find our object within the scope of the transaction",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(serverAddress + objectInTxCommit)));

        /* and commit */
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPost(txLocation + "/fcr:tx/fcr:commit")));

        /* fetch the object-in-tx outside of the tx after it has been committed */
        try (CloseableGraphStore graphStore = getGraphStore(new HttpGet(serverAddress + objectInTxCommit))) {
            assertTrue("Expected to  find our object after the transaction was committed",
                    graphStore.contains(ANY, createURI(serverAddress + objectInTxCommit), ANY, ANY));
        }
    }

    /**
     * Tests whether a Sparql update is visible within a transaction and if the update is made persistent along with
     * the commit.
     *
     * @throws IOException exception thrown during this function
     */
    @Test
    public void testIngestNewWithSparqlPatchWithinTransaction() throws IOException {
        final String objectInTxCommit = getRandomUniqueId();

        /* create new tx */
        final String txLocation = createTransaction();

        final HttpPost postNew = new HttpPost(txLocation);
        postNew.addHeader("Slug", objectInTxCommit);
        final String newObjectLocation;
        try (CloseableHttpResponse resp = execute(postNew)) {
            assertEquals(CREATED.getStatusCode(), getStatus(resp));
            newObjectLocation = getLocation(resp);
        }

        /* update sparql */
        final HttpPatch method = new HttpPatch(newObjectLocation);
        method.addHeader("Content-Type", "application/sparql-update");
        final String title = "this is a new title";
        method.setEntity(new StringEntity("INSERT { <> <http://purl.org/dc/elements/1.1/title> \"" + title +
                "\" } WHERE {}"));
        assertEquals("Didn't get a NO CONTENT status!", NO_CONTENT.getStatusCode(), getStatus(method));
        /* make sure the change was made within the tx */
        try (final CloseableGraphStore graphStore = getGraphStore(new HttpGet(newObjectLocation))) {
            assertTrue("The sparql update did not succeed within a transaction", graphStore.contains(ANY,
                    createURI(newObjectLocation), DC_TITLE.asNode(), createLiteral(title)));
        }
        /* commit */
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPost(txLocation + "/fcr:tx/fcr:commit")));

        /* it must exist after commit */
        try (final CloseableGraphStore graphStoreAfterCommit =
                getGraphStore(new HttpGet(serverAddress + objectInTxCommit))) {
            assertTrue("The inserted triple does not exist after the transaction has committed",
                    graphStoreAfterCommit.contains(ANY, ANY, DC_TITLE.asNode(), createLiteral(title)));
        }
    }

    @Test
    public void testGetNonExistingObject() throws IOException {
        final String txLocation = createTransaction();
        final String newObjectLocation = txLocation + "/idontexist";
        assertEquals("Status should be NOT FOUND", NOT_FOUND.getStatusCode(),
                getStatus(new HttpGet(newObjectLocation)));
    }

    /**
     * Tests that transactions cannot be hijacked
     *
     * @throws IOException exception thrown during this function
     */
    @Test
    public void testTransactionHijackingNotPossible() throws IOException {

        /* "fedoraAdmin" creates a transaction */
        final String txLocation;
        try (final CloseableHttpResponse response =
                executeWithBasicAuth(new HttpPost(serverAddress + "fcr:tx"), "fedoraAdmin", "fedoraAdmin")) {
            assertEquals("Status should be CREATED after creating a transaction with user fedoraAdmin",
                    CREATED.getStatusCode(), getStatus(response));
            txLocation = getLocation(response);
        }
        /* "fedoraUser" puts to "fedoraAdmin"'s transaction and fails */
        try (final CloseableHttpResponse responseFedoraUser =
                executeWithBasicAuth(new HttpPut(txLocation), "fedoraUser", "fedoraUser")) {
            assertEquals("Status should be GONE because putting on a transaction of a different user is not allowed",
                    GONE.getStatusCode(), getStatus(responseFedoraUser));
        }
        /* anonymous user puts to "fedoraAdmin"'s transaction and fails */
        assertEquals("Status should be GONE because putting on a transaction of a different user is not allowed",
                    GONE.getStatusCode(), getStatus(new HttpPut(txLocation)));

        /* transaction is still intact and "fedoraAdmin" - the owner - can successfully put to it */
        try (final CloseableHttpResponse responseFromPutToTx =
                executeWithBasicAuth(
                        new HttpPut(txLocation + "/" + getRandomUniqueId()), "fedoraAdmin", "fedoraAdmin")) {
            assertEquals("Status should be CREATED after putting",
                    CREATED.getStatusCode(), getStatus(responseFromPutToTx));
        }
    }

    /**
     * Tests that transactions cannot be hijacked, even if created by an anonymous user
     *
     * @throws IOException exception thrown during this function
     */
    @Test
    public void testTransactionHijackingNotPossibleAnoymous() throws IOException {

        /* anonymous user creates a transaction */
        final String txLocation = createTransaction();

        /* fedoraAdmin attempts to puts to anonymous transaction and fails */
        try (final CloseableHttpResponse responseFedoraAdmin =
                executeWithBasicAuth(new HttpPut(txLocation), "fedoraAdmin", "fedoraAdmin")) {
            assertEquals(
                    "Status should be GONE because putting on a transaction of a different user is not permitted",
                    GONE.getStatusCode(), getStatus(responseFedoraAdmin));
        }

        /* fedoraUser attempts to put to anonymous transaction and fails */
        try (final CloseableHttpResponse responseFedoraUser =
                executeWithBasicAuth(new HttpPut(txLocation), "fedoraUser", "fedoraUser")) {
            assertEquals("Status should be GONE because putting on a transaction of a different user isn't permitted",
                    GONE.getStatusCode(), getStatus(responseFedoraUser));
        }

        /* transaction is still intact and any anonymous user can successfully put to it */
        assertEquals("Status should be CREATED after putting",
                CREATED.getStatusCode(), getStatus(new HttpPut(txLocation + "/" + getRandomUniqueId())));
    }

    /**
     * Tests that caching headers are disabled during transactions. The Last-Modified date is only updated when
     * Modeshape's <code>Session#save()</code> is invoked. Since this operation is not invoked during a Fedora
     * transaction, the Last-Modified date never gets updated during a transaction and the delivered content may be
     * stale. Etag won't work either because it is directly derived from Last-Modified.
     *
     * @throws IOException exception thrown during this function
     */
    @Test
    public void testNoCachingHeadersDuringTransaction() throws IOException {
        final String txLocation = createTransaction();
        final String location;
        try (final CloseableHttpResponse resp = execute(new HttpPost(txLocation))) {
            assertFalse("Last-Modified musn't be present during a transaction", resp.containsHeader("Last-Modified"));
            assertFalse("ETag must not be present during a transaction", resp.containsHeader("ETag"));
            // Assert Cache-Control headers are present to invalidate caches
            location = getLocation(resp);
        }
        try (final CloseableHttpResponse responseFromGet = execute(new HttpGet(location))) {
            final Header[] headers = responseFromGet.getHeaders(CACHE_CONTROL);
            assertEquals("Two cache control headers expected: ", 2, headers.length);
            assertEquals(CACHE_CONTROL + "expected", CACHE_CONTROL, headers[0].getName());
            assertEquals(CACHE_CONTROL + "expected", CACHE_CONTROL, headers[1].getName());
            assertEquals("must-revalidate expected", "must-revalidate", headers[0].getValue());
            assertEquals("max-age=0 expected", "max-age=0", headers[1].getValue());
        }
    }

    /**
     * Tests that transactions are treated as atomic with regards to nodes. A common use case for applications written
     * against fedora is that an operation checks some property of a fedora object and acts on it accordingly. In
     * order for this to work in a multi-client or multi-threaded environment that comparison+action combination needs
     * to be atomic. Imagine a scenario where we have one process that deletes all objects in the repository that
     * don't have a "preserve" property set to the literal "true", and we have any number of other clients that add
     * such a property. We want to ensure that there is no way for a client to successfully add this property between
     * when the "deleter" process has determined that no such property exists and when it deletes the object. In other
     * words, if there are only clients adding properties and the "deleter" deleting objects it should not be possible
     * for an object to be deleted if a client has added a title and received a successful http response code.
     *
     * @throws IOException exception thrown during this function
     */
    @Test
    @Ignore("Until we implement some kind of record level locking.")
    public void testTransactionAndConcurrentConflictingUpdate() throws IOException {
        final String preserveProperty = "preserve";
        final String preserveValue = "true";

        /* create the object in question */
        final String objId = getRandomUniqueId();
        createObject(objId);

         /* create the deleter transaction */
        final String deleterTxLocation = createTransaction();
        final String deleterTxId = deleterTxLocation.substring(serverAddress.length());

        /* assert that the object is eligible for delete in the transaction */
        verifyProperty("No preserve property should be set!", objId, deleterTxId, preserveProperty, preserveValue,
                false);

        /* delete that object in the transaction */
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(deleterTxLocation + "/" + objId)));

        /* fetch the object-deleted-in-tx outside of the tx */
        assertEquals("Expected to find our object outside the scope of the tx,"
                + " despite it being deleted in the uncommitted transaction.",
                OK.getStatusCode(), getStatus(new HttpGet(serverAddress + objId)));

        /* mark the object as not deletable outside the context of the transaction */
        setProperty(objId, preserveProperty, preserveValue);
        /* commit that transaction */
        assertNotEquals("Transaction is not atomic with regards to the object!",
                NO_CONTENT.getStatusCode(), getStatus(new HttpPost(deleterTxLocation + "/fcr:tx/fcr:commit")));
    }

    private void verifyProperty(final String assertionMessage, final String pid, final String txId,
            final String propertyUri, final String propertyValue, final boolean shouldExist) throws IOException {
        final HttpGet getObjCommitted = new HttpGet(serverAddress + (txId != null ? txId + "/" : "") + pid);
        try (final CloseableGraphStore graphStore = getGraphStore(getObjCommitted)) {
            final boolean exists = graphStore.contains(ANY,
                    createURI(serverAddress + pid), createURI(propertyUri), createLiteral(propertyValue));
            if (shouldExist) {
                assertTrue(assertionMessage, exists);
            } else {
                assertFalse(assertionMessage, exists);
            }
        }
    }
}
