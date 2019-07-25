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
package org.fcrepo.integration.rdf;

import org.apache.jena.query.Dataset;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.Assert.assertFalse;

/**
 * @author cabeer
 */
@Ignore // TODO FIX THESE TESTS
public class LdpIT extends AbstractIntegrationRdfIT {
    @Test
    public void testExample10() throws IOException {
        final String pid = getRandomUniqueId();
        final HttpResponse response = createObject(pid);
        final String location = response.getFirstHeader("Location").getValue();

        final String body = "\n" +
                "@prefix ldp: <http://www.w3.org/ns/ldp#>.\n" +
                "@prefix dcterms: <http://purl.org/dc/terms/>.\n" +
                "@prefix o: <http://example.org/ontology#>.\n" +
                "<> dcterms:title \"The liabilities of JohnZSmith\";\n" +
                "   ldp:membershipResource <" + location + ">;\n" +
                "   ldp:hasMemberRelation o:liability;\n";

        final Map<String, String> headers = new HashMap();
        headers.put("Link", "<http://www.w3.org/ns/ldp#DirectContainer>;rel=type");
        createLDPRSAndCheckResponse(pid + "/liabilities", body, headers);

        final HttpPost httpPost1 = new HttpPost(serverAddress + pid + "/liabilities");
        httpPost1.setHeader("Slug", "l1");
        checkResponse(execute(httpPost1), CREATED);

        final HttpPost httpPost2 = new HttpPost(serverAddress + pid + "/liabilities");
        httpPost2.setHeader("Slug", "l2");
        checkResponse(execute(httpPost2), CREATED);

        final HttpPost httpPost3 = new HttpPost(serverAddress + pid + "/liabilities");
        httpPost3.setHeader("Slug", "l3");
        checkResponse(execute(httpPost3), CREATED);

        final HttpGet httpGet = new HttpGet(location);
        httpGet.addHeader("Prefer", "return=representation; " +
                "include=\"http://www.w3.org/ns/ldp#PreferMembership " +
                    "http://www.w3.org/ns/ldp#PreferMinimalContainer\"; " +
                "omit=\"http://fedora.info/definitions/v4/repository#ServerManaged\"");
        final Dataset dataset = getDataset(httpGet);

        assertFalse(dataset.asDatasetGraph().isEmpty());
    }
}
