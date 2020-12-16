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
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
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
import static org.fcrepo.http.commons.session.TransactionConstants.TX_ENDPOINT_REL;
import static org.fcrepo.http.commons.session.TransactionConstants.TX_PREFIX;
import static org.fcrepo.kernel.api.RdfLexicon.ARCHIVAL_GROUP;
import static org.fcrepo.kernel.impl.TransactionImpl.TIMEOUT_SYSTEM_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response.Status;

import org.fcrepo.common.lang.CheckedRunnable;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.storage.ocfl.CommitType;
import org.fcrepo.storage.ocfl.DefaultOcflObjectSessionFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.context.TestExecutionListeners;

/**
 * <p>TransactionsIT class.</p>
 *
 * @author awoods
 */
@TestExecutionListeners(
        listeners = { TestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class TransactionsIT extends AbstractResourceIT {

    public static final long REAP_INTERVAL = 1000;

    public static final Pattern TX_ID_PATTERN = Pattern.compile(".+/" + TX_PREFIX + "([0-9a-f\\-]+)$");

    private static final String ARCHIVAL_GROUP_TYPE = "<" + ARCHIVAL_GROUP + ">;rel=\"type\"";

    private DefaultOcflObjectSessionFactory objectSessionFactory;
    private ContainmentIndex containmentIndex;
    private OcflPropsConfig ocflConfig;

    @Before
    public void setup() {
        objectSessionFactory = getBean(DefaultOcflObjectSessionFactory.class);
        containmentIndex = getBean("containmentIndex", ContainmentIndex.class);
        ocflConfig = getBean(OcflPropsConfig.class);
    }

    @After
    public void after() {
        objectSessionFactory.setDefaultCommitType(CommitType.NEW_VERSION);
    }

    @Test
    public void testRootHasTxEndpoint() throws Exception {
        final var getRoot = new HttpGet(serverAddress);
        try (final CloseableHttpResponse response = execute(getRoot)) {
            final String txEndpointUri = serverAddress + TX_PREFIX;
            checkForLinkHeader(response, txEndpointUri, TX_ENDPOINT_REL);
        }
    }

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

            final String commitUri = serverAddress + TX_PREFIX + txId;
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

        try (final CloseableHttpResponse resp = execute(new HttpGet(location))) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), getStatus(resp));
            assertHeaderIsRfc1123Date(resp, ATOMIC_EXPIRES_HEADER);
            consume(resp.getEntity());
        }

        sleep(REAP_INTERVAL * 2);
        try {
            assertEquals("Transaction did not expire", GONE.getStatusCode(), getStatus(new HttpGet(location)));
        } finally {
            System.clearProperty(TIMEOUT_SYSTEM_PROPERTY);
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
        try (final CloseableHttpResponse resp = execute(postNew)) {
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
    public void testRollbackShouldNotLeaveDbInPartiallyUpdatedState() throws IOException {
        /* create a tx */
        final String txLocation = createTransaction();

        /* create a new object inside the tx */
        final String newLocation;
        final HttpPost postNew = new HttpPost(serverAddress);
        postNew.addHeader(ATOMIC_ID_HEADER, txLocation);
        try (final CloseableHttpResponse resp = execute(postNew)) {
            assertEquals(CREATED.getStatusCode(), getStatus(resp));
            newLocation = getLocation(resp);
        }

        final var resourceId = StringUtils.substringAfterLast(newLocation, "/");

        /* fetch the created tx from the endpoint */
        try (final CloseableDataset dataset = getDataset(addTxTo(new HttpGet(newLocation), txLocation))) {
            assertTrue(dataset.asDatasetGraph().contains(ANY, createURI(newLocation), ANY, ANY));
        }
        /* fetch the created tx from the endpoint */
        assertEquals("Expected to not find our object within the scope of the transaction",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(newLocation)));

        addConflictingContainmentRecord(resourceId);

        // Commit transaction -- should fail
        assertEquals(CONFLICT.getStatusCode(), getStatus(new HttpPut(txLocation)));

        assertEquals("Rolled back transaction should be gone",
                GONE.getStatusCode(), getStatus(new HttpGet(txLocation)));

        assertEquals("Expected to not find our object after rollback",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(newLocation)));

        assertObjectDoesNotExistOnDisk(FedoraId.create(resourceId));
    }

    @Test
    public void rollbackShouldRollbackAllObjectsModifiedInTransaction() throws IOException {
        final String txLocation1 = createTransaction();

        final var bin1 = UUID.randomUUID().toString();
        final var bin2 = UUID.randomUUID().toString();

        putBinary(bin1, txLocation1, "test 1");

        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txLocation1)));

        assertBinaryContent("test 1", bin1, null);

        final String txLocation2 = createTransaction();

        putBinary(bin1, txLocation2, "test 1 -- updated!");
        putBinary(bin2, txLocation2, "test 2 -- I'm new!");

        assertBinaryContent("test 1 -- updated!", bin1, txLocation2);
        assertBinaryContent("test 2 -- I'm new!", bin2, txLocation2);

        addConflictingContainmentRecord(bin2);

        // Commit transaction -- should fail
        assertEquals(CONFLICT.getStatusCode(), getStatus(new HttpPut(txLocation2)));

        assertEquals("Rolled back transaction should be gone",
                GONE.getStatusCode(), getStatus(new HttpGet(txLocation2)));

        assertBinaryContent("test 1", bin1, null);
        assertEquals("Expected to not find our object after rollback",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(serverAddress + bin2)));
        assertObjectDoesNotExistOnDisk(FedoraId.create(bin2));
    }

    @Test
    public void rollbackSucceedsWhenAutoVersioningNotUsedAndFailureInDbCommit() throws IOException {
        objectSessionFactory.setDefaultCommitType(CommitType.UNVERSIONED);

        final String txLocation1 = createTransaction();

        final var bin1 = UUID.randomUUID().toString();
        final var bin2 = UUID.randomUUID().toString();

        putBinary(bin1, txLocation1, "test 1");

        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txLocation1)));

        assertBinaryContent("test 1", bin1, null);

        final String txLocation2 = createTransaction();

        putBinary(bin1, txLocation2, "test 1 -- updated!");
        putBinary(bin2, txLocation2, "test 2 -- I'm new!");

        assertBinaryContent("test 1 -- updated!", bin1, txLocation2);
        assertBinaryContent("test 2 -- I'm new!", bin2, txLocation2);

        addConflictingContainmentRecord(bin2);

        // Commit transaction -- should fail
        assertEquals(CONFLICT.getStatusCode(), getStatus(new HttpPut(txLocation2)));

        assertEquals("Rolled back transaction should be gone",
                GONE.getStatusCode(), getStatus(new HttpGet(txLocation2)));

        // bin1 was not changed
        assertBinaryContent("test 1", bin1, null);

        // bin2 was rolled back
        assertEquals("Expected to not find our object after rollback",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(serverAddress + bin2)));
        assertObjectDoesNotExistOnDisk(FedoraId.create(bin2));
    }

    @Test
    public void rollbackFailsWhenAutoVersioningNotUsedAndFailureInOcflCommit() throws IOException {
        objectSessionFactory.setDefaultCommitType(CommitType.UNVERSIONED);

        final String txLocation1 = createTransaction();

        // need prefix so they're ordered deterministically
        final var bin1 = "1" + UUID.randomUUID().toString();
        final var bin2 = "2" + UUID.randomUUID().toString();
        final var bin3 = "3" + UUID.randomUUID().toString();

        putBinary(bin1, txLocation1, "test 1");
        putBinary(bin3, txLocation1, "test 3");

        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txLocation1)));

        assertBinaryContent("test 1", bin1, null);
        assertBinaryContent("test 3", bin3, null);

        final String txLocation2 = createTransaction();

        putBinary(bin1, txLocation2, "test 1 -- updated!");
        putBinary(bin2, txLocation2, "test 2 -- I'm new!");
        putBinary(bin3, txLocation2, "test 3 -- updated!");

        assertBinaryContent("test 1 -- updated!", bin1, txLocation2);
        assertBinaryContent("test 2 -- I'm new!", bin2, txLocation2);

        corruptStagedBinary(bin3);

        // Commit transaction -- should fail
        assertEquals(CONFLICT.getStatusCode(), getStatus(new HttpPut(txLocation2)));

        assertEquals("Rolled back transaction should be gone",
                GONE.getStatusCode(), getStatus(new HttpGet(txLocation2)));

        // bin1 was not rolled back
        assertBinaryContent("test 1 -- updated!", bin1, null);

        // bin2 was rolled back
        assertEquals("Expected to not find our object after rollback",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(serverAddress + bin2)));
        assertObjectDoesNotExistOnDisk(FedoraId.create(bin2));

        // bin1 was not committed
        assertBinaryContent("test 3", bin3, null);
    }

    @Test
    public void rollbackSucceedsWhenAutoVersioningUsedAndFailureInOcflCommit() throws IOException {
        final String txLocation1 = createTransaction();

        // need prefix so they're ordered deterministically
        final var bin1 = "1" + UUID.randomUUID().toString();
        final var bin2 = "2" + UUID.randomUUID().toString();
        final var bin3 = "3" + UUID.randomUUID().toString();

        putBinary(bin1, txLocation1, "test 1");
        putBinary(bin3, txLocation1, "test 3");

        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txLocation1)));

        assertBinaryContent("test 1", bin1, null);
        assertBinaryContent("test 3", bin3, null);

        final String txLocation2 = createTransaction();

        putBinary(bin1, txLocation2, "test 1 -- updated!");
        putBinary(bin2, txLocation2, "test 2 -- I'm new!");
        putBinary(bin3, txLocation2, "test 3 -- updated!");

        assertBinaryContent("test 1 -- updated!", bin1, txLocation2);
        assertBinaryContent("test 2 -- I'm new!", bin2, txLocation2);

        corruptStagedBinary(bin3);

        // Commit transaction -- should fail
        assertEquals(CONFLICT.getStatusCode(), getStatus(new HttpPut(txLocation2)));

        assertEquals("Rolled back transaction should be gone",
                GONE.getStatusCode(), getStatus(new HttpGet(txLocation2)));

        // bin1 was rolled back
        assertBinaryContent("test 1", bin1, null);

        // bin2 was rolled back
        assertEquals("Expected to not find our object after rollback",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(serverAddress + bin2)));
        assertObjectDoesNotExistOnDisk(FedoraId.create(bin2));

        // bin1 was not committed
        assertBinaryContent("test 3", bin3, null);
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
        try (final CloseableHttpResponse resp = execute(postNew)) {
            assertEquals(CREATED.getStatusCode(), resp.getStatusLine().getStatusCode());
            assertHasAtomicId(txLocation, resp);
            datasetLoc = getLocation(resp);
        }

        // Retrieve the object inside of the transaction
        final HttpGet getRequest = new HttpGet(datasetLoc);
        getRequest.addHeader(ATOMIC_ID_HEADER, txLocation);
        try (final CloseableDataset dataset = getDataset(getRequest)) {
            assertTrue(dataset.asDatasetGraph().contains(ANY,
                        createURI(datasetLoc), ANY, ANY));
        }

        /* fetch the object-in-tx outside of the tx */
        assertEquals("Expected to not find our object within the scope of the transaction",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(datasetLoc)));
        /* and commit */
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txLocation)));

        /* fetch the object-in-tx outside of the tx after it has been committed */
        try (final CloseableDataset dataset = getDataset(new HttpGet(datasetLoc))) {
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
        try (final CloseableHttpResponse resp = execute(postNew)) {
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
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txLocation)));

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
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txLocation)));
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
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txLocation)));

        // Attempt to create object inside completed tx
        final HttpPost postNew = new HttpPost(serverAddress);
        postNew.addHeader(ATOMIC_ID_HEADER, txLocation);
        assertEquals(Status.CONFLICT.getStatusCode(), getStatus(postNew));
    }

    @Test
    public void testRequestWithBareTxUuid() throws Exception {
        final String txLocation = createTransaction();
        final String uuid = txLocation.substring(txLocation.lastIndexOf("/") + 1);

        final String newLocation;
        // Attempt to create object in tx using just the uuid
        final HttpPost postNew = addTxTo(new HttpPost(serverAddress), uuid);
        try (final CloseableHttpResponse resp = execute(postNew)) {
            assertEquals(CREATED.getStatusCode(), getStatus(resp));
            newLocation = getLocation(resp);
        }

        // Retrieve in tx using uuid
        assertEquals(OK.getStatusCode(), getStatus(addTxTo(new HttpGet(newLocation), uuid)));

        // Commit tx
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txLocation)));

        // Retrieve outside of tx
        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(newLocation)));
    }

    @Test
    public void testRequestWitMadeUpTxUuid() throws Exception {
        // Attempt to create object inside completed tx
        final HttpPost postNew = new HttpPost(serverAddress);
        postNew.addHeader(ATOMIC_ID_HEADER, UUID.randomUUID().toString());
        assertEquals(Status.CONFLICT.getStatusCode(), getStatus(postNew));
    }

    /**
     * Test creating and deleting an object in a single transaction.
     * @throws Exception http client might throw an exception.
     */
    @Test
    public void testCreateAndDeleteInSingleTransaction() throws Exception {
        // Do an RDF Container
        final String txLocation = createTransaction();
        final HttpPost post = postObjMethod();
        addTxTo(post, txLocation);
        final String containerUri;
        try (final CloseableHttpResponse response = execute(post)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            containerUri = getLocation(response);
        }
        final HttpDelete delete = new HttpDelete(containerUri);
        addTxTo(delete, txLocation);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(delete));

        // Container was never committed, so we get a 404 instead of 410.
        final HttpGet getContainer = new HttpGet(containerUri);
        addTxTo(getContainer, txLocation);
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(getContainer));

        // Now do a binary
        final HttpPost postBin = postObjMethod();
        addTxTo(postBin, txLocation);
        postBin.setEntity(new StringEntity("Some test text"));
        final String binaryUri;
        try (final CloseableHttpResponse response = execute(postBin)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            binaryUri = getLocation(response);
        }
        final HttpDelete deleteBin = new HttpDelete(binaryUri);
        addTxTo(deleteBin, txLocation);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteBin));

        // Container was never committed, so we get a 404 instead of 410.
        final HttpGet getBinary = new HttpGet(binaryUri);
        addTxTo(getBinary, txLocation);
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(getBinary));
    }

    /**
     * Test creating an AG and child container in a transaction and deleting the child container.
     * @throws Exception http client might throw an exception.
     */
    @Test
    public void testCreateAndDeleteInSingleTransactionSubPathFull() throws Exception {
        final String txLocation = createTransaction();
        // Create an Archival Group.
        final HttpPost httpPost = postObjMethod();
        addTxTo(httpPost, txLocation);
        httpPost.setHeader("Link", ARCHIVAL_GROUP_TYPE);
        final String parent;
        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            parent = getLocation(response);
        }
        // Test GET the parent.
        final HttpGet parentGet = new HttpGet(parent);
        addTxTo(parentGet, txLocation);
        assertEquals(OK.getStatusCode(), getStatus(parentGet));

        // Create a container child of the AG.
        final HttpPost postChild = new HttpPost(parent);
        addTxTo(postChild, txLocation);
        final String id;
        try (final CloseableHttpResponse response = execute(postChild)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            id = getLocation(response);
        }
        // Test GET the child.
        final HttpGet childGetContainer = new HttpGet(id);
        addTxTo(childGetContainer, txLocation);
        assertEquals(OK.getStatusCode(), getStatus(childGetContainer));
        // Delete the child container.
        final HttpDelete deleteContainer = new HttpDelete(id);
        addTxTo(deleteContainer, txLocation);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteContainer));
        // Test GET the child again.
        final HttpGet childGetContainer2 = new HttpGet(id);
        addTxTo(childGetContainer2, txLocation);
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(childGetContainer2));

        // Create a binary child of the AG.
        final HttpPost postBinary = new HttpPost(parent);
        addTxTo(postBinary, txLocation);
        final String binaryId;
        try (final CloseableHttpResponse response = execute(postBinary)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            binaryId = getLocation(response);
        }
        // Test GET the child.
        final HttpGet childGetBinary = new HttpGet(binaryId);
        addTxTo(childGetBinary, txLocation);
        assertEquals(OK.getStatusCode(), getStatus(childGetBinary));
        // Delete the child container.
        final HttpDelete deleteBinary = new HttpDelete(binaryId);
        addTxTo(deleteBinary, txLocation);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteBinary));
        // Test GET the child again.
        final HttpGet childGetBinary2 = new HttpGet(binaryId);
        addTxTo(childGetBinary2, txLocation);
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(childGetBinary2));
    }


    /**
     * Test creating an AG. Then create a child container and delete it in the same transaction.
     * @throws Exception http client might throw an exception.
     */
    @Test
    public void testCreateAndDeleteInSingleTransactionSubPathPartial() throws Exception {
        // Create an Archival Group.
        final HttpPost httpPost = postObjMethod();
        httpPost.setHeader("Link", ARCHIVAL_GROUP_TYPE);
        final String parent;
        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            parent = getLocation(response);
        }
        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(parent)));

        final String txLocation = createTransaction();
        // Create a child RDF container.
        final HttpPost postContainer = new HttpPost(parent);
        addTxTo(postContainer, txLocation);
        final String containerId;
        try (final CloseableHttpResponse response = execute(postContainer)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            containerId = getLocation(response);
        }
        // Test GET the container.
        final HttpGet getContainer = new HttpGet(containerId);
        addTxTo(getContainer, txLocation);
        assertEquals(OK.getStatusCode(), getStatus(getContainer));
        // Delete the container.
        final HttpDelete deleteContainer = new HttpDelete(containerId);
        addTxTo(deleteContainer, txLocation);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteContainer));
        // Test GET the container again.
        final HttpGet childGetContainer2 = new HttpGet(containerId);
        addTxTo(childGetContainer2, txLocation);
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(childGetContainer2));

        // Create a child binary.
        final HttpPost postBinary = new HttpPost(parent);
        addTxTo(postBinary, txLocation);
        postBinary.setEntity(new StringEntity("Some test text"));
        final String binaryId;
        try (final CloseableHttpResponse response = execute(postBinary)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            binaryId = getLocation(response);
        }
        // Test GET the binary.
        final HttpGet getBinary = new HttpGet(binaryId);
        addTxTo(getBinary, txLocation);
        assertEquals(OK.getStatusCode(), getStatus(getBinary));
        // Delete the binary.
        final HttpDelete deleteBinary = new HttpDelete(binaryId);
        addTxTo(deleteBinary, txLocation);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteBinary));
        // Test GET the binary again.
        final HttpGet childGetBinary2 = new HttpGet(binaryId);
        addTxTo(childGetBinary2, txLocation);
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(childGetBinary2));
    }

    @Test
    public void lockAgResourceInTxWhenAgPartIsUpdated() throws Exception {
        final var agId = getRandomUniqueId();
        final var childId = agId + "/child";
        final var binaryId = agId + "/child/bin";

        putAg(agId);
        putContainer(childId, null);
        putBinary(binaryId, null, "binary");

        final String txLocation = createTransaction();

        putBinary(binaryId, txLocation, "binary - updated!");

        assertConcurrentUpdate(() -> putBinary(binaryId, null, "concurrent update!"));
        assertConcurrentUpdate(() -> updateContainerTitle(childId, "concurrent update!", null));
        assertConcurrentUpdate(() -> updateContainerTitle(agId, "concurrent update!", null));
        assertConcurrentUpdate(() -> putContainer(agId + "/child2", null));

        commitTransaction(txLocation);

        assertEquals("binary - updated!", getResource(binaryId, null));

        putBinary(binaryId, null, "unlocked!");
        assertEquals("unlocked!", getResource(binaryId, null));
    }

    @Test
    public void lockBothBinaryAndDescWhenEitherIsUpdatedInTx() throws Exception {
        final var binaryId = getRandomUniqueId();

        putBinary(binaryId, null, "binary");

        final String txLocation = createTransaction();

        putBinary(binaryId, txLocation, "binary - updated!");

        assertConcurrentUpdate(() -> putBinary(binaryId, null, "concurrent update!"));
        assertConcurrentUpdate(() -> updateContainerTitle(binaryId + "/fcr:metadata", "concurrent update!", null));

        commitTransaction(txLocation);

        assertEquals("binary - updated!", getResource(binaryId, null));
    }

    @Test
    public void concurrentSparqlUpdatesShouldNotBeAllowed() throws Exception {
        final var containerId = getRandomUniqueId();

        putContainer(containerId, null);

        final String txLocation = createTransaction();

        updateContainerTitle(containerId, "new title", txLocation);

        assertConcurrentUpdate(() -> updateContainerTitle(containerId, "concurrent update!", null));

        commitTransaction(txLocation);

        final var response = getResource(containerId, null);
        assertTrue("title should have been updated", response.contains("new title"));
        assertFalse("concurrent update should not have been applied", response.contains("concurrent update!"));
    }

    private void assertConcurrentUpdate(final CheckedRunnable runnable) throws Exception {
        try {
            runnable.run();
            fail("Request should fail because the resource should be locked by another transaction.");
        } catch (HttpResponseException e) {
            assertEquals(CONFLICT.getStatusCode(), e.getStatusCode());
            assertTrue("concurrent update exception",
                    e.getReasonPhrase().contains("updated by another transaction"));
        }
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

    private void assertObjectDoesNotExistOnDisk(final FedoraId fedoraId) {
        assertFalse(String.format("Expected %s to not exist on disk", fedoraId), objectExistsOnDisk(fedoraId));
    }

    private void assertObjectExistsOnDisk(final FedoraId fedoraId) {
        assertTrue(String.format("Expected %s to exist on disk", fedoraId), objectExistsOnDisk(fedoraId));
    }

    private boolean objectExistsOnDisk(final FedoraId fedoraId) {
        try (final var session = objectSessionFactory.newSession(fedoraId.getResourceId())) {
            return session.containsResource(fedoraId.getResourceId());
        }
    }

    private void assertBinaryContent(final String expected,
                                     final String id,
                                     final String txLocation) throws IOException {
        final var actual = getResource(id, txLocation);
        assertEquals("Expected binary content for " + id, expected, actual);
    }

    private void putAg(final String id) throws IOException {
        final var put = putObjMethod(id);
        put.setHeader("Link", ARCHIVAL_GROUP_TYPE);
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }
    }

    private void putContainer(final String id, final String txLocation) throws IOException {
        final var put = putObjMethod(id);
        if (txLocation != null) {
            put.addHeader(ATOMIC_ID_HEADER, txLocation);
        }
        try (final CloseableHttpResponse resp = execute(put)) {
            final var code = getStatus(resp);
            if (code != CREATED.getStatusCode()) {
                throw new HttpResponseException(code, EntityUtils.toString(resp.getEntity()));
            }
        }
    }

    private void updateContainerTitle(final String id, final String title, final String txLocation) throws IOException {
        final var patch = patchObjMethod(id);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity("INSERT { <> <http://purl.org/dc/elements/1.1/title> \"" + title +
                "\" } WHERE {}"));
        if (txLocation != null) {
            patch.addHeader(ATOMIC_ID_HEADER, txLocation);
        }
        try (final CloseableHttpResponse resp = execute(patch)) {
            final var code = getStatus(resp);
            if (code != NO_CONTENT.getStatusCode()) {
                throw new HttpResponseException(code, EntityUtils.toString(resp.getEntity()));
            }
        }
    }

    private void putBinary(final String id, final String txLocation, final String content) throws IOException {
        final HttpPut put = new HttpPut(serverAddress + id);
        put.setEntity(new StringEntity(content == null ? "" : content));
        put.setHeader(CONTENT_TYPE, TEXT_PLAIN);
        put.setHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        if (txLocation != null) {
            put.addHeader(ATOMIC_ID_HEADER, txLocation);
        }
        try (final CloseableHttpResponse resp = execute(put)) {
            final var code = getStatus(resp);
            if (code != CREATED.getStatusCode() && code != NO_CONTENT.getStatusCode()) {
                throw new HttpResponseException(code, EntityUtils.toString(resp.getEntity()));
            }
        }
    }

    private String getResource(final String id, final String txLocation) throws IOException {
        final var get = new HttpGet(serverAddress + id);
        if (txLocation != null) {
            get.addHeader(ATOMIC_ID_HEADER, txLocation);
        }
        try (final CloseableHttpResponse response = execute(get)) {
            final HttpEntity entity = response.getEntity();
            final String content = EntityUtils.toString(entity);
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            return content;
        }
    }

    private void commitTransaction(final String txLocation) throws IOException {
        try (final CloseableHttpResponse resp = execute(new HttpPut(txLocation))) {
            final var code = getStatus(resp);
            if (code != NO_CONTENT.getStatusCode()) {
                throw new HttpResponseException(code, EntityUtils.toString(resp.getEntity()));
            }
        }
    }

    private void addConflictingContainmentRecord(final String resourceId) {
        final var txId = UUID.randomUUID().toString();
        containmentIndex.addContainedBy(txId, FedoraId.getRepositoryRootId(), FedoraId.create(resourceId));
        containmentIndex.commitTransaction(txId);
    }

    private void corruptStagedBinary(final String resourceId) {
        final var lastPart = resourceId.contains("/") ?
                StringUtils.substringAfterLast(resourceId, "/") : resourceId;
        final var stagingRoot = ocflConfig.getFedoraOcflStaging();
        try {
            final var binary = Files.find(stagingRoot, 10, (file, attrs) -> {
                return attrs.isRegularFile() &&
                        file.getFileName().toString().equals(lastPart);
            }).findFirst().get();

            Files.writeString(binary, "corrupted!");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
