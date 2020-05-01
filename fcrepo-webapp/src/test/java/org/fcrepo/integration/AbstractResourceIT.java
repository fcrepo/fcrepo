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
package org.fcrepo.integration;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.base.Strings;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * Base class for ITs
 * @author awoods
 * @author escowles
**/
public abstract class AbstractResourceIT {

    protected Logger logger;

    public static final Credentials FEDORA_ADMIN_CREDENTIALS = new UsernamePasswordCredentials("fedoraAdmin",
            "fedoraAdmin");

    @Before
    public void setLogger() {
        logger = getLogger(this.getClass());
    }

    private static final int SERVER_PORT = parseInt(Objects.requireNonNullElse(
            Strings.emptyToNull(System.getProperty("fcrepo.dynamic.test.port")), "8080"));

    private static final String CONTEXT_PATH = System
            .getProperty("fcrepo.test.context.path");

    private static final String HOSTNAME = "localhost";

    private static final String PROTOCOL = "http";

    protected static final String serverAddress = PROTOCOL + "://" + HOSTNAME + ":" +
            SERVER_PORT + CONTEXT_PATH + "rest/";

    protected static final HttpClient client = createClient();

    private static HttpClient createClient() {
        return HttpClientBuilder.create().setMaxConnPerRoute(MAX_VALUE)
                .setMaxConnTotal(MAX_VALUE).build();
    }

}
