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

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BasicHttpEntity;
import org.junit.Test;
import org.junit.Ignore;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.update.GraphStore;

import javax.ws.rs.core.Link;

/**
 * <p>FedoraNodesIT class.</p>
 *
 * @author awoods
 */
public class FedoraNodesIT extends AbstractResourceIT {

    @Test
    public void testCopy() throws Exception {
        final HttpResponse response  = createObject("");
        final String pid = getRandomUniquePid();
        final String location = response.getFirstHeader("Location").getValue();
        final HttpCopy request = new HttpCopy(location);
        request.addHeader("Destination", serverAddress + pid);
        client.execute(request);

        final HttpGet httpGet = new HttpGet(serverAddress + pid);

        final HttpResponse copiedResult = client.execute(httpGet);
        assertEquals(OK.getStatusCode(), copiedResult.getStatusLine().getStatusCode());

        final HttpResponse originalResult = client.execute(new HttpGet(location));
        assertEquals(OK.getStatusCode(), originalResult.getStatusLine().getStatusCode());
    }

    @Test
    public void testCopyDestExists() throws Exception {

        final HttpResponse response1 = createObject("");
        final String location1 = response1.getFirstHeader("Location").getValue();
        final HttpResponse response2 = createObject("");
        final String location2 = response2.getFirstHeader("Location").getValue();

        final HttpCopy request = new HttpCopy(location1);
        request.addHeader("Destination", location2);
        final HttpResponse result = client.execute(request);

        assertEquals(PRECONDITION_FAILED.getStatusCode(), result.getStatusLine().getStatusCode());
    }

    @Test
    public void testCopyInvalidDest() throws Exception {

        final HttpResponse response1 = createObject("");
        final String location1 = response1.getFirstHeader("Location").getValue();

        final HttpCopy request = new HttpCopy(location1);
        request.addHeader("Destination", serverAddress + "non/existent/path");
        assertEquals(CONFLICT.getStatusCode(), getStatus(request));
    }

    @Test
    public void testMoveAndTombstoneFromRoot() throws Exception {

        final String pid = getRandomUniquePid();
        final String location = serverAddress + getRandomUniquePid();
        createObject(location.substring(serverAddress.length()));

        final HttpMove request = new HttpMove(location);
        request.addHeader("Destination", serverAddress + pid);
        client.execute(request);

        final HttpGet httpGet = new HttpGet(serverAddress + pid);

        final HttpResponse copiedResult = client.execute(httpGet);
        assertEquals(OK.getStatusCode(), copiedResult.getStatusLine().getStatusCode());

        final HttpResponse originalResult = client.execute(new HttpGet(location));
        assertEquals(GONE.getStatusCode(), originalResult.getStatusLine().getStatusCode());

        final Link tombstone = Link.valueOf(originalResult.getFirstHeader("Link").getValue());
        assertEquals("hasTombstone", tombstone.getRel());
    }

    @Test
    public void testMoveAndTombstone() throws Exception {

        final String pid = getRandomUniquePid();
        final HttpResponse response = createObject("");
        final String location = response.getFirstHeader("Location").getValue();

        final HttpMove request = new HttpMove(location);
        request.addHeader("Destination", serverAddress + pid);
        client.execute(request);

        final HttpGet httpGet = new HttpGet(serverAddress + pid);

        final HttpResponse copiedResult = client.execute(httpGet);
        assertEquals(OK.getStatusCode(), copiedResult.getStatusLine().getStatusCode());

        final HttpResponse originalResult = client.execute(new HttpGet(location));
        assertEquals(GONE.getStatusCode(), originalResult.getStatusLine().getStatusCode());

        final Link tombstone = Link.valueOf(originalResult.getFirstHeader("Link").getValue());
        assertEquals("hasTombstone", tombstone.getRel());
    }

    @Test
    public void testMoveDestExists() throws Exception {

        final HttpResponse response1 = createObject("");
        final String location1 = response1.getFirstHeader("Location").getValue();
        final HttpResponse response2 = createObject("");
        final String location2 = response2.getFirstHeader("Location").getValue();

        final HttpMove request = new HttpMove(location1);
        request.addHeader("Destination", location2);
        final HttpResponse result = client.execute(request);

        assertEquals(PRECONDITION_FAILED.getStatusCode(), result.getStatusLine().getStatusCode());
    }

    @Test
    public void testMoveInvalidDest() throws Exception {

        final HttpResponse response1 = createObject("");
        final String location1 = response1.getFirstHeader("Location").getValue();

        final HttpMove request = new HttpMove(location1);
        request.addHeader("Destination", serverAddress + "non/existent/destination");
        assertEquals(CONFLICT.getStatusCode(), getStatus(request));
    }

    @Test
    public void testMoveWithBadEtag() throws Exception {

        final String pid = getRandomUniquePid();
        final HttpResponse response = createObject("");
        final String location = response.getFirstHeader("Location").getValue();

        final HttpMove request = new HttpMove(location);
        request.addHeader("Destination", serverAddress + pid);
        request.addHeader("If-Match", "\"doesnt-match\"");
        final HttpResponse moveResponse = client.execute(request);
        assertEquals(412, moveResponse.getStatusLine().getStatusCode());
    }

