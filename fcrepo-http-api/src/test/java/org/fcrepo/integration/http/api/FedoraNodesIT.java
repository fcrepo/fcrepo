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

import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static org.apache.http.annotation.ThreadingBehavior.UNSAFE;
import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.net.URI;

import javax.ws.rs.core.Link;

import org.apache.http.annotation.Contract;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
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
            final Link tombstone = Link.valueOf(originalResult.getFirstHeader(LINK).getValue());
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
            final Link tombstone = Link.valueOf(originalResult.getFirstHeader(LINK).getValue());
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

    @Test
    public void testMoveBinary() throws IOException {
        final String id = getRandomUniqueId();
        final String oldLocation = getLocation(putDSMethod(id, "oldName", "test content"));
        final String newLocation = getLocation(postObjMethod()) + "/newName";

        final HttpMove request = new HttpMove(oldLocation);
        request.addHeader("Destination", newLocation);
        assertEquals(CREATED.getStatusCode(), getStatus(request));

        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(newLocation)));
        assertEquals(GONE.getStatusCode(), getStatus(new HttpGet(oldLocation)));
    }

    @Test
    public void testRenameBinary() throws IOException {
        final String id = getRandomUniqueId();
        final String oldLocation = getLocation(putDSMethod(id, "oldName", "test content"));
        final String newLocation = oldLocation + "2";

        final HttpMove request = new HttpMove(oldLocation);
        request.addHeader("Destination", newLocation);
        assertEquals(CREATED.getStatusCode(), getStatus(request));

        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(newLocation)));
        assertEquals(GONE.getStatusCode(), getStatus(new HttpGet(oldLocation)));
    }

    @Test
    public void testRenameContainer() throws IOException {
        final String oldLocation = getLocation(postObjMethod());
        final String newLocation = oldLocation + "2";

        final HttpMove request = new HttpMove(oldLocation);
        request.addHeader("Destination", newLocation);
        assertEquals(CREATED.getStatusCode(), getStatus(request));

        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(newLocation)));
        assertEquals(GONE.getStatusCode(), getStatus(new HttpGet(oldLocation)));
    }

    @Contract(threading = UNSAFE) // HttpRequestBase is @NotThreadSafe
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

    @Contract(threading = UNSAFE) // HttpRequestBase is @NotThreadSafe
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

}
