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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;

import org.fcrepo.http.api.services.EtagService;
import org.fcrepo.http.api.services.HttpRdfService;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.http.commons.domain.MultiPrefer;
import org.fcrepo.http.commons.domain.PreferTag;
import org.fcrepo.http.commons.responses.RdfNamespacedStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.CannotCreateResourceException;
import org.fcrepo.kernel.api.exception.ExternalMessageBodyException;
import org.fcrepo.kernel.api.exception.InsufficientStorageException;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PreconditionException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.exception.UnsupportedMediaTypeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.ResourceHelper;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.rdf.LdpTriplePreferences;
import org.fcrepo.kernel.api.rdf.RdfNamespaceRegistry;
import org.fcrepo.kernel.api.services.CreateResourceService;
import org.fcrepo.kernel.api.services.DeleteResourceService;
import org.fcrepo.kernel.api.services.ReplacePropertiesService;
import org.fcrepo.kernel.api.services.ResourceTripleService;
import org.fcrepo.kernel.api.services.TimeMapService;
import org.fcrepo.kernel.api.services.UpdatePropertiesService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.base.Predicates.containsPattern;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
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
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.http.api.ContentExposingResource.buildLink;
import static org.fcrepo.http.api.ContentExposingResource.getSimpleContentType;
import static org.fcrepo.http.api.FedoraLdp.HTTP_HEADER_ACCEPT_PATCH;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES_TYPE;
import static org.fcrepo.http.commons.test.util.TestHelpers.getServletContextImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.EXTERNAL_CONTENT;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI;
import static org.fcrepo.kernel.api.RdfLexicon.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_CONTAINMENT;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MEMBERSHIP;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONED_RESOURCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
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

