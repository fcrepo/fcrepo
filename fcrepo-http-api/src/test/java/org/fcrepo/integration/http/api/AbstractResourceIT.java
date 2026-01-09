/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Strings;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import org.fcrepo.config.AuthPropsConfig;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.fcrepo.http.commons.test.util.ContainerWrapper;
import org.fcrepo.kernel.api.RepositoryInitializationStatus;
import org.fcrepo.kernel.api.auth.ACLHandle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response.Status;
import jakarta.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_LOCATION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.HttpHeaders.LINK;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.GONE;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.vocabulary.DC_11.title;
import static org.fcrepo.http.commons.session.TransactionConstants.ATOMIC_ID_HEADER;
import static org.fcrepo.http.commons.test.util.TestHelpers.parseTriples;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.EXTERNAL_CONTENT;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_SERVER_MANAGED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>Abstract AbstractResourceIT class.</p>
 *
 * @author awoods
 * @author ajs6f
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("/spring-test/test-container.xml")
public abstract class AbstractResourceIT {

    protected static Logger logger;

    protected static final String NON_RDF_SOURCE_LINK_HEADER = "<" + NON_RDF_SOURCE.getURI() + ">;rel=\"type\"";
    protected static final String BASIC_CONTAINER_LINK_HEADER = "<" + BASIC_CONTAINER.getURI() + ">;rel=\"type\"";
    protected static final String DIRECT_CONTAINER_LINK_HEADER = "<" + DIRECT_CONTAINER.getURI() + ">; rel=\"type\"";

    private static final String OMIT_SERVER_PREFER_HEADER = "return=representation; omit=\"" + PREFER_SERVER_MANAGED +
            "\"";

    protected static final Node DCTITLE = title.asNode();

    @Inject
    protected ContainerWrapper containerWrapper;

    protected FedoraPropsConfig propsConfig;

    protected AuthPropsConfig authPropsConfig;

