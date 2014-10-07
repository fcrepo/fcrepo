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
package org.fcrepo.http.api;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import org.apache.commons.io.IOUtils;
import org.fcrepo.http.commons.api.rdf.UriAwareIdentifierConverter;
import org.fcrepo.http.commons.domain.Prefer;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.services.VersionService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.io.InputStream;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES_TYPE;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.fcrepo.kernel.RdfLexicon.INBOUND_REFERENCES;
import static org.fcrepo.kernel.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.rdf.GraphProperties.PROBLEMS_MODEL_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * @author cabeer
 */
public class FedoraLdpTest {

    private String path = "/some/path";
    private String binaryPath = "/some/binary/path";
    private String binaryDescriptionPath = "/some/other/path";
    private FedoraLdp testObj;

    @Mock
    private Request mockRequest;

    private HttpServletResponse mockResponse;

    private Session mockSession;

    @Mock
    private Node mockNode;

    @Mock
    private FedoraObject mockObject;

    @Mock
    private Node mockObjectNode;

    @Mock
    private Datastream mockDatastream;

    @Mock
    private Node mockDatastreamNode;

    @Mock
    private FedoraBinary mockBinary;

    @Mock
    private Node mockBinaryNode;

    private IdentifierConverter<Resource, Node> identifierConverter;

    @Mock
    private NodeService mockNodeService;

    @Mock
    private ObjectService mockObjectService;

    @Mock
    private DatastreamService mockDatastreamService;

    @Mock
    private VersionService mockVersionService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = spy(new FedoraLdp(path));

