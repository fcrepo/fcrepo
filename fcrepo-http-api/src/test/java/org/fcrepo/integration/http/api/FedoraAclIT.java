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
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.junit.Assert.assertEquals;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
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

        final HttpPut put = new HttpPut(subjectUri + "/" + FCR_ACL);
        final String aclLocation;
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            aclLocation = response.getFirstHeader("Location").getValue();
        }

        final HttpPut put1 = new HttpPut(aclLocation + "/" + FCR_ACL);
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(put1));
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

        final HttpPatch patch = new HttpPatch(subjectUri + "/" + FCR_ACL);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity("PREFIX acl: <http://www.w3.org/ns/auth/acl#> " +
                "INSERT DATA { <#readAccess> acl:mode acl:write . }"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(patch));
        // TODO add a test to verify inserted triples were actually on the resource.
        // after the @GET method is implemented on FedoraAcl.java

    }

    @Test
    public void testPatchAclOnAclResource() throws Exception {
        createObjectAndClose(id);

        final HttpPut put = new HttpPut(subjectUri + "/" + FCR_ACL);
        final String aclLocation;
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            aclLocation = response.getFirstHeader("Location").getValue();
        }

        final HttpPatch patch = new HttpPatch(aclLocation + "/" + FCR_ACL);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity("PREFIX acl: <http://www.w3.org/ns/auth/acl#> " +
                "INSERT DATA { <#readAccess> acl:mode acl:write . }"));
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(patch));

    }

}
