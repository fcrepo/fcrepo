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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static java.net.URI.create;
import static org.fcrepo.http.commons.session.TransactionProvider.JMS_BASEURL_PROP;
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

    private TransactionProvider createProvider(final UriInfo uriInfo) {
        return new TransactionProvider(transactionManager, request, uriInfo.getBaseUri());
    }

    @Test
    public void testSetUserAgent() {
        // Obtain a concrete instance of UriInfo
        final URI baseUri = create("http://localhost/fcrepo");
        final URI requestUri = create("http://localhost/fcrepo/foo");
        final ContainerRequest req = new ContainerRequest(baseUri, requestUri, "GET", mock(SecurityContext.class),
                mock(PropertiesDelegate.class));
        final UriInfo info = spy(req.getUriInfo());

        final String expectedBaseUrl = baseUri.toString();
        final String expectedUserAgent = "fedoraAdmin";

        when(request.getHeader("user-agent")).thenReturn(expectedUserAgent);

        createProvider(info).provide();

        verify(transaction).setBaseUri(expectedBaseUrl);
        verify(transaction).setUserAgent(expectedUserAgent);
        verify(info).getBaseUri();
    }

    /**
     * Demonstrates that when the {@link TransactionProvider#JMS_BASEURL_PROP fcrepo.jms.baseUrl} system property is not
     * set, the url used for JMS messages is the same as the base url found in the {@code UriInfo}.
     * <p>
     * Implementation note: this test requires a concrete instance of {@link UriInfo}, because it is the interaction of
     * {@code javax.ws.rs.core.UriBuilder} and {@code TransactionProvider} that is being tested.
     * </p>
     */
    @Test
    public void testJmsBaseUrlDefault() {
        // Obtain a concrete instance of UriInfo
        final URI baseUri = create("http://localhost/fcrepo");
        final URI requestUri = create("http://localhost/fcrepo/foo");
        final ContainerRequest req = new ContainerRequest(baseUri, requestUri, "GET", mock(SecurityContext.class),
                mock(PropertiesDelegate.class));
        final UriInfo info = spy(req.getUriInfo());

        final String expectedBaseUrl = baseUri.toString();

        createProvider(info).provide();

        verify(transaction).setBaseUri(expectedBaseUrl);
        verify(info).getBaseUri();
    }

    /**
     * Demonstrates that the host supplied by the {@link TransactionProvider#JMS_BASEURL_PROP fcrepo.jms.baseUrl} system
     * property is used as the as the base url for JMS messages, and not the base url found in {@code UriInfo}.
     * <p>
     * Note: the path from the request is preserved, the host from the fcrepo.jms.baseUrl is used
     * </p>
     * <p>
     * Implementation note: this test requires a concrete instance of {@link UriInfo}, because it is the interaction of
     * {@code javax.ws.rs.core.UriBuilder} and {@code TransactionProvider} that is being tested.
     * </p>
     */
    @Test
    public void testJmsBaseUrlOverrideHost() {
        // Obtain a concrete implementation of UriInfo
        final URI baseUri = create("http://localhost/fcrepo");
        final URI requestUri = create("http://localhost/fcrepo/foo");
        final ContainerRequest req = new ContainerRequest(baseUri, requestUri, "GET", mock(SecurityContext.class),
                mock(PropertiesDelegate.class));
        final UriInfo info = spy(req.getUriInfo());

        final String baseUrl = "http://example.org";
        final String expectedBaseUrl = baseUrl + baseUri.getPath();
        System.setProperty(JMS_BASEURL_PROP, baseUrl);

        createProvider(info).provide();

        verify(transaction).setBaseUri(expectedBaseUrl);
        System.clearProperty(JMS_BASEURL_PROP);
    }

    /**
     * Demonstrates that the host and port supplied by the {@link TransactionProvider#JMS_BASEURL_PROP
     * fcrepo.jms.baseUrl} system property is used as the as the base url for JMS messages, and not the base url found
     * in {@code UriInfo}.
     * <p>
     * Note: the path from the request is preserved, but the host and port from the request is overridden by the values
     * from fcrepo.jms.baseUrl
     * </p>
     * <p>
     * Implementation note: this test requires a concrete instance of {@link UriInfo}, because it is the interaction of
     * {@code javax.ws.rs.core.UriBuilder} and {@code TransactionProvider} that is being tested.
     * </p>
     */
    @Test
    public void testJmsBaseUrlOverrideHostAndPort() {
        // Obtain a concrete implementation of UriInfo
        final URI baseUri = create("http://localhost/fcrepo");
        final URI requestUri = create("http://localhost/fcrepo/foo");
        final ContainerRequest req = new ContainerRequest(baseUri, requestUri, "GET", mock(SecurityContext.class),
                mock(PropertiesDelegate.class));
        final UriInfo info = spy(req.getUriInfo());

        final String baseUrl = "http://example.org:9090";
        final String expectedBaseUrl = baseUrl + baseUri.getPath();
        System.setProperty(JMS_BASEURL_PROP, baseUrl);

        createProvider(info).provide();

        verify(transaction).setBaseUri(expectedBaseUrl);
        System.clearProperty(JMS_BASEURL_PROP);
    }

    /**
     * Demonstrates that the url supplied by the {@link TransactionProvider#JMS_BASEURL_PROP fcrepo.jms.baseUrl} system
     * property is used as the as the base url for JMS messages, and not the base url found in {@code UriInfo}.
     * <p>
     * Note: the host and path from the request is overridden by the values from fcrepo.jms.baseUrl
     * </p>
     * <p>
     * Implementation note: this test requires a concrete instance of {@link UriInfo}, because it is the interaction of
     * {@code javax.ws.rs.core.UriBuilder} and {@code TransactionProvider} that is being tested.
     * </p>
     */
    @Test
    public void testJmsBaseUrlOverrideUrl() {
        // Obtain a concrete implementation of UriInfo
        final URI baseUri = create("http://localhost/fcrepo");
        final URI requestUri = create("http://localhost/fcrepo/foo");
        final ContainerRequest req = new ContainerRequest(baseUri, requestUri, "GET", mock(SecurityContext.class),
                mock(PropertiesDelegate.class));
        final UriInfo info = spy(req.getUriInfo());

        final String expectedBaseUrl = "http://example.org/fcrepo/rest";
        System.setProperty(JMS_BASEURL_PROP, expectedBaseUrl);

        createProvider(info).provide();

        verify(transaction).setBaseUri(expectedBaseUrl);
        System.clearProperty(JMS_BASEURL_PROP);
    }

    /**
     * Demonstrates that when the the base url in {@code UriInfo} contains a port number, and the base url defined by
     * {@link TransactionProvider#JMS_BASEURL_PROP fcrepo.jms.baseUrl} does <em>not</em> contain a port number, that the
     * base url for JMS messages does not contain a port number.
     * <p>
     * Note: the host, port, and path from the request is overridden by values from fcrepo.jms.baseUrl
     * </p>
     * <p>
     * Implementation note: this test requires a concrete instance of {@link UriInfo}, because it is the interaction of
     * {@code javax.ws.rs.core.UriBuilder} and {@code TransactionProvider} that is being tested.
     * </p>
     */
    @Test
    public void testJmsBaseUrlOverrideRequestUrlWithPort8080() {
        // Obtain a concrete implementation of UriInfo
        final URI baseUri = create("http://localhost:8080/fcrepo/rest");
        final URI reqUri = create("http://localhost:8080/fcrepo/rest/foo");
        final ContainerRequest req = new ContainerRequest(baseUri, reqUri, "GET", mock(SecurityContext.class),
                mock(PropertiesDelegate.class));
        final UriInfo info = spy(req.getUriInfo());

        final String expectedBaseUrl = "http://example.org/fcrepo/rest/";
        System.setProperty(JMS_BASEURL_PROP, expectedBaseUrl);

        createProvider(info).provide();

        verify(transaction).setBaseUri(expectedBaseUrl);
        System.clearProperty(JMS_BASEURL_PROP);
    }

}
