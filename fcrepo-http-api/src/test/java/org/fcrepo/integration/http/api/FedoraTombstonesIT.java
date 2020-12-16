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

import static org.fcrepo.kernel.api.FedoraTypes.FCR_TOMBSTONE;
import static org.junit.Assert.assertEquals;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;

import javax.ws.rs.core.Link;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;

import org.apache.commons.codec.Charsets;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.junit.Test;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Tests related to tombstone stuff.
 *
 * @author whikloj
 */
@TestExecutionListeners(
        listeners = { TestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class FedoraTombstonesIT extends AbstractResourceIT {

    private Link getTombstoneLink(final HttpResponse res) {
        final Collection<String> headers = getLinkHeaders(res);
        return headers.stream().map(Link::valueOf).filter(t -> t.getRel().equals("hasTombstone")).findFirst()
                .orElse(null);
    }

    private Link getTombstoneLink(final HttpUriRequest req) throws IOException {
        try (final CloseableHttpResponse response = execute(req)) {
            return getTombstoneLink(response);
        }
    }

    @Test
    public void testDeleteObjectAndTombstone() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(serverAddress + id)));
        assertDeleted(id);
        final HttpGet httpGet = getObjMethod(id);
        final Link tombstone = getTombstoneLink(httpGet);
        assertEquals("hasTombstone", tombstone.getRel());
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(tombstone.getUri())));
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(httpGet));
    }

    @Test
    public void testTrailingSlashTombstoneLink() throws IOException {
        final String id = getRandomUniqueId();
        final URI expectedTombstone = URI.create(serverAddress + id + "/fcr:tombstone");
        createObjectAndClose(id);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(serverAddress + id)));
        assertDeleted(id);
        final HttpGet get1 = getObjMethod(id);
        final Link tombstone = getTombstoneLink(get1);
        assertEquals("hasTombstone", tombstone.getRel());
        assertEquals(expectedTombstone, tombstone.getUri());
        // Now with a trailing slash
        final HttpGet get2 = getObjMethod(id + "/");
        final Link tombstone2 = getTombstoneLink(get2);
        assertEquals("hasTombstone", tombstone2.getRel());
        assertEquals(expectedTombstone, tombstone2.getUri());
    }

    @Test
    public void testDisallowedMethods() throws Exception {
        final String id = getRandomUniqueId();
        final HttpPut putMethod = putObjMethod(id);
        final HttpEntity body = new StringEntity("<> <http://example.org#title> 'a title'", Charsets.UTF_8);
        putMethod.setEntity(body);
        putMethod.setHeader(CONTENT_TYPE, "text/turtle");
        assertEquals(CREATED.getStatusCode(), getStatus(putMethod));

        final HttpGet getContainer = getObjMethod(id);
        assertEquals(OK.getStatusCode(), getStatus(getContainer));

        final HttpDelete deleteContainer = deleteObjMethod(id);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteContainer));

        final HttpGet getContainer2 = getObjMethod(id);
        final Link tombstoneUri;
        try (final CloseableHttpResponse res = execute(getContainer2)) {
            assertEquals(GONE.getStatusCode(), getStatus(res));
            tombstoneUri = getTombstoneLink(res);
        }

        final HttpGet getTombstone = new HttpGet(tombstoneUri.getUri());
        assertEquals(METHOD_NOT_ALLOWED.getStatusCode(), getStatus(getTombstone));

        final HttpPut putTombstone = new HttpPut(tombstoneUri.getUri());
        assertEquals(METHOD_NOT_ALLOWED.getStatusCode(), getStatus(putTombstone));

        final HttpPost postTombstone = new HttpPost(tombstoneUri.getUri());
        assertEquals(METHOD_NOT_ALLOWED.getStatusCode(), getStatus(postTombstone));

        final HttpDelete deleteTombstone = new HttpDelete(tombstoneUri.getUri());
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteTombstone));
    }

    @Test
    public void testPurgingGrandchildren() throws Exception {
        final HttpPost post = postObjMethod();
        post.setHeader(LINK, "<http://fedora.info/definitions/v4/repository#ArchivalGroup>;rel=\"type\"");
        final String agPath;
        try (final CloseableHttpResponse response = execute(post)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            agPath = getLocation(response);
        }

        final HttpGet get1 = new HttpGet(agPath);
        assertEquals(OK.getStatusCode(), getStatus(get1));

        final HttpPost postChild = new HttpPost(agPath);
        final String childPath;
        try (final CloseableHttpResponse response = execute(postChild)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            childPath = getLocation(response);
        }

        final HttpGet get2 = new HttpGet(childPath);
        assertEquals(OK.getStatusCode(), getStatus(get2));

        final HttpPost postGrandChild = new HttpPost(childPath);
        final String grandchildPath;
        try (final CloseableHttpResponse response = execute(postGrandChild)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            grandchildPath = getLocation(response);
        }

        final HttpGet get3 = new HttpGet(grandchildPath);
        assertEquals(OK.getStatusCode(), getStatus(get3));

        final HttpDelete deleteChild = new HttpDelete(childPath);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteChild));

        final HttpGet get4 = new HttpGet(childPath);
        assertEquals(GONE.getStatusCode(), getStatus(get4));

        final HttpGet get5 = new HttpGet(grandchildPath);
        assertEquals(GONE.getStatusCode(), getStatus(get5));

        final HttpDelete purgeGrandchild = new HttpDelete(grandchildPath + "/" + FCR_TOMBSTONE);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(purgeGrandchild));

        final HttpGet get6 = new HttpGet(grandchildPath);
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(get6));

        final HttpDelete purgeChild = new HttpDelete(childPath + "/" + FCR_TOMBSTONE);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(purgeChild));

        final HttpGet get7 = new HttpGet(agPath);
        assertEquals(OK.getStatusCode(), getStatus(get7));

        final HttpDelete purgeBeforeDelete = new HttpDelete(agPath + "/" + FCR_TOMBSTONE);
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(purgeBeforeDelete));

        final HttpDelete deleteAg = new HttpDelete(agPath);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteAg));

        final HttpGet get8 = new HttpGet(agPath);
        assertEquals(GONE.getStatusCode(), getStatus(get8));

        final HttpDelete finalPurge  = new HttpDelete(agPath + "/" + FCR_TOMBSTONE);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(finalPurge));

        final HttpGet finalGet = new HttpGet(agPath);
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(finalGet));
    }

    @Test
    public void testNoChildrenOfTombstone() throws Exception {
        final String uri;
        try (final var response = execute(postObjMethod())) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            uri = getLocation(response);
        }
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(uri)));
        assertEquals(GONE.getStatusCode(), getStatus(new HttpPut(uri)));
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(new HttpPut(uri + "/" + getRandomUniqueId())));
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(new HttpPut(uri + "/" + getRandomUniqueId() + "/" +
                getRandomUniqueId())));
    }
}
