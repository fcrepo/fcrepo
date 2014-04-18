/**
 * Copyright 2013 DuraSpace, Inc.
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

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.update.GraphStore;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.IOException;

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.fcrepo.kernel.RdfLexicon.HAS_LOCK_TOKEN;
import static org.fcrepo.kernel.RdfLexicon.LOCKS;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Mike Durbin
 */
public class FedoraLocksIT extends AbstractResourceIT implements FedoraJcrTypes {

    private static final Logger LOGGER = getLogger(FedoraLocksIT.class);

    private static final long TIMEOUT = 300;

    /**
     * Test whether a lock can be created, it prevents updates
     * without the token, allows updates with the token and can
     * be deleted only with the token.
     * @throws IOException
     */
    @Test
    public void testBasicLockingScenario() throws IOException {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final String lockToken = getLockToken(lockObject(pid));

        assertCannotSetPropertyWithoutLockToken(pid);

        assertCanSetProperty("With the lock token, property updates must be allowed!", pid, lockToken);

        Assert.assertEquals("The lock must only be able to be removed with the lock token!",
                CONFLICT.getStatusCode(),
                unlockObject(pid, null).getStatusLine().getStatusCode());

        assertUnlockWithToken(pid, lockToken);
    }

    /**
     * Test whether a created lock can be viewed by both
     * the creator (supplying the token) and by a request
     * without the token and that the token is only visible
     * when it was supplied to the session.
     */
    @Test
    public void testLockMetadataAvailability() throws IOException {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final String lockToken = getLockToken(lockObject(pid));

        final Node lockURI = createURI(serverAddress + pid + "/" + FCR_LOCK);
        final Node nodeURI = createURI(serverAddress + pid);

        GraphStore lockTriples = getLockProperties(pid, lockToken);

        Assert.assertTrue("Lock relationship must be present!",
                lockTriples.contains(ANY, lockURI, createURI(LOCKS.getURI()), nodeURI));

        Assert.assertTrue("Lock token must be present!",
                lockTriples.contains(ANY, lockURI, createURI(HAS_LOCK_TOKEN.getURI()), createLiteral(lockToken)));

        lockTriples = getLockProperties(pid, null);

        Assert.assertTrue("Lock relationship must be present!",
                lockTriples.contains(ANY, lockURI, createURI(LOCKS.getURI()), nodeURI));

        Assert.assertFalse("Lock token must be hidden!",
                lockTriples.contains(ANY, lockURI, createURI(HAS_LOCK_TOKEN.getURI()), createLiteral(lockToken)));

        assertUnlockWithToken(pid, lockToken);
    }

    /**
     * A basic test to ensure that deep locking prevents updates to child nodes
     * while a shallow lock does not.
     */
    @Test
    public void testDeepVsShallowLocks() throws IOException {
        final String pid = getRandomUniquePid();
        createObject(pid);
        final String childPid = pid + "/" + getRandomUniquePid();
        createObject(childPid);

        // Test shallow lock
        final String shallowLockToken = getLockToken(lockObject(pid));

        assertCannotSetPropertyWithoutLockToken(pid);
        assertCanSetProperty(pid, shallowLockToken);
        assertCanSetProperty("Properties of a child of a shallow locked node"
                + " should be updatable without the lock token!", childPid, null);
        assertUnlockWithToken(pid, shallowLockToken);

        // Test deep lock
        final String deepLockToken = getLockToken(lockObject(pid, TIMEOUT, true));

        assertCannotSetPropertyWithoutLockToken(pid);
        assertCannotSetPropertyWithoutLockToken("Deep lock must prevent property updates on child nodes!", childPid);
        assertCanSetProperty(pid, deepLockToken);
        assertCanSetProperty("Properties of a child of a shallow locked node"
                + " should be updatable without the lock token!", childPid, deepLockToken);
        assertUnlockWithToken(pid, deepLockToken);
        assertCanSetProperty("Child of unlocked object must be able to be updated now!", childPid, null);
    }

