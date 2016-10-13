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
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.stream.Stream.of;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.TEMPORARY_REDIRECT;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.http.api.ContentExposingResource.getSimpleContentType;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES_TYPE;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_BASIC_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.ObservationManager;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.http.api.PathLockManager.AcquiredLock;
import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.http.commons.domain.MultiPrefer;
import org.fcrepo.http.commons.responses.RdfNamespacedStream;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.TripleCategory;
import org.fcrepo.kernel.api.exception.InsufficientStorageException;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.services.NamespaceService;
import org.fcrepo.kernel.api.services.NodeService;
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
    private final String binaryDescriptionPath = "/some/other/path";
    private FedoraLdp testObj;

    private static final String BASEURL_PROP = "fcrepo.jms.baseUrl";

    @Mock
    private Request mockRequest;

    private HttpServletResponse mockResponse;

    private Session mockSession;

    @Mock
    private Node mockNode;

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
    private NamespaceService mockNamespaceService;

    @Mock
    private FedoraHttpConfiguration mockHttpConfiguration;

    @Mock
    private HttpHeaders mockHeaders;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Mock
    private PathLockManager mockLockManager;

    @Mock
    private AcquiredLock mockLock;

    private static final Logger log = getLogger(FedoraLdpTest.class);


    @Before
    public void setUp() throws RepositoryException {
        testObj = spy(new FedoraLdp(path));

        mockResponse = new MockHttpServletResponse();

        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);

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
        setField(testObj, "namespaceService", mockNamespaceService);
        setField(testObj, "lockManager", mockLockManager);

        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
        when(mockNamespaceRegistry.getPrefixes()).thenReturn(new String[]{});
        when(mockNamespaceService.getNamespaces(any(Session.class))).thenReturn(emptyMap());

        when(mockHttpConfiguration.putRequiresIfMatch()).thenReturn(false);

        when(mockContainer.getEtagValue()).thenReturn("");
        when(mockContainer.getPath()).thenReturn(path);
        when(mockContainer.getDescription()).thenReturn(mockContainer);
        when(mockContainer.getDescribedResource()).thenReturn(mockContainer);

        when(mockNonRdfSourceDescription.getEtagValue()).thenReturn("");
        when(mockNonRdfSourceDescription.getPath()).thenReturn(binaryDescriptionPath);
        when(mockNonRdfSourceDescription.getDescribedResource()).thenReturn(mockBinary);

        when(mockBinary.getEtagValue()).thenReturn("");
        when(mockBinary.getPath()).thenReturn(binaryPath);
        when(mockBinary.getDescription()).thenReturn(mockNonRdfSourceDescription);

        when(mockHeaders.getHeaderString("user-agent")).thenReturn("Test UserAgent");

        when(mockLockManager.lockForRead(any())).thenReturn(mockLock);
        when(mockLockManager.lockForWrite(any(), any(), any())).thenReturn(mockLock);
        when(mockLockManager.lockForDelete(any())).thenReturn(mockLock);
    }

    private FedoraResource setResource(final Class<? extends FedoraResource> klass) throws RepositoryException {
        final FedoraResource mockResource = mock(klass);
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
        assertTrue("Should have a Link header", mockResponse.containsHeader("Link"));
        assertTrue("Should have an Allow header", mockResponse.containsHeader("Allow"));
        assertTrue("Should be an LDP Resource",
                mockResponse.getHeaders("Link").contains("<" + LDP_NAMESPACE + "Resource>;rel=\"type\""));
    }

    @Test
    public void testHeadWithObject() throws Exception {
        setResource(Container.class);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertTrue("Should advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));
    }

    @Test
    public void testHeadWithDefaultContainer() throws Exception {
        setResource(Container.class);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP BasicContainer",
                mockResponse.getHeaders("Link").contains("<" + BASIC_CONTAINER.getURI() + ">;rel=\"type\""));
    }


    @Test
    public void testHeadWithBasicContainer() throws Exception {
        final FedoraResource resource = setResource(Container.class);
        when(resource.hasType(LDP_BASIC_CONTAINER)).thenReturn(true);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP BasicContainer",
                mockResponse.getHeaders("Link").contains("<" + BASIC_CONTAINER.getURI() + ">;rel=\"type\""));
    }

    @Test
    public void testHeadWithDirectContainer() throws Exception {
        final FedoraResource resource = setResource(Container.class);
        when(resource.hasType(LDP_DIRECT_CONTAINER)).thenReturn(true);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP DirectContainer",
                mockResponse.getHeaders("Link").contains("<" + DIRECT_CONTAINER.getURI() + ">;rel=\"type\""));
    }

    @Test
    public void testHeadWithIndirectContainer() throws Exception {
        final FedoraResource resource = setResource(Container.class);
        when(resource.hasType(LDP_INDIRECT_CONTAINER)).thenReturn(true);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP IndirectContainer",
                mockResponse.getHeaders("Link").contains("<" + INDIRECT_CONTAINER.getURI() + ">;rel=\"type\""));
    }

    @Test
    public void testHeadWithBinary() throws Exception {
        final FedoraBinary mockResource = (FedoraBinary)setResource(FedoraBinary.class);
        when(mockResource.getDescription()).thenReturn(mockNonRdfSourceDescription);
        when(mockResource.getMimeType()).thenReturn("image/jpeg");
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP NonRDFSource", mockResponse.getHeaders("Link").contains("<" + LDP_NAMESPACE +
                "NonRDFSource>;rel=\"type\""));
        assertFalse("Should not advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertFalse("Should not advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));
        assertTrue("Should contain a link to the binary description",
                mockResponse.getHeaders("Link")
                        .contains("<" + idTranslator.toDomain(binaryDescriptionPath + "/fcr:metadata")
                                + ">; rel=\"describedby\""));
    }

    @Test
    public void testHeadWithExternalBinary() throws Exception {
        final FedoraBinary mockResource = (FedoraBinary)setResource(FedoraBinary.class);
        when(mockResource.getDescription()).thenReturn(mockNonRdfSourceDescription);
        when(mockResource.getMimeType()).thenReturn("message/external-body; access-type=URL; URL=\"some:uri\"");
        final Response actual = testObj.head();
        assertEquals(TEMPORARY_REDIRECT.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP NonRDFSource", mockResponse.getHeaders("Link").contains("<" + LDP_NAMESPACE +
                "NonRDFSource>;rel=\"type\""));
        assertFalse("Should not advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertFalse("Should not advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));
        assertTrue("Should contain a link to the binary description",
                mockResponse.getHeaders("Link")
                        .contains("<" + idTranslator.toDomain(binaryDescriptionPath + "/fcr:metadata")
                                + ">; rel=\"describedby\""));
    }

    @Test
    public void testHeadWithBinaryDescription() throws Exception {
        final NonRdfSourceDescription mockResource
                = (NonRdfSourceDescription)setResource(NonRdfSourceDescription.class);
        when(mockResource.getDescribedResource()).thenReturn(mockBinary);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP RDFSource", mockResponse.getHeaders("Link").contains("<" + LDP_NAMESPACE +
                "RDFSource>;rel=\"type\""));
        assertFalse("Should not advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertTrue("Should advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));
        assertTrue("Should contain a link to the binary",
                mockResponse.getHeaders("Link")
                        .contains("<" + idTranslator.toDomain(binaryPath) + ">; rel=\"describes\""));
    }

    @Test
    public void testOption() throws Exception {
        setResource(FedoraResource.class);
        final Response actual = testObj.options();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should have an Allow header", mockResponse.containsHeader("Allow"));
    }

    @Test
    public void testOptionWithObject() throws Exception {
        setResource(Container.class);
        final Response actual = testObj.options();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertTrue("Should advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));
    }

    @Test
    public void testOptionWithBinary() throws Exception {
        final FedoraBinary mockResource = (FedoraBinary)setResource(FedoraBinary.class);
        when(mockResource.getDescription()).thenReturn(mockNonRdfSourceDescription);
        final Response actual = testObj.options();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertFalse("Should not advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertFalse("Should not advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));
        assertTrue("Should contain a link to the binary description",
                mockResponse.getHeaders("Link")
                        .contains("<" + idTranslator.toDomain(binaryDescriptionPath + "/fcr:metadata")
                                + ">; rel=\"describedby\""));
    }

    @Test
    public void testOptionWithBinaryDescription() throws Exception {
        final NonRdfSourceDescription mockResource
                = (NonRdfSourceDescription)setResource(NonRdfSourceDescription.class);
        when(mockResource.getDescribedResource()).thenReturn(mockBinary);
        final Response actual = testObj.options();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertFalse("Should not advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertTrue("Should advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));
        assertTrue("Should contain a link to the binary",
                mockResponse.getHeaders("Link")
                        .contains("<" + idTranslator.toDomain(binaryPath) + ">; rel=\"describes\""));
    }


    @Test
    public void testGet() throws Exception {
        setResource(FedoraResource.class);
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should have a Link header", mockResponse.containsHeader("Link"));
        assertTrue("Should have an Allow header", mockResponse.containsHeader("Allow"));
        assertTrue("Should be an LDP Resource",
                mockResponse.getHeaders("Link").contains("<" + LDP_NAMESPACE + "Resource>;rel=\"type\""));

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
        assertTrue("Should advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));

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
        assertTrue("Should be an LDP BasicContainer",
                mockResponse.getHeaders("Link").contains("<" + BASIC_CONTAINER.getURI() + ">;rel=\"type\""));
    }

    @Test
    public void testGetWithDirectContainer() throws Exception {
        final FedoraResource resource = setResource(Container.class);
        when(resource.hasType(LDP_DIRECT_CONTAINER)).thenReturn(true);
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP DirectContainer",
                mockResponse.getHeaders("Link").contains("<" + DIRECT_CONTAINER.getURI() + ">;rel=\"type\""));
    }

    @Test
    public void testGetWithIndirectContainer() throws Exception {
        final FedoraResource resource = setResource(Container.class);
        when(resource.hasType(LDP_INDIRECT_CONTAINER)).thenReturn(true);
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP IndirectContainer",
                mockResponse.getHeaders("Link").contains("<" + INDIRECT_CONTAINER.getURI() + ">;rel=\"type\""));
    }

    @Test
    public void testGetWithObjectPreferMinimal() throws Exception {

        setResource(Container.class);
        setField(testObj, "prefer", new MultiPrefer("return=minimal"));
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());

        try (final RdfNamespacedStream entity = (RdfNamespacedStream) actual.getEntity()) {
            final Model model = entity.stream.collect(toModel());
            final List<String> rdfNodes = model.listObjects().mapWith(RDFNode::toString).toList();
            assertTrue("Expected RDF contexts missing", rdfNodes.stream()
                    .filter(x -> x.contains("PROPERTIES") && x.contains("MINIMAL")).findFirst().isPresent());
            assertFalse("Included non-minimal contexts", rdfNodes.contains("LDP_MEMBERSHIP"));
            assertFalse("Included non-minimal contexts", rdfNodes.contains("LDP_CONTAINMENT"));
        }

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
    public void testGetWithObjectIncludeReferences() throws ParseException, IOException, RepositoryException {
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
        when(mockResource.getContent()).thenReturn(toInputStream("xyz"));
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP NonRDFSource",
                mockResponse.getHeaders("Link").contains("<" + LDP_NAMESPACE + "NonRDFSource>;rel=\"type\""));
        assertFalse("Should not advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertFalse("Should not advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));
        assertTrue("Should contain a link to the binary description",
                mockResponse.getHeaders("Link")
                        .contains("<" + idTranslator.toDomain(binaryDescriptionPath + "/fcr:metadata")
                                + ">; rel=\"describedby\""));
        assertTrue(IOUtils.toString((InputStream)actual.getEntity()).equals("xyz"));
    }

    @Test
    public void testGetWithExternalMessageBinary() throws Exception {
        final FedoraBinary mockResource = (FedoraBinary)setResource(FedoraBinary.class);
        when(mockResource.getDescription()).thenReturn(mockNonRdfSourceDescription);
        when(mockResource.getMimeType()).thenReturn("message/external-body; access-type=URL; URL=\"some:uri\"");
        when(mockResource.getContent()).thenReturn(toInputStream("xyz"));
        final Response actual = testObj.getResource(null);
        assertEquals(TEMPORARY_REDIRECT.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP NonRDFSource", mockResponse.getHeaders("Link").contains("<" + LDP_NAMESPACE +
                "NonRDFSource>;rel=\"type\""));
        assertTrue("Should contain a link to the binary description", mockResponse.getHeaders("Link").contains("<" +
                idTranslator.toDomain(binaryDescriptionPath + "/fcr:metadata") + ">; rel=\"describedby\""));
        assertEquals(new URI("some:uri"), actual.getLocation());
    }


    @Test
    @SuppressWarnings({"resource", "unchecked"})
    public void testGetWithBinaryDescription() throws RepositoryException, IOException {

        final NonRdfSourceDescription mockResource
                = (NonRdfSourceDescription)setResource(NonRdfSourceDescription.class);
        when(mockResource.getDescribedResource()).thenReturn(mockBinary);
        when(mockBinary.getTriples(eq(idTranslator), any(TripleCategory.class)))
            .thenReturn(new DefaultRdfStream(createURI("mockBinary")));
        when(mockBinary.getTriples(eq(idTranslator), any(EnumSet.class)))
            .thenReturn(new DefaultRdfStream(createURI("mockBinary"), of(new Triple
                (createURI("mockBinary"), createURI("called"), createURI("child:properties")))));
        final Response actual = testObj.getResource(null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP RDFSource",
                mockResponse.getHeaders("Link").contains("<" + LDP_NAMESPACE + "RDFSource>;rel=\"type\""));
        assertFalse("Should not advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertTrue("Should advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));
        assertTrue("Should contain a link to the binary",
                mockResponse.getHeaders("Link")
                        .contains("<" + idTranslator.toDomain(binaryPath) + ">; rel=\"describes\""));

        final Model model = ((RdfNamespacedStream) actual.getEntity()).stream.collect(toModel());
        final List<String> rdfNodes = model.listObjects().mapWith(RDFNode::toString).toList();
        log.info("Found RDF objects\n{}", rdfNodes);
        assertTrue("Expected RDF contexts missing", rdfNodes.containsAll(ImmutableSet.of(
                "LDP_CONTAINMENT", "LDP_MEMBERSHIP", "PROPERTIES", "SERVER_MANAGED")));

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

        when(mockNodeService.exists(mockSession, "/some/path")).thenReturn(false);
        when(mockContainerService.findOrCreate(mockSession, "/some/path")).thenReturn(mockContainer);

        final Response actual = testObj.createOrReplaceObjectRdf(null, null, null, null, null, null);

        assertEquals(CREATED.getStatusCode(), actual.getStatus());
    }

    @Test(expected = ServerErrorException.class)
    public void testPutNewObjectLdpr() throws Exception {
        testObj.createOrReplaceObjectRdf(null, null, null, null,
                "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\"", null);
    }

    @Test
    public void testPutNewObjectWithRdf() throws Exception {

        setField(testObj, "externalPath", "some/path");
        final FedoraBinary mockObject = (FedoraBinary)setResource(FedoraBinary.class);
        doReturn(mockObject).when(testObj).resource();
        when(mockContainer.isNew()).thenReturn(true);

        when(mockNodeService.exists(mockSession, "/some/path")).thenReturn(false);
        when(mockContainerService.findOrCreate(mockSession, "/some/path")).thenReturn(mockContainer);

        final Response actual = testObj.createOrReplaceObjectRdf(NTRIPLES_TYPE,
                toInputStream("_:a <info:x> _:c ."), null, null, null, null);

        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockContainer).replaceProperties(eq(idTranslator), any(Model.class), any(RdfStream.class));
    }

    @Test
    public void testPutNewBinary() throws Exception {
        setField(testObj, "externalPath", "some/path");
        final FedoraBinary mockObject = (FedoraBinary)setResource(FedoraBinary.class);
        doReturn(mockObject).when(testObj).resource();
        when(mockBinary.isNew()).thenReturn(true);

        when(mockNodeService.exists(mockSession, "/some/path")).thenReturn(false);
        when(mockBinaryService.findOrCreate(mockSession, "/some/path")).thenReturn(mockBinary);

        final Response actual = testObj.createOrReplaceObjectRdf(TEXT_PLAIN_TYPE,
                toInputStream("xyz"), null, null, null, null);

        assertEquals(CREATED.getStatusCode(), actual.getStatus());
    }

    @Test
    public void testPutReplaceRdfObject() throws Exception {

        setField(testObj, "externalPath", "some/path");
        final Container mockObject = (Container)setResource(Container.class);
        doReturn(mockObject).when(testObj).resource();
        when(mockObject.isNew()).thenReturn(false);

        when(mockNodeService.exists(mockSession, "/some/path")).thenReturn(true);
        when(mockContainerService.findOrCreate(mockSession, "/some/path")).thenReturn(mockObject);

        final Response actual = testObj.createOrReplaceObjectRdf(NTRIPLES_TYPE,
                toInputStream("_:a <info:x> _:c ."), null, null, null, null);

        assertEquals(NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(mockObject).replaceProperties(eq(idTranslator), any(Model.class), any(RdfStream.class));
    }

    @Test(expected = ClientErrorException.class)
    public void testPutWithStrictIfMatchHandling() throws Exception {

        when(mockHttpConfiguration.putRequiresIfMatch()).thenReturn(true);
        final Container mockObject = (Container)setResource(Container.class);
        doReturn(mockObject).when(testObj).resource();
        when(mockObject.isNew()).thenReturn(false);

        when(mockNodeService.exists(mockSession, "/some/path")).thenReturn(true);
        when(mockContainerService.findOrCreate(mockSession, "/some/path")).thenReturn(mockObject);

        testObj.createOrReplaceObjectRdf(NTRIPLES_TYPE,
                toInputStream("_:a <info:x> _:c ."), null, null, null, null);

    }

    @Test
    public void testPatchObject() throws Exception {

        setResource(Container.class);

        testObj.updateSparql(toInputStream("xyz"));
    }


    @Test
    @SuppressWarnings({"resource", "unchecked"})
    public void testPatchBinaryDescription() throws RepositoryException, MalformedRdfException, IOException {

        final NonRdfSourceDescription mockObject = (NonRdfSourceDescription)setResource(NonRdfSourceDescription.class);
        when(mockObject.getDescribedResource()).thenReturn(mockBinary);

        when(mockBinary.getTriples(eq(idTranslator), any(TripleCategory.class)))
            .thenReturn(new DefaultRdfStream(createURI("mockBinary")));
        when(mockBinary.getTriples(eq(idTranslator), any(EnumSet.class)))
            .thenReturn(new DefaultRdfStream(createURI("mockBinary"),
                        of(new Triple(createURI("mockBinary"), createURI("called"),
                            createURI("child:properties")))));

        doReturn(mockObject).when(testObj).resource();

        testObj.updateSparql(toInputStream("xyz"));
    }

    @Test(expected = BadRequestException.class)
    public void testPatchWithoutContent() throws MalformedRdfException, IOException {
        testObj.updateSparql(null);
    }

    @Test(expected = BadRequestException.class)
    public void testPatchWithMissingContent() throws RepositoryException, MalformedRdfException, IOException {
        setResource(Container.class);
        testObj.updateSparql(toInputStream(""));
    }

    @Test(expected = BadRequestException.class)
    public void testPatchBinary() throws RepositoryException, MalformedRdfException, IOException {
        setResource(FedoraBinary.class);
        testObj.updateSparql(toInputStream(""));
    }

    @Test
    public void testCreateNewObject() throws RepositoryException, MalformedRdfException, InvalidChecksumException,
           IOException {
        setResource(Container.class);
        when(mockContainerService.findOrCreate(mockSession, "/b")).thenReturn(mockContainer);
        final Response actual = testObj.createObject(null, null, "b", null, null, null);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
    }

    @Test
    public void testCreateNewObjectWithSparql() throws RepositoryException, MalformedRdfException,
           InvalidChecksumException, IOException {

        setResource(Container.class);
        when(mockContainerService.findOrCreate(mockSession, "/b")).thenReturn(mockContainer);
        final Response actual = testObj.createObject(null,
                MediaType.valueOf(contentTypeSPARQLUpdate), "b", toInputStream("x"), null, null);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockContainer).updateProperties(eq(idTranslator), eq("x"), any(RdfStream.class));
    }

    @Test
    public void testCreateNewObjectWithRdf() throws RepositoryException, MalformedRdfException,
           InvalidChecksumException, IOException {
        setResource(Container.class);
        when(mockContainerService.findOrCreate(mockSession, "/b")).thenReturn(mockContainer);
        final Response actual = testObj.createObject(null, NTRIPLES_TYPE, "b",
                toInputStream("_:a <info:b> _:c ."), null, null);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockContainer).replaceProperties(eq(idTranslator), any(Model.class), any(RdfStream.class));
    }


    @Test
    public void testCreateNewBinary() throws RepositoryException, MalformedRdfException, InvalidChecksumException,
           IOException {
        setResource(Container.class);
        when(mockBinaryService.findOrCreate(mockSession, "/b")).thenReturn(mockBinary);
        try (final InputStream content = toInputStream("x")) {
            final Response actual = testObj.createObject(null, APPLICATION_OCTET_STREAM_TYPE, "b", content, null, null);
            assertEquals(CREATED.getStatusCode(), actual.getStatus());
            verify(mockBinary).setContent(content, APPLICATION_OCTET_STREAM, Collections.emptySet(), "", null);
        }
    }

    @Test(expected = InsufficientStorageException.class)
    public void testCreateNewBinaryWithInsufficientResources() throws RepositoryException, MalformedRdfException,
           InvalidChecksumException, IOException {
        setResource(Container.class);
        when(mockBinaryService.findOrCreate(mockSession, "/b")).thenReturn(mockBinary);


        try (final InputStream content = toInputStream("x")) {

            final RuntimeException ex = new RuntimeException(new IOException("root exception", new IOException(
                    "No space left on device")));
            doThrow(ex).when(mockBinary).setContent(content, APPLICATION_OCTET_STREAM_TYPE.toString(),
                    Collections
                    .emptySet(),
                    "", null);

            testObj.createObject(null, APPLICATION_OCTET_STREAM_TYPE, "b", content, null, null);
        }
    }

    @Test
    public void testCreateNewBinaryWithContentTypeWithParams() throws RepositoryException, MalformedRdfException,
           InvalidChecksumException, IOException {

        setResource(Container.class);
        when(mockBinaryService.findOrCreate(mockSession, "/b")).thenReturn(mockBinary);
        try (final InputStream content = toInputStream("x")) {
            final MediaType requestContentType = MediaType.valueOf("some/mime-type; with=some; param=s");
            final Response actual = testObj.createObject(null, requestContentType, "b", content, null, null);
            assertEquals(CREATED.getStatusCode(), actual.getStatus());
            verify(mockBinary).setContent(content, requestContentType.toString(), Collections.emptySet(), "", null);
        }
    }

    @Test
    public void testCreateNewBinaryWithChecksumSHA() throws RepositoryException, MalformedRdfException,
           InvalidChecksumException, IOException {

        setResource(Container.class);
        when(mockBinaryService.findOrCreate(mockSession, "/b")).thenReturn(mockBinary);
        try (final InputStream content = toInputStream("x")) {
            final MediaType requestContentType = MediaType.valueOf("some/mime-type; with=some; param=s");
            final String sha = "07a4d371f3b7b6283a8e1230b7ec6764f8287bf2";
            final String requestSHA = "sha1=" + sha;
            final Set<URI> shaURI = singleton(URI.create("urn:sha1:" + sha));
            final Response actual = testObj.createObject(null, requestContentType, "b", content, null, requestSHA);
            assertEquals(CREATED.getStatusCode(), actual.getStatus());
            verify(mockBinary).setContent(content, requestContentType.toString(), shaURI, "", null);
        }
    }

    @Test
    public void testCreateNewBinaryWithChecksumMD5() throws RepositoryException, MalformedRdfException,
            InvalidChecksumException, IOException {

        setResource(Container.class);
        when(mockBinaryService.findOrCreate(mockSession, "/b")).thenReturn(mockBinary);
        try (final InputStream content = toInputStream("x")) {
            final MediaType requestContentType = MediaType.valueOf("some/mime-type; with=some; param=s");
            final String md5 = "HUXZLQLMuI/KZ5KDcJPcOA==";
            final String requestMD5 = "md5=" + md5;
            final Set<URI> md5URI = singleton(URI.create("urn:md5:" + md5));
            final Response actual = testObj.createObject(null, requestContentType, "b", content, null, requestMD5);
            assertEquals(CREATED.getStatusCode(), actual.getStatus());
            verify(mockBinary).setContent(content, requestContentType.toString(), md5URI, "", null);
        }
    }

    @Test
    public void testCreateNewBinaryWithChecksumSHAandMD5() throws RepositoryException, MalformedRdfException,
           InvalidChecksumException, IOException {

        setResource(Container.class);
        when(mockBinaryService.findOrCreate(mockSession, "/b")).thenReturn(mockBinary);
        try (final InputStream content = toInputStream("x")) {
            final MediaType requestContentType = MediaType.valueOf("some/mime-type; with=some; param=s");

            final String sha = "07a4d371f3b7b6283a8e1230b7ec6764f8287bf2";
            final String requestSHA = "sha1=" + sha;
            final URI shaURI = URI.create("urn:sha1:" + sha);

            final String md5 = "HUXZLQLMuI/KZ5KDcJPcOA==";
            final String requestMD5 = "md5=" + md5;
            final URI md5URI = URI.create("urn:md5:" + md5);

            final String requestChecksum = requestSHA + "," + requestMD5;
            final HashSet<URI> checksumURIs = new HashSet<>(asList(shaURI, md5URI));

            final Response actual = testObj.createObject(null, requestContentType, "b", content, null, requestChecksum);
            assertEquals(CREATED.getStatusCode(), actual.getStatus());
            verify(mockBinary).setContent(content, requestContentType.toString(), checksumURIs, "", null);
        }
    }

    @Test(expected = ClientErrorException.class)
    public void testPostToBinary() throws MalformedRdfException, InvalidChecksumException,
           IOException, RepositoryException {
        final FedoraBinary mockObject = (FedoraBinary)setResource(FedoraBinary.class);
        doReturn(mockObject).when(testObj).resource();
        testObj.createObject(null, null, null, null, null, null);
    }

    @Test(expected = ServerErrorException.class)
    public void testLDPRNotImplemented() throws MalformedRdfException, InvalidChecksumException,
            IOException {
        testObj.createObject(null, null, null, null, "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\"", null);
    }

    @Test(expected = ClientErrorException.class)
    public void testLDPRNotImplementedInvalidLink() throws MalformedRdfException, InvalidChecksumException,
           IOException {
        testObj.createObject(null, null, null, null, "Link: <http://www.w3.org/ns/ldp#Resource;rel=type", null);
    }

    @Test
    public void testGetSimpleContentType() {
        final MediaType mediaType = new MediaType("text", "plain", ImmutableMap.of("charset", "UTF-8"));
        final MediaType sanitizedMediaType = getSimpleContentType(mediaType);
        assertEquals("text/plain", sanitizedMediaType.toString());
    }

    @Test
    public void testSetUpJMSBaseURIs() throws RepositoryException {
        final ObservationManager mockManager = mock(ObservationManager.class);
        doReturn(mockManager).when(mockWorkspace).getObservationManager();
        final String json = "{\"baseUrl\":\"http://localhost/fcrepo\",\"userAgent\":\"Test UserAgent\"}";
        testObj.setUpJMSInfo(getUriInfoImpl(), mockHeaders);
        verify(mockManager).setUserData(eq(json));
    }

    @Test
    public void testSetUpJMSBaseURIsWithSystemProperty() throws RepositoryException {
        System.setProperty(BASEURL_PROP, "https://localhome:8443");

        final ObservationManager mockManager = mock(ObservationManager.class);
        doReturn(mockManager).when(mockWorkspace).getObservationManager();
        final String json = "{\"baseUrl\":\"https://localhome:8443/fcrepo\",\"userAgent\":\"Test UserAgent\"}";

        testObj.setUpJMSInfo(getUriInfoImpl(), mockHeaders);
        verify(mockManager).setUserData(eq(json));
    }
}
