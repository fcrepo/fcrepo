/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration;
import static java.lang.Integer.MAX_VALUE;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

import com.google.common.base.Strings;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;

import javax.ws.rs.core.Link;

/**
 * <p>SanityCheckIT class.</p>
 *
 * @author fasseg
 */
public class SanityCheckIT {

    /**
     * The server port of the application, set as system property by
     * maven-failsafe-plugin.
     */
    private static final String SERVER_PORT = Objects.requireNonNullElse(
            Strings.emptyToNull(System.getProperty("fcrepo.dynamic.test.port")), "8080");

    /**
    * The context path of the application (including the leading "/"), set as
    * system property by maven-failsafe-plugin.
    */
    private static final String CONTEXT_PATH = System.getProperty("fcrepo.test.context.path");

    private Logger logger;

    @BeforeEach
    public void setLogger() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    private static final String HOSTNAME = "localhost";

    private static final String serverAddress = "http://" + HOSTNAME + ":" +
            SERVER_PORT + CONTEXT_PATH + (CONTEXT_PATH.endsWith("/") ? "" : "/") + "rest";

    private static final HttpClient client;

    static {
        final CredentialsProvider creds = new DefaultCredentialsProvider();
        creds.setCredentials(AuthScope.ANY, AbstractResourceIT.FEDORA_ADMIN_CREDENTIALS);
        client =
                HttpClientBuilder.create().setMaxConnPerRoute(MAX_VALUE)
                        .setMaxConnTotal(MAX_VALUE).setDefaultCredentialsProvider(creds).build();
    }

    @Test
    public void doASanityCheck() throws IOException {
        executeAndVerify(new HttpGet(serverAddress), SC_OK);
    }

    private HttpResponse executeAndVerify(final HttpUriRequest method, final int statusCode) throws IOException {
        logger.debug("Executing: " + method.getMethod() + " to " + method.getURI());
        final HttpResponse response = client.execute(method);

        assertEquals(statusCode, response.getStatusLine().getStatusCode());
        return response;
    }

    @Test
    public void testConstraintLink() throws Exception {
        // Create a resource
        final HttpPost post = new HttpPost(serverAddress);
        final HttpResponse postResponse = executeAndVerify(post, SC_CREATED);

        final String location = postResponse.getFirstHeader("Location").getValue();
        logger.debug("new resource location: {}", location);

        // GET the new resource
        final HttpGet get = new HttpGet(location);
        final HttpResponse getResponse = executeAndVerify(get, SC_OK);

        final String body = EntityUtils.toString(getResponse.getEntity());
        logger.debug("new resource body: {}", body);

        // PUT the exact body back to the new resource... successfully
        final HttpPut put = new HttpPut(location);
        put.setEntity(new StringEntity(body));
        put.setHeader(CONTENT_TYPE, "text/turtle");
        put.setHeader("Prefer", "handling=lenient");
        executeAndVerify(put, SC_NO_CONTENT);

        // Update a server managed property in the resource body... not allowed!
        final String body2 = body.replaceFirst("fedora:created \"2\\d\\d\\d", "fedora:created \"1999");

        // PUT the erroneous body back to the new resource... unsuccessfully
        final HttpPut put2 = new HttpPut(location);
        put2.setEntity(new StringEntity(body2));
        put2.setHeader(CONTENT_TYPE, "text/turtle");
        final HttpResponse put2Response = executeAndVerify(put2, SC_CONFLICT);

        // Verify the returned LINK header
        final String linkHeader = put2Response.getFirstHeader(LINK).getValue();
        final Link link = Link.valueOf(linkHeader);
        logger.debug("constraint linkHeader: {}", linkHeader);

        // Verify the LINK rel
        final String linkRel = link.getRel();
        assertEquals(CONSTRAINED_BY.getURI(), linkRel);

        // Verify the LINK URI by fetching it
        final URI linkURI = link.getUri();
        logger.debug("constraint linkURI: {}", linkURI);

        final HttpGet getLink = new HttpGet(linkURI);
        executeAndVerify(getLink, SC_OK);
    }

    @Test
    public void testCannotCreateResourceConstraintLink() throws Exception {
        // Create a ldp:Resource resource, this should fail
        final HttpPost post = new HttpPost(serverAddress);
        post.setHeader(LINK,"<http://www.w3.org/ns/ldp#Resource>; rel=\"type\"");
        final HttpResponse postResponse = executeAndVerify(post, SC_BAD_REQUEST);

        // Verify the returned LINK header
        final String linkHeader = postResponse.getFirstHeader(LINK).getValue();
        final Link link = Link.valueOf(linkHeader);
        logger.debug("constraint linkHeader: {}", linkHeader);

        // Verify the LINK rel
        final String linkRel = link.getRel();
        assertEquals(CONSTRAINED_BY.getURI(), linkRel);

        // Verify the LINK URI by fetching it
        final URI linkURI = link.getUri();
        logger.debug("constraint linkURI: {}", linkURI);

        final HttpGet getLink = new HttpGet(linkURI);
        executeAndVerify(getLink, SC_OK);
    }

    @Test
    public void testUnicodeCharsAllowed() throws Exception {
        final var id = "ÅŤéșţ!";
        final var url = serverAddress + "/" + id;

        final HttpPut put = new HttpPut(url);
        put.setEntity(new StringEntity("testing"));
        put.setHeader(CONTENT_TYPE, "text/plain");
        executeAndVerify(put, SC_CREATED);

        executeAndVerify(new HttpGet(url), SC_OK);
    }

    @Test
    public void testEncodedSlash() throws IOException {
        final String targetResource = serverAddress + "/rest/" + randomUUID() + "/admin_set%2Fdefault";
        final var putTest = new HttpPut(targetResource);
        executeAndVerify(putTest, SC_CREATED);
    }
}