    /**
     * A basic test to ensure that conflicting locks cannot be created.
     */
    @Test
    public void testConflictingLocks() throws IOException {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final String lockToken = getLockToken(lockObject(pid));
        Assert.assertEquals("May not take out a second lock on a locked node!",
                CONFLICT.getStatusCode(), lockObject(pid).getStatusLine().getStatusCode());
        assertUnlockWithToken(pid, lockToken);
    }

    /**
     * A basic test to ensure that shallow locks can be taken out within
     * the same hierarchy.
     */
    @Test
    public void testChildLocks() throws IOException {
        final String pid = getRandomUniquePid();
        createObject(pid);
        final String childPid = pid + "/" + getRandomUniquePid();
        createObject(childPid);

        final String childLockToken = getLockToken(lockObject(childPid));
        final String parentShallowLockToken = getLockToken(lockObject(pid));
        assertUnlockWithToken(childPid, childLockToken);
        assertUnlockWithToken(pid, parentShallowLockToken);
    }

    /**
     * A basic test to ensure that a deep lock may not be
     * taken out when a child is already locked.
     */
    @Test
    public void testConflictingDeepLocks() throws IOException {
        final String pid = getRandomUniquePid();
        createObject(pid);
        final String childPid = pid + "/" + getRandomUniquePid();
        createObject(childPid);

        final String childLockToken = getLockToken(lockObject(childPid));
        Assert.assertEquals("May not take out a deep lock when a child is locked!",
                CONFLICT.getStatusCode(), lockObject(pid, TIMEOUT, true).getStatusLine().getStatusCode());
    }

    /**
     * A sanity test to make sure you get an error if you try to unlock
     * with a bogus token.
     */
    @Test
    public void testSupplyBogusLockToken() throws IOException {
        final String pid = getRandomUniquePid();
        createObject(pid);
        final String lockToken = getLockToken(lockObject(pid));
        Assert.assertEquals("Should get a BAD_REQUEST when using bogus lock token!",
                BAD_REQUEST.getStatusCode(), unlockObject(pid, lockToken + "-fake").getStatusLine().getStatusCode());
        assertUnlockWithToken(pid, lockToken);

    }

    /**
     * A test to make sure that if you try to access (GET) a lock
     * that doesn't exist you'll get a NOT_FOUND.
     */
    @Test
    public void testViewMissingLock() throws IOException {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpGet get = new HttpGet(serverAddress + pid + "/" + FCR_LOCK);
        Assert.assertEquals("May not view lock that doesn't exist!",
                NOT_FOUND.getStatusCode(), execute(get).getStatusLine().getStatusCode());
    }

    /**
     * Test locking a node that doesn't exist.
     */
    @Test
    public void testLockMissingNode() throws IOException {
        final String pid = getRandomUniquePid();
        Assert.assertEquals("Must get a NOT_FOUND response when locking a path at which no node exists.",
                NOT_FOUND.getStatusCode(), lockObject(pid).getStatusLine().getStatusCode());
    }

    /**
     * Test that a transaction will fail on commit if a lock has been taken
     * out on any touched resources between the completion of that operation
     * within the transaction and the commit.
     *
     * This test won't work until https://www.pivotaltracker.com/story/show/69734118
     * has been resolved.
     */
    @Test
    @Ignore
    public void testTransactionFailureOnCommit() throws IOException {
        final String rootPid = getRandomUniquePid();
        final String childPid = rootPid + "/" + getRandomUniquePid();
        createObject(rootPid);
        createObject(childPid);

        final String inTxPid = getRandomUniquePid();
        final String txId = createTransaction().replace(serverAddress, "");
        LOGGER.info("Created transaction: " + txId);

        // perform non-conflicting operation in transaction
        createObject(txId + "/" + inTxPid);

        // perform conflicting operation in transaction
        assertCanSetProperty("Must be able to update properties on unlocked node within transaction!",
                txId + "/" + childPid, null);

        // take out a lock on an affected resource (out of transaction)
        final String lockToken = getLockToken(lockObject(rootPid, TIMEOUT, true));

        // commit the transaction (which should fail with CONFLICT)
        Assert.assertEquals(CONFLICT.getStatusCode(), commitTransaction(txId).getStatusLine().getStatusCode());

        // verify that non-conflicting operation was not successful.
        Assert.assertEquals(NOT_FOUND.getStatusCode(), getObjectProperties(inTxPid).getStatusLine().getStatusCode());
    }