        mockResponse = new MockHttpServletResponse();

        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);

        identifierConverter = new UriAwareIdentifierConverter(mockSession,
                UriBuilder.fromUri("http://localhost/fcrepo/{path: .*}"));

        setField(testObj, "request", mockRequest);
        setField(testObj, "servletResponse", mockResponse);
        setField(testObj, "uriInfo", getUriInfoImpl());
        setField(testObj, "identifierTranslator", identifierConverter);
        setField(testObj, "nodeService", mockNodeService);
        setField(testObj, "objectService", mockObjectService);
        setField(testObj, "datastreamService", mockDatastreamService);
        setField(testObj, "versionService", mockVersionService);

        when(mockObject.getNode()).thenReturn(mockObjectNode);
        when(mockObject.getEtagValue()).thenReturn("");
        when(mockObjectNode.getPath()).thenReturn(path);

        when(mockDatastream.getNode()).thenReturn(mockDatastreamNode);
        when(mockDatastream.getEtagValue()).thenReturn("");
        when(mockDatastreamNode.getPath()).thenReturn(binaryDescriptionPath);
        when(mockDatastream.getBinary()).thenReturn(mockBinary);

        when(mockBinary.getNode()).thenReturn(mockBinaryNode);
        when(mockBinary.getEtagValue()).thenReturn("");
        when(mockBinaryNode.getPath()).thenReturn(binaryPath);
        when(mockBinary.getDescription()).thenReturn(mockDatastream);
    }

    private FedoraResource setResource(final Class<? extends FedoraResource> klass) throws RepositoryException {
        final FedoraResource mockResource = mock(klass);

        doReturn(mockResource).when(testObj).resource();
        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn(path);
        when(mockResource.getEtagValue()).thenReturn("");
        when(mockResource.getTriples(eq(identifierConverter), any(Class.class))).thenAnswer(new Answer<RdfStream>() {
            @Override
            public RdfStream answer(final InvocationOnMock invocationOnMock) throws Throwable {
                return new RdfStream(Triple.create(NodeFactory.createURI(invocationOnMock.getMock().toString()),
                                                   NodeFactory.createURI("called"),
                                                   NodeFactory.createLiteral(
                                                           invocationOnMock.getArguments()[1].toString())
                        ));
            }
        });

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
        setResource(FedoraObject.class);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP DirectContainer",
                mockResponse.getHeaders("Link").contains("<" + LDP_NAMESPACE + "DirectContainer>;rel=\"type\""));
        assertTrue("Should advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertTrue("Should advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));
    }

    @Test
    public void testHeadWithBinary() throws Exception {
        final FedoraBinary mockResource = (FedoraBinary)setResource(FedoraBinary.class);
        when(mockResource.getDescription()).thenReturn(mockDatastream);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP NonRDFSource",
                mockResponse.getHeaders("Link").contains("<" + LDP_NAMESPACE + "NonRDFSource>;rel=\"type\""));
        assertFalse("Should not advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertFalse("Should not advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));
        assertTrue("Should contain a link to the binary description",
                mockResponse.getHeaders("Link")
                        .contains("<" + identifierConverter.toDomain(binaryDescriptionPath) + ">;rel=\"describedby\""));
    }

    @Test
    public void testHeadWithBinaryDescription() throws Exception {
        final Datastream mockResource = (Datastream)setResource(Datastream.class);
        when(mockResource.getBinary()).thenReturn(mockBinary);
        final Response actual = testObj.head();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP RDFSource",
                mockResponse.getHeaders("Link").contains("<" + LDP_NAMESPACE + "RDFSource>;rel=\"type\""));
        assertFalse("Should not advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertTrue("Should advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));
        assertTrue("Should contain a link to the binary",
                mockResponse.getHeaders("Link")
                        .contains("<" + identifierConverter.toDomain(binaryPath) + ">;rel=\"describes\""));
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
        setResource(FedoraObject.class);
        final Response actual = testObj.options();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertTrue("Should advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));
    }

    @Test
    public void testOptionWithBinary() throws Exception {
        final FedoraBinary mockResource = (FedoraBinary)setResource(FedoraBinary.class);
        when(mockResource.getDescription()).thenReturn(mockDatastream);
        final Response actual = testObj.options();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertFalse("Should not advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertFalse("Should not advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));
        assertTrue("Should contain a link to the binary description",
                mockResponse.getHeaders("Link")
                        .contains("<" + identifierConverter.toDomain(binaryDescriptionPath) + ">;rel=\"describedby\""));
    }

    @Test
    public void testOptionWithBinaryDescription() throws Exception {
        final Datastream mockResource = (Datastream)setResource(Datastream.class);
        when(mockResource.getBinary()).thenReturn(mockBinary);
        final Response actual = testObj.options();
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertFalse("Should not advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertTrue("Should advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));
        assertTrue("Should contain a link to the binary",
                mockResponse.getHeaders("Link")
                        .contains("<" + identifierConverter.toDomain(binaryPath) + ">;rel=\"describes\""));
    }


    @Test
    public void testGet() throws Exception {
        setResource(FedoraResource.class);
        final Response actual = testObj.describe(new Prefer(""), null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should have a Link header", mockResponse.containsHeader("Link"));
        assertTrue("Should have an Allow header", mockResponse.containsHeader("Allow"));
        assertTrue("Should be an LDP Resource",
                mockResponse.getHeaders("Link").contains("<" + LDP_NAMESPACE + "Resource>;rel=\"type\""));

        final RdfStream entity = (RdfStream) actual.getEntity();
        final Model model = entity.asModel();
        final List<String> rdfNodes = Lists.transform(Lists.newArrayList(model.listObjects()),
                new Function<RDFNode, String>() {
            @Override
            public String apply(final RDFNode input) {
                return input.toString();
            }
        });
        assertTrue("Expected RDF contexts missing", rdfNodes.containsAll(ImmutableSet.of(
                "class org.fcrepo.kernel.impl.rdf.impl.LdpContainerRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.ReferencesRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.TypeRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.ContainerRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.ChildrenRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.AclRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.ParentRdfContext"
        )));

    }

    @Test
    public void testGetWithObject() throws Exception {
        setResource(FedoraObject.class);
        final Response actual = testObj.describe(new Prefer(""), null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP DirectContainer",
                mockResponse.getHeaders("Link").contains("<" + LDP_NAMESPACE + "DirectContainer>;rel=\"type\""));
        assertTrue("Should advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertTrue("Should advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));

        final RdfStream entity = (RdfStream) actual.getEntity();
        final Model model = entity.asModel();
        final List<String> rdfNodes = Lists.transform(Lists.newArrayList(model.listObjects()),
                new Function<RDFNode, String>() {
                    @Override
                    public String apply(final RDFNode input) {
                        return input.toString();
                    }
                });
        assertTrue("Expected RDF contexts missing", rdfNodes.containsAll(ImmutableSet.of(
                "class org.fcrepo.kernel.impl.rdf.impl.LdpContainerRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.ReferencesRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.TypeRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.ContainerRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.ChildrenRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.AclRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.ParentRdfContext"
        )));

    }

    @Test
    public void testGetWithObjectPreferMinimal() throws Exception {
        setResource(FedoraObject.class);
        final Response actual = testObj.describe(new Prefer("return=minimal"), null);
        assertEquals(OK.getStatusCode(), actual.getStatus());

        final RdfStream entity = (RdfStream) actual.getEntity();
        final Model model = entity.asModel();
        final List<String> rdfNodes = Lists.transform(Lists.newArrayList(model.listObjects()),
                new Function<RDFNode, String>() {
                    @Override
                    public String apply(final RDFNode input) {
                        return input.toString();
                    }
                });
        assertTrue("Expected RDF contexts missing", rdfNodes.containsAll(ImmutableSet.of(
                "class org.fcrepo.kernel.impl.rdf.impl.TypeRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.AclRdfContext"
        )));

        assertFalse("Included non-minimal contexts",
                rdfNodes.contains("class org.fcrepo.kernel.impl.rdf.impl.LdpContainerRdfContext"));

        assertFalse("Included non-minimal contexts",
                rdfNodes.contains("class org.fcrepo.kernel.impl.rdf.impl.ChildrenRdfContext"));

    }

    @Test
    public void testGetWithObjectOmitContainment() throws Exception {
        setResource(FedoraObject.class);
        final Response actual = testObj.describe(
                new Prefer("return=representation; omit=\"" + LDP_NAMESPACE + "PreferContainment\""),
                null);
        assertEquals(OK.getStatusCode(), actual.getStatus());

        final RdfStream entity = (RdfStream) actual.getEntity();
        final Model model = entity.asModel();
        final List<String> rdfNodes = Lists.transform(Lists.newArrayList(model.listObjects()),
                new Function<RDFNode, String>() {
                    @Override
                    public String apply(final RDFNode input) {
                        return input.toString();
                    }
                });
        assertTrue("Should include membership contexts",
                rdfNodes.contains("class org.fcrepo.kernel.impl.rdf.impl.LdpContainerRdfContext"));

        assertFalse("Should not include containment contexts",
                rdfNodes.contains("class org.fcrepo.kernel.impl.rdf.impl.ChildrenRdfContext"));

    }

    @Test
    public void testGetWithObjectOmitMembership() throws Exception {
        setResource(FedoraObject.class);
        final Response actual = testObj.describe(
                new Prefer("return=representation; omit=\"" + LDP_NAMESPACE + "PreferMembership\""),
                null);
        assertEquals(OK.getStatusCode(), actual.getStatus());

        final RdfStream entity = (RdfStream) actual.getEntity();
        final Model model = entity.asModel();
        final List<String> rdfNodes = Lists.transform(Lists.newArrayList(model.listObjects()),
                new Function<RDFNode, String>() {
                    @Override
                    public String apply(final RDFNode input) {
                        return input.toString();
                    }
                });
        assertFalse("Should not include membership contexts",
                rdfNodes.contains("class org.fcrepo.kernel.impl.rdf.impl.LdpContainerRdfContext"));

        assertTrue("Should include containment contexts",
                rdfNodes.contains("class org.fcrepo.kernel.impl.rdf.impl.ChildrenRdfContext"));

    }

    @Test
    public void testGetWithObjectOmitReferences() throws Exception {
        setResource(FedoraObject.class);
        final Response actual = testObj.describe(
                new Prefer("return=representation; omit=\"" + INBOUND_REFERENCES + "\""),
                null);
        assertEquals(OK.getStatusCode(), actual.getStatus());

        final RdfStream entity = (RdfStream) actual.getEntity();
        final Model model = entity.asModel();
        final List<String> rdfNodes = Lists.transform(Lists.newArrayList(model.listObjects()),
                new Function<RDFNode, String>() {
                    @Override
                    public String apply(final RDFNode input) {
                        return input.toString();
                    }
                });

        assertFalse("Should not include references contexts",
                rdfNodes.contains("class org.fcrepo.kernel.impl.rdf.impl.ReferencesRdfContext"));

    }

    @Test
    public void testGetWithBinary() throws Exception {
        final FedoraBinary mockResource = (FedoraBinary)setResource(FedoraBinary.class);
        when(mockResource.getDescription()).thenReturn(mockDatastream);
        when(mockResource.getMimeType()).thenReturn("text/plain");
        when(mockResource.getContent()).thenReturn(toInputStream("xyz"));
        final Response actual = testObj.describe(new Prefer(""), null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP NonRDFSource",
                mockResponse.getHeaders("Link").contains("<" + LDP_NAMESPACE + "NonRDFSource>;rel=\"type\""));
        assertFalse("Should not advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertFalse("Should not advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));
        assertTrue("Should contain a link to the binary description",
                mockResponse.getHeaders("Link")
                        .contains("<" + identifierConverter.toDomain(binaryDescriptionPath) + ">;rel=\"describedby\""));
        assertTrue(IOUtils.toString((InputStream)actual.getEntity()).equals("xyz"));
    }

    @Test
    public void testGetWithBinaryDescription() throws Exception {
        final Datastream mockResource = (Datastream)setResource(Datastream.class);
        when(mockResource.getBinary()).thenReturn(mockBinary);
        final Response actual = testObj.describe(new Prefer(""), null);
        assertEquals(OK.getStatusCode(), actual.getStatus());
        assertTrue("Should be an LDP RDFSource",
                mockResponse.getHeaders("Link").contains("<" + LDP_NAMESPACE + "RDFSource>;rel=\"type\""));
        assertFalse("Should not advertise Accept-Post flavors", mockResponse.containsHeader("Accept-Post"));
        assertTrue("Should advertise Accept-Patch flavors", mockResponse.containsHeader("Accept-Patch"));
        assertTrue("Should contain a link to the binary",
                mockResponse.getHeaders("Link")
                        .contains("<" + identifierConverter.toDomain(binaryPath) + ">;rel=\"describes\""));

        final RdfStream entity = (RdfStream) actual.getEntity();
        final Model model = entity.asModel();
        final List<String> rdfNodes = Lists.transform(Lists.newArrayList(model.listObjects()),
                new Function<RDFNode, String>() {
                    @Override
                    public String apply(final RDFNode input) {
                        return input.toString();
                    }
                });
        assertTrue("Expected RDF contexts missing", rdfNodes.containsAll(ImmutableSet.of(
                "class org.fcrepo.kernel.impl.rdf.impl.LdpContainerRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.ReferencesRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.TypeRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.ContainerRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.ChildrenRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.AclRdfContext",
                "class org.fcrepo.kernel.impl.rdf.impl.ParentRdfContext"
        )));

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
        when(mockObject.isNew()).thenReturn(true);

        when(mockNodeService.exists(mockSession, "/some/path")).thenReturn(false);
        when(mockObjectService.findOrCreateObject(mockSession, "/some/path")).thenReturn(mockObject);

        final Response actual = testObj.createOrReplaceObjectRdf(null, null, null, null);

        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockVersionService).nodeUpdated(mockObjectNode);
    }

    @Test
    public void testPutNewObjectWithRdf() throws Exception {

        setField(testObj, "externalPath", "some/path");
        when(mockObject.isNew()).thenReturn(true);

        when(mockNodeService.exists(mockSession, "/some/path")).thenReturn(false);
        when(mockObjectService.findOrCreateObject(mockSession, "/some/path")).thenReturn(mockObject);

        final Response actual = testObj.createOrReplaceObjectRdf(NTRIPLES_TYPE,
                toInputStream("_:a <info:x> _:c ."), null, null);

        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockVersionService).nodeUpdated(mockObjectNode);
        verify(mockObject).replaceProperties(eq(identifierConverter), any(Model.class), any(RdfStream.class));
    }

    @Test
    public void testPutNewBinary() throws Exception {
        setField(testObj, "externalPath", "some/path");
        when(mockBinary.isNew()).thenReturn(true);

        when(mockNodeService.exists(mockSession, "/some/path")).thenReturn(false);
        when(mockDatastreamService.findOrCreateDatastream(mockSession, "/some/path")).thenReturn(mockDatastream);

        final Response actual = testObj.createOrReplaceObjectRdf(TEXT_PLAIN_TYPE,
                toInputStream("xyz"), null, null);

        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockVersionService).nodeUpdated(mockBinaryNode);
    }

    @Test
    public void testPutReplaceRdfObject() throws Exception {

        setField(testObj, "externalPath", "some/path");
        final FedoraObject mockObject = (FedoraObject)setResource(FedoraObject.class);
        doReturn(mockObject).when(testObj).resource();
        when(mockObject.isNew()).thenReturn(false);

        when(mockNodeService.exists(mockSession, "/some/path")).thenReturn(true);
        when(mockObjectService.findOrCreateObject(mockSession, "/some/path")).thenReturn(mockObject);

        final Response actual = testObj.createOrReplaceObjectRdf(NTRIPLES_TYPE,
                toInputStream("_:a <info:x> _:c ."), null, null);

        assertEquals(NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(mockVersionService).nodeUpdated(mockObject.getNode());
        verify(mockObject).replaceProperties(eq(identifierConverter), any(Model.class), any(RdfStream.class));
    }

    @Test
    public void testPatchObject() throws Exception {

        final FedoraResource mockObject = setResource(FedoraObject.class);

        final Dataset dataset = mock(Dataset.class);
        final Model mockModel = mock(Model.class);
        when(dataset.getNamedModel(PROBLEMS_MODEL_NAME)).thenReturn(mockModel);
        when(mockModel.isEmpty()).thenReturn(true);
        when(mockObject.updatePropertiesDataset(identifierConverter, "xyz")).thenReturn(dataset);
        testObj.updateSparql(toInputStream("xyz"));
        verify(mockVersionService).nodeUpdated(mockObject.getNode());
    }

    @Test
    public void testPatchBinaryDescription() throws Exception {

        final Datastream mockObject = (Datastream)setResource(Datastream.class);
        doReturn(mockObject).when(testObj).resource();
        when(mockObject.getContentNode()).thenReturn(mockBinaryNode);

        final Dataset dataset = mock(Dataset.class);
        final Model mockModel = mock(Model.class);
        when(dataset.getNamedModel(PROBLEMS_MODEL_NAME)).thenReturn(mockModel);
        when(mockModel.isEmpty()).thenReturn(true);
        when(mockObject.updatePropertiesDataset(identifierConverter, "xyz")).thenReturn(dataset);
        testObj.updateSparql(toInputStream("xyz"));
        verify(mockVersionService).nodeUpdated(mockObject.getNode());
        verify(mockVersionService).nodeUpdated(mockObject.getContentNode());
    }

    @Test(expected = BadRequestException.class)
    public void testPatchWithoutContent() throws Exception {
        testObj.updateSparql(null);
    }

    @Test(expected = BadRequestException.class)
    public void testPatchWithMissingContent() throws Exception {
        setResource(FedoraObject.class);

        testObj.updateSparql(toInputStream(""));
    }

    @Test(expected = BadRequestException.class)
    public void testPatchBinary() throws Exception {
        setResource(FedoraBinary.class);

        testObj.updateSparql(toInputStream(""));
    }

    @Test
    public void testCreateNewObject() throws Exception {

        setResource(FedoraObject.class);

        when(mockObjectService.findOrCreateObject(mockSession, "/b")).thenReturn(mockObject);

        final Response actual = testObj.createObject(null, null, null, null, "b", null);

        assertEquals(CREATED.getStatusCode(), actual.getStatus());
    }

    @Test
    public void testCreateNewObjectWithSparql() throws Exception {

        setResource(FedoraObject.class);

        when(mockObjectService.findOrCreateObject(mockSession, "/b")).thenReturn(mockObject);

        final Response actual = testObj.createObject(null, null, null,
                MediaType.valueOf(contentTypeSPARQLUpdate), "b", toInputStream("x"));

        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockObject).updatePropertiesDataset(identifierConverter, "x");
    }

    @Test
    public void testCreateNewObjectWithRdf() throws Exception {

        setResource(FedoraObject.class);

        when(mockObjectService.findOrCreateObject(mockSession, "/b")).thenReturn(mockObject);

        final Response actual = testObj.createObject(null, null, null, NTRIPLES_TYPE, "b",
                toInputStream("_:a <info:b> _:c ."));

        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockObject).replaceProperties(eq(identifierConverter), any(Model.class), any(RdfStream.class));
    }


    @Test
    public void testCreateNewBinary() throws Exception {

        setResource(FedoraObject.class);

        when(mockDatastreamService.findOrCreateDatastream(mockSession, "/b")).thenReturn(mockDatastream);

        final InputStream content = toInputStream("x");
        final Response actual = testObj.createObject(null, null, null, APPLICATION_OCTET_STREAM_TYPE, "b",
                content);

        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockBinary).setContent(content, APPLICATION_OCTET_STREAM, null, "", null);
    }

    @Test(expected = ClientErrorException.class)
    public void testPostToBinary() throws Exception {
        final FedoraBinary mockObject = (FedoraBinary)setResource(FedoraBinary.class);
        doReturn(mockObject).when(testObj).resource();

        testObj.createObject(null, null, null, null, null, null);

    }
}