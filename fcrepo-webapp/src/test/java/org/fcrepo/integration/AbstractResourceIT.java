/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Objects;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;

import com.google.common.base.Strings;

/**
 * Base class for ITs
 * @author awoods
 * @author escowles
**/
public abstract class AbstractResourceIT {

    protected Logger logger;

    public static final Credentials FEDORA_ADMIN_CREDENTIALS = new UsernamePasswordCredentials("fedoraAdmin",
            "fedoraAdmin");

    @BeforeEach
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

    protected static HttpClient createClient() {
        final Credentials credentials = new UsernamePasswordCredentials("fedoraAdmin", "fedoraAdmin");
        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, credentials);
        return HttpClientBuilder.create().setMaxConnPerRoute(MAX_VALUE)
                .setMaxConnTotal(MAX_VALUE).setDefaultCredentialsProvider(credsProvider).build();
    }

}
