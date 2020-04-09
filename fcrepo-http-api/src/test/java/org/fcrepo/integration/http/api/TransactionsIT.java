/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import static java.lang.Math.min;
import static java.lang.Thread.sleep;
import static java.time.Duration.ofMinutes;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.util.EntityUtils.consume;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.vocabulary.DC_11.title;
import static org.fcrepo.http.commons.session.TransactionConstants.ATOMIC_EXPIRES_HEADER;
import static org.fcrepo.http.commons.session.TransactionConstants.ATOMIC_ID_HEADER;
import static org.fcrepo.http.commons.session.TransactionConstants.EXPIRES_RFC_1123_FORMATTER;
import static org.fcrepo.http.commons.session.TransactionConstants.TX_COMMIT_REL;
import static org.fcrepo.http.commons.session.TransactionConstants.TX_COMMIT_SUFFIX;
import static org.fcrepo.http.commons.session.TransactionConstants.TX_PREFIX;
import static org.fcrepo.http.commons.session.TransactionProvider.TX_ID_PATTERN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import javax.ws.rs.core.Response.Status;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.junit.Ignore;
import org.junit.Test;

/**
 * <p>TransactionsIT class.</p>
 *
 * @author awoods
 */
public class TransactionsIT extends AbstractResourceIT {

    public static final long REAP_INTERVAL = 1000;

    public static final String TIMEOUT_SYSTEM_PROPERTY = "fcrepo.session.timeout";

    public static final String DEFAULT_TIMEOUT = Long.toString(ofMinutes(3).toMillis());

    @Test
    public void testCreateTransaction() throws IOException {
        final HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
        try (final CloseableHttpResponse response = execute(createTx)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            final var location = getLocation(response);
            String txId = null;
            final Matcher txMatcher = TX_ID_PATTERN.matcher(location);
            if (txMatcher.matches()) {
                txId = txMatcher.group(1);
            }

            assertNotNull("Expected Location header to send us to root node path within the transaction",
                    txId);

            final String commitUri = serverAddress + TX_PREFIX + txId + TX_COMMIT_SUFFIX;
            checkForLinkHeader(response, commitUri, TX_COMMIT_REL);

            assertHeaderIsRfc1123Date(response, "Expires");
        }
    }

