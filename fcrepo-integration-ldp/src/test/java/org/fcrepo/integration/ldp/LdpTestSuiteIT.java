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
package org.fcrepo.integration.ldp;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3.ldp.testsuite.LdpTestSuite;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author cabeer
 * @since 10/6/14
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test/test-container.xml")
public class LdpTestSuiteIT {

    protected static final int SERVER_PORT = parseInt(System.getProperty(
            "test.port", "8080"));

    protected static final String HOSTNAME = "localhost";

    protected static final String PROTOCOL = "http";

    protected static final String serverAddress = PROTOCOL + "://" + HOSTNAME + ":" +
            SERVER_PORT + "/";

    protected static HttpClient client = createClient();

    protected static HttpClient createClient() {
        return HttpClientBuilder.create().setMaxConnPerRoute(MAX_VALUE)
                .setMaxConnTotal(MAX_VALUE).build();
    }

    @Test
    public void runLDPTestSuite() throws IOException {
        final String pid = "ldp-test-" + UUID.randomUUID().toString().substring(0,5);

        final HttpResponse response = client.execute(new HttpPut(serverAddress + pid));
        assertEquals(CREATED.getStatusCode(), response.getStatusLine().getStatusCode());

        final HashMap<String, String> options = new HashMap<>();
        options.put("server", serverAddress + pid);
        options.put("direct", "true");
        options.put("non-rdf", "true");
        options.put("read-only-prop", "http://fedora.info/definitions/v4/repository#uuid");
        final LdpTestSuite testSuite = new LdpTestSuite(options);
        testSuite.run();
        assertTrue("The LDP test suite is only informational", true);
    }
}
