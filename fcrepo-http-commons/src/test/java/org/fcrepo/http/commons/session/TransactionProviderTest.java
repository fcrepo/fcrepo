/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.http.commons.session;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;

import static java.net.URI.create;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author pwinckles
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class TransactionProviderTest {

    @Mock
    private TransactionManager transactionManager;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Transaction transaction;

    @Before
    public void setup() {
        when(transactionManager.create()).thenReturn(transaction);
    }

    private TransactionProvider createProvider(final UriInfo uriInfo, final String jmsBaseUrl) {
        return new TransactionProvider(transactionManager, request, uriInfo.getBaseUri(), jmsBaseUrl);
    }

    @Test
    public void testSetUserAgent() {
        // Obtain a concrete instance of UriInfo
        final URI baseUri = create("http://localhost/fcrepo");
        final URI requestUri = create("http://localhost/fcrepo/foo");
        final ContainerRequest req = new ContainerRequest(baseUri, requestUri, "GET", mock(SecurityContext.class),
                mock(PropertiesDelegate.class), mock(Configuration.class));
        final UriInfo info = spy(req.getUriInfo());

        final String expectedBaseUrl = baseUri.toString();
        final String expectedUserAgent = "fedoraAdmin";

        when(request.getHeader("user-agent")).thenReturn(expectedUserAgent);

        createProvider(info, "").provide();

        verify(transaction).setBaseUri(expectedBaseUrl);
        verify(transaction).setUserAgent(expectedUserAgent);
        verify(info).getBaseUri();
    }

    /**
     * Demonstrates that when the jmsBaseUrl is not set, the url used for JMS messages is the same as the base url
     * found in the {@code UriInfo}.
     * <p>
     * Implementation note: this test requires a concrete instance of {@link UriInfo}, because it is the interaction of
     * {@code jakarta.ws.rs.core.UriBuilder} and {@code TransactionProvider} that is being tested.
     * </p>
     */
    @Test
    public void testJmsBaseUrlDefault() {
        // Obtain a concrete instance of UriInfo
        final URI baseUri = create("http://localhost/fcrepo");
        final URI requestUri = create("http://localhost/fcrepo/foo");
        final ContainerRequest req = new ContainerRequest(baseUri, requestUri, "GET", mock(SecurityContext.class),
                mock(PropertiesDelegate.class), mock(Configuration.class));
        final UriInfo info = spy(req.getUriInfo());

        final String expectedBaseUrl = baseUri.toString();

        createProvider(info, "").provide();

        verify(transaction).setBaseUri(expectedBaseUrl);
        verify(info).getBaseUri();
    }

    /**
     * Demonstrates that the host supplied by the jmsBaseUrl is used as the as the base url for JMS messages, and not
     * the base url found in {@code UriInfo}.
     * <p>
     * Note: the path from the request is preserved, the host from the fcrepo.jms.baseUrl is used
     * </p>
     * <p>
     * Implementation note: this test requires a concrete instance of {@link UriInfo}, because it is the interaction of
     * {@code jakarta.ws.rs.core.UriBuilder} and {@code TransactionProvider} that is being tested.
     * </p>
     */
    @Test
    public void testJmsBaseUrlOverrideHost() {
        // Obtain a concrete implementation of UriInfo
        final URI baseUri = create("http://localhost/fcrepo");
        final URI requestUri = create("http://localhost/fcrepo/foo");
        final ContainerRequest req = new ContainerRequest(baseUri, requestUri, "GET", mock(SecurityContext.class),
                mock(PropertiesDelegate.class), mock(Configuration.class));
        final UriInfo info = spy(req.getUriInfo());

        final String baseUrl = "http://example.org";
        final String expectedBaseUrl = baseUrl + baseUri.getPath();

        createProvider(info, baseUrl).provide();

        verify(transaction).setBaseUri(expectedBaseUrl);
    }

    /**
     * Demonstrates that the host and port supplied by the jmsBaseUrl is used as the as the base url for JMS messages,
     * and not the base url found
     * in {@code UriInfo}.
     * <p>
     * Note: the path from the request is preserved, but the host and port from the request is overridden by the values
     * from fcrepo.jms.baseUrl
     * </p>
     * <p>
     * Implementation note: this test requires a concrete instance of {@link UriInfo}, because it is the interaction of
     * {@code jakarta.ws.rs.core.UriBuilder} and {@code TransactionProvider} that is being tested.
     * </p>
     */
    @Test
    public void testJmsBaseUrlOverrideHostAndPort() {
        // Obtain a concrete implementation of UriInfo
        final URI baseUri = create("http://localhost/fcrepo");
        final URI requestUri = create("http://localhost/fcrepo/foo");
        final ContainerRequest req = new ContainerRequest(baseUri, requestUri, "GET", mock(SecurityContext.class),
                mock(PropertiesDelegate.class), mock(Configuration.class));
        final UriInfo info = spy(req.getUriInfo());

        final String baseUrl = "http://example.org:9090";
        final String expectedBaseUrl = baseUrl + baseUri.getPath();

        createProvider(info, baseUrl).provide();

        verify(transaction).setBaseUri(expectedBaseUrl);
    }

    /**
     * Demonstrates that the url supplied by the jmsBaseUrl is used as the as the base url for JMS messages, and not
     * the base url found in {@code UriInfo}.
     * <p>
     * Note: the host and path from the request is overridden by the values from fcrepo.jms.baseUrl
     * </p>
     * <p>
     * Implementation note: this test requires a concrete instance of {@link UriInfo}, because it is the interaction of
     * {@code jakarta.ws.rs.core.UriBuilder} and {@code TransactionProvider} that is being tested.
     * </p>
     */
    @Test
    public void testJmsBaseUrlOverrideUrl() {
        // Obtain a concrete implementation of UriInfo
        final URI baseUri = create("http://localhost/fcrepo");
        final URI requestUri = create("http://localhost/fcrepo/foo");
        final ContainerRequest req = new ContainerRequest(baseUri, requestUri, "GET", mock(SecurityContext.class),
                mock(PropertiesDelegate.class), mock(Configuration.class));
        final UriInfo info = spy(req.getUriInfo());

        final String expectedBaseUrl = "http://example.org/fcrepo/rest";

        createProvider(info, expectedBaseUrl).provide();

        verify(transaction).setBaseUri(expectedBaseUrl);
    }

    /**
     * Demonstrates that when the the base url in {@code UriInfo} contains a port number, and the base url does
     * <em>not</em> contain a port number, that the
     * base url for JMS messages does not contain a port number.
     * <p>
     * Note: the host, port, and path from the request is overridden by values from fcrepo.jms.baseUrl
     * </p>
     * <p>
     * Implementation note: this test requires a concrete instance of {@link UriInfo}, because it is the interaction of
     * {@code jakarta.ws.rs.core.UriBuilder} and {@code TransactionProvider} that is being tested.
     * </p>
     */
    @Test
    public void testJmsBaseUrlOverrideRequestUrlWithPort8080() {
        // Obtain a concrete implementation of UriInfo
        final URI baseUri = create("http://localhost:8080/fcrepo/rest");
        final URI reqUri = create("http://localhost:8080/fcrepo/rest/foo");
        final ContainerRequest req = new ContainerRequest(baseUri, reqUri, "GET", mock(SecurityContext.class),
                mock(PropertiesDelegate.class), mock(Configuration.class));
        final UriInfo info = spy(req.getUriInfo());

        final String expectedBaseUrl = "http://example.org/fcrepo/rest/";

        createProvider(info, expectedBaseUrl).provide();

        verify(transaction).setBaseUri(expectedBaseUrl);
    }

}
