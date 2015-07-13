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
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.core.Link;

import org.fcrepo.http.commons.test.util.CloseableGraphStore;

import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.junit.Ignore;
import org.junit.Test;

/**
 * <p>FedoraNodesIT class.</p>
 *
 * @author awoods
 * @author ajs6f
 */
public class FedoraNodesIT extends AbstractResourceIT {

    @Test
    public void testCopy() throws IOException {
        final String subject = serverAddress + getRandomUniqueId();
        final String location = getLocation(postObjMethod());
        final HttpCopy request = new HttpCopy(location);
        request.addHeader("Destination", subject);
        executeAndClose(request);
        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(subject)));
        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(location)));
    }

    @Test
    public void testCopyDestExists() throws IOException {
        final HttpCopy request = new HttpCopy(getLocation(postObjMethod()));
        request.addHeader("Destination", getLocation(postObjMethod()));
        assertEquals(PRECONDITION_FAILED.getStatusCode(), getStatus(request));
    }

    @Test
    public void testCopyInvalidDest() throws IOException {
        final String location1 = getLocation(postObjMethod());
        final HttpCopy request = new HttpCopy(location1);
        request.addHeader("Destination", serverAddress + "non/existent/path");
        assertEquals(CONFLICT.getStatusCode(), getStatus(request));
    }

    @Test
    public void testMoveAndTombstoneFromRoot() throws IOException {
        final String subject = serverAddress + getRandomUniqueId();
        final String location = serverAddress + getRandomUniqueId();
        createObjectAndClose(location.substring(serverAddress.length()));
        final HttpMove request = new HttpMove(location);
        request.addHeader("Destination", subject);
        executeAndClose(request);

        final HttpGet httpGet = new HttpGet(subject);
        assertEquals(OK.getStatusCode(), getStatus(httpGet));
        try (final CloseableHttpResponse originalResult = execute(new HttpGet(location))) {
            assertEquals(GONE.getStatusCode(), getStatus(originalResult));
            final Link tombstone = Link.valueOf(originalResult.getFirstHeader("Link").getValue());
            assertEquals("hasTombstone", tombstone.getRel());
        }
    }

    @Test
    public void testMoveAndTombstone() throws IOException {
        final String id = getRandomUniqueId();
        final String location = getLocation(postObjMethod());

        final HttpMove request = new HttpMove(location);
        request.addHeader("Destination", serverAddress + id);
        executeAndClose(request);

        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(serverAddress + id)));
        try (final CloseableHttpResponse originalResult = execute(new HttpGet(location))) {
            assertEquals(GONE.getStatusCode(), getStatus(originalResult));
            final Link tombstone = Link.valueOf(originalResult.getFirstHeader("Link").getValue());
            assertEquals("hasTombstone", tombstone.getRel());
        }
    }

    @Test
    public void testMoveDestExists() throws IOException {
        final HttpMove request = new HttpMove(getLocation(postObjMethod()));
        request.addHeader("Destination", getLocation(postObjMethod()));
        assertEquals(PRECONDITION_FAILED.getStatusCode(), getStatus(request));
    }

    @Test
    public void testMoveInvalidDest() throws IOException {
        final HttpMove request = new HttpMove(getLocation(postObjMethod()));
        request.addHeader("Destination", serverAddress + "non/existent/destination");
        assertEquals(CONFLICT.getStatusCode(), getStatus(request));
    }

    @Test
    public void testMoveWithBadEtag() throws IOException {
        final HttpMove request = new HttpMove(getLocation(postObjMethod()));
        request.addHeader("Destination", serverAddress + getRandomUniqueId());
        request.addHeader("If-Match", "\"doesnt-match\"");
        assertEquals(PRECONDITION_FAILED.getStatusCode(), getStatus(request));
    }

    @NotThreadSafe // HttpRequestBase is @NotThreadSafe
    private class HttpCopy extends HttpRequestBase {

        /**
         * @throws IllegalArgumentException if the uri is invalid.
         */
        public HttpCopy(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        @Override
        public String getMethod() {
            return "COPY";
        }
    }

    @NotThreadSafe // HttpRequestBase is @NotThreadSafe
    private class HttpMove extends HttpRequestBase {

        /**
         * @throws IllegalArgumentException if the uri is invalid.
         */
        public HttpMove(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        @Override
        public String getMethod() {
            return "MOVE";
        }
    }

    /**
     * I should be able to copy objects from the repository to a federated filesystem.
     *
     * @throws IOException exception thrown during this function
    **/
    @Ignore("Enabled once the FedoraFileSystemConnector becomes readable/writable")
    public void testCopyToProjection() throws IOException {
        // create object in the repository
        final String pid = getRandomUniqueId();
        createDatastream(pid, "ds1", "abc123");

        // copy to federated filesystem
        final HttpCopy request = new HttpCopy(serverAddress + pid);
        request.addHeader("Destination", serverAddress + "files/copy-" + pid);
        assertEquals(CREATED.getStatusCode(), getStatus(request));

        // federated copy should now exist
        final HttpGet copyGet = new HttpGet(serverAddress + "files/copy-" + pid);
        assertEquals(OK.getStatusCode(), getStatus(copyGet));

        // repository copy should still exist
        final HttpGet originalGet = new HttpGet(serverAddress + pid);
        assertEquals(OK.getStatusCode(), getStatus(originalGet));
    }

    /**
     * I should be able to copy objects from a federated filesystem to the repository.
    **/
    @Test
    public void testCopyFromProjection() {
        final String destination = serverAddress + "copy-" + getRandomUniqueId() + "-ds1";
        final String source = serverAddress + "files/FileSystem1/ds1";

        // ensure the source is present
        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(source)));

        // copy to repository
        final HttpCopy request = new HttpCopy(source);
        request.addHeader("Destination", destination);
        assertEquals(CREATED.getStatusCode(), getStatus(request));

        // repository copy should now exist
        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(destination)));
        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(source)));
    }

    /**
     * I should be able to move a node within a federated filesystem with
     * properties preserved.
     *
     * @throws IOException exception thrown during this function
    **/
    @Ignore("Enabled once the FedoraFileSystemConnector becomes readable/writable")
    public void testFederatedMoveWithProperties() throws IOException {
        // create object on federation
        final String pid = getRandomUniqueId();
        final String source = serverAddress + "files/" + pid + "/src";
        createObject("files/" + pid + "/src");

        // add properties
        final HttpPatch patch = new HttpPatch(source);
        patch.addHeader("Content-Type", "application/sparql-update");
        patch.setEntity(new StringEntity(
                "insert { <> <http://purl.org/dc/elements/1.1/identifier> \"identifier.123\" . "
                        + "<> <http://purl.org/dc/elements/1.1/title> \"title.123\" } where {}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(patch));

        // move object
        final String destination = serverAddress + "files/" + pid + "/dst";
        final HttpMove request = new HttpMove(source);
        request.addHeader("Destination", destination);
        assertEquals(CREATED.getStatusCode(), getStatus(request));

        // check properties
        final HttpGet get = new HttpGet(destination);
        get.addHeader("Accept", "application/n-triples");
        try (final CloseableGraphStore graphStore = getGraphStore(get)) {
            assertTrue(graphStore.contains(ANY, createURI(destination),
                    createURI("http://purl.org/dc/elements/1.1/identifier"), createLiteral("identifier.123")));
            assertTrue(graphStore.contains(ANY, createURI(destination),
                    createURI("http://purl.org/dc/elements/1.1/title"), createLiteral("title.123")));
        }
    }
}
