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
package org.fcrepo.http.api;

import static com.google.common.base.Predicates.containsPattern;
import static java.net.URI.create;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Stream.of;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.TEMPORARY_REDIRECT;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.http.api.ContentExposingResource.getSimpleContentType;
import static org.fcrepo.http.api.FedoraBaseResource.JMS_BASEURL_PROP;
import static org.fcrepo.http.api.FedoraLdp.HTTP_HEADER_ACCEPT_PATCH;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES_TYPE;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_BASIC_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONED_RESOURCE;
import static org.fcrepo.kernel.api.observer.OptionalValues.BASE_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.http.api.PathLockManager.AcquiredLock;
import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.http.commons.domain.MultiPrefer;
import org.fcrepo.http.commons.domain.PreferTag;
import org.fcrepo.http.commons.responses.RdfNamespacedStream;
import org.fcrepo.http.commons.session.HttpSession;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.TripleCategory;
import org.fcrepo.kernel.api.exception.CannotCreateResourceException;
import org.fcrepo.kernel.api.exception.InsufficientStorageException;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.PreconditionException;
import org.fcrepo.kernel.api.exception.UnsupportedAccessTypeException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.services.NodeService;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.springframework.mock.web.MockHttpServletResponse;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * @author cabeer
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class FedoraLdpTest {

    private final String path = "/some/path";
    private final String binaryPath = "/some/binary/path";

    private final String binaryDescriptionPath = binaryPath + "/fedora:description";
    private final String containerConstraints = "http://localhost/static/constraints/ContainerConstraints.rdf";
    private final String nonRDFSourceConstraints = "http://localhost/static/constraints/NonRDFSourceConstraints.rdf";
    private FedoraLdp testObj;

    private final List<String> nonRDFSourceLink = Arrays.asList(
            Link.fromUri(NON_RDF_SOURCE.toString()).rel("type").build().toString());

    @Mock
    private Request mockRequest;

    private HttpServletResponse mockResponse;

    @Mock
    private HttpSession mockSession;

    @Mock
    private FedoraSession mockFedoraSession;

    @Mock
    private Container mockContainer;

    @Mock
    private NonRdfSourceDescription mockNonRdfSourceDescription;

    @Mock
    private FedoraBinary mockBinary;

    private IdentifierConverter<Resource, FedoraResource> idTranslator;

    @Mock
    private NodeService mockNodeService;

    @Mock
    private ContainerService mockContainerService;

    @Mock
    private BinaryService mockBinaryService;

    @Mock
    private FedoraHttpConfiguration mockHttpConfiguration;

    @Mock
    private HttpHeaders mockHeaders;

    @Mock
    private SecurityContext mockSecurityContext;

    @Mock
    private ServletContext mockServletContext;

    @Mock
    private PathLockManager mockLockManager;

    @Mock
    private AcquiredLock mockLock;

    @Mock
    private MultiPrefer prefer;

    @Mock
    private PreferTag preferTag;

    private static final Logger log = getLogger(FedoraLdpTest.class);


    @Before
    public void setUp() {
        testObj = spy(new FedoraLdp(path));

        mockResponse = new MockHttpServletResponse();

        idTranslator = new HttpResourceConverter(mockSession,
                UriBuilder.fromUri("http://localhost/fcrepo/{path: .*}"));

        setField(testObj, "request", mockRequest);
        setField(testObj, "servletResponse", mockResponse);
        setField(testObj, "uriInfo", getUriInfoImpl());
        setField(testObj, "headers", mockHeaders);
        setField(testObj, "idTranslator", idTranslator);
        setField(testObj, "nodeService", mockNodeService);
        setField(testObj, "containerService", mockContainerService);
        setField(testObj, "binaryService", mockBinaryService);
        setField(testObj, "httpConfiguration", mockHttpConfiguration);
        setField(testObj, "session", mockSession);
        setField(testObj, "securityContext", mockSecurityContext);
        setField(testObj, "lockManager", mockLockManager);
        setField(testObj, "context", mockServletContext);
        setField(testObj, "prefer", prefer);

        when(mockHttpConfiguration.putRequiresIfMatch()).thenReturn(false);

        when(mockContainer.getEtagValue()).thenReturn("");
        when(mockContainer.getPath()).thenReturn(path);
        when(mockContainer.getDescription()).thenReturn(mockContainer);
        when(mockContainer.getDescribedResource()).thenReturn(mockContainer);

        when(mockNonRdfSourceDescription.getEtagValue()).thenReturn("");
        when(mockNonRdfSourceDescription.getPath()).thenReturn(binaryDescriptionPath);
        when(mockNonRdfSourceDescription.getDescribedResource()).thenReturn(mockBinary);
        when(mockNonRdfSourceDescription.getOriginalResource()).thenReturn(mockNonRdfSourceDescription);

        when(mockBinary.getEtagValue()).thenReturn("");
        when(mockBinary.getPath()).thenReturn(binaryPath);
        when(mockBinary.getDescription()).thenReturn(mockNonRdfSourceDescription);
        when(mockBinary.getDescribedResource()).thenReturn(mockBinary);
        when(mockBinary.getOriginalResource()).thenReturn(mockBinary);

        when(mockHeaders.getHeaderString("user-agent")).thenReturn("Test UserAgent");

        when(mockLockManager.lockForRead(any())).thenReturn(mockLock);
        when(mockLockManager.lockForWrite(any(), any(), any())).thenReturn(mockLock);
        when(mockLockManager.lockForDelete(any())).thenReturn(mockLock);
        when(mockSession.getId()).thenReturn("foo1234");
        when(mockSession.getFedoraSession()).thenReturn(mockFedoraSession);

        when(mockServletContext.getContextPath()).thenReturn("/");

        when(prefer.getReturn()).thenReturn(preferTag);
                doAnswer((Answer<HttpServletResponse>) invocation -> {
                    mockResponse.addHeader("Preference-Applied", "return=representation");
                    return null;
                }).when(preferTag).addResponseHeaders(mockResponse);
    }

    private FedoraResource setResource(final Class<? extends FedoraResource> klass) {
        final FedoraResource mockResource = mock(klass);
        if (mockResource instanceof FedoraBinary) {
            when(((FedoraBinary) mockResource).getContentSize()).thenReturn(1l);
        }

        final Answer<RdfStream> answer = invocationOnMock -> new DefaultRdfStream(
                createURI(invocationOnMock.getMock().toString()),
                of(Triple.create(createURI(invocationOnMock.getMock().toString()),
                        createURI("called"),
                        createURI(invocationOnMock.getArguments()[1].toString()))));

        doReturn(mockResource).when(testObj).resource();
        when(mockResource.getPath()).thenReturn(path);
        when(mockResource.getEtagValue()).thenReturn("");
        when(mockResource.getDescription()).thenReturn(mockResource);
        when(mockResource.getDescribedResource()).thenReturn(mockResource);
        when(mockResource.getTriples(eq(idTranslator), anySetOf(TripleCategory.class))).thenAnswer(answer);
        when(mockResource.getTriples(eq(idTranslator), any(TripleCategory.class))).thenAnswer(answer);

        return mockResource;
    }

    @Test
    public void testHead() throws Exception {
        setResource(FedoraResource.class);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should have a Link header", mockResponse.containsHeader(LINK));
        assertTrue("Should have an Allow header", mockResponse.containsHeader("Allow"));
        assertTrue("Should have a Preference-Applied header", mockResponse.containsHeader("Preference-Applied"));
        assertTrue("Should have a Vary header", mockResponse.containsHeader("Vary"));
        assertTrue("Should be an LDP Resource",
                mockResponse.getHeaders(LINK).contains("<" + LDP_NAMESPACE + "Resource>;rel=\"type\""));
        assertShouldHaveConstraintsLink();
    }

    private void assertShouldHaveConstraintsLink() {
        assertTrue("Should have a constraints document",
                mockResponse.getHeaders(LINK).contains("<" + containerConstraints + ">; rel=\"" +
                CONSTRAINED_BY.toString() + "\""));
    }

    @Test
    public void testHeadWithObject() throws Exception {
        setResource(Container.class);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertTrue("Should advertise Accept-Patch flavors", mockResponse.containsHeader(
                FedoraLdp.HTTP_HEADER_ACCEPT_PATCH));
    }

    @Test
    public void testHeadWithDefaultContainer() throws Exception {
        setResource(Container.class);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldBeAnLDPBasicContainer();
        assertShouldHaveConstraintsLink();
    }

    private void assertShouldBeAnLDPBasicContainer() {
        assertTrue("Should be an LDP BasicContainer",
                mockResponse.getHeaders(LINK).contains("<" + BASIC_CONTAINER.getURI() + ">;rel=\"type\""));
    }

    @Test
    public void testHeadWithBasicContainer() throws Exception {
        final FedoraResource resource = setResource(Container.class);
        when(resource.hasType(LDP_BASIC_CONTAINER)).thenReturn(true);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldBeAnLDPBasicContainer();
        assertShouldHaveConstraintsLink();
    }

    @Test
    public void testHeadWithDirectContainer() throws Exception {
        final FedoraResource resource = setResource(Container.class);
        when(resource.hasType(LDP_DIRECT_CONTAINER)).thenReturn(true);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldBeAnLDPDirectContainer();
        assertShouldHaveConstraintsLink();
    }

    private void assertShouldBeAnLDPDirectContainer() {
        assertTrue("Should be an LDP DirectContainer",
                mockResponse.getHeaders(LINK).contains("<" + DIRECT_CONTAINER.getURI() + ">;rel=\"type\""));
    }

    @Test
    public void testHeadWithIndirectContainer() throws Exception {
        final FedoraResource resource = setResource(Container.class);
        when(resource.hasType(LDP_INDIRECT_CONTAINER)).thenReturn(true);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldBeAnLDPIndirectContainer();
        assertShouldHaveConstraintsLink();
    }

    private void assertShouldBeAnLDPIndirectContainer() {
        assertTrue("Should be an LDP IndirectContainer",
                mockResponse.getHeaders(LINK).contains("<" + INDIRECT_CONTAINER.getURI() + ">;rel=\"type\""));
    }

    @Test
    public void testHeadWithBinary() throws Exception {
        final FedoraBinary mockResource = (FedoraBinary)setResource(FedoraBinary.class);
        when(mockResource.getDescription()).thenReturn(mockNonRdfSourceDescription);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockResource.getMimeType()).thenReturn("image/jpeg");
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertContentLengthGreaterThan0(mockResponse.getHeader(CONTENT_LENGTH));
        assertShouldBeAnLDPNonRDFSource();
        assertShouldNotAdvertiseAcceptPatchFlavors();
        assertShouldContainLinkToBinaryDescription();
        assertShouldHaveNonRDFSourceConstraintsLink();
    }

    private void assertContentLengthGreaterThan0(final String contentLength) {
        assertTrue("Should have a content length header greater than 0", Integer.parseInt(contentLength) > 0);
    }

    private void assertShouldContainLinkToBinaryDescription() {
        assertTrue("Should contain a link to the binary description",
                mockResponse.getHeaders(LINK)
                        .contains("<" + idTranslator.toDomain(binaryPath + "/fcr:metadata")
                                + ">; rel=\"describedby\""));
    }

    private void assertShouldHaveNonRDFSourceConstraintsLink() {
        assertTrue("Should have a constraints document",
                mockResponse.getHeaders(LINK).contains("<" + nonRDFSourceConstraints + ">; rel=\"" +
                        CONSTRAINED_BY.toString() + "\""));
    }

    private void assertShouldNotAdvertiseAcceptPatchFlavors() {
        assertFalse("Should not advertise Accept-Patch flavors", mockResponse.containsHeader(HTTP_HEADER_ACCEPT_PATCH));
    }

    private void assertShouldNotAdvertiseAcceptPostFlavors() {
        assertFalse("Should not advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
    }

    private void assertShouldHaveAcceptExternalContentHandlingHeader() {
        assertTrue("Should have Accept-External-Content-Handling header",
                mockResponse.containsHeader(FedoraLdp.ACCEPT_EXTERNAL_CONTENT));
        assertEquals("Should support copy, redirect, and proxy", "copy,redirect,proxy",
                mockResponse.getHeader(FedoraLdp.ACCEPT_EXTERNAL_CONTENT));
    }

    private void assertShouldNotHaveAcceptExternalContentHandlingHeader() {
        assertFalse("Should not have Accept-External-Content-Handling header",
                mockResponse.containsHeader(FedoraLdp.ACCEPT_EXTERNAL_CONTENT));
    }

    @Test
    public void testHeadWithExternalBinary() throws Exception {
        final FedoraBinary mockResource = (FedoraBinary)setResource(FedoraBinary.class);
        when(mockResource.getDescription()).thenReturn(mockNonRdfSourceDescription);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockResource.getMimeType()).thenReturn("message/external-body; access-type=URL; URL=\"some:uri\"");
        final Response actual = testObj.head();
        assertEquals(TEMPORARY_REDIRECT.getStatusCode(), actual.getStatus());
        assertShouldBeAnLDPNonRDFSource();
        assertShouldNotAdvertiseAcceptPatchFlavors();
        assertShouldContainLinkToBinaryDescription();
    }

    @Test
    public void testHeadWithBinaryDescription() throws Exception {
        final NonRdfSourceDescription mockResource
                = (NonRdfSourceDescription)setResource(NonRdfSourceDescription.class);
        when(mockResource.getDescribedResource()).thenReturn(mockBinary);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP RDFSource", mockResponse.getHeaders(LINK).contains("<" + LDP_NAMESPACE +
                "RDFSource>;rel=\"type\""));
        assertShouldNotAdvertiseAcceptPostFlavors();
        assertShouldAdvertiseAcceptPatchFlavors();
        assertShouldContainLinkToTheBinary();
        assertShouldAllowOnlyResourceDescriptionMethods();
    }

    private void assertShouldContainLinkToTheBinary() {
        assertTrue("Should contain a link to the binary",
                mockResponse.getHeaders(LINK)
                        .contains("<" + idTranslator.toDomain(binaryPath) + ">; rel=\"describes\""));
    }

    private void assertShouldAdvertiseAcceptPatchFlavors() {
        assertTrue("Should advertise Accept-Patch flavors", mockResponse.containsHeader(HTTP_HEADER_ACCEPT_PATCH));
    }

    @Test
    public void testOption() throws Exception {
        setResource(FedoraResource.class);
        final Response actual = testObj.options();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should have an Allow header", mockResponse.containsHeader("Allow"));
        assertShouldNotHaveAcceptExternalContentHandlingHeader();
    }

    @Test
    public void testOptionWithObject() throws Exception {
        setResource(Container.class);
        final Response actual = testObj.options();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertShouldAdvertiseAcceptPatchFlavors();
        assertShouldNotHaveAcceptExternalContentHandlingHeader();
    }

    @Test
    public void testOptionWithBinary() throws Exception {
        final FedoraBinary mockResource = (FedoraBinary)setResource(FedoraBinary.class);
        when(mockResource.getDescription()).thenReturn(mockNonRdfSourceDescription);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        final Response actual = testObj.options();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldNotAdvertiseAcceptPostFlavors();
        assertShouldNotAdvertiseAcceptPatchFlavors();
        assertShouldContainLinkToBinaryDescription();
        assertShouldHaveAcceptExternalContentHandlingHeader();
    }

    @Test
    public void testOptionWithBinaryDescription() throws Exception {
        final NonRdfSourceDescription mockResource
                = (NonRdfSourceDescription)setResource(NonRdfSourceDescription.class);
        when(mockResource.getDescribedResource()).thenReturn(mockBinary);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        final Response actual = testObj.options();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldNotAdvertiseAcceptPostFlavors();
        assertShouldAdvertiseAcceptPatchFlavors();
        assertShouldContainLinkToTheBinary();
        assertShouldNotHaveAcceptExternalContentHandlingHeader();
    }


    @Test
    public void testGet() throws Exception {
        setResource(FedoraResource.class);
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should have a Link header", mockResponse.containsHeader(LINK));
        assertTrue("Should have an Allow header", mockResponse.containsHeader("Allow"));
        assertTrue("Should be an LDP Resource",
                mockResponse.getHeaders(LINK).contains("<" + LDP_NAMESPACE + "Resource>;rel=\"type\""));

        try (final RdfNamespacedStream entity = (RdfNamespacedStream) actual.getEntity()) {
            final Model model = entity.stream.collect(toModel());
            final List<String> rdfNodes = model.listObjects().mapWith(RDFNode::toString).toList();
            assertTrue("Expected RDF contexts missing", rdfNodes.containsAll(ImmutableSet.of(
                    "LDP_CONTAINMENT", "LDP_MEMBERSHIP", "PROPERTIES", "SERVER_MANAGED")));
        }
    }

    @Test
    public void testGetWithObject() throws Exception {
        setResource(Container.class);
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertShouldAdvertiseAcceptPatchFlavors();
        assertShouldHaveConstraintsLink();

        try (final RdfNamespacedStream entity = (RdfNamespacedStream) actual.getEntity()) {
            final Model model = entity.stream.collect(toModel());
            final List<String> rdfNodes = model.listObjects().mapWith(RDFNode::toString).toList();
            assertTrue("Expected RDF contexts missing", rdfNodes.containsAll(ImmutableSet.of(
                    "LDP_CONTAINMENT", "LDP_MEMBERSHIP", "PROPERTIES", "SERVER_MANAGED")));
        }
    }


    @Test
    public void testGetWithBasicContainer() throws Exception {
        final FedoraResource resource = setResource(Container.class);
        when(resource.hasType(LDP_BASIC_CONTAINER)).thenReturn(true);
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldBeAnLDPBasicContainer();
        assertShouldHaveConstraintsLink();
    }

    @Test
    public void testGetWithDirectContainer() throws Exception {
        final FedoraResource resource = setResource(Container.class);
        when(resource.hasType(LDP_DIRECT_CONTAINER)).thenReturn(true);
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldBeAnLDPDirectContainer();
        assertShouldHaveConstraintsLink();
    }

    @Test
    public void testGetWithIndirectContainer() throws Exception {
        final FedoraResource resource = setResource(Container.class);
        when(resource.hasType(LDP_INDIRECT_CONTAINER)).thenReturn(true);
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldBeAnLDPIndirectContainer();
        assertShouldHaveConstraintsLink();
    }

    @Test
    public void testGetWithObjectPreferMinimal() throws Exception {

        setResource(Container.class);
        setField(testObj, "prefer", new MultiPrefer("return=minimal"));
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldHaveConstraintsLink();

        try (final RdfNamespacedStream entity = (RdfNamespacedStream) actual.getEntity()) {
            final Model model = entity.stream.collect(toModel());
            final List<String> rdfNodes = model.listObjects().mapWith(RDFNode::toString).toList();
            assertTrue("Expected RDF contexts missing", rdfNodes.stream()
                    .filter(x -> x.contains("PROPERTIES") && x.contains("MINIMAL")).findFirst().isPresent());
            assertFalse("Included non-minimal contexts", rdfNodes.contains("LDP_MEMBERSHIP"));
            assertFalse("Included non-minimal contexts", rdfNodes.contains("LDP_CONTAINMENT"));
        }

    }

    /**
     * Emulates an 'If-None-Match' precondition failing for a GET request.  There should not be any entity body set
     * on the response.
     *
     * @throws Exception if something exceptional happens
     */
    @Test
    public void testGetWhenIfNoneMatchPreconditionFails() throws Exception {
        setResource(FedoraResource.class);

        // Set up the expectations for the ResponseBuilder
        final Response.ResponseBuilder builder = mock(Response.ResponseBuilder.class);
        final Response response = mock(Response.class);

        when(builder.entity(any())).thenReturn(builder);
        when(builder.cacheControl(any())).thenReturn(builder);
        when(builder.lastModified((any()))).thenReturn(builder);
        when(builder.tag(any(EntityTag.class))).thenReturn(builder);
        when(builder.build()).thenReturn(response);
        when(response.getEntity()).thenReturn("precondition failed message");
        when(response.getStatus()).thenReturn(NOT_MODIFIED.getStatusCode());

        // Set up expectations for the Request; returning a ResponseBuilder from evaluatePreconditions(...) indicates
        // that satisfying the precondition fails, which is what we want to simulate in this test
        when(mockRequest.evaluatePreconditions(any(EntityTag.class))).thenReturn(builder);

        // Execute the method under test.  Preconditions should fail, resulting in an exception being thrown.
        try {
            testObj.evaluateRequestPreconditions(mockRequest, mockResponse, testObj.resource(),
                    mockSession, true);
            fail("Expected " + PreconditionException.class.getName() + " to be thrown.");
        } catch (final PreconditionException e) {
            // expected
        }

        // an entity body should _not_ be set under these conditions
        verify(mockRequest).evaluatePreconditions(any(EntityTag.class));
        verify(builder, times(0)).entity(any());
    }

    /**
     * Emulates an 'If-Modified-Since' precondition failing for a GET request.  There should not be any entity body set
     * on the response.
     *
     * @throws Exception if something exceptional happens
     */
    @Test
    public void testGetWhenIfModifiedSincePreconditionFails() throws Exception {
        setResource(FedoraResource.class);

        // Set up the expectations for the ResponseBuilder
        final Response.ResponseBuilder builder = mock(Response.ResponseBuilder.class);
        final Response response = mock(Response.class);

        when(builder.entity(any())).thenReturn(builder);
        when(builder.cacheControl(any())).thenReturn(builder);
        when(builder.lastModified((any()))).thenReturn(builder);
        when(builder.tag(any(EntityTag.class))).thenReturn(builder);
        when(builder.build()).thenReturn(response);
        when(response.getStatus()).thenReturn(NOT_MODIFIED.getStatusCode());

        when(response.getEntity()).thenReturn("precondition failed message");
        // Set up expectations for the Request; returning a ResponseBuilder from evaluatePreconditions(...) indicates
        // that satisfying the precondition fails, which is what we want to simulate in this test
        when(mockRequest.evaluatePreconditions(any(Date.class))).thenReturn(builder);

        // Execute the method under test.  Preconditions should fail, resulting in an exception being thrown.
        try {
            testObj.evaluateRequestPreconditions(mockRequest, mockResponse, testObj.resource(),
                    mockSession, true);
            fail("Expected " + PreconditionException.class.getName() + " to be thrown.");
        } catch (final PreconditionException e) {
            // expected
        }

        // an entity body should _not_ be set under these conditions
        verify(mockRequest).evaluatePreconditions(any(Date.class));
        verify(builder, times(0)).entity(any());
    }

    @Test
    public void testGetWithObjectOmitContainment() throws Exception {
        setResource(Container.class);
        setField(testObj, "prefer",
                new MultiPrefer("return=representation; omit=\"" + LDP_NAMESPACE + "PreferContainment\""));
        final Response actual = testObj.getResource( null);
        assertEquals(OK.getStatusCode(), actual.getStatus());

        try (final RdfNamespacedStream entity = (RdfNamespacedStream) actual.getEntity()) {
            final Model model = entity.stream.collect(toModel());
            final List<String> rdfNodes = model.listObjects().mapWith(RDFNode::toString).toList();
            assertTrue("Should include membership contexts", rdfNodes.contains("LDP_MEMBERSHIP"));
            assertFalse("Should not include containment contexts", rdfNodes.contains("LDP_CONTAINMENT"));
        }
    }

    @Test
    public void testGetWithObjectOmitMembership() throws Exception {
        setResource(Container.class);
        setField(testObj, "prefer",
                new MultiPrefer("return=representation; omit=\"" + LDP_NAMESPACE + "PreferMembership\""));
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());

        try (final RdfNamespacedStream entity = (RdfNamespacedStream) actual.getEntity()) {
            final Model model = entity.stream.collect(toModel());
            final List<String> rdfNodes = model.listObjects().mapWith(RDFNode::toString).toList();
            assertFalse("Should not include membership contexts", rdfNodes.contains("LDP_MEMBERSHIP"));
            assertTrue("Should include containment contexts", rdfNodes.contains("LDP_CONTAINMENT"));
        }
    }

    @Test
    public void testGetWithObjectIncludeReferences()
            throws ParseException, IOException, UnsupportedAlgorithmException, UnsupportedAccessTypeException {
        setResource(Container.class);
        setField(testObj, "prefer", new MultiPrefer("return=representation; include=\"" + INBOUND_REFERENCES + "\""));
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());

        try (final RdfNamespacedStream entity = (RdfNamespacedStream) actual.getEntity()) {
            final Model model = entity.stream.collect(toModel());

            final List<String> rdfNodes = model.listObjects().mapWith(RDFNode::toString).toList();
            log.debug("Received RDF nodes: {}", rdfNodes);
            assertTrue("Should include references contexts",
                    rdfNodes.stream().anyMatch(containsPattern("REFERENCES")::apply));
        }
    }

    @Test
    public void testGetWithBinary() throws Exception {
        final FedoraBinary mockResource = (FedoraBinary)setResource(FedoraBinary.class);
        when(mockResource.getDescription()).thenReturn(mockNonRdfSourceDescription);
        when(mockResource.getMimeType()).thenReturn("text/plain");
        when(mockResource.getContent()).thenReturn(toInputStream("xyz", UTF_8));
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldBeAnLDPNonRDFSource();
        assertShouldNotAdvertiseAcceptPatchFlavors();
        assertShouldContainLinkToBinaryDescription();
        assertTrue(IOUtils.toString((InputStream)actual.getEntity(), UTF_8).equals("xyz"));
    }

    private void assertShouldBeAnLDPNonRDFSource() {
        assertTrue("Should be an LDP NonRDFSource",
                mockResponse.getHeaders(LINK).contains("<" + LDP_NAMESPACE + "NonRDFSource>;rel=\"type\""));
        assertShouldNotAdvertiseAcceptPostFlavors();
    }

    @Test
    public void testGetWithExternalMessageBinary() throws Exception {
        final FedoraBinary mockResource = (FedoraBinary)setResource(FedoraBinary.class);
        when(mockResource.getDescription()).thenReturn(mockNonRdfSourceDescription);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockResource.getMimeType()).thenReturn("message/external-body; access-type=URL; URL=\"some:uri\"");
        when(mockResource.getContent()).thenReturn(toInputStream("xyz", UTF_8));
        final Response actual = testObj.getResource(null);
        assertEquals(TEMPORARY_REDIRECT.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP NonRDFSource", mockResponse.getHeaders(LINK).contains("<" + LDP_NAMESPACE +
                "NonRDFSource>;rel=\"type\""));
        assertShouldContainLinkToBinaryDescription();
        assertEquals(new URI("some:uri"), actual.getLocation());
    }

    @Test(expected = UnsupportedAccessTypeException.class)
    public void testGetWithExternalMessageMissingURLBinary() throws Exception {
        final FedoraBinary mockResource = (FedoraBinary)setResource(FedoraBinary.class);
        when(mockResource.getDescription()).thenReturn(mockNonRdfSourceDescription);
        when(mockResource.getMimeType()).thenReturn("message/external-body; access-type=URL;");
        when(mockResource.getContent()).thenReturn(toInputStream("xyz", UTF_8));
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        final Response actual = testObj.getResource(null);
        assertEquals(UNSUPPORTED_MEDIA_TYPE.getStatusCode(), actual.getStatus());
    }

    @Test
    @SuppressWarnings({"resource", "unchecked"})
    public void testGetWithBinaryDescription()
            throws IOException, UnsupportedAlgorithmException, UnsupportedAccessTypeException {

        final NonRdfSourceDescription mockResource
                = (NonRdfSourceDescription)setResource(NonRdfSourceDescription.class);
        when(mockResource.getDescribedResource()).thenReturn(mockBinary);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockBinary.getTriples(eq(idTranslator), any(TripleCategory.class)))
            .thenReturn(new DefaultRdfStream(createURI("mockBinary")));
        when(mockBinary.getTriples(eq(idTranslator), any(EnumSet.class)))
            .thenReturn(new DefaultRdfStream(createURI("mockBinary"), of(new Triple
                (createURI("mockBinary"), createURI("called"), createURI("child:properties")))));
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP RDFSource",
                mockResponse.getHeaders(LINK).contains("<" + LDP_NAMESPACE + "RDFSource>;rel=\"type\""));
        assertShouldNotAdvertiseAcceptPostFlavors();
        assertShouldAdvertiseAcceptPatchFlavors();
        assertShouldContainLinkToTheBinary();
        assertShouldAllowOnlyResourceDescriptionMethods();

        final Model model = ((RdfNamespacedStream) actual.getEntity()).stream.collect(toModel());
        final List<String> rdfNodes = model.listObjects().mapWith(RDFNode::toString).toList();
        log.info("Found RDF objects\n{}", rdfNodes);
        assertTrue("Expected RDF contexts missing", rdfNodes.containsAll(ImmutableSet.of(
                "LDP_CONTAINMENT", "LDP_MEMBERSHIP", "PROPERTIES", "SERVER_MANAGED")));

    }

    private void assertShouldAllowOnlyResourceDescriptionMethods() {
        final String[] allows = mockResponse.getHeader(HttpHeaders.ALLOW).split(",");

        final Set<String> allowedMethods = new HashSet<>(Arrays.asList(allows));
        final Set<String> validMethods = ImmutableSet.of("GET", "HEAD", "DELETE", "PUT", "PATCH",
                "OPTIONS");

        allowedMethods.removeAll(validMethods);

        assertEquals("Allow header contains invalid methods: " + allowedMethods, 0, allowedMethods.size());
    }

    @Test
    public void testDelete() throws Exception {
        final FedoraResource fedoraResource = setResource(FedoraResource.class);
        final Response actual = testObj.deleteObject();
        assertEquals(NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(fedoraResource).delete();
    }

    @Test
    public void testPutNewObject() throws Exception {
        setField(testObj, "externalPath", "some/path");
        final FedoraBinary mockObject = (FedoraBinary)setResource(FedoraBinary.class);
        doReturn(mockObject).when(testObj).resource();
        when(mockContainer.isNew()).thenReturn(true);

        when(mockNodeService.exists(mockFedoraSession, "/some/path")).thenReturn(false);
        when(mockContainerService.findOrCreate(mockFedoraSession, "/some/path")).thenReturn(mockContainer);

        final Response actual = testObj.createOrReplaceObjectRdf(null, null, null, null, null, null);

        assertEquals(CREATED.getStatusCode(), actual.getStatus());
    }

    @Test(expected = CannotCreateResourceException.class)
    public void testPutNewObjectLdpr() throws Exception {
        testObj.createOrReplaceObjectRdf(null, null, null, null,
                asList("<http://www.w3.org/ns/ldp#Resource>; rel=\"type\""), null);
    }

    @Test
    public void testPutNewObjectWithRdf() throws Exception {

        setField(testObj, "externalPath", "some/path");
        final FedoraBinary mockObject = (FedoraBinary)setResource(FedoraBinary.class);
        doReturn(mockObject).when(testObj).resource();
        when(mockContainer.isNew()).thenReturn(true);

        when(mockNodeService.exists(mockFedoraSession, "/some/path")).thenReturn(false);
        when(mockContainerService.findOrCreate(mockFedoraSession, "/some/path")).thenReturn(mockContainer);

        final Response actual = testObj.createOrReplaceObjectRdf(NTRIPLES_TYPE,
                toInputStream("_:a <info:x> _:c .", UTF_8), null, null, null, null);

        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockContainer).replaceProperties(eq(idTranslator), any(Model.class), any(RdfStream.class));
    }

    @Test
    public void testPutNewBinary() throws Exception {
        setField(testObj, "externalPath", "some/path");
        final FedoraBinary mockObject = (FedoraBinary)setResource(FedoraBinary.class);
        doReturn(mockObject).when(testObj).resource();
        when(mockBinary.isNew()).thenReturn(true);

        when(mockNodeService.exists(mockFedoraSession, "/some/path")).thenReturn(false);
        when(mockBinaryService.findOrCreate(mockFedoraSession, "/some/path")).thenReturn(mockBinary);

        final Response actual = testObj.createOrReplaceObjectRdf(TEXT_PLAIN_TYPE,
                toInputStream("xyz", UTF_8), null, null, nonRDFSourceLink, null);

        assertEquals(CREATED.getStatusCode(), actual.getStatus());
    }

    @Test
    public void testPutReplaceRdfObject() throws Exception {

        setField(testObj, "externalPath", "some/path");
        final Container mockObject = (Container)setResource(Container.class);
        doReturn(mockObject).when(testObj).resource();
        when(mockObject.isNew()).thenReturn(false);

        when(mockNodeService.exists(mockFedoraSession, "/some/path")).thenReturn(true);
        when(mockContainerService.findOrCreate(mockFedoraSession, "/some/path")).thenReturn(mockObject);

        final Response actual = testObj.createOrReplaceObjectRdf(NTRIPLES_TYPE,
                toInputStream("_:a <info:x> _:c .", UTF_8), null, null, null, null);

        assertEquals(NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(mockObject).replaceProperties(eq(idTranslator), any(Model.class), any(RdfStream.class));
    }

    @Test(expected = ClientErrorException.class)
    public void testPutWithStrictIfMatchHandling() throws Exception {

        when(mockHttpConfiguration.putRequiresIfMatch()).thenReturn(true);
        final Container mockObject = (Container)setResource(Container.class);
        doReturn(mockObject).when(testObj).resource();
        when(mockObject.isNew()).thenReturn(false);

        when(mockNodeService.exists(mockFedoraSession, "/some/path")).thenReturn(true);
        when(mockContainerService.findOrCreate(mockFedoraSession, "/some/path")).thenReturn(mockObject);

        testObj.createOrReplaceObjectRdf(NTRIPLES_TYPE,
                toInputStream("_:a <info:x> _:c .", UTF_8), null, null, null, null);

    }

    @Test
    public void testPatchObject() throws Exception {

        setResource(Container.class);

        testObj.updateSparql(toInputStream("xyz", UTF_8));
    }


    @Test
    @SuppressWarnings({"resource", "unchecked"})
    public void testPatchBinaryDescription() throws MalformedRdfException, IOException {

        final NonRdfSourceDescription mockObject = (NonRdfSourceDescription)setResource(NonRdfSourceDescription.class);
        when(mockObject.getDescribedResource()).thenReturn(mockBinary);

        when(mockBinary.getTriples(eq(idTranslator), any(TripleCategory.class)))
            .thenReturn(new DefaultRdfStream(createURI("mockBinary")));
        when(mockBinary.getTriples(eq(idTranslator), any(EnumSet.class)))
            .thenReturn(new DefaultRdfStream(createURI("mockBinary"),
                        of(new Triple(createURI("mockBinary"), createURI("called"),
                            createURI("child:properties")))));

        doReturn(mockObject).when(testObj).resource();

        testObj.updateSparql(toInputStream("xyz", UTF_8));
    }

    @Test(expected = BadRequestException.class)
    public void testPatchWithoutContent() throws MalformedRdfException, IOException {
        testObj.updateSparql(null);
    }

    @Test(expected = BadRequestException.class)
    public void testPatchWithMissingContent() throws MalformedRdfException, IOException {
        setResource(Container.class);
        testObj.updateSparql(toInputStream("", UTF_8));
    }

    @Test(expected = BadRequestException.class)
    public void testPatchBinary() throws MalformedRdfException, IOException {
        setResource(FedoraBinary.class);
        testObj.updateSparql(toInputStream("", UTF_8));
    }

    @Test
    public void testCreateNewObject() throws MalformedRdfException, InvalidChecksumException,
           IOException, UnsupportedAlgorithmException, UnsupportedAccessTypeException {
        setResource(Container.class);
        when(mockContainerService.findOrCreate(mockFedoraSession, "/b")).thenReturn(mockContainer);
        final Response actual = testObj.createObject(null, null, "b", null, null, null);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
    }

    @Test
    public void testCreateNewObjectWithVersionedResource() throws MalformedRdfException, InvalidChecksumException,
           IOException, UnsupportedAlgorithmException, UnsupportedAccessTypeException {
        setResource(Container.class);
        when(mockContainerService.findOrCreate(mockFedoraSession, "/b")).thenReturn(mockContainer);
        final String versionedResourceLink = "<" + VERSIONED_RESOURCE.getURI() + ">;rel=\"type\"";
        final Response actual = testObj.createObject(null, null, "b", null, Arrays.asList(versionedResourceLink), null);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
    }

    @Test
    public void testCreateNewObjectWithSparql() throws MalformedRdfException,
           InvalidChecksumException, UnsupportedAlgorithmException, IOException, UnsupportedAccessTypeException {
        setResource(Container.class);
        when(mockContainerService.findOrCreate(mockFedoraSession, "/b")).thenReturn(mockContainer);
        final Response actual = testObj.createObject(null,
                MediaType.valueOf(contentTypeSPARQLUpdate), "b", toInputStream("x", UTF_8), null, null);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockContainer).updateProperties(eq(idTranslator), eq("x"), any(RdfStream.class));
    }

    @Test
    public void testCreateNewObjectWithRdf() throws MalformedRdfException,
           InvalidChecksumException, IOException, UnsupportedAlgorithmException, UnsupportedAccessTypeException {
        setResource(Container.class);
        when(mockContainerService.findOrCreate(mockFedoraSession, "/b")).thenReturn(mockContainer);
        final Response actual = testObj.createObject(null, NTRIPLES_TYPE, "b",
                toInputStream("_:a <info:b> _:c .", UTF_8), null, null);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockContainer).replaceProperties(eq(idTranslator), any(Model.class), any(RdfStream.class));
    }


    @Test
    public void testCreateNewBinary() throws MalformedRdfException, InvalidChecksumException,
           IOException, UnsupportedAlgorithmException, UnsupportedAccessTypeException {
        setResource(Container.class);
        when(mockBinaryService.findOrCreate(mockFedoraSession, "/b")).thenReturn(mockBinary);
        try (final InputStream content = toInputStream("x", UTF_8)) {
            final Response actual = testObj.createObject(null, APPLICATION_OCTET_STREAM_TYPE, "b", content,
                nonRDFSourceLink, null);
            assertEquals(CREATED.getStatusCode(), actual.getStatus());
            verify(mockBinary).setContent(content, APPLICATION_OCTET_STREAM, Collections.emptySet(), "", null);
        }
    }

    @Test(expected = InsufficientStorageException.class)
    public void testCreateNewBinaryWithInsufficientResources() throws MalformedRdfException,
           InvalidChecksumException, IOException, UnsupportedAlgorithmException, UnsupportedAccessTypeException {
        setResource(Container.class);
        when(mockBinaryService.findOrCreate(mockFedoraSession, "/b")).thenReturn(mockBinary);


        try (final InputStream content = toInputStream("x", UTF_8)) {

            final RuntimeException ex = new RuntimeException(new IOException("root exception", new IOException(
                    FedoraLdp.INSUFFICIENT_SPACE_IDENTIFYING_MESSAGE)));
            doThrow(ex).when(mockBinary).setContent(content, APPLICATION_OCTET_STREAM_TYPE.toString(),
                    Collections
                    .emptySet(),
                    "",
                    null);

            testObj.createObject(null, APPLICATION_OCTET_STREAM_TYPE, "b", content, nonRDFSourceLink, null);
        }
    }

    @Test
    public void testCreateNewBinaryWithContentTypeWithParams() throws MalformedRdfException,
           InvalidChecksumException, IOException, UnsupportedAlgorithmException, UnsupportedAccessTypeException {

        setResource(Container.class);
        when(mockBinaryService.findOrCreate(mockFedoraSession, "/b")).thenReturn(mockBinary);
        try (final InputStream content = toInputStream("x", UTF_8)) {
            final MediaType requestContentType = MediaType.valueOf("some/mime-type; with=some; param=s");
            final Response actual = testObj.createObject(null, requestContentType, "b", content, nonRDFSourceLink,
                null);
            assertEquals(CREATED.getStatusCode(), actual.getStatus());
            verify(mockBinary).setContent(content, requestContentType.toString(), Collections.emptySet(), "", null);
        }
    }

    @Test
    public void testCreateNewBinaryWithChecksumSHA() throws MalformedRdfException,
           InvalidChecksumException, IOException, UnsupportedAlgorithmException, UnsupportedAccessTypeException {

        setResource(Container.class);
        when(mockBinaryService.findOrCreate(mockFedoraSession, "/b")).thenReturn(mockBinary);
        try (final InputStream content = toInputStream("x", UTF_8)) {
            final MediaType requestContentType = MediaType.valueOf("some/mime-type; with=some; param=s");
            final String sha = "07a4d371f3b7b6283a8e1230b7ec6764f8287bf2";
            final String requestSHA = "sha=" + sha;
            final Set<URI> shaURI = singleton(URI.create("urn:sha1:" + sha));
            final Response actual = testObj.createObject(null, requestContentType, "b", content, nonRDFSourceLink,
                requestSHA);
            assertEquals(CREATED.getStatusCode(), actual.getStatus());
            verify(mockBinary).setContent(content, requestContentType.toString(), shaURI, "", null);
        }
    }

    @Test
    public void testCreateNewBinaryWithChecksumSHA256() throws MalformedRdfException,
        InvalidChecksumException, IOException, UnsupportedAlgorithmException, UnsupportedAccessTypeException {

        setResource(Container.class);
        when(mockBinaryService.findOrCreate(mockFedoraSession, "/b")).thenReturn(mockBinary);
        try (final InputStream content = toInputStream("x", UTF_8)) {
            final MediaType requestContentType = MediaType.valueOf("some/mime-type; with=some; param=s");
            final String sha = "73cb3858a687a8494ca3323053016282f3dad39d42cf62ca4e79dda2aac7d9ac";
            final String requestSHA = "sha256=" + sha;
            final Set<URI> shaURI = singleton(URI.create("urn:sha256:" + sha));
            final Response actual = testObj.createObject(null, requestContentType, "b", content, nonRDFSourceLink,
                requestSHA);
            assertEquals(CREATED.getStatusCode(), actual.getStatus());
            verify(mockBinary).setContent(content, requestContentType.toString(), shaURI, "", null);
        }
    }

    @Test
    public void testCreateNewBinaryWithChecksumMD5() throws MalformedRdfException,
            InvalidChecksumException, IOException, UnsupportedAlgorithmException, UnsupportedAccessTypeException {

        setResource(Container.class);
        when(mockBinaryService.findOrCreate(mockFedoraSession, "/b")).thenReturn(mockBinary);
        try (final InputStream content = toInputStream("x", UTF_8)) {
            final MediaType requestContentType = MediaType.valueOf("some/mime-type; with=some; param=s");
            final String md5 = "HUXZLQLMuI/KZ5KDcJPcOA==";
            final String requestMD5 = "md5=" + md5;
            final Set<URI> md5URI = singleton(URI.create("urn:md5:" + md5));
            final Response actual = testObj.createObject(null, requestContentType, "b", content, nonRDFSourceLink,
                requestMD5);
            assertEquals(CREATED.getStatusCode(), actual.getStatus());
            verify(mockBinary).setContent(content, requestContentType.toString(), md5URI, "", null);
        }
    }

    @Test
    public void testCreateNewBinaryWithChecksumSHAandMD5() throws MalformedRdfException,
           InvalidChecksumException, IOException, UnsupportedAlgorithmException, UnsupportedAccessTypeException {

        setResource(Container.class);
        when(mockBinaryService.findOrCreate(mockFedoraSession, "/b")).thenReturn(mockBinary);
        try (final InputStream content = toInputStream("x", UTF_8)) {
            final MediaType requestContentType = MediaType.valueOf("some/mime-type; with=some; param=s");

            final String sha = "07a4d371f3b7b6283a8e1230b7ec6764f8287bf2";
            final String requestSHA = "sha=" + sha;
            final URI shaURI = URI.create("urn:sha1:" + sha);

            final String md5 = "HUXZLQLMuI/KZ5KDcJPcOA==";
            final String requestMD5 = "md5=" + md5;
            final URI md5URI = URI.create("urn:md5:" + md5);

            final String requestChecksum = requestSHA + "," + requestMD5;
            final HashSet<URI> checksumURIs = new HashSet<>(asList(shaURI, md5URI));

            final Response actual = testObj.createObject(null, requestContentType, "b", content, nonRDFSourceLink,
                requestChecksum);
            assertEquals(CREATED.getStatusCode(), actual.getStatus());
            verify(mockBinary).setContent(content, requestContentType.toString(), checksumURIs, "", null);
        }
    }

    @Test(expected = ClientErrorException.class)
    public void testPostToBinary() throws MalformedRdfException, InvalidChecksumException, IOException,
            UnsupportedAlgorithmException, UnsupportedAccessTypeException {
        final FedoraBinary mockObject = (FedoraBinary)setResource(FedoraBinary.class);
        doReturn(mockObject).when(testObj).resource();
        testObj.createObject(null, null, null, null, null, null);
    }

    @Test(expected = CannotCreateResourceException.class)
    public void testLDPRNotImplemented() throws MalformedRdfException, InvalidChecksumException,
            IOException, UnsupportedAlgorithmException, UnsupportedAccessTypeException {
        setResource(Container.class);
        when(mockContainerService.findOrCreate(mockFedoraSession, "/x")).thenReturn(mockContainer);
        testObj.createObject(null, null, "x", null, asList("<http://www.w3.org/ns/ldp#Resource>; rel=\"type\""), null);
    }

    @Test(expected = ClientErrorException.class)
    public void testLDPRNotImplementedInvalidLink() throws MalformedRdfException, InvalidChecksumException,
            IOException, UnsupportedAlgorithmException, UnsupportedAccessTypeException {
        setResource(Container.class);
        when(mockContainerService.findOrCreate(mockFedoraSession, "/x")).thenReturn(mockContainer);
        testObj.createObject(null, null, "x", null, asList("<http://foo;rel=\"type\""), null);
    }

    @Test
    public void testGetSimpleContentType() {
        final MediaType mediaType = new MediaType("text", "plain", ImmutableMap.of("charset", "UTF-8"));
        final MediaType sanitizedMediaType = getSimpleContentType(mediaType);
        assertEquals("text/plain", sanitizedMediaType.toString());
    }

    /**
     * Demonstrates that when the {@link FedoraBaseResource#JMS_BASEURL_PROP fcrepo.jms.baseUrl} system property is not
     * set, the url used for JMS messages is the same as the base url found in the {@code UriInfo}.
     * <p>
     * Implementation note: this test requires a concrete instance of {@link UriInfo}, because it is the interaction of
     * {@code javax.ws.rs.core.UriBuilder} and {@code FedoraBaseResource} that is being tested.
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

        testObj.setUpJMSInfo(info, mockHeaders);

        verify(mockFedoraSession).addSessionData(BASE_URL, expectedBaseUrl);
        verify(info, times(0)).getBaseUriBuilder();
        verify(info).getBaseUri();
    }

    /**
     * Demonstrates that the host supplied by the {@link FedoraBaseResource#JMS_BASEURL_PROP fcrepo.jms.baseUrl} system
     * property is used as the as the base url for JMS messages, and not the base url found in {@code UriInfo}.
     * <p>
     * Note: the path from the request is preserved, the host from the fcrepo.jms.baseUrl is used
     * </p>
     * <p>
     * Implementation note: this test requires a concrete instance of {@link UriInfo}, because it is the interaction of
     * {@code javax.ws.rs.core.UriBuilder} and {@code FedoraBaseResource} that is being tested.
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

        testObj.setUpJMSInfo(info, mockHeaders);

        verify(mockFedoraSession).addSessionData(BASE_URL, expectedBaseUrl);
        verify(info).getBaseUriBuilder();
        System.clearProperty(JMS_BASEURL_PROP);
    }

    /**
     * Demonstrates that the host and port supplied by the {@link FedoraBaseResource#JMS_BASEURL_PROP
     * fcrepo.jms.baseUrl} system property is used as the as the base url for JMS messages, and not the base url found
     * in {@code UriInfo}.
     * <p>
     * Note: the path from the request is preserved, but the host and port from the request is overridden by the values
     * from fcrepo.jms.baseUrl
     * </p>
     * <p>
     * Implementation note: this test requires a concrete instance of {@link UriInfo}, because it is the interaction of
     * {@code javax.ws.rs.core.UriBuilder} and {@code FedoraBaseResource} that is being tested.
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

        testObj.setUpJMSInfo(info, mockHeaders);

        verify(mockFedoraSession).addSessionData(BASE_URL, expectedBaseUrl);
        verify(info).getBaseUriBuilder();
        System.clearProperty(JMS_BASEURL_PROP);
    }

    /**
     * Demonstrates that the url supplied by the {@link FedoraBaseResource#JMS_BASEURL_PROP fcrepo.jms.baseUrl} system
     * property is used as the as the base url for JMS messages, and not the base url found in {@code UriInfo}.
     * <p>
     * Note: the host and path from the request is overridden by the values from fcrepo.jms.baseUrl
     * </p>
     * <p>
     * Implementation note: this test requires a concrete instance of {@link UriInfo}, because it is the interaction of
     * {@code javax.ws.rs.core.UriBuilder} and {@code FedoraBaseResource} that is being tested.
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

        testObj.setUpJMSInfo(info, mockHeaders);

        verify(mockFedoraSession).addSessionData(BASE_URL, expectedBaseUrl);
        verify(info).getBaseUriBuilder();
        System.clearProperty(JMS_BASEURL_PROP);
    }

    /**
     * Demonstrates that when the the base url in {@code UriInfo} contains a port number, and the base url defined by
     * {@link FedoraBaseResource#JMS_BASEURL_PROP fcrepo.jms.baseUrl} does <em>not</em> contain a port number, that the
     * base url for JMS messages does not contain a port number.
     * <p>
     * Note: the host, port, and path from the request is overridden by values from fcrepo.jms.baseUrl
     * </p>
     * <p>
     * Implementation note: this test requires a concrete instance of {@link UriInfo}, because it is the interaction of
     * {@code javax.ws.rs.core.UriBuilder} and {@code FedoraBaseResource} that is being tested.
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

        testObj.setUpJMSInfo(info, mockHeaders);

        verify(mockFedoraSession).addSessionData(BASE_URL, expectedBaseUrl);
        verify(info).getBaseUriBuilder();
        System.clearProperty(JMS_BASEURL_PROP);
    }

}
