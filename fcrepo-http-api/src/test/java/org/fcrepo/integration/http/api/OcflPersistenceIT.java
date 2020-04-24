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

import static org.slf4j.LoggerFactory.getLogger;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * Test of OCFL specific issues from the HTTP layer.
 */
public class OcflPersistenceIT extends AbstractResourceIT {

    private static final Logger LOGGER = getLogger(OcflPersistenceIT.class);

    @Test
    public void testDeleteAgChild() throws Exception {
        final String id = getRandomUniqueId();
        final HttpPost postAg = postObjMethod();
        postAg.setHeader("Link", "<http://fedora.info/definitions/v4/repository#ArchivalGroup>;rel=\"type\"");
        postAg.setHeader("Slug", id);
        assertEquals(CREATED.getStatusCode(), getStatus(postAg));

        assertEquals(OK.getStatusCode(), getStatus(getObjMethod(id)));

        final String childId = getRandomUniqueId();
        final HttpPost postChild = postObjMethod(id);
        postAg.setHeader("Slug", childId);
        final String childLocation;
        try (final CloseableHttpResponse response = execute(postChild)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            childLocation = getLocation(response);
        }

        final HttpGet getChild = new HttpGet(childLocation);
        assertEquals(OK.getStatusCode(), getStatus(getChild));

        final HttpDelete deleteChild = new HttpDelete(childLocation);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteChild));

        final HttpGet getChildAgain = new HttpGet(childLocation);
        assertEquals(GONE.getStatusCode(), getStatus(getChildAgain));
    }
}
