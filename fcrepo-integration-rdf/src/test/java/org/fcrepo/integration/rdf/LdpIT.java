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
package org.fcrepo.integration.rdf;

import com.hp.hpl.jena.update.GraphStore;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.Test;

import java.io.IOException;

import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.Assert.assertFalse;

/**
 * @author cabeer
 */
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
                "<> a ldp:DirectContainer ;" +
                "   dcterms:title \"The liabilities of JohnZSmith\";\n" +
                "   ldp:membershipResource <" + location + ">;\n" +
                "   ldp:hasMemberRelation o:liability;\n";

        createLDPRSAndCheckResponse(pid + "/liabilities", body);

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
        final GraphStore graphStore = getGraphStore(httpGet);

        assertFalse(graphStore.isEmpty());
    }
}