    @Test
    public void testRequestsInTransactionThatDoestExist() {
        /* create a tx */
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(new HttpPost(serverAddress + "fcr:tx/123idontexist")));
    }

    @Test
    public void testCreateAndTimeoutTransaction() throws IOException, InterruptedException {

        /* create a short-lived tx */
        final long testTimeout = min(500, REAP_INTERVAL / 2);
        System.setProperty(TIMEOUT_SYSTEM_PROPERTY, Long.toString(testTimeout));

        /* create a tx */
        final String location = createTransaction();

        try (CloseableHttpResponse resp = execute(new HttpGet(location))) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), getStatus(resp));
            assertHeaderIsRfc1123Date(resp, ATOMIC_EXPIRES_HEADER);
            consume(resp.getEntity());
        }

        sleep(REAP_INTERVAL * 2);
        try {
            assertEquals("Transaction did not expire", GONE.getStatusCode(), getStatus(new HttpGet(location)));
        } finally {
            System.setProperty(TIMEOUT_SYSTEM_PROPERTY, DEFAULT_TIMEOUT);
            System.clearProperty("fcrepo.transactions.timeout");
        }
    }

    private void assertHeaderIsRfc1123Date(final CloseableHttpResponse response, final String headerName) {
        final Header header = response.getFirstHeader(headerName);
        assertNotNull("Header " + headerName + " was not set", header);
        try {
            EXPIRES_RFC_1123_FORMATTER.parse(header.getValue());
        } catch (final DateTimeParseException e) {
            fail("Expected header " + headerName + " to be an RFC1123 date, but was " + header.getValue());
        }
    }

    @Test
    public void testCreateDoStuffAndRollbackTransaction() throws IOException {
        /* create a tx */
        final String txLocation = createTransaction();

        /* create a new object inside the tx */
        final String newLocation;
        final HttpPost postNew = new HttpPost(serverAddress);
        postNew.addHeader(ATOMIC_ID_HEADER, txLocation);
        try (CloseableHttpResponse resp = execute(postNew)) {
            assertEquals(CREATED.getStatusCode(), getStatus(resp));
            newLocation = getLocation(resp);
        }

        /* fetch the created tx from the endpoint */
        try (final CloseableDataset dataset = getDataset(addTxTo(new HttpGet(newLocation), txLocation))) {
            assertTrue(dataset.asDatasetGraph().contains(ANY, createURI(newLocation), ANY, ANY));
        }
        /* fetch the created tx from the endpoint */
        assertEquals("Expected to not find our object within the scope of the transaction",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(newLocation)));

        /* and rollback */
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(txLocation)));

        assertEquals("Rolled back transaction should be gone",
                GONE.getStatusCode(), getStatus(new HttpGet(txLocation)));

        assertEquals("Expected to not find our object after rollback",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(newLocation)));

        assertEquals("Expected to not find our object in transaction after rollback",
                CONFLICT.getStatusCode(), getStatus(addTxTo(new HttpGet(newLocation), txLocation)));
    }

    @Test
    public void testTransactionKeepAlive() throws IOException {
        /* create a tx */
        final String txLocation = createTransaction();
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPost(txLocation)));
    }

    @Test
    public void testCreateDoStuffAndCommitTransaction() throws IOException {
        /* create a tx */
        final String txLocation = createTransaction();
        /* create a new object inside the tx */
        final HttpPost postNew = new HttpPost(serverAddress);
        postNew.addHeader(ATOMIC_ID_HEADER, txLocation);

        final String datasetLoc;
        try (CloseableHttpResponse resp = execute(postNew)) {
            assertEquals(CREATED.getStatusCode(), resp.getStatusLine().getStatusCode());
            assertHasAtomicId(txLocation, resp);
            datasetLoc = getLocation(resp);
        }

        // Retrieve the object inside of the transaction
        final HttpGet getRequest = new HttpGet(datasetLoc);
        getRequest.addHeader(ATOMIC_ID_HEADER, txLocation);
        try (CloseableDataset dataset = getDataset(getRequest)) {
            assertTrue(dataset.asDatasetGraph().contains(ANY,
                        createURI(datasetLoc), ANY, ANY));
        }

        /* fetch the object-in-tx outside of the tx */
        assertEquals("Expected to not find our object within the scope of the transaction",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(datasetLoc)));
        /* and commit */
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txLocation + TX_COMMIT_SUFFIX)));

        /* fetch the object-in-tx outside of the tx after it has been committed */
        try (CloseableDataset dataset = getDataset(new HttpGet(datasetLoc))) {
            assertTrue("Expected to  find our object after the transaction was committed",
                    dataset.asDatasetGraph().contains(ANY, createURI(datasetLoc), ANY, ANY));
        }

        assertEquals("Expect conflict when trying to retrieve from committed transaction",
                CONFLICT.getStatusCode(), getStatus(addTxTo(new HttpGet(datasetLoc), txLocation)));
    }

    private void assertHasAtomicId(final String txId, final CloseableHttpResponse resp) {
        final Header header = resp.getFirstHeader(ATOMIC_ID_HEADER);
        assertNotNull("No atomic id header present in response", header);

        assertEquals("Header did not match the expected atomic id", txId, header.getValue());
    }

    /**
     * Tests whether a Sparql update is visible within a transaction and if the update is made persistent along with
     * the commit.
     *
     * @throws IOException exception thrown during this function
     */
    @Ignore("Pending PATCH implementation")
    @Test
    public void testIngestNewWithSparqlPatchWithinTransaction() throws IOException {
        final String objectInTxCommit = getRandomUniqueId();

        /* create new tx */
        final String txLocation = createTransaction();

        final HttpPost postNew = addTxTo(new HttpPost(serverAddress), txLocation);
        final String newObjectLocation;
        try (CloseableHttpResponse resp = execute(postNew)) {
            assertEquals(CREATED.getStatusCode(), getStatus(resp));
            assertHasAtomicId(txLocation, resp);
            newObjectLocation = getLocation(resp);
        }

        /* update sparql */
        final HttpPatch method = addTxTo(new HttpPatch(newObjectLocation), txLocation);
        method.addHeader(CONTENT_TYPE, "application/sparql-update");
        final String newTitle = "this is a new title";
        method.setEntity(new StringEntity("INSERT { <> <http://purl.org/dc/elements/1.1/title> \"" + newTitle +
                "\" } WHERE {}"));
        assertEquals("Didn't get a NO CONTENT status!", NO_CONTENT.getStatusCode(), getStatus(method));

        /* make sure the change was made within the tx */
        try (final CloseableDataset dataset = getDataset(addTxTo(new HttpGet(newObjectLocation), txLocation))) {
            assertTrue("The sparql update did not succeed within a transaction", dataset.asDatasetGraph().contains(ANY,
                    createURI(newObjectLocation), title.asNode(), createLiteral(newTitle)));
        }

        // Verify that the change is not visible outside the TX
        try (final CloseableDataset dataset = getDataset(new HttpGet(newObjectLocation))) {
            assertFalse("Sparql update changes must not be visible out of tx", dataset.asDatasetGraph().contains(ANY,
                    createURI(newObjectLocation), title.asNode(), createLiteral(newTitle)));
        }

        /* commit */
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txLocation + TX_COMMIT_SUFFIX)));

        /* it must exist after commit */
        try (final CloseableDataset dataset = getDataset(new HttpGet(serverAddress + objectInTxCommit))) {
            assertTrue("The inserted triple does not exist after the transaction has committed",
                    dataset.asDatasetGraph().contains(ANY, ANY, title.asNode(), createLiteral(newTitle)));
        }
    }

    @Test
    public void testGetNonExistingObject() throws IOException {
        final String txLocation = createTransaction();
        final String newObjectLocation = serverAddress + "idontexist";
        assertEquals("Status should be NOT FOUND", NOT_FOUND.getStatusCode(),
                getStatus(addTxTo(new HttpGet(newObjectLocation), txLocation)));
    }

    /**
     * Tests that transactions cannot be hijacked
     *
     * @throws IOException exception thrown during this function
     */
    @Ignore //TODO Fix this test
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
    @Ignore //TODO Fix this test
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

    /*
     *  Caching headers should now be present during transactions. They were not in previous modeshape based versions.
     */
    @Test
    public void testCachingHeadersDuringTransaction() throws IOException {
        final String txLocation = createTransaction();
        final String location;
        try (final CloseableHttpResponse resp = execute(addTxTo(new HttpPost(serverAddress), txLocation))) {
            assertTrue("Last-Modified must be present during a transaction", resp.containsHeader("Last-Modified"));
            assertTrue("ETag must be present during a transaction", resp.containsHeader("ETag"));
            assertTrue("Expected an X-State-Token header", resp.getHeaders("X-State-Token").length > 0);
            // Assert Cache-Control headers are present to invalidate caches
            location = getLocation(resp);
        }
        try (final CloseableHttpResponse resp = execute(addTxTo(new HttpGet(location), txLocation))) {
            assertTrue("Last-Modified must be present during a transaction", resp.containsHeader("Last-Modified"));
            assertTrue("ETag must be present during a transaction", resp.containsHeader("ETag"));
            assertTrue("Expected an X-State-Token header", resp.getHeaders("X-State-Token").length > 0);
            final Header[] headers = resp.getHeaders(CACHE_CONTROL);
            assertEquals("Two cache control headers expected: ", 2, headers.length);
            assertEquals("must-revalidate expected", "must-revalidate", headers[0].getValue());
            assertEquals("max-age=0 expected", "max-age=0", headers[1].getValue());
            consume(resp.getEntity());
        }
    }

    private <T extends HttpRequestBase> T addTxTo(final T req, final String txId) {
        req.addHeader(ATOMIC_ID_HEADER, txId);
        return req;
    }

    /**
     * Test for issue https://jira.duraspace.org/browse/FCREPO-2975
     * @throws java.lang.Exception exception thrown during this function
     */
    @Test
    public void testHeadAndDeleteInTransaction() throws Exception {
        final String id = getRandomUniqueId();
        createObject(id);
        final String objUri = serverAddress + "/" + id;

        try (final CloseableHttpResponse resp = execute(new HttpHead(objUri))) {
            assertEquals(OK.getStatusCode(), resp.getStatusLine().getStatusCode());
        }

        final String txLocation = createTransaction();

        // Make a head request against the object within the transaction
        try (final CloseableHttpResponse resp = execute(addTxTo(new HttpHead(objUri), txLocation))) {
            assertEquals(OK.getStatusCode(), resp.getStatusLine().getStatusCode());
        }

        // Delete the binary within the transaction
        try (final CloseableHttpResponse resp = execute(addTxTo(new HttpDelete(objUri), txLocation))) {
            assertEquals(NO_CONTENT.getStatusCode(), resp.getStatusLine().getStatusCode());
        }

        // Commit the transaction containing deletion
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txLocation + TX_COMMIT_SUFFIX)));
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

    @Test
    public void testRequestResourceInvalidTx() throws Exception {
        /* create a tx */
        final String txLocation = createTransaction();

        // Commit tx
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txLocation + TX_COMMIT_SUFFIX)));

        // Attempt to create object inside completed tx
        final HttpPost postNew = new HttpPost(serverAddress);
        postNew.addHeader(ATOMIC_ID_HEADER, txLocation);
        assertEquals(Status.CONFLICT.getStatusCode(), getStatus(postNew));
    }

    private void verifyProperty(final String assertionMessage, final String pid, final String txId,
            final String propertyUri, final String propertyValue, final boolean shouldExist) throws IOException {
        final HttpGet getObjCommitted = new HttpGet(serverAddress + (txId != null ? txId + "/" : "") + pid);
        try (final CloseableDataset dataset = getDataset(getObjCommitted)) {
            final boolean exists = dataset.asDatasetGraph().contains(ANY,
                    createURI(serverAddress + pid), createURI(propertyUri), createLiteral(propertyValue));
            if (shouldExist) {
                assertTrue(assertionMessage, exists);
            } else {
                assertFalse(assertionMessage, exists);
            }
        }
    }
}