    protected void restartContainer() throws Exception {
        this.containerWrapper.stop();
        this.containerWrapper.start();
    }
    /**
     * Decode the Digest header
     * @param digestHeader the digest header value.
     * @return Map with keys of algorithms and values of hashes.
     */
    protected static Map<String, String> decodeDigestHeader(final String digestHeader) {
        return stream(digestHeader.split(",")).map(h -> h.split("=", 2))
                .collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : ""));
    }

    @BeforeEach
    public void setLogger() throws InterruptedException {
        // must wait for the repo to be initialized
        final var status = getBean(RepositoryInitializationStatus.class);
        int i = 0;
        while (!status.isInitializationComplete()) {
            if (++i > 6000) {
                throw new RuntimeException("Repository failed to initialize");
            }
            TimeUnit.MILLISECONDS.sleep(10);
        }
        logger = getLogger(this.getClass());
        propsConfig = getBean(FedoraPropsConfig.class);
        authPropsConfig = getBean(AuthPropsConfig.class);
    }

    private static final int SERVER_PORT = parseInt(Objects.requireNonNullElse(
            Strings.emptyToNull(System.getProperty("fcrepo.dynamic.test.port")), "8080"));

    private static final String HOSTNAME = "localhost";

    private static final String PROTOCOL = "http";

    protected static final String serverAddress = PROTOCOL + "://" + HOSTNAME + ":" + SERVER_PORT + "/";

    protected <T> T getBean(final Class<T> type) {
        return containerWrapper.getSpringAppContext().getBean(type);
    }

    protected  <T> T getBean(final String name, final Class<T> type) {
        return containerWrapper.getSpringAppContext().getBean(name, type);
    }

    protected static final CloseableHttpClient client = createClient();

    protected static CloseableHttpClient createClient() {
        return createClient(false);
    }

    protected static CloseableHttpClient createClient(final boolean disableRedirects) {
        final HttpClientBuilder client =
            HttpClientBuilder.create().setMaxConnPerRoute(MAX_VALUE).setMaxConnTotal(MAX_VALUE);
        if (disableRedirects) {
            client.disableRedirectHandling();
        }
        return client.build();
    }

    protected static HttpPost postObjMethod() {
        return postObjMethod("/");
    }

    protected static HttpPost postObjMethod(final String id) {
        return new HttpPost(serverAddress + id);
    }

    protected static HttpPut putObjMethod(final String id) {
        return new HttpPut(serverAddress + id);
    }

    protected static HttpGet getObjMethod(final String id) {
        return new HttpGet(serverAddress + id);
    }

    protected static HttpHead headObjMethod(final String id) {
        return new HttpHead(serverAddress + id);
    }

    protected static HttpDelete deleteObjMethod(final String id) {
        return new HttpDelete(serverAddress + id);
    }

    protected static HttpPatch patchObjMethod(final String id) {
        return new HttpPatch(serverAddress + id);
    }

    protected static HttpPut putDSMethod(final String pid, final String ds, final String content)
            throws UnsupportedEncodingException {
        return putDSMethod(pid + "/" + ds, content);
    }

    protected static HttpPut putDSMethod(final String id, final String content) throws
            UnsupportedEncodingException {
        final HttpPut put = new HttpPut(serverAddress + id);
        put.setEntity(new StringEntity(content == null ? "" : content));
        put.setHeader(CONTENT_TYPE, TEXT_PLAIN);
        put.setHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        return put;
    }

    protected static HttpPut putObjMethod(final String pid, final String contentType, final String content)
            throws UnsupportedEncodingException {
        final HttpPut put = new HttpPut(serverAddress + pid);
        put.setEntity(new StringEntity(content));
        put.setHeader(CONTENT_TYPE, contentType);
        return put;
    }

    protected static HttpGet getDSMethod(final String pid, final String ds) {
        return getDSMethod(pid, ds, false);
    }

    protected static HttpGet getDSMethod(final String pid, final String ds, final boolean inline) {
        return new HttpGet(serverAddress + pid + "/" + ds + (inline ? "?inline=true" : ""));
    }

    protected static HttpGet getDSDescMethod(final String pid, final String ds) {
        return new HttpGet(serverAddress + pid + "/" + ds + "/" + FCR_METADATA);
    }

    /**
     * Execute an HTTP request and return the open response.
     *
     * @param req Request to execute
     * @return the open response
     * @throws IOException in case of an IOException
     */
    protected static CloseableHttpResponse execute(final HttpUriRequest req) throws IOException {
        logger.debug("Executing: " + req.getMethod() + " to " + req.getURI());
        try {
            return client.execute(req);
        } catch (final NoHttpResponseException e) {
            // sometimes the server is slow starting up -- retry once
            try {
                TimeUnit.SECONDS.sleep(2);
                return client.execute(req);
            } catch (final InterruptedException e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    /**
     * Execute an HTTP request and close the response.
     *
     * @param req the request to execute
     */
    protected static void executeAndClose(final HttpUriRequest req) {
        logger.debug("Executing: " + req.getMethod() + " to " + req.getURI());
        try {
            execute(req).close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Execute an HTTP request with preemptive basic authentication.
     *
     * @param request the request to execute
     * @param username usename to use
     * @param password password to use
     * @return the open responses
     * @throws IOException in case of IOException
     */
    @SuppressWarnings("resource")
    protected CloseableHttpResponse executeWithBasicAuth(final HttpUriRequest request, final String username,
            final String password) throws IOException {
        final HttpHost target = new HttpHost(HOSTNAME, SERVER_PORT, PROTOCOL);
        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(target.getHostName(), target.getPort()),
                new UsernamePasswordCredentials(username, password));
        final CloseableHttpClient httpclient =
                HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
        final AuthCache authCache = new BasicAuthCache();
        final BasicScheme basicAuth = new BasicScheme();
        authCache.put(target, basicAuth);

        final HttpClientContext localContext = HttpClientContext.create();
        localContext.setAuthCache(authCache);
        return httpclient.execute(request, localContext);
    }

    /**
     * Retrieve the HTTP status code from an open response.
     *
     * @param response the open response
     * @return the HTTP status code of the response
     */
    protected static int getStatus(final HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }

    /**
     * Executes an HTTP request and returns the status code of the response, closing the response.
     *
     * @param req the request to execute
     * @return the HTTP status code of the response
     */
    protected static int getStatus(final HttpUriRequest req) {
        try (final CloseableHttpResponse response = execute(req)) {
            final int result = getStatus(response);
            if (!(result > 199) || !(result < 400)) {
                logger.warn("Got status {}", result);
                if (response.getEntity() != null) {
                    logger.warn(EntityUtils.toString(response.getEntity()));
                }
            }
            EntityUtils.consume(response.getEntity());
            return result;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes an HTTP request and returns the first Location header in the response, then closes the response.
     *
     * @param req the request to execute
     * @return the value of the first Location header in the response
     * @throws IOException in case of IOException
     */
    protected static String getLocation(final HttpUriRequest req) throws IOException {
        try (final CloseableHttpResponse response = execute(req)) {
            EntityUtils.consume(response.getEntity());
            return getLocation(response);
        }
    }

    /**
     * Retrieve the value of the first Location header from an open HTTP response.
     *
     * @param response the open response
     * @return the value of the first Location header in the response
     */
    protected static String getLocation(final HttpResponse response) {
        return response.getFirstHeader("Location").getValue();
    }

     /**
     * Retrieve the value of the first Content-Location header from an open HTTP response.
     *
     * @param response the open response
     * @return the value of the first Content-Location header in the response
     */
    protected static String getContentLocation(final HttpResponse response) {
        return response.getFirstHeader(CONTENT_LOCATION).getValue();
    }

    protected String getContentType(final HttpUriRequest method) throws IOException {
        return getContentType(method, OK);
    }

    protected String getContentType(final HttpUriRequest method, final Status httpStatus) throws IOException {
        try (final CloseableHttpResponse response = execute(method)) {
            final int result = getStatus(response);
            assertEquals(httpStatus.getStatusCode(), result);
            EntityUtils.consume(response.getEntity());
            return response.getFirstHeader(CONTENT_TYPE).getValue();
        }
    }

    /**
     * Get the etag of the given URI, as returned from a HEAD request
     *
     * @param uri uri of the resource
     * @return etag
     * @throws IOException if the uri is invalid
     */
    protected String getEtag(final String uri) throws IOException {
        return getEtag(new HttpHead(uri));
    }

    /**
     * Get the etag returned when executing the provided method
     * @param method method to execute
     * @return etag
     * @throws IOException in case of a problem or the connection was aborted
     */
    protected String getEtag(final HttpUriRequest method) throws IOException {
        try (final CloseableHttpResponse response = execute(method)) {
            return getEtag(response);
        }
    }

    /**
     * Get the etag header value present in the provided response
     * @param response response
     * @return etag header value or null
     */
    protected String getEtag(final HttpResponse response) {
        final var etag = response.getFirstHeader(HttpHeaders.ETAG);
        return etag == null ? null : etag.getValue();
    }

    protected static Collection<String> getLinkHeaders(final HttpResponse response) {
        return getHeader(response, LINK);
    }

    protected Collection<String> getLinkHeaders(final HttpUriRequest method) throws IOException {
        try (final CloseableHttpResponse response = execute(method)) {
            return getLinkHeaders(response);
        }
    }

    protected static List<String> headerValues(final HttpResponse response, final String headerName) {
        return stream(response.getHeaders(headerName)).map(Header::getValue).map(s -> s.split(",")).flatMap(
                Arrays::stream).map(String::trim).collect(toList());
    }

    protected static Collection<String> getHeader(final HttpResponse response, final String header) {
        return stream(response.getHeaders(header)).map(Header::getValue).collect(toList());
    }

    /**
     * Executes an HTTP request and parses the RDF found in the response, returning it in a
     * {@link CloseableDataset}, then closes the response.
     *
     * @param client the client to use
     * @param req the request to execute
     * @return the graph retrieved
     * @throws IOException in case of IOException
     */
    private CloseableDataset getDataset(final CloseableHttpClient client, final HttpUriRequest req)
            throws IOException {
        if (!req.containsHeader(ACCEPT)) {
            req.addHeader(ACCEPT, "application/n-triples");
        }
        logger.debug("Retrieving RDF using mimeType: {}", req.getFirstHeader(ACCEPT));

        try (final CloseableHttpResponse response = client.execute(req)) {
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            final CloseableDataset result = parseTriples(response.getEntity());
            logger.trace("Retrieved RDF: {}", result);
            return result;
        }

    }

    /**
     * Parses the RDF found in and HTTP response, returning it in a {@link CloseableDataset}.
     *
     * @param response the response to parse
     * @return the graph retrieved
     * @throws IOException in case of IOException
     */
    protected CloseableDataset getDataset(final HttpResponse response) throws IOException {
        assertEquals(OK.getStatusCode(), getStatus(response));
        final CloseableDataset result = parseTriples(response.getEntity());
        logger.trace("Retrieved RDF: {}", result);
        return result;
    }

    /**
     * Executes an HTTP request and parses the RDF found in the response, returning it in a
     * {@link CloseableDataset}, then closes the response.
     *
     * @param req the request to execute
     * @return the constructed graph
     * @throws IOException in case of IOException
     */
    protected CloseableDataset getDataset(final HttpUriRequest req) throws IOException {
        return getDataset(client, req);
    }

    protected Model getModel(final String pid) throws Exception {
        return getModel(null, pid, false);
    }

    protected Model getModel(final String pid, final boolean omitSMTs) throws IOException {
        return getModel(null, pid, omitSMTs);
    }

    protected Model getModel(final String txUri, final String pid) throws Exception {
        return getModel(txUri, pid, false);
    }

    /**
     * Get a model of the triples for the resource.
     * @param txUri id of the transaction
     * @param pid id of the resource
     * @param omitSMTs whether to omit server managed triples from the response.
     * @return the model of the resource triples.
     * @throws IOException on problems getting response.
     */
    protected Model getModel(final String txUri, final String pid, final boolean omitSMTs) throws IOException {
        final Model model = createDefaultModel();
        final HttpGet get = getObjMethod(pid);
        if (txUri != null) {
            get.addHeader(ATOMIC_ID_HEADER, txUri);
        }
        if (omitSMTs) {
            get.addHeader("Prefer", OMIT_SERVER_PREFER_HEADER);
        }
        try (final CloseableHttpResponse response = execute(get)) {
            model.read(response.getEntity().getContent(), serverAddress + pid, "TURTLE");
        }
        return model;
    }

    protected static InputStream streamModel(final Model model, final RDFFormat format) throws IOException {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            RDFDataMgr.write(bos, model, format);
            return new ByteArrayInputStream(bos.toByteArray());
        }
    }

    protected CloseableHttpResponse createObject() {
        return createObject("");
    }

    protected CloseableHttpResponse createObject(final String pid) {
        return createObjectWithLinkHeader(pid, null);
    }

    protected CloseableHttpResponse createObjectWithLinkHeader(final String pid, final String... linkHeaders) {
        final HttpPost httpPost = postObjMethod("/");
        if (isNotEmpty(pid)) {
            httpPost.addHeader("Slug", URLEncoder.encode(pid, StandardCharsets.UTF_8));
        }

        if (linkHeaders != null && linkHeaders.length > 0) {
            for (final String linkHeader : linkHeaders) {
                httpPost.addHeader(LINK, linkHeader);
            }
        }
        try {
            final CloseableHttpResponse response = execute(httpPost);
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            return response;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void createObjectAndClose(final String pid) {
        try {
            createObject(pid).close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void createObjectAndClose(final String pid, final String... linkHeaders) {
        try {

            createObjectWithLinkHeader(pid, linkHeaders).close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String createDatastream(final String id, final String content) throws IOException {
        try (final var response = execute(putDSMethod(id, content))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            return getLocation(response);
        }
    }

    protected String createDatastream(final String pid, final String dsid, final String content) throws IOException {
        logger.trace("Attempting to create datastream for object: {} at datastream ID: {}", pid, dsid);
        try (final var response = execute(putDSMethod(pid, dsid, content))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            return getLocation(response);
        }
    }

    protected CloseableHttpResponse setProperty(final String pid, final String propertyUri, final String value)
            throws IOException {
        return setProperty(pid, null, propertyUri, "\"" + value + "\"");
    }

    protected CloseableHttpResponse setProperty(final String pid, final String propertyUri, final URI value)
            throws IOException {
        return setProperty(pid, null, propertyUri, "<" + value.toString() + ">");
    }

    private CloseableHttpResponse setProperty(final String id, final String txId, final String propertyUri,
                                              final String value) throws IOException {
        final HttpPatch postProp = new HttpPatch(serverAddress + id);
        if (txId != null) {
            addTxTo(postProp, txId);
        }
        postProp.setHeader(CONTENT_TYPE, "application/sparql-update");
        final String updateString =
                "INSERT { <" + serverAddress + id.replace("/" + FCR_METADATA, "") +
                        "> <" + propertyUri + "> " + value + " } WHERE { }";
        postProp.setEntity(new StringEntity(updateString));
        final CloseableHttpResponse dcResp = execute(postProp);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(dcResp));
        postProp.releaseConnection();
        return dcResp;
    }

    protected CloseableHttpResponse setDescriptionProperty(final String id, final String txId,
            final String propertyUri, final String value) throws IOException {
        final HttpPatch postProp = new HttpPatch(serverAddress + (txId != null ? txId + "/" : "") + id +
                "/fcr:metadata");
        postProp.setHeader(CONTENT_TYPE, "application/sparql-update");
        final String updateString =
                "INSERT { <" + serverAddress + id + "> <" + propertyUri + "> \"" + value + "\" } WHERE { }";
        postProp.setEntity(new StringEntity(updateString));
        final CloseableHttpResponse dcResp = execute(postProp);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(dcResp));
        postProp.releaseConnection();
        return dcResp;
    }

    /**
     * Creates a transaction, asserts that it's successful and returns the transaction location.
     *
     * @return string containing transaction location
     * @throws IOException exception thrown during the function
     */
    protected String createTransaction() throws IOException {
        final HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
        try (final CloseableHttpResponse response = execute(createTx)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            return getLocation(response);
        }
    }

    /**
     * Add a transaction id to a http request.
     *
     * @param req a http request object.
     * @param txId the transaction id.
     * @return the http request object with the transaction id added as a header.
     */
    protected <T extends HttpRequestBase> T addTxTo(final T req, final String txId) {
        req.addHeader(ATOMIC_ID_HEADER, txId);
        return req;
    }

    /**
     * Gets a random (but valid) id for use in testing. This id is guaranteed to be unique within runs of this
     * application.
     *
     * @return string containing new id
     */
    protected static String getRandomUniqueId() {
        return randomUUID().toString();
    }

    protected static void assertDeleted(final String id) {
        final String location = serverAddress + id;
        assertThat("Expected object to be deleted", getStatus(new HttpHead(location)), is(GONE.getStatusCode()));
        assertThat("Expected object to be deleted", getStatus(new HttpGet(location)), is(GONE.getStatusCode()));
    }

    protected static void assertNotFound(final String id) {
        final String location = serverAddress + id;
        assertThat("Expected object to return 404", getStatus(new HttpHead(location)), is(NOT_FOUND.getStatusCode()));
        assertThat("Expected object to return 404", getStatus(new HttpGet(location)), is(NOT_FOUND.getStatusCode()));
    }

    protected static void assertNotDeleted(final String id) {
        final String location = serverAddress + id;
        assertThat("Expected object not to be deleted", getStatus(new HttpHead(location)), is(OK.getStatusCode()));
        assertThat("Expected object not to be deleted", getStatus(new HttpGet(location)), is(OK.getStatusCode()));
    }

    protected static String getTTLThatUpdatesServerManagedTriples(final String createdBy, final Calendar created,
                                                                  final String modifiedBy, final Calendar modified) {
        final StringBuilder ttl = new StringBuilder();
        if (createdBy != null) {
            addClause(ttl, CREATED_BY.getURI(), "\"" + createdBy + "\"");
        }
        if (created != null) {
            addClause(ttl, CREATED_DATE.getURI(),
                    "\"" + DatatypeConverter.printDateTime(created)
                    + "\"^^<http://www.w3.org/2001/XMLSchema#dateTime>");
        }
        if (modifiedBy != null) {
            addClause(ttl, LAST_MODIFIED_BY.getURI(), "\"" + modifiedBy + "\"");
        }
        if (modified != null) {
            addClause(ttl, LAST_MODIFIED_DATE.getURI(),
                    "\"" + DatatypeConverter.printDateTime(modified)
                    + "\"^^<http://www.w3.org/2001/XMLSchema#dateTime>");
        }
        ttl.append(" .\n");
        return ttl.toString();

    }

    private static void addClause(final StringBuilder ttl, final String predicateUri, final String literal) {
        if (ttl.length() == 0) {
            ttl.append("<>");
        } else {
            ttl.append(" ;\n");
        }
        ttl.append(" <" + predicateUri + "> ");
        ttl.append(literal);
    }

    /**
     * Test a response for the absence of a specific LINK header
     *
     * @param response the HTTP response
     * @param uri the URI not to exist in the LINK header
     * @param rel the rel argument to check for
     */
    protected static void assertNoLinkHeader(final HttpResponse response, final String uri, final String rel) {
        assertEquals(0, countLinkHeader(response, uri, rel));
    }

    /**
     * Test a response for a specific LINK header
     *
     * @param response the HTTP response
     * @param uri the URI expected in the LINK header
     * @param rel the rel argument to check for
     */
    protected static void checkForLinkHeader(final HttpResponse response, final String uri, final String rel) {
        assertEquals(1, countLinkHeader(response, uri, rel));
    }

    /**
     * Utility for counting LINK headers
     *
     * @param response the HTTP response
     * @param uri the URI expected in the LINK header
     * @param rel the rel argument to check for
     * @return the count of LINK headers.
     */
    private static int countLinkHeader(final HttpResponse response, final String uri, final String rel) {
        final Link linkA = Link.valueOf("<" + uri + ">; rel=" + rel);
        return (int) Arrays.stream(response.getHeaders(LINK)).filter(x -> {
            final Link linkB = Link.valueOf(x.getValue());
            return linkB.equals(linkA);
        }).count();
    }

    protected static String getOriginalResourceUri(final CloseableHttpResponse response) {
        return getLinkHeaders(response).stream()
            .map(x -> Link.valueOf(x))
            .filter(x -> x.getRel().equals("original"))
            .findFirst().get().getUri().toString();
    }

    protected String getExternalContentLinkHeader(final String url, final String handling, final String mimeType) {
        // leave lots of room to leave things out of the link to test variations.
        String link = "";
        if (url != null && !url.isEmpty()) {
            link += "<" + url + ">";
        }

        link += "; rel=\"" + EXTERNAL_CONTENT + "\"";

        if (handling != null && !handling.isEmpty()) {
            link += "; handling=\"" + handling + "\"";
        }

        if (mimeType != null && !mimeType.isEmpty()) {
            link += "; type=\"" + mimeType + "\"";
        }
        return link;
    }

    protected static void assertConstrainedByPresent(final CloseableHttpResponse response) {
        final Collection<String> linkHeaders = getLinkHeaders(response);
        assertTrue(linkHeaders.stream().map(Link::valueOf)
                        .anyMatch(l -> l.getRel().equals(CONSTRAINED_BY.getURI())),
                "Constrained by link header not present");
    }


    /**
     * Create a Prefer header
     * @param includes String of include URIs or null if none
     * @param omits String of omit URIs or null if none
     * @return The Prefer header.
     */
    protected static String preferLink(final String includes, final String omits) {
        if (includes != null || omits != null) {
            String link = "return=representation; ";
            if (includes != null) {
                link += "include=\"" + includes + "\"";
            }
            if (includes != null && omits != null) {
                link += "; ";
            }
            if (omits != null) {
                link += "omit=\"" + omits + "\"";
            }
            return link;
        }
        return "";
    }

    /**
     * Compare two N-Triple response bodies to determine if they are identical
     * @param responseBodyA the first n-triple body
     * @param responseBodyB the second n-triple body
     */
    protected static void confirmResponseBodyNTriplesAreEqual(final String responseBodyA, final String responseBodyB) {
        final String[] aTriples = responseBodyA.split(".(\\r\\n|\\r|\\n)");
        final String[] bTriples = responseBodyB.split(".(\\r\\n|\\r|\\n)");
        Arrays.stream(aTriples).map(String::trim).sorted().toArray(unused -> aTriples);
        Arrays.stream(bTriples).map(String::trim).sorted().toArray(unused -> bTriples);
        assertArrayEquals(aTriples, bTriples);
    }

    /**
     * Instantiation of the authentication handle cache for integration tests.
     */
    @Configuration
    static class TestConfig {
        @Bean
        public Cache<String, Optional<ACLHandle>> authHandleCache() {
            return Caffeine.newBuilder().weakValues().expireAfterAccess(10, TimeUnit.SECONDS)
                    .maximumSize(10).build();
        }
    }
}