    @NotThreadSafe // HttpRequestBase is @NotThreadSafe
    private class HttpCopy extends HttpRequestBase {

        public final static String METHOD_NAME = "COPY";


        /**
         * @throws IllegalArgumentException if the uri is invalid.
         */
        public HttpCopy(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }

    }

    @NotThreadSafe // HttpRequestBase is @NotThreadSafe
    private class HttpMove extends HttpRequestBase {

        public final static String METHOD_NAME = "MOVE";


        /**
         * @throws IllegalArgumentException if the uri is invalid.
         */
        public HttpMove(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }

    }

    /**
     * I should be able to copy objects from the repository to a federated filesystem.
    **/
    @Ignore("Enabled once the FedoraFileSystemConnector becomes readable/writable")
    public void testCopyToProjection() throws IOException {
        // create object in the repository
        final String pid = getRandomUniquePid();
        createDatastream(pid, "ds1", "abc123");

        // copy to federated filesystem
        final HttpCopy request = new HttpCopy(serverAddress + pid);
        request.addHeader("Destination", serverAddress + "files/copy-" + pid);
        final HttpResponse copyResponse = client.execute(request);
        assertEquals(CREATED.getStatusCode(), copyResponse.getStatusLine().getStatusCode());

        // federated copy should now exist
        final HttpGet copyGet = new HttpGet(serverAddress + "files/copy-" + pid);
        final HttpResponse copiedResult = client.execute(copyGet);
        assertEquals(OK.getStatusCode(), copiedResult.getStatusLine().getStatusCode());

        // repository copy should still exist
        final HttpGet originalGet = new HttpGet(serverAddress + pid);
        final HttpResponse originalResult = client.execute(originalGet);
        assertEquals(OK.getStatusCode(), originalResult.getStatusLine().getStatusCode());
    }

    /**
     * I should be able to copy objects from a federated filesystem to the repository.
    **/
    @Test
    public void testCopyFromProjection() throws IOException {
        final String destination = serverAddress + "copy-" + getRandomUniquePid() + "-ds1";
        final String source = serverAddress + "files/FileSystem1/ds1";

        // ensure the source is present
        final HttpGet get = new HttpGet(source);
        final HttpResponse getResponse = client.execute(get);
        assertEquals(OK.getStatusCode(), getResponse.getStatusLine().getStatusCode());

        // copy to repository
        final HttpCopy request = new HttpCopy(source);
        request.addHeader("Destination", destination);
        final HttpResponse copyRequest = client.execute(request);
        assertEquals(CREATED.getStatusCode(), copyRequest.getStatusLine().getStatusCode());

        // repository copy should now exist
        final HttpGet copyGet = new HttpGet(destination);
        final HttpResponse copiedResult = client.execute(copyGet);
        assertEquals(OK.getStatusCode(), copiedResult.getStatusLine().getStatusCode());

        // federated filesystem copy should still exist
        final HttpGet originalGet = new HttpGet(source);
        final HttpResponse originalResult = client.execute(originalGet);
        assertEquals(OK.getStatusCode(), originalResult.getStatusLine().getStatusCode());
    }

    /**
     * I should be able to move a node within a federated filesystem with
     * properties preserved.
    **/
    @Ignore("Enabled once the FedoraFileSystemConnector becomes readable/writable")
    public void testFederatedMoveWithProperties() throws Exception {
        // create object on federation
        final String pid = getRandomUniquePid();
        final String source = serverAddress + "files/" + pid + "/src";
        createObject("files/" + pid + "/src");

        // add properties
        final HttpPatch patch = new HttpPatch(source);
        patch.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        final String sparql = "insert { <> <http://purl.org/dc/elements/1.1/identifier> \"identifier.123\" . "
            + "<> <http://purl.org/dc/elements/1.1/title> \"title.123\" } where {}";
        e.setContent(new ByteArrayInputStream(sparql.getBytes()));
        patch.setEntity(e);
        final HttpResponse response = client.execute(patch);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());

        // move object
        final String destination = serverAddress + "files/" + pid + "/dst";
        final HttpMove request = new HttpMove(source);
        request.addHeader("Destination", destination);
        final HttpResponse moveRequest = client.execute(request);
        assertEquals(CREATED.getStatusCode(), moveRequest.getStatusLine().getStatusCode());

        // check properties
        final HttpGet get =  new HttpGet(destination);
        get.addHeader("Accept", "application/n-triples");
        final GraphStore graphStore = getGraphStore(get);
        assertTrue(graphStore.contains(Node.ANY, NodeFactory.createURI(destination),
                                                 NodeFactory.createURI("http://purl.org/dc/elements/1.1/identifier"),
                                                 NodeFactory.createLiteral("identifier.123")));
        assertTrue(graphStore.contains(Node.ANY, NodeFactory.createURI(destination),
                                                 NodeFactory.createURI("http://purl.org/dc/elements/1.1/title"),
                                                 NodeFactory.createLiteral("title.123")));
    }


}
