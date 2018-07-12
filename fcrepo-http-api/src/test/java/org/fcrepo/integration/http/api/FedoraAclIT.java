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

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.jena.sparql.core.DatasetGraph;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.junit.Before;
import org.junit.Test;

/**
 * @author lsitu
 * @author 4/20/2018
 */
public class FedoraAclIT extends AbstractResourceIT {

    private String subjectUri;
    private String id;

    @Before
    public void init() {
        id = getRandomUniqueId();
        subjectUri = serverAddress + id;
    }

    @Test
    public void testCreateAcl() throws Exception {
        createObjectAndClose(id);

        final HttpPut put = new HttpPut(subjectUri + "/" + FCR_ACL);
        final String aclLocation;
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            aclLocation = response.getFirstHeader("Location").getValue();
            // verify the acl container is translated to fcr:acl
            assertEquals(subjectUri + "/" + FCR_ACL, aclLocation);
        }
    }

    @Test
    public void testCreateAclOnAclResource() throws Exception {
        createObjectAndClose(id);

        final String aclLocation = createACL();

        final HttpPut put1 = new HttpPut(aclLocation + "/" + FCR_ACL);
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(put1));
    }

    private String createACL() throws IOException {
        final HttpPut put = new HttpPut(subjectUri + "/" + FCR_ACL);
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            return response.getFirstHeader("Location").getValue();
        }
    }

    @Test
    public void testCreateAclOnBinary() throws Exception {
        createDatastream(id, "x", "some content");

        final HttpPut put = new HttpPut(subjectUri + "/x/" + FCR_ACL);
        final String aclLocation;
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            aclLocation = response.getFirstHeader("Location").getValue();
            // verify the acl container for binary is translated to fcr:acl
            assertEquals(subjectUri + "/x/" + FCR_ACL, aclLocation);
        }
    }

    @Test
    public void testPatchAcl() throws Exception {
        createObjectAndClose(id);
        final String aclURI = createACL();
        final HttpPatch patch = new HttpPatch(aclURI);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity("PREFIX acl: <http://www.w3.org/ns/auth/acl#> " +
                                         "INSERT { <#writeAccess> acl:mode acl:write . } WHERE { }"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(patch));

        //verify the patch worked
        try (final CloseableDataset dataset = getDataset(new HttpGet(aclURI))) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue(graph.contains(ANY,
                                      createURI(aclURI + "#writeAccess"),
                                      createURI("http://www.w3.org/ns/auth/acl#mode"),
                                      createURI("http://www.w3.org/ns/auth/acl#write")));
        }

    }

    @Test
    public void testCreateAndRetrieveAcl() throws Exception {
        createObjectAndClose(id);

        final HttpPut put = new HttpPut(subjectUri + "/" + FCR_ACL);
        final String aclLocation;
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            aclLocation = response.getFirstHeader("Location").getValue();
            // verify the acl container is translated to fcr:acl
            assertEquals(subjectUri + "/" + FCR_ACL, aclLocation);
        }

        final HttpGet get = new HttpGet(aclLocation);
        assertEquals(OK.getStatusCode(), getStatus(get));

    }

    @Test
    public void testDeleteAcl() throws Exception {
        createObjectAndClose(id);

        final HttpPut put = new HttpPut(subjectUri + "/" + FCR_ACL);
        final String aclLocation;
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            aclLocation = response.getFirstHeader("Location").getValue();
            // verify the acl container is translated to fcr:acl
            assertEquals(subjectUri + "/" + FCR_ACL, aclLocation);
        }

        final HttpGet get = new HttpGet(aclLocation);
        assertEquals(OK.getStatusCode(), getStatus(get));

        final HttpDelete delete = new HttpDelete(aclLocation);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(delete));

        final HttpGet getNotFound = new HttpGet(aclLocation);
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(getNotFound));

    }

    @Test
    public void testGetNonExistentAcl() throws Exception {
        createObjectAndClose(id);
        final HttpGet getNotFound = new HttpGet(subjectUri + "/" + FCR_ACL);
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(getNotFound));

    }
}
