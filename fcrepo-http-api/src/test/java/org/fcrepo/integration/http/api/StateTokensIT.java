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

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.junit.Test;
import org.springframework.test.context.TestExecutionListeners;

/**
 * @author dbernstein
 */
@TestExecutionListeners(
        listeners = { TestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class StateTokensIT extends AbstractResourceIT {

    private static final String X_STATE_TOKEN_HEADER = "X-State-Token";
    private static final String X_IF_STATE_TOKEN_HEADER = "X-If-State-Token";

    @Test
    public void testGetHasStateTokenRDFSource() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        try (final CloseableHttpResponse response = execute(new HttpGet(serverAddress + id))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            assertNotNull(X_STATE_TOKEN_HEADER, response.getFirstHeader(X_STATE_TOKEN_HEADER).getValue());
        }
    }

    @Test
    public void testHeadHasStateTokenRDFSource() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        try (final CloseableHttpResponse response = execute(new HttpHead(serverAddress + id))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            assertNotNull(X_STATE_TOKEN_HEADER, response.getFirstHeader(X_STATE_TOKEN_HEADER).getValue());
        }
    }

    @Test
    public void testAclGetHasStateTokenRDFSource() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final String aclPid = id + "/fcr:acl";

        final HttpPut method = putObjMethod(aclPid, "text/turtle",
                                            "<#auth>  a <http://www.w3.org/ns/auth/acl#Authorization> .");

        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }

        try (final CloseableHttpResponse response = execute(new HttpGet(serverAddress + aclPid))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            assertNotNull(X_STATE_TOKEN_HEADER, response.getFirstHeader(X_STATE_TOKEN_HEADER).getValue());
        }
    }

    @Test
    public void testAclHeadHasStateTokenRDFSource() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final String aclPid = id + "/fcr:acl";

        final HttpPut method = putObjMethod(aclPid, "text/turtle",
                                            "<#auth>  a <http://www.w3.org/ns/auth/acl#Authorization> .");

        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }

        try (final CloseableHttpResponse response = execute(new HttpHead(serverAddress + aclPid))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            assertNotNull(X_STATE_TOKEN_HEADER, response.getFirstHeader(X_STATE_TOKEN_HEADER).getValue());
        }
    }

    @Test
    public void testLdpcvGetHasStateTokenRDFSource() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        try (final CloseableHttpResponse response = execute(new HttpGet(serverAddress + id + "/fcr:versions"))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            assertNotNull(X_STATE_TOKEN_HEADER, response.getFirstHeader(X_STATE_TOKEN_HEADER).getValue());
        }
    }

    @Test
    public void testLdpcvHeadHasStateTokenRDFSource() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        try (final CloseableHttpResponse response = execute(new HttpHead(serverAddress + id + "/fcr:versions"))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            assertNotNull(X_STATE_TOKEN_HEADER, response.getFirstHeader(X_STATE_TOKEN_HEADER).getValue());
        }
    }

    @Test
    public void testGetHasStateTokenNonRDFSource() throws IOException {
        final String id = getRandomUniqueId();
        final String location = serverAddress + id + "/binary";
        final HttpPut method = putDSMethod(id, "binary", "foo");
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }

        try (final CloseableHttpResponse response = execute(new HttpGet(location))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            assertNotNull(X_STATE_TOKEN_HEADER, response.getFirstHeader(X_STATE_TOKEN_HEADER).getValue());
        }
    }

    @Test
    public void testHeadHasStateTokenNonRDFSource() throws IOException {
        final String id = getRandomUniqueId();
        final String location = serverAddress + id + "/binary";
        final HttpPut method = putDSMethod(id, "binary", "foo");
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }

        try (final CloseableHttpResponse response = execute(new HttpHead(location))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            assertNotNull(X_STATE_TOKEN_HEADER, response.getFirstHeader(X_STATE_TOKEN_HEADER).getValue());
        }
    }

    @Test
    public void testPutWithStateTokenOnNonRDFSource() throws IOException {
        final String id = getRandomUniqueId();
        final String location = serverAddress + id + "/binary";
        //create a binary
        final HttpPut method = putDSMethod(id, "binary", "foo");
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }

        //get state token
        final String stateToken;
        try (final CloseableHttpResponse response = execute(new HttpHead(location))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            stateToken = response.getFirstHeader(X_STATE_TOKEN_HEADER).getValue();
        }

        //perform put with valid X-If-State-Token
        final HttpPut validPut = putDSMethod(id, "binary", "bar");
        validPut.addHeader(X_IF_STATE_TOKEN_HEADER, stateToken);
        try (final CloseableHttpResponse response = execute(validPut)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        //perform put with an invalid X-If-State-Token
        final HttpPut invalidPut = putDSMethod(id, "binary", "boo");
        invalidPut.addHeader(X_IF_STATE_TOKEN_HEADER, "invalid_token");
        try (final CloseableHttpResponse response = execute(invalidPut)) {
            assertEquals(PRECONDITION_FAILED.getStatusCode(), getStatus(response));
        }

    }

    @Test
    public void testPatchWithStateTokenOnRDFSource() throws IOException {
        final String id = getRandomUniqueId();
        final String location = serverAddress + id;
        //create a resource
        final HttpPut method = putObjMethod(id);
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }

        //get state token
        final String stateToken;
        try (final CloseableHttpResponse response = execute(new HttpHead(location))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            stateToken = response.getFirstHeader(X_STATE_TOKEN_HEADER).getValue();
        }

        //perform patch with an invalid X-If-State-Token
        final HttpPatch invalidPatch = patchObjMethod(id);
        invalidPatch.addHeader(X_IF_STATE_TOKEN_HEADER, "invalid_token");
        invalidPatch.setHeader("Content-Type", "application/sparql-update");
        invalidPatch.setEntity(new StringEntity("INSERT { <> a <http://example.org/test> } WHERE {}"));

        try (final CloseableHttpResponse response = execute(invalidPatch)) {
            assertEquals(PRECONDITION_FAILED.getStatusCode(), getStatus(response));
        }


        //perform patch with valid X-If-State-Token
        final HttpPatch validPatch = patchObjMethod(id);
        validPatch.addHeader(X_IF_STATE_TOKEN_HEADER, stateToken);
        validPatch.setHeader("Content-Type", "application/sparql-update");
        validPatch.setEntity(new StringEntity("INSERT { <> a <http://example.org/test> } WHERE {}"));
        try (final CloseableHttpResponse response = execute(validPatch)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testPatchWithStateTokenOnAcl() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final String aclPid = id + "/fcr:acl";
        final String location = serverAddress + aclPid;
        final HttpPut method = putObjMethod(aclPid, "text/turtle",
                                            "<#auth>  a <http://www.w3.org/ns/auth/acl#Authorization> .");

        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }

        final String stateToken;
        try (final CloseableHttpResponse response = execute(new HttpHead(location))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            stateToken = response.getFirstHeader(X_STATE_TOKEN_HEADER).getValue();
        }

        //perform patch with an invalid X-If-State-Token
        final HttpPatch invalidPatch = patchObjMethod(aclPid);
        invalidPatch.addHeader(X_IF_STATE_TOKEN_HEADER, "invalid_token");
        invalidPatch.setHeader("Content-Type", "application/sparql-update");
        invalidPatch.setEntity(new StringEntity("INSERT { <> a <http://example.org/test> } WHERE {}"));

        try (final CloseableHttpResponse response = execute(invalidPatch)) {
            assertEquals(PRECONDITION_FAILED.getStatusCode(), getStatus(response));
        }

        //perform patch with valid X-If-State-Token
        final HttpPatch validPatch = patchObjMethod(aclPid);
        validPatch.addHeader(X_IF_STATE_TOKEN_HEADER, stateToken);
        validPatch.setHeader("Content-Type", "application/sparql-update");
        validPatch.setEntity(new StringEntity("INSERT { <> a <http://example.org/test> } WHERE {}"));
        try (final CloseableHttpResponse response = execute(validPatch)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }
    }
}
