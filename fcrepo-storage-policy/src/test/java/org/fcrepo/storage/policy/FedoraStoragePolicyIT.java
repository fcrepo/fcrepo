/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.storage.policy;

import static org.fcrepo.storage.policy.FedoraStoragePolicy.POLICY_RESOURCE;
import static org.junit.Assert.assertEquals;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.tika.io.IOUtils;
import org.fcrepo.kernel.services.policy.StoragePolicy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

// Runs integration test for restful interface for storage policies

/**
 * <p>FedoraStoragePolicyIT class.</p>
 *
 * @author awoods
 */
public class FedoraStoragePolicyIT extends AbstractResourceIT {

    private static final String MIME_KEY = "mix:mimeType";
    private static final String MIME = "image/tiff";
    private static final String STORE = "cloud-store";

    private Set<String> policyKeys;

    @Before
    public void setUp() {
        policyKeys = new HashSet<>();
    }

    @After
    public void tearDown() {
        for (String policyKey : policyKeys) {
            deletePolicy(policyKey);
        }
    }

    @Test
    public void testPolicyCreateByPost() throws Exception {
        final HttpPost objMethod = HttpPostObjMethod(POLICY_RESOURCE);
        final StringEntity input = new StringEntity(MIME_KEY + " " + MIME + " " + STORE,
                                              "UTF-8");
        input.setContentType(APPLICATION_FORM_URLENCODED);
        objMethod.setEntity(input);
        final HttpResponse response = client.execute(objMethod);

        final String body = IOUtils.toString(response.getEntity().getContent());
        assertEquals(body, 201, response.getStatusLine().getStatusCode());

        policyKeys.add(MIME_KEY);

        final Header[] headers = response.getHeaders("Location");
        assertNotNull(headers);
        assertEquals(1, headers.length);
        assertEquals(objMethod.getURI()
                             .toString()
                             .replace(POLICY_RESOURCE, MIME_KEY),
                     headers[0].getValue());
    }

    @Test
    public void testGetStoragePolicy() throws Exception {
        // Test no policy
        final HttpGet getMethod0 = HttpGetObjMethod(MIME_KEY);
        final HttpResponse response0 = client.execute(getMethod0);
        assertNotNull(response0);
        assertEquals(IOUtils.toString(response0.getEntity().getContent()),
                     404,
                     response0.getStatusLine().getStatusCode());

        // Add a policy
        testPolicyCreateByPost();

        // Test Get Storage Policy
        final HttpGet getMethod1 = HttpGetObjMethod(MIME_KEY);
        final HttpResponse response1 = client.execute(getMethod1);
        assertNotNull(response1);

        final String body = IOUtils.toString(response1.getEntity().getContent());
        assertEquals(body, 200, response1.getStatusLine().getStatusCode());
        assertEquals(MIME + ":" + STORE, body);
    }

    @Test
    public void testGetStoragePolicies() throws Exception {
        // Add a policy
        testPolicyCreateByPost();

        // Test Get Storage Policy
        final HttpGet getMethod1 = HttpGetObjMethod(POLICY_RESOURCE);
        final HttpResponse response1 = client.execute(getMethod1);
        assertNotNull(response1);

        final String body = IOUtils.toString(response1.getEntity().getContent());
        assertEquals(body, 200, response1.getStatusLine().getStatusCode());

        final StoragePolicy policy = new MimeTypeStoragePolicy(MIME, STORE);
        assertEquals("policies=[" + policy.toString() + "]", body);
    }

    @Test
    public void testInvalidPoliciesCreateByPost() throws Exception {
        final HttpPost objMethod = HttpPostObjMethod(POLICY_RESOURCE);
        final StringEntity input = new StringEntity("mix:newType " + MIME + " " + STORE,
                                              "UTF-8");
        input.setContentType(APPLICATION_FORM_URLENCODED);
        objMethod.setEntity(input);
        final HttpResponse response = client.execute(objMethod);
        assertEquals(response.getStatusLine().getStatusCode(), 500);
    }

    @Test
    public void testPolicyDelete() throws Exception {
        testPolicyCreateByPost();

        final HttpResponse response = deletePolicy(MIME_KEY);
        assertEquals(204, response.getStatusLine().getStatusCode());

        final HttpGet objGetMethod = HttpGetObjMethod(POLICY_RESOURCE);
        final HttpResponse getResponse = client.execute(objGetMethod);
        assertEquals("No Policies Found",
                     IOUtils.toString(getResponse.getEntity().getContent())
        );
    }

    private HttpResponse deletePolicy(final String policyKey) {
        final HttpDelete objMethod = HttpDeleteObjMethod(policyKey);
        try {
            return client.execute(objMethod);

        } catch (IOException e) {
            fail(e.getMessage());
            return null; // never
        }
    }

}
