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
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.fcrepo.kernel.RdfLexicon.HAS_LOCK_TOKEN;
import static org.fcrepo.kernel.RdfLexicon.LOCKS;

/**
 * @author Mike Durbin
 */
public class FedoraLocksIT extends AbstractResourceIT implements FedoraJcrTypes {

    /**
     * Test whether a lock can be created, it prevents updates
     * without the token, allows updates with the token and can
     * be deleted only with the token.
     * @throws IOException
     */
    @Test
    public void testBasicLockingScenario() throws IOException {
        final String pid = UUID.randomUUID().toString();
        createObject(pid);

        final String lockToken = getLockToken(lockObject(pid));

        Assert.assertEquals("Lock must prevent property updates!",
                CONFLICT.getStatusCode(),
                setProperty(pid, null, null, "test", "test").getStatusLine().getStatusCode());

        Assert.assertEquals(NO_CONTENT.getStatusCode(),
                setProperty(pid, null, lockToken, "test", "test").getStatusLine().getStatusCode());

        Assert.assertEquals("The lock must only be able to be removed with the lock token!",
                CONFLICT.getStatusCode(),
                unlockObject(pid, null).getStatusLine().getStatusCode());

        Assert.assertEquals(NO_CONTENT.getStatusCode(),
                unlockObject(pid, lockToken).getStatusLine().getStatusCode());

        Assert.assertEquals("Unlocked object must be able to be updated now!",
                NO_CONTENT.getStatusCode(),
                setProperty(pid, null, null, "test", "test").getStatusLine().getStatusCode());
    }

    /**
     * Test whether a created lock can be viewed by both
     * the creator (supplying the token) and by a request
     * without the token and that the token is only visible
     * when it was supplied to the session.
     */
    @Test
    public void testLockMetadataAvailability() throws IOException {
        final String pid = UUID.randomUUID().toString();
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

        Assert.assertEquals("Session with the lock token must be able to delete the lock!",
                NO_CONTENT.getStatusCode(),
                unlockObject(pid, lockToken).getStatusLine().getStatusCode());

    }

    /**
     * Assumes (and asserts) that a request to lock a resource was
     * successful and returns the lock token from the response.
     */
    public String getLockToken(HttpResponse response) {
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
    public HttpResponse lockObject(String pid) throws IOException {
        final HttpPost post = new HttpPost(serverAddress + pid + "/" + FCR_LOCK);
        return client.execute(post);
    }

    /**
     * Attempts to unlock lock an object.
     */
    public HttpResponse unlockObject(String pid, String lockToken) throws IOException {
        final HttpDelete delete = new HttpDelete(serverAddress + pid + "/" + FCR_LOCK);
        addLockToken(delete, lockToken);
        return client.execute(delete);
    }

    public GraphStore getLockProperties(String pid, String lockToken) throws IOException {
        final HttpGet get = new HttpGet(serverAddress + pid + "/" + FCR_LOCK);
        addLockToken(get, lockToken);
        return getGraphStore(get);
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
    protected HttpResponse setProperty(final String pid,
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

}