/**
 * @author cabeer
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class FedoraLdpTest {

    private final String path = "/some/path";
    private final String binaryPath = "/some/binary/path";
    private final String binaryDescriptionPath = binaryPath + "/" + FCR_METADATA;

    private final FedoraId pathId = FedoraId.create(path);
    private final FedoraId binaryPathId = FedoraId.create(binaryPath);
    private final FedoraId binaryDescId = binaryPathId.resolve(FCR_METADATA);

    private FedoraLdp testObj;

    private final List<String> nonRDFSourceLink = singletonList(
            Link.fromUri(NON_RDF_SOURCE.toString()).rel("type").build().toString());

    private final HttpIdentifierConverter identifierConverter = new HttpIdentifierConverter(
            getUriInfoImpl().getBaseUriBuilder().clone().path("/{path: .*}"));

    private final InputStream emptyStream = IOUtils.toInputStream("", Charsets.UTF_8);

    @Mock
    private Request mockRequest;

    private HttpServletResponse mockResponse;

    @Mock
    private Transaction mockTransaction;

    @Mock
    private Container mockContainer;

    @Mock
    private NonRdfSourceDescription mockNonRdfSourceDescription;

    @Mock
    private Binary mockBinary;

    @Mock
    private ResourceFactory resourceFactory;

    @Mock
    private ResourceHelper resourceHelper;

    @Mock
    private TimeMapService mockTimeMapService;

    @Mock
    private FedoraHttpConfiguration mockHttpConfiguration;

    @Mock
    private RdfNamespaceRegistry rdfNamespaceRegistry;

    @Mock
    private HttpHeaders mockHeaders;

    @Mock
    private SecurityContext mockSecurityContext;

    @Mock
    private ServletContext mockServletContext;

    @Mock
    private MultiPrefer prefer;

    @Mock
    private PreferTag preferTag;

    @Mock
    private ExternalContentHandlerFactory extContentHandlerFactory;

    @Mock
    private ReplacePropertiesService replacePropertiesService;

    @Mock
    private UpdatePropertiesService updatePropertiesService;

    @Mock
    private DeleteResourceService deleteResourceService;

    @Mock
    private CreateResourceService createResourceService;

    @Mock
    private Principal principal;

    @Mock
    private ResourceTripleService resourceTripleService;

    @Mock
    private EtagService etagService;

    private final List<URI> typeList = new ArrayList<>();

    private static final Logger log = getLogger(FedoraLdpTest.class);

    @Before
    public void setUp() {
        testObj = spy(new FedoraLdp(path));

        mockResponse = new MockHttpServletResponse();

        final HttpRdfService httpRdfService = new HttpRdfService();

        setField(testObj, "request", mockRequest);
        setField(testObj, "servletResponse", mockResponse);
        setField(testObj, "uriInfo", getUriInfoImpl());
        setField(testObj, "headers", mockHeaders);
        setField(testObj, "resourceFactory", resourceFactory);
        setField(testObj, "timeMapService", mockTimeMapService);
        setField(testObj, "httpConfiguration", mockHttpConfiguration);
        setField(testObj, "transaction", mockTransaction);
        setField(testObj, "securityContext", mockSecurityContext);
        setField(testObj, "prefer", prefer);
        setField(testObj, "context", getServletContextImpl());
        setField(testObj, "extContentHandlerFactory", extContentHandlerFactory);
        setField(testObj, "namespaceRegistry", rdfNamespaceRegistry);
        setField(testObj, "deleteResourceService", deleteResourceService);
        setField(testObj, "resourceTripleService", resourceTripleService);
        setField(testObj, "httpRdfService", httpRdfService);
        setField(testObj, "createResourceService", createResourceService);
        setField(testObj, "replacePropertiesService", replacePropertiesService);
        setField(testObj, "updatePropertiesService", updatePropertiesService);
        setField(testObj, "resourceHelper", resourceHelper);
        setField(testObj, "etagService", etagService);

        when(rdfNamespaceRegistry.getNamespaces()).thenReturn(new HashMap<>());

        when(mockHttpConfiguration.putRequiresIfMatch()).thenReturn(false);

        when(principal.getName()).thenReturn("testUser");
        when(mockSecurityContext.getUserPrincipal()).thenReturn(principal);

        when(mockContainer.getEtagValue()).thenReturn("");
        when(mockContainer.getStateToken()).thenReturn("");
        when(mockContainer.getDescription()).thenReturn(mockContainer);
        when(mockContainer.getDescribedResource()).thenReturn(mockContainer);
        when(mockContainer.getFedoraId()).thenReturn(pathId);

        when(resourceHelper.doesResourceExist(mockTransaction, pathId, true)).thenReturn(false);

        when(mockNonRdfSourceDescription.getEtagValue()).thenReturn("");
        when(mockNonRdfSourceDescription.getStateToken()).thenReturn("");
        when(mockNonRdfSourceDescription.getDescribedResource()).thenReturn(mockBinary);
        when(mockNonRdfSourceDescription.getOriginalResource()).thenReturn(mockNonRdfSourceDescription);
        when(mockNonRdfSourceDescription.getFedoraId()).thenReturn(binaryDescId);

        when(mockBinary.getEtagValue()).thenReturn("");
        when(mockBinary.getStateToken()).thenReturn("");
        when(mockBinary.getDescription()).thenReturn(mockNonRdfSourceDescription);
        when(mockBinary.getDescribedResource()).thenReturn(mockBinary);
        when(mockBinary.getOriginalResource()).thenReturn(mockBinary);
        when(mockBinary.getFedoraId()).thenReturn(binaryPathId);

        when(mockHeaders.getHeaderString("user-agent")).thenReturn("Test UserAgent");
        when(mockHeaders.getHeaderString("X-If-State-Token")).thenReturn(null);

        when(mockTransaction.getId()).thenReturn("foo1234");
        when(mockTransaction.isShortLived()).thenReturn(true);

        when(mockServletContext.getContextPath()).thenReturn("/");

        when(prefer.getReturn()).thenReturn(preferTag);

        doAnswer((Answer<HttpServletResponse>) invocation -> {
            mockResponse.addHeader("Preference-Applied", "return=representation");
            return null;
        }).when(preferTag).addResponseHeaders(mockResponse);

        when(etagService.getRdfResourceEtag(nullable(String.class), any(FedoraResource.class),
                nullable(LdpTriplePreferences.class), any())).thenReturn("etagval");
    }

    private FedoraResource setResource(final Class<? extends FedoraResource> klass) {
        final List<tripleTypes> defaultTriples = List.of(
                tripleTypes.PROPERTIES,
                tripleTypes.LDP_CONTAINMENT,
                tripleTypes.SERVER_MANAGED,
                tripleTypes.LDP_MEMBERSHIP
        );
        return setResource(klass, defaultTriples);
    }

    private FedoraResource setResource(final Class<? extends FedoraResource> klass,
                                       final List<tripleTypes> tripleTypesList) {
        final FedoraResource mockResource = mock(klass);
        typeList.add(URI.create(RESOURCE.toString()));
        if (mockResource instanceof Binary) {
            when(((Binary) mockResource).getContentSize()).thenReturn(1L);
            when(mockResource.getOriginalResource()).thenReturn(mockResource);
            when(mockResource.getDescription()).thenReturn(mockNonRdfSourceDescription);
            typeList.add(URI.create(NON_RDF_SOURCE.toString()));
        } else if (mockResource instanceof NonRdfSourceDescription) {
            when(mockResource.getOriginalResource()).thenReturn(mockBinary);
            when(mockBinary.getDescription()).thenReturn(mockResource);
            typeList.addAll(List.of(URI.create(FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI),
                    URI.create(RDF_SOURCE.toString())));
        } else if (mockResource instanceof Container) {
            typeList.addAll(List.of(URI.create(BASIC_CONTAINER.toString()), URI.create(RDF_SOURCE.toString())));
        }
        when(mockResource.getTypes()).thenReturn(typeList);

        doReturn(mockResource).when(testObj).resource();
        when(mockResource.getFedoraId()).thenReturn(FedoraId.create(path));
        when(mockResource.getEtagValue()).thenReturn("");
        when(mockResource.getStateToken()).thenReturn("");
        when(mockResource.getDescribedResource()).thenReturn(mockResource);
        setupResourceService(mockResource, tripleTypesList);
        return mockResource;
    }

    private enum tripleTypes {
        PROPERTIES, SERVER_MANAGED, LDP_CONTAINMENT, INBOUND_REFERENCES, LDP_MEMBERSHIP
    }

    private void setupResourceService(final FedoraResource mockResource, final List<tripleTypes> response) {
        final var testUri = createURI("test");
        final List<Triple> triples = new ArrayList<>();
        if (response.contains(tripleTypes.PROPERTIES)) {
            triples.add(Triple.create(testUri, createURI("called"), createURI("PROPERTIES")));
        }
        if (response.contains(tripleTypes.SERVER_MANAGED)) {
            triples.add(Triple.create(testUri, createURI("managed"), createURI("SERVER_MANAGED")));
        }
        if (response.contains(tripleTypes.LDP_CONTAINMENT)) {
            triples.add(Triple.create(testUri, createURI("contains"), createURI("LDP_CONTAINMENT")));
        }
        if (response.contains(tripleTypes.INBOUND_REFERENCES)) {
            triples.add(Triple.create(testUri, createURI("references"), createURI("INBOUND_REFERENCES")));
        }
        if (response.contains(tripleTypes.LDP_MEMBERSHIP)) {
            triples.add(Triple.create(testUri, createURI("membership"), createURI("LDP_MEMBERSHIP")));
        }
        final Answer<Stream<Triple>> answer = invocationOnMock -> triples.stream();
        when(resourceTripleService.getResourceTriples(any(Transaction.class),
                eq(mockResource), any(LdpTriplePreferences.class), anyInt())).thenAnswer(answer);
    }

    @Test
    public void testHead() throws Exception {
        setResource(FedoraResource.class);
        when(mockRequest.getMethod()).thenReturn("HEAD");
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should have a Link header", mockResponse.containsHeader(LINK));
        assertTrue("Should have an Allow header", mockResponse.containsHeader("Allow"));
        assertTrue("Should have a Preference-Applied header", mockResponse.containsHeader("Preference-Applied"));
        assertTrue("Should have a Vary header", mockResponse.containsHeader("Vary"));
        assertTrue("Should be an LDP Resource",
                mockResponse.getHeaders(LINK).contains("<" + RESOURCE + ">; rel=\"type\""));
    }

    @Test
    public void testHeadWithObject() throws Exception {
        setResource(Container.class);
        when(mockRequest.getMethod()).thenReturn("HEAD");
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertTrue("Should advertise Accept-Patch flavors", mockResponse.containsHeader(
                FedoraLdp.HTTP_HEADER_ACCEPT_PATCH));
    }

    @Test
    public void testHeadWithDefaultContainer() throws Exception {
        setResource(Container.class);
        when(mockRequest.getMethod()).thenReturn("HEAD");
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldBeAnLDPBasicContainer();
    }

    private void assertShouldBeAnLDPBasicContainer() {
        assertTrue("Should be an LDP BasicContainer",
                mockResponse.getHeaders(LINK).contains("<" + BASIC_CONTAINER.getURI() + ">; rel=\"type\""));
    }

    @Test
    public void testHeadWithBasicContainer() throws Exception {
        final FedoraResource resource = setResource(Container.class);
        when(mockRequest.getMethod()).thenReturn("HEAD");
        when(resource.hasType(BASIC_CONTAINER.toString())).thenReturn(true);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldBeAnLDPBasicContainer();
    }

    @Test
    public void testHeadWithDirectContainer() throws Exception {
        final FedoraResource resource = setResource(Container.class);
        when(mockRequest.getMethod()).thenReturn("HEAD");
        typeList.add(URI.create(DIRECT_CONTAINER.toString()));
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldBeAnLDPDirectContainer();
    }

    private void assertShouldBeAnLDPDirectContainer() {
        assertTrue("Should be an LDP DirectContainer",
                mockResponse.getHeaders(LINK).contains("<" + DIRECT_CONTAINER.getURI() + ">; rel=\"type\""));
    }

    @Test
    public void testHeadWithIndirectContainer() throws Exception {
        final FedoraResource resource = setResource(Container.class);
        when(mockRequest.getMethod()).thenReturn("HEAD");
        typeList.add(URI.create(INDIRECT_CONTAINER.toString()));
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldBeAnLDPIndirectContainer();
    }

    private void assertShouldBeAnLDPIndirectContainer() {
        assertTrue("Should be an LDP IndirectContainer",
                mockResponse.getHeaders(LINK).contains("<" + INDIRECT_CONTAINER.getURI() + ">; rel=\"type\""));
    }

    @Test
    public void testHeadWithBinary() throws Exception {
        final Binary mockResource = (Binary)setResource(Binary.class);
        when(mockRequest.getMethod()).thenReturn("HEAD");
        when(mockResource.getDescription()).thenReturn(mockNonRdfSourceDescription);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockResource.getMimeType()).thenReturn("image/jpeg");
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertContentLengthGreaterThan0(mockResponse.getHeader(CONTENT_LENGTH));
        assertShouldBeAnLDPNonRDFSource();
        assertShouldNotAdvertiseAcceptPatchFlavors();
        assertShouldContainLinkToBinaryDescription();
    }

    private void assertContentLengthGreaterThan0(final String contentLength) {
        assertTrue("Should have a content length header greater than 0", Integer.parseInt(contentLength) > 0);
    }

    private void assertShouldContainLinkToBinaryDescription() {
        final String described = identifierConverter.toDomain(binaryPath + "/fcr:metadata");
        assertTrue("Should contain a link to the binary description",
                mockResponse.getHeaders(LINK)
                        .contains("<" + described + ">; rel=\"describedby\""));
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

    @Test
    public void testHeadWithExternalBinary() throws Exception {
        final Binary mockResource = (Binary)setResource(Binary.class);
        when(mockRequest.getMethod()).thenReturn("HEAD");
        final String url = "http://example.com/some/url";
        when(mockResource.getDescription()).thenReturn(mockNonRdfSourceDescription);
        when(mockResource.getMimeType()).thenReturn("text/plain");
        when(mockResource.isProxy()).thenReturn(false);
        when(mockResource.isRedirect()).thenReturn(true);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockResource.getExternalURL()).thenReturn(url);
        when(mockResource.getExternalURI()).thenReturn(URI.create(url));
        final Response actual = testObj.head();
        assertEquals(TEMPORARY_REDIRECT.getStatusCode(), actual.getStatus());
        assertEquals(new URI(url), actual.getLocation());
        assertShouldBeAnLDPNonRDFSource();
        assertShouldNotAdvertiseAcceptPatchFlavors();
        assertShouldContainLinkToBinaryDescription();
    }

    @Test
    public void testHeadWithBinaryDescription() throws Exception {
        final NonRdfSourceDescription mockResource
                = (NonRdfSourceDescription)setResource(NonRdfSourceDescription.class);
        when(mockRequest.getMethod()).thenReturn("HEAD");
        when(mockResource.getDescribedResource()).thenReturn(mockBinary);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP RDFSource",
            mockResponse.getHeaders(LINK).contains(buildLink(RDF_SOURCE.getURI(), "type")));
        assertShouldNotAdvertiseAcceptPostFlavors();
        assertShouldAdvertiseAcceptPatchFlavors();
        assertShouldContainLinkToTheBinary();
        assertShouldAllowOnlyResourceDescriptionMethods();
    }

    private void assertShouldContainLinkToTheBinary() {
        assertTrue("Should contain a link to the binary",
                mockResponse.getHeaders(LINK)
                        .contains("<" + identifierConverter.toDomain(binaryPath) + ">; rel=\"describes\""));
    }

    private void assertShouldAdvertiseAcceptPatchFlavors() {
        assertTrue("Should advertise Accept-Patch flavors", mockResponse.containsHeader(HTTP_HEADER_ACCEPT_PATCH));
    }

    @Test
    public void testOption() {
        setResource(FedoraResource.class);
        final Response actual = testObj.options();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should have an Allow header", mockResponse.containsHeader("Allow"));
        assertShouldHaveAcceptExternalContentHandlingHeader();
    }

    @Test
    public void testOptionWithLDPRS() {
        setResource(Container.class);
        final Response actual = testObj.options();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertShouldAdvertiseAcceptPatchFlavors();
        assertShouldHaveAcceptExternalContentHandlingHeader();
    }

    @Test
    public void testOptionWithBinary() throws Exception {
        setField(testObj, "externalPath", binaryPath);
        when(resourceFactory.getResource(mockTransaction, binaryPathId)).thenReturn(mockBinary);
        final Response actual = testObj.options();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldNotAdvertiseAcceptPostFlavors();
        assertShouldNotAdvertiseAcceptPatchFlavors();
        assertShouldContainLinkToBinaryDescription();
        assertShouldHaveAcceptExternalContentHandlingHeader();
    }

    @Test
    public void testOptionWithBinaryDescription() throws Exception {
        setField(testObj, "externalPath", binaryDescriptionPath);
        when(resourceFactory.getResource(mockTransaction, binaryDescId))
                .thenReturn(mockNonRdfSourceDescription);
        final Response actual = testObj.options();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldNotAdvertiseAcceptPostFlavors();
        assertShouldAdvertiseAcceptPatchFlavors();
        assertShouldContainLinkToTheBinary();
        assertShouldHaveAcceptExternalContentHandlingHeader();
    }


    @Test
    public void testGet() throws Exception {
        final var resource = setResource(Container.class);
        setField(testObj, "externalPath", path);
        when(mockRequest.getMethod()).thenReturn("GET");
        when(resourceFactory.getResource((Transaction)any(), any(FedoraId.class))).thenReturn(resource);
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should have a Link header", mockResponse.containsHeader(LINK));
        assertTrue("Should have an Allow header", mockResponse.containsHeader("Allow"));
        assertTrue("Should be an LDP Resource",
                mockResponse.getHeaders(LINK).contains("<" + RESOURCE + ">; rel=\"type\""));

        try (final RdfNamespacedStream entity = (RdfNamespacedStream) actual.getEntity()) {
            final Model model = entity.stream.collect(toModel());
            final List<String> rdfNodes = model.listObjects().mapWith(RDFNode::toString).toList();
            assertTrue("Expected RDF contexts missing", rdfNodes.containsAll(ImmutableSet.of(
                    "LDP_CONTAINMENT", "PROPERTIES", "SERVER_MANAGED", "LDP_MEMBERSHIP")));
        }
    }

    @Test
    public void testGetWithObject() throws Exception {
        setResource(Container.class);
        when(mockRequest.getMethod()).thenReturn("GET");
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertShouldAdvertiseAcceptPatchFlavors();

        try (final RdfNamespacedStream entity = (RdfNamespacedStream) actual.getEntity()) {
            final Model model = entity.stream.collect(toModel());
            final List<String> rdfNodes = model.listObjects().mapWith(RDFNode::toString).toList();
            assertTrue("Expected RDF contexts missing", rdfNodes.containsAll(ImmutableSet.of(
                    "LDP_CONTAINMENT", "PROPERTIES", "SERVER_MANAGED")));
            // TODO: Above list is missing LDP_MEMBERSHIP - https://jira.lyrasis.org/browse/FCREPO-3165
        }
    }


    @Test
    public void testGetWithBasicContainer() throws Exception {
        final FedoraResource resource = setResource(Container.class);
        when(mockRequest.getMethod()).thenReturn("GET");
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldBeAnLDPBasicContainer();
    }

    @Test
    public void testGetWithDirectContainer() throws Exception {
        final FedoraResource resource = setResource(Container.class);
        when(mockRequest.getMethod()).thenReturn("GET");
        typeList.add(URI.create(DIRECT_CONTAINER.toString()));
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldBeAnLDPDirectContainer();
    }

    @Test
    public void testGetWithIndirectContainer() throws Exception {
        final FedoraResource resource = setResource(Container.class);
        when(mockRequest.getMethod()).thenReturn("GET");
        typeList.add(URI.create(INDIRECT_CONTAINER.toString()));
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldBeAnLDPIndirectContainer();
    }

    @Ignore("Requires minimal representation - https://jira.lyrasis.org/browse/FCREPO-3334")
    @Test
    public void testGetWithObjectPreferMinimal() throws Exception {
        setResource(Container.class);
        when(mockRequest.getMethod()).thenReturn("GET");
        setField(testObj, "prefer", new MultiPrefer("return=minimal"));
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());

        try (final RdfNamespacedStream entity = (RdfNamespacedStream) actual.getEntity()) {
            final Model model = entity.stream.collect(toModel());
            final List<String> rdfNodes = model.listObjects().mapWith(RDFNode::toString).toList();
            assertTrue("Expected RDF contexts missing", rdfNodes.stream()
                    .anyMatch(x -> x.contains("PROPERTIES") && x.contains("MINIMAL")));
            assertFalse("Included non-minimal contexts", rdfNodes.contains("LDP_MEMBERSHIP"));
            assertFalse("Included non-minimal contexts", rdfNodes.contains("LDP_CONTAINMENT"));
        }

    }

    /**
     * Emulates an 'If-None-Match' precondition failing for a GET request.  There should not be any entity body set
     * on the response.
     *
     */
    @Test
    public void testGetWhenIfNoneMatchPreconditionFails() {
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
                    mockTransaction, true);
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
     */
    @Test
    public void testGetWhenIfModifiedSincePreconditionFails() {
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
                    mockTransaction, true);
            fail("Expected " + PreconditionException.class.getName() + " to be thrown.");
        } catch (final PreconditionException e) {
            // expected
        }

        // an entity body should _not_ be set under these conditions
        verify(mockRequest).evaluatePreconditions(any(Date.class));
        verify(builder, times(0)).entity(any());
    }

    @Ignore("Needs membership triples - FCREPO-3165")
    @Test
    public void testGetWithObjectOmitContainment() throws Exception {
        setResource(Container.class);
        when(mockRequest.getMethod()).thenReturn("GET");
        setField(testObj, "prefer",
                new MultiPrefer("return=representation; omit=\"" + PREFER_CONTAINMENT + "\""));
        final Response actual = testObj.getResource( null);
        assertEquals(OK.getStatusCode(), actual.getStatus());

        try (final RdfNamespacedStream entity = (RdfNamespacedStream) actual.getEntity()) {
            final Model model = entity.stream.collect(toModel());
            final List<String> rdfNodes = model.listObjects().mapWith(RDFNode::toString).toList();
            assertTrue("Should include membership contexts", rdfNodes.contains("LDP_MEMBERSHIP"));
            assertFalse("Should not include containment contexts", rdfNodes.contains("LDP_CONTAINMENT"));
        }
    }

    @Ignore("Membership Triples not implemented - FCREPO-3165")
    @Test
    public void testGetWithObjectOmitMembership() throws Exception {
        setResource(Container.class);
        when(mockRequest.getMethod()).thenReturn("GET");
        setField(testObj, "prefer",
                new MultiPrefer("return=representation; omit=\"" + PREFER_MEMBERSHIP + "\""));
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
            throws IOException, UnsupportedAlgorithmException {
        final List<tripleTypes> triples = List.of(
                tripleTypes.PROPERTIES,
                tripleTypes.LDP_CONTAINMENT,
                tripleTypes.SERVER_MANAGED,
                tripleTypes.LDP_MEMBERSHIP,
                tripleTypes.INBOUND_REFERENCES
        );
        setResource(Container.class, triples);
        when(mockRequest.getMethod()).thenReturn("GET");
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
        final Binary mockResource = (Binary)setResource(Binary.class);
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockResource.getDescription()).thenReturn(mockNonRdfSourceDescription);
        when(mockResource.getMimeType()).thenReturn("text/plain");
        when(mockResource.getContent()).thenReturn(toInputStream("xyz", UTF_8));
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertShouldBeAnLDPNonRDFSource();
        assertShouldNotAdvertiseAcceptPatchFlavors();
        assertShouldContainLinkToBinaryDescription();
        assertEquals("xyz", IOUtils.toString((InputStream) actual.getEntity(), UTF_8));
    }

    private void assertShouldBeAnLDPNonRDFSource() {
        assertTrue("Should be an LDP NonRDFSource",
                mockResponse.getHeaders(LINK).contains("<" + NON_RDF_SOURCE + ">; rel=\"type\""));
        assertShouldNotAdvertiseAcceptPostFlavors();
    }

    @Test
    public void testGetWithExternalMessageBinary() throws Exception {
        final Binary mockResource = (Binary)setResource(Binary.class);
        when(mockRequest.getMethod()).thenReturn("GET");
        final String url = "http://example.com/some/url";
        when(mockResource.getDescription()).thenReturn(mockNonRdfSourceDescription);
        when(mockResource.getMimeType()).thenReturn("text/plain");
        when(mockResource.isProxy()).thenReturn(false);
        when(mockResource.isRedirect()).thenReturn(true);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockResource.getExternalURI()).thenReturn(URI.create(url));
        when(mockResource.getExternalURL()).thenReturn(url);
        when(mockResource.getContent()).thenReturn(toInputStream("xyz", UTF_8));
        final Response actual = testObj.getResource(null);
        assertEquals(TEMPORARY_REDIRECT.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP NonRDFSource", mockResponse.getHeaders(LINK).contains("<" +
                NON_RDF_SOURCE + ">; rel=\"type\""));
        assertShouldContainLinkToBinaryDescription();
        assertEquals(new URI(url), actual.getLocation());
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testGetWithExternalMessageMissingURLBinary() throws Exception {
        when(extContentHandlerFactory.createFromLinks(anyList()))
                .thenThrow(new ExternalMessageBodyException(""));

        final String badExternal = Link.fromUri("http://test.com")
                .rel(EXTERNAL_CONTENT.toString())
                .param("handling", "proxy")
                .type("text/plain")
                .build()
                .toString()
                .replaceAll("<.*>", "< >");

        testObj.createObject(null, null, null, null, singletonList(badExternal), null);
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testPostWithExternalMessageBadHandling() throws Exception {
        when(extContentHandlerFactory.createFromLinks(anyList()))
                .thenThrow(new ExternalMessageBodyException(""));

         final String badExternal = Link.fromUri("http://test.com")
            .rel(EXTERNAL_CONTENT.toString()).param("handling", "boogie").type("text/plain").build().toString();
        testObj.createObject(null, null, null, null, singletonList(badExternal), null);
    }

    @Test
    public void testGetWithBinaryDescription()
            throws IOException, UnsupportedAlgorithmException {

        final NonRdfSourceDescription mockResource
                = (NonRdfSourceDescription)setResource(NonRdfSourceDescription.class);
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockResource.getDescribedResource()).thenReturn(mockBinary);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockBinary.getTriples())
            .thenReturn(new DefaultRdfStream(createURI("mockBinary")));
        when(mockBinary.getTriples())
            .thenReturn(new DefaultRdfStream(createURI("mockBinary"), of(new Triple
                    (createURI("mockBinary"), createURI("called"), createURI("child:properties")))));
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());

        assertTrue("Should be an LDP RDFSource",
            mockResponse.getHeaders(LINK).contains(buildLink(RDF_SOURCE.getURI(), "type")));
        assertShouldNotAdvertiseAcceptPostFlavors();
        assertShouldAdvertiseAcceptPatchFlavors();
        assertShouldContainLinkToTheBinary();
        assertShouldAllowOnlyResourceDescriptionMethods();

        final Model model = ((RdfNamespacedStream) actual.getEntity()).stream.collect(toModel());
        final List<String> rdfNodes = model.listObjects().mapWith(RDFNode::toString).toList();
        log.info("Found RDF objects\n{}", rdfNodes);
        assertTrue("Expected RDF contexts missing", rdfNodes.containsAll(ImmutableSet.of(
                "LDP_CONTAINMENT", "PROPERTIES", "SERVER_MANAGED")));
        // TODO: Above list is missing LDP_MEMBERSHIP - https://jira.lyrasis.org/browse/FCREPO-3165

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
        when(resourceFactory.getResource(mockTransaction, pathId)).thenReturn(mockContainer);
        when(mockRequest.getMethod()).thenReturn("DELETE");
        final Response actual = testObj.deleteObject();
        assertEquals(NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(deleteResourceService).perform(
                eq(mockTransaction),
                eq(mockContainer),
                anyString());
    }

    @Test
    public void testPutNewObject() throws Exception {
        setField(testObj, "externalPath", "some/path");
        final Binary mockObject = (Binary)setResource(Binary.class);
        when(mockRequest.getMethod()).thenReturn("PUT");
        when(resourceFactory.getResource(mockTransaction, FedoraId.create("some/path"))).thenReturn(mockObject);

        final Response actual = testObj.createOrReplaceObjectRdf(null, emptyStream,
                null, null, null, null);

        assertEquals(CREATED.getStatusCode(), actual.getStatus());
    }

    @Test(expected = CannotCreateResourceException.class)
    public void testPutNewObjectLdpr() throws Exception {
        testObj.createOrReplaceObjectRdf(null, null, null, null,
                singletonList("<http://www.w3.org/ns/ldp#Resource>; rel=\"type\""), null);
    }

    @Test
    public void testPutNewObjectWithRdf() throws Exception {

        when(mockRequest.getMethod()).thenReturn("PUT");
        when(resourceFactory.getResource(mockTransaction, pathId)).thenReturn(mockContainer);

        final Response actual = testObj.createOrReplaceObjectRdf(NTRIPLES_TYPE,
                toInputStream("_:a <info:x> _:c .", UTF_8), null, null, null, null);

        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(createResourceService).perform(
                eq(mockTransaction),
                anyString(),
                eq(pathId),
                isNull(),
                any(Model.class));
    }

    @Test
    public void testPutNewBinary() throws Exception {
        setField(testObj, "externalPath", "some/path");
        final Binary mockObject = (Binary)setResource(Binary.class);
        when(mockRequest.getMethod()).thenReturn("PUT");
        when(resourceFactory.getResource(mockTransaction, FedoraId.create("some/path"))).thenReturn(mockObject);

        final Response actual = testObj.createOrReplaceObjectRdf(TEXT_PLAIN_TYPE,
                toInputStream("xyz", UTF_8), null, null, nonRDFSourceLink, null);

        assertEquals(CREATED.getStatusCode(), actual.getStatus());
    }

    @Test
    public void testPutReplaceRdfObject() throws Exception {

        setField(testObj, "externalPath", "some/path");
        final Container mockObject = (Container)setResource(Container.class);
        when(mockRequest.getMethod()).thenReturn("PUT");
        when(resourceFactory.getResource(mockTransaction, pathId)).thenReturn(mockObject);
        when(resourceHelper.doesResourceExist(mockTransaction, pathId, true)).thenReturn(true);

        final Response actual = testObj.createOrReplaceObjectRdf(NTRIPLES_TYPE,
                toInputStream("_:a <info:x> _:c .", UTF_8), null, null, null, null);

        assertEquals(NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(replacePropertiesService).perform(eq(mockTransaction), anyString(), eq(pathId),
                any(Model.class));
    }

    @Test(expected = ClientErrorException.class)
    public void testPutWithStrictIfMatchHandling() throws Exception {

        when(mockHttpConfiguration.putRequiresIfMatch()).thenReturn(true);
        when(resourceFactory.getResource(mockTransaction, pathId)).thenReturn(mockContainer);
        when(resourceHelper.doesResourceExist(mockTransaction, pathId, true)).thenReturn(true);

        testObj.createOrReplaceObjectRdf(NTRIPLES_TYPE,
                toInputStream("_:a <info:x> _:c .", UTF_8), null, null, null, null);

    }

    @Test
    public void testPatchObject() throws Exception {

        setResource(Container.class);
        when(mockRequest.getMethod()).thenReturn("PATCH");
        testObj.updateSparql(toInputStream("INSERT DATA { <> <http://some/predicate> \"xyz\" }", UTF_8));
    }


    @Test
    public void testPatchBinaryDescription() throws Exception {
        setField(testObj, "externalPath", binaryDescriptionPath);
        when(mockRequest.getMethod()).thenReturn("PATCH");

        when(mockNonRdfSourceDescription.getTriples())
            .thenReturn(new DefaultRdfStream(createURI("mockBinary"),
                        of(new Triple(createURI("mockBinary"), createURI("called"),
                            createURI("child:properties")))));

        when(resourceFactory.getResource(mockTransaction, binaryDescId))
                .thenReturn(mockNonRdfSourceDescription);
        testObj.updateSparql(toInputStream("INSERT DATA { <> <http://some/predicate> \"xyz\" }", UTF_8));
        verify(updatePropertiesService).updateProperties(
                eq(mockTransaction),
                anyString(),
                eq(binaryDescId),
                contains("<http://some/predicate> \"xyz\"")
        );
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
        setResource(Binary.class);
        testObj.updateSparql(toInputStream("", UTF_8));
    }

    @Test
    public void testCreateNewObject() throws Exception {
        final var resource = setResource(Container.class);
        final var finalId = pathId.resolve("b");
        when(resourceFactory.getResource((Transaction)any(), eq(finalId))).thenReturn(resource);
        final Response actual = testObj.createObject(null, null, "b",
                emptyStream, null, null);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
    }

    @Test
    public void testCreateNewObjectWithVersionedResource() throws Exception {
        final var resource = setResource(Container.class);
        final var finalId = pathId.resolve("b");
        when(resourceFactory.getResource((Transaction)any(), eq(finalId))).thenReturn(resource);
        final String versionedResourceLink = "<" + VERSIONED_RESOURCE.getURI() + ">;rel=\"type\"";
        final Response actual = testObj.createObject(null, null, "b",
                toInputStream("", UTF_8), singletonList(versionedResourceLink), null);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
    }

    @Test(expected = UnsupportedMediaTypeException.class)
    public void testCreateNewObjectWithSparql() throws Exception {
        final var resource = setResource(Container.class);
        final var finalId = pathId.resolve("b");
        when(resourceFactory.getResource((Transaction)any(), eq(finalId))).thenReturn(resource);
        final Response actual = testObj.createObject(null,
                MediaType.valueOf(contentTypeSPARQLUpdate),
                "b",
                toInputStream("INSERT DATA { <> <http://example.org/somePredicate> \"x\" }", UTF_8),
                null,
                null);
    }

    @Test
    public void testCreateNewObjectWithRdf() throws Exception {
        final var resource = setResource(Container.class);
        final var finalId = pathId.resolve("b");
        when(resourceFactory.getResource((Transaction)any(), eq(finalId))).thenReturn(resource);
        final Response actual = testObj.createObject(null, NTRIPLES_TYPE, "b",
                toInputStream("_:a <info:b> _:c .", UTF_8), null, null);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(createResourceService).perform(
                eq(mockTransaction),
                anyString(),
                eq(finalId),
               any(),
               any(Model.class));
    }


    @Test
    public void testCreateNewBinary() throws MalformedRdfException, InvalidChecksumException,
           IOException, UnsupportedAlgorithmException, PathNotFoundException {
        setResource(Container.class);
        final var finalId = pathId.resolve("b");
        when(resourceFactory.getResource((Transaction)any(), eq(finalId))).thenReturn(mockBinary);
        try (final InputStream content = toInputStream("x", UTF_8)) {
            final Response actual = testObj.createObject(null, APPLICATION_OCTET_STREAM_TYPE, "b", content,
                nonRDFSourceLink, null);
            assertEquals(CREATED.getStatusCode(), actual.getStatus());
            verify(createResourceService).perform(
                    eq(mockTransaction),
                    any(),
                    eq(finalId),
                    eq(APPLICATION_OCTET_STREAM),
                    eq(""),
                    anyLong(),
                    any(),
                    any(),
                    eq(content),
                    eq(null));
        }
    }

    @Ignore("Insufficient space root exception not thrown and checkForInsufficientStorageException is not called")
    @Test(expected = InsufficientStorageException.class)
    public void testCreateNewBinaryWithInsufficientResources() throws Exception {
        final var finalId = pathId.resolve("b");
        when(resourceFactory.getResource((Transaction)any(), eq(finalId))).thenReturn(mockBinary);

        try (final InputStream content = toInputStream("x", UTF_8)) {
            final RuntimeException ex = new RuntimeException(new IOException("root exception", new IOException(
                    FedoraLdp.INSUFFICIENT_SPACE_IDENTIFYING_MESSAGE)));
            doThrow(ex).when(createResourceService).perform(any(), anyString(), eq(finalId), anyString(),
                    anyString(), any(Long.class), anyList(), isNull(), any(InputStream.class), isNull());

            testObj.createObject(null, APPLICATION_OCTET_STREAM_TYPE, "b", content, nonRDFSourceLink, null);
        }
    }

    @Test
    public void testCreateNewBinaryWithContentTypeWithParams() throws MalformedRdfException,
           InvalidChecksumException, IOException, UnsupportedAlgorithmException, PathNotFoundException {

        setResource(Container.class);
        final var finalId = pathId.resolve("b");
        when(resourceFactory.getResource((Transaction)any(), eq(finalId))).thenReturn(mockBinary);
        try (final InputStream content = toInputStream("x", UTF_8)) {
            final MediaType requestContentType = MediaType.valueOf("some/mime-type; with=some; param=s");
            final Response actual = testObj.createObject(null, requestContentType, "b", content, nonRDFSourceLink,
                null);
            assertEquals(CREATED.getStatusCode(), actual.getStatus());
            verify(createResourceService).perform(
                    any(),
                    any(),
                    eq(finalId),
                    eq(requestContentType.toString()),
                    eq(""),
                    anyLong(),
                    eq(nonRDFSourceLink),
                    any(),
                    eq(content),
                    eq(null));
        }
    }

    @Test
    public void testCreateNewBinaryWithChecksumSHA() throws MalformedRdfException,
           InvalidChecksumException, IOException, UnsupportedAlgorithmException, PathNotFoundException {

        final var finalId = pathId.resolve("b");
        when(resourceFactory.getResource((Transaction)any(), eq(finalId))).thenReturn(mockBinary);
        try (final InputStream content = toInputStream("x", UTF_8)) {
            final MediaType requestContentType = MediaType.valueOf("some/mime-type; with=some; param=s");
            final String sha = "07a4d371f3b7b6283a8e1230b7ec6764f8287bf2";
            final String requestSHA = "sha=" + sha;
            final Set<URI> shaURI = singleton(URI.create("urn:sha1:" + sha));
            final Response actual = testObj.createObject(null, requestContentType, "b", content, nonRDFSourceLink,
                requestSHA);
            assertEquals(CREATED.getStatusCode(), actual.getStatus());
            verify(createResourceService).perform(
                    any(),
                    any(),
                    eq(finalId),
                    eq(requestContentType.toString()),
                    eq(""),
                    anyLong(),
                    eq(nonRDFSourceLink),
                    eq(shaURI),
                    eq(content),
                    eq(null));
        }
    }

    @Test
    public void testCreateNewBinaryWithChecksumSHA256() throws MalformedRdfException,
        InvalidChecksumException, IOException, UnsupportedAlgorithmException, PathNotFoundException {

        final var finalId = pathId.resolve("b");
        when(resourceFactory.getResource((Transaction)any(), eq(finalId))).thenReturn(mockBinary);
        try (final InputStream content = toInputStream("x", UTF_8)) {
            final MediaType requestContentType = MediaType.valueOf("some/mime-type; with=some; param=s");
            final String sha = "73cb3858a687a8494ca3323053016282f3dad39d42cf62ca4e79dda2aac7d9ac";
            final String requestSHA = "sha-256=" + sha;
            final Set<URI> shaURI = singleton(URI.create("urn:sha-256:" + sha));
            final Response actual = testObj.createObject(null, requestContentType, "b", content, nonRDFSourceLink,
                requestSHA);
            assertEquals(CREATED.getStatusCode(), actual.getStatus());
            verify(createResourceService).perform(
                    any(),
                    any(),
                    eq(finalId),
                    eq(requestContentType.toString()),
                    eq(""),
                    anyLong(),
                    eq(nonRDFSourceLink),
                    eq(shaURI),
                    eq(content),
                    eq(null));
        }
    }

    @Test
    public void testCreateNewBinaryWithChecksumMD5() throws MalformedRdfException,
            InvalidChecksumException, IOException, UnsupportedAlgorithmException, PathNotFoundException {

        final var finalId = pathId.resolve("b");
        when(resourceFactory.getResource((Transaction)any(), eq(finalId))).thenReturn(mockBinary);
        try (final InputStream content = toInputStream("x", UTF_8)) {
            final MediaType requestContentType = MediaType.valueOf("some/mime-type; with=some; param=s");
            final String md5 = "HUXZLQLMuI/KZ5KDcJPcOA==";
            final String requestMD5 = "md5=" + md5;
            final Set<URI> md5URI = singleton(URI.create("urn:md5:" + md5));
            final Response actual = testObj.createObject(null, requestContentType, "b", content, nonRDFSourceLink,
                requestMD5);
            assertEquals(CREATED.getStatusCode(), actual.getStatus());
            verify(createResourceService).perform(
                    any(),
                    any(),
                    eq(finalId),
                    eq(requestContentType.toString()),
                    eq(""),
                    anyLong(),
                    eq(nonRDFSourceLink),
                    eq(md5URI),
                    eq(content),
                    eq(null));
        }
    }

    @Test
    public void testCreateNewBinaryWithChecksumSHAandMD5() throws MalformedRdfException,
           InvalidChecksumException, IOException, UnsupportedAlgorithmException, PathNotFoundException {

        final var finalId = pathId.resolve("b");
        when(resourceFactory.getResource((Transaction)any(), eq(finalId))).thenReturn(mockBinary);
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
            verify(createResourceService).perform(
                    any(),
                    any(),
                    eq(finalId),
                    eq(requestContentType.toString()),
                    eq(""),
                    anyLong(),
                    eq(nonRDFSourceLink),
                    eq(checksumURIs),
                    eq(content),
                    eq(null));
        }
    }

    @Test(expected = CannotCreateResourceException.class)
    public void testLDPRNotImplemented() throws Exception {
        final var resource = setResource(Container.class);
        when(resourceFactory.getResource(mockTransaction, pathId.resolve("x"))).thenReturn(resource);
        testObj.createObject(null, null, "x", null,
                singletonList("<http://www.w3.org/ns/ldp#Resource>; rel=\"type\""), null);
    }

    @Test(expected = ClientErrorException.class)
    public void testLDPRNotImplementedInvalidLink() throws Exception {
        final var resource = setResource(Container.class);
        when(resourceFactory.getResource(mockTransaction, pathId.resolve("x"))).thenReturn(resource);
        testObj.createObject(null, null, "x", null, singletonList("<http://foo;rel=\"type\""), null);
    }

    @Test
    public void testGetSimpleContentType() {
        final MediaType mediaType = new MediaType("text", "plain", ImmutableMap.of("charset", "UTF-8"));
        final MediaType sanitizedMediaType = MediaType.valueOf(getSimpleContentType(mediaType));
        assertEquals("text/plain", sanitizedMediaType.toString());
    }

}