    /**
     * Test whether a created and abandoned lock will actually timeout
     * within the ballpark of the specified timeout interval.
     *
     * Modeshape's lock cleanup interval is not configurable and is
     * currently set to around 10 minutes.  It is not practical to verify
     * this during our integration tests, so this test is ignored.
     *
     * Furthermore, this test fails anyway.
     */
    @Test
    @Ignore
    public void testLockTimeout() throws IOException {
        final long maxWaitTimeMs = 6000000;
        final long timeoutInSeconds = 5;
        final long timeoutInMs = timeoutInSeconds * 1000;
        final String pid = getRandomUniquePid();
        createObject(pid);
        LOGGER.info("Starting lock timeout test -- May take up to " + maxWaitTimeMs + "ms");
        final long start = System.currentTimeMillis();
        getLockToken(lockObject(pid, timeoutInSeconds, false));
        final long timeout = start + timeoutInMs;
        final int statusOfImmediateRequest
                = setProperty(pid, null, null, "test", "test").getStatusLine().getStatusCode();
        final long timeOfImmediateRequestCompletion = System.currentTimeMillis();
        final long msUntilImmediateRequest = timeOfImmediateRequestCompletion - start;
        if (msUntilImmediateRequest >= (timeoutInMs)) {
            Assert.fail("Tests are running too slow to gauge timeouts! (2 requests took "
                    + msUntilImmediateRequest + "ms!");
        } else {
            Assert.assertEquals("Request within " + msUntilImmediateRequest
                    + "ms of lock creation should be affected by a " + timeoutInMs
                    + "ms lock!", CONFLICT.getStatusCode(), statusOfImmediateRequest);
        }
        long sleepTime = 1000;
        while (System.currentTimeMillis() < (start + maxWaitTimeMs)) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                // no worries.. we'll sleep again when the loop iterates
            }
            sleepTime *=2;
            final long attemptTime = System.currentTimeMillis();
            final int status = setProperty(pid, null, null, "test", "test").getStatusLine().getStatusCode();
            final long duration = attemptTime - start;
            if (status != NO_CONTENT.getStatusCode()) {
                if (duration > timeoutInMs) {
                    LOGGER.warn("After " + duration + "ms, the lock still holds.");
                } else {
                    LOGGER.info("After " + duration + "ms, the lock still holds.");
                }
            } else {
                Assert.assertTrue("Lock must be held as long as the specified timeout! (unlocked after "
                        + duration + "ms).", duration > timeoutInMs);
                return;
            }
        }
        Assert.assertEquals("The " + timeoutInMs + "ms lock should have timed out after "
                + (System.currentTimeMillis() - start) + "ms!", NO_CONTENT.getStatusCode(),
                setProperty(pid, null, null, "test", "test").getStatusLine().getStatusCode());
    }

    /**
     * Unlocks a lock with its token, asserts that it is successful and further
     * asserts that property updates can again be made without the token.
     */
    private void assertUnlockWithToken(String pid, String lockToken) throws IOException {
        Assert.assertEquals(NO_CONTENT.getStatusCode(),
                unlockObject(pid, lockToken).getStatusLine().getStatusCode());

        assertCanSetProperty("Unlocked object must be able to be updated now!", pid, null);
    }

    /**
     * Assumes (and asserts) that a request to lock a resource was
     * successful and returns the lock token from the response.
     */
    private String getLockToken(HttpResponse response) {
        final StatusLine status = response.getStatusLine();
        Assert.assertEquals(CREATED.getStatusCode(),
                response.getStatusLine().getStatusCode());
        final Header lockToken = response.getFirstHeader("Lock-Token");
        Assert.assertNotNull("Lock-Token was not provided in response!", lockToken);
        return lockToken.getValue();
    }

    /**
     * Attempts to lock an object.
     */
    private HttpResponse lockObject(String pid) throws IOException {
        return lockObject(pid, TIMEOUT, false);
    }

    /**
     * Attempts to lock an object with the given timeout and
     * deep locking status.
     */
    private HttpResponse lockObject(String pid, long timeout, boolean deep) throws IOException {
        StringBuffer query = new StringBuffer();
        if (timeout >= 1) {
            query.append("timeout=" + timeout);
        }
        if (deep) {
            if (query.length() > 0) {
                query.append("&");
            }
            query.append("deep=true");
        }
        final HttpPost post = new HttpPost(serverAddress + pid + "/" + FCR_LOCK
                + (query.length() > 0 ? "?" + query.toString() : ""));
        return client.execute(post);
    }

    /**
     * Attempts to unlock lock an object.
     */
    private HttpResponse unlockObject(String pid, String lockToken) throws IOException {
        final HttpDelete delete = new HttpDelete(serverAddress + pid + "/" + FCR_LOCK);
        addLockToken(delete, lockToken);
        return client.execute(delete);
    }

    private GraphStore getLockProperties(String pid, String lockToken) throws IOException {
        final HttpGet get = new HttpGet(serverAddress + pid + "/" + FCR_LOCK);
        addLockToken(get, lockToken);
        return getGraphStore(get);
    }

    private void assertCanSetProperty(final String pid, final String lockToken) throws IOException {
        assertCanSetProperty(null, pid, lockToken);
    }

    private void assertCanSetProperty(final String message, final String pid, final String lockToken)
            throws IOException {
        final String propertyName = getRandomPropertyName();
        final String propertyValue = getRandomPropertyValue();
        Assert.assertEquals(message == null
                    ? "Properties of a locked node must be updatable with the token!"
                    : message, NO_CONTENT.getStatusCode(),
                setProperty(pid, null, lockToken, propertyName, propertyValue).getStatusLine().getStatusCode());
    }

    private void assertCannotSetPropertyWithoutLockToken(final String pid) throws IOException {
        assertCannotSetPropertyWithoutLockToken(null, pid);
    }

    private void assertCannotSetPropertyWithoutLockToken(final String message, final String pid) throws IOException {
        final String propertyName = getRandomPropertyName();
        final String propertyValue = getRandomPropertyValue();
        Assert.assertEquals(message == null ? "Lock must prevent property updates!" : message,
                CONFLICT.getStatusCode(),
                setProperty(pid, null, null, propertyName, propertyValue).getStatusLine().getStatusCode());
    }

    /**
     * Attempts to set a property on an object.
     * @param pid the pid
     * @param txId a transaction ID (may be null)
     * @param lockToken a lock token (may be null)
     * @param propertyUri the property to set
     * @param value the value to set
     * @return the HttpResponse
     */
    private HttpResponse setProperty(final String pid,
                                       final String txId,
                                       final String lockToken,
                                       final String propertyUri,
                                       final String value) throws IOException {
        final HttpPatch postProp = new HttpPatch(serverAddress
                + (txId != null ? txId + "/" : "") + pid);
        addLockToken(postProp, lockToken);
        postProp.setHeader("Content-Type", "application/sparql-update");
        final String updateString =
                "INSERT { <"
                        + serverAddress + pid
                        + "> <" + propertyUri + "> \"" + value + "\" } WHERE { }";
        postProp.setEntity(new StringEntity(updateString));
        return execute(postProp);
    }

    /**
     * Adds a "Lock-Token" header to the request if token is not null.
     */
    private void addLockToken(HttpRequestBase method, String token) {
        if (token != null) {
            method.addHeader("Lock-Token", token);
        }
    }

    /**
     * Creates a transaction, asserts that it's successful and
     * returns the transaction location.
     * @return
     * @throws IOException
     */
    private String createTransaction() throws IOException {
        final HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
        final HttpResponse response = execute(createTx);
        Assert.assertEquals(CREATED.getStatusCode(), response.getStatusLine().getStatusCode());
        return response.getFirstHeader("Location").getValue();
    }

    private HttpResponse commitTransaction(String txId) throws IOException {
        final HttpPost commitTx = new HttpPost(serverAddress + txId + "/fcr:tx/fcr:commit");
        return execute(commitTx);
    }

    private HttpResponse getObjectProperties(String pid) throws IOException {
        return execute(new HttpGet(serverAddress + pid));
    }
}

