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

import static com.hp.hpl.jena.graph.NodeFactory.createAnon;
import static javax.jcr.PropertyType.PATH;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.fcrepo.http.commons.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_OBJECT;
import static org.fcrepo.kernel.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.rdf.GraphProperties.PROBLEMS_MODEL_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionManager;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.http.commons.domain.Prefer;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.impl.FedoraResourceImpl;
import org.fcrepo.kernel.identifiers.PidMinter;
import org.fcrepo.kernel.impl.rdf.impl.ChildrenRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ContainerRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ParentRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ReferencesRdfContext;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.services.VersionService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.util.Context;


/**
 * <p>FedoraNodesTest class.</p>
 *
 * @author awoods
 */
public class FedoraNodesTest {

    FedoraNodes testObj;

    @Mock
    private ObjectService mockObjects;

    @Mock
    private NodeService mockNodes;

    @Mock
    private VersionService mockVersions;

    @Mock
    private Node mockNode;

    @Mock
    private NodeType mockNodeType;

    @Mock
    private DatastreamService mockDatastreams;

    @Mock
    private Request mockRequest;

    @Mock
    private FedoraResourceImpl mockResource;

    Session mockSession;

    @Mock
    private FedoraObject mockObject;

    @Mock
    private Dataset mockDataset;

    private RdfStream mockRdfStream = new RdfStream().topic(createAnon());

    private RdfStream mockRdfStream2 = new RdfStream().topic(createAnon());
    private RdfStream mockRdfStream3 = new RdfStream().topic(createAnon());

    @Mock
    private Model mockModel;

    @Mock
    private Context mockContext;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private PidMinter mockPidMinter;

    @Mock
    private Date mockDate;

    private UriInfo mockUriInfo;

    @Mock
    private Value mockValue;

    @Mock
    private ValueFactory mockValueFactory;

    private String path = "/some/path";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new FedoraNodes(path);

        setField(testObj, "request", mockRequest);
        setField(testObj, "servletResponse", mockResponse);
        setField(testObj, "uriInfo", mockUriInfo);
        setField(testObj, "datastreamService", mockDatastreams);
        setField(testObj, "nodeService", mockNodes);
        setField(testObj, "versionService", mockVersions);
        this.mockUriInfo = getUriInfoImpl();
        setField(testObj, "pidMinter", mockPidMinter);
        setField(testObj, "objectService", mockObjects);
        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);
        final Workspace mockWorkspace = mock(Workspace.class);
        when(mockWorkspace.getName()).thenReturn("default");
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        final VersionManager mockVM = mock(VersionManager.class);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVM);
        when(mockDate.getTime()).thenReturn(0L);
        when(mockNode.getPath()).thenReturn("/test/path");
        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockObject.getPath()).thenReturn("/test/path");
        when(mockObject.getEtagValue()).thenReturn("XYZ");
        when(mockNodeType.getName()).thenReturn("nt:folder");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);

    }

    @Test
    public void testCreateChildObject() throws Exception {

        setField(testObj, "pidMinter", mockPidMinter);
        final String childPath = path + "/a";
        when(mockNodes.exists(mockSession, path)).thenReturn(true);
        when(mockPidMinter.mintPid()).thenReturn("a");
        when(mockObjects.findOrCreateObject(mockSession, childPath)).thenReturn(mockObject);
        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockObject.getPath()).thenReturn(childPath);
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);
        when(mockValueFactory.createValue("a", PATH)).thenReturn(mockValue);
        when(mockNodes.getObject(mockSession, path)).thenReturn(mockResource);
        when(mockResource.hasContent()).thenReturn(false);

        final Response actual =
            testObj.createObject(FEDORA_OBJECT, null, null,
                                    null, null, null);
        assertNotNull(actual);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        assertTrue(actual.getEntity().toString().endsWith("a"));
        verify(mockObjects).findOrCreateObject(mockSession, childPath);
        verify(mockSession).save();
    }

    @Test
    public void testCreateObjectWithSparqlUpdate() throws Exception {

        setField(testObj, "pidMinter", mockPidMinter);
        final String childPath = path + "/a";
        when(mockNodes.exists(mockSession, path)).thenReturn(true);
        when(mockPidMinter.mintPid()).thenReturn("a");
        when(mockObjects.findOrCreateObject(mockSession, childPath)).thenReturn(mockObject);
        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn(childPath);
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);
        when(mockValueFactory.createValue("a", PATH)).thenReturn(mockValue);
        when(mockNodes.getObject(mockSession, path)).thenReturn(mockResource);
        when(mockResource.hasContent()).thenReturn(false);

        final InputStream mockStream =
                new ByteArrayInputStream("my-sparql-statement".getBytes());
        final Response actual = testObj.createObject(FEDORA_OBJECT, null, null,
                MediaType.valueOf("application/sparql-update"), null, mockStream);
        assertNotNull(actual);
        assertEquals(NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(mockObjects).findOrCreateObject(mockSession, childPath);
        verify(mockSession).save();
    }

    @Test
    public void testCreateObjectWithRDF() throws Exception {

        setField(testObj, "pidMinter", mockPidMinter);
        final String childPath = path + "/a";
        when(mockNodes.exists(mockSession, path)).thenReturn(true);
        when(mockPidMinter.mintPid()).thenReturn("a");
        when(mockObjects.findOrCreateObject(mockSession, childPath)).thenReturn(mockObject);
        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn(childPath);
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);
        when(mockValueFactory.createValue("a", PATH)).thenReturn(mockValue);
        when(mockNodes.getObject(mockSession, path)).thenReturn(mockResource);
        when(mockResource.hasContent()).thenReturn(false);

        final InputStream mockStream =
                new ByteArrayInputStream("<> <http://purl.org/dc/elements/1.1/title> 'foo'".getBytes());
        final Response actual = testObj.createObject(FEDORA_OBJECT, null, null,
                MediaType.valueOf("text/n3"), null, mockStream);
        assertNotNull(actual);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockObjects).findOrCreateObject(mockSession, childPath);
        verify(mockSession).save();
    }

    @Test
    public void testCreateObjectWithDatastream() throws Exception {

        setField(testObj, "pidMinter", mockPidMinter);
        final String childPath = path + "/a";
        when(mockNodes.exists(mockSession, path)).thenReturn(true);
        when(mockPidMinter.mintPid()).thenReturn("a");
        final Node contentNode = mock(Node.class);
        final Datastream mockDatastream = mock(Datastream.class);
        when(mockDatastreams.findOrCreateDatastream(mockSession, childPath)).thenReturn(mockDatastream);
        final FedoraBinary mockBinary = mock(FedoraBinary.class);
        when(mockDatastream.getBinary()).thenReturn(mockBinary);
        when(mockBinary.getPath()).thenReturn(childPath + "/jcr:content");
        when(mockDatastream.getNode()).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn(childPath);
        when(mockDatastream.getContentNode()).thenReturn(contentNode);
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);
        when(mockValueFactory.createValue("a", PATH)).thenReturn(mockValue);
        when(mockNodes.getObject(mockSession, path)).thenReturn(mockResource);
        when(mockResource.hasContent()).thenReturn(true);
        when(mockDatastream.getPath()).thenReturn(childPath);
        when(mockDatastream.getEtagValue()).thenReturn("");

        final InputStream mockStream =
                new ByteArrayInputStream("random-image-bytes".getBytes());
        final Response actual = testObj.createObject(FEDORA_DATASTREAM,
                "urn:sha1:ebd0438cfbab7365669a7f8a64379e93c8112490", "inline; filename=foo.tiff",
                MediaType.valueOf("image/tiff"), null, mockStream);
        assertNotNull(actual);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockBinary).setContent(mockStream,
                "image/tiff",
                new URI("urn:sha1:ebd0438cfbab7365669a7f8a64379e93c8112490"),
                "foo.tiff",
                mockDatastreams.getStoragePolicyDecisionPoint());
        verify(mockSession).save();
    }

    @Test
    public void testCreateChildObjectWithSlug() throws Exception {
        setField(testObj, "pidMinter", mockPidMinter);

        final String childPath = path + "/some-slug";
        when(mockNodes.exists(mockSession, path)).thenReturn(true);
        when(mockObjects.findOrCreateObject(mockSession, childPath)).thenReturn(mockObject);
        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);
        when(mockValueFactory.createValue("a", PATH)).thenReturn(mockValue);
        when(mockNodes.getObject(mockSession, path)).thenReturn(mockResource);
        when(mockResource.hasContent()).thenReturn(false);
        when(mockObject.getPath()).thenReturn(childPath);

        final Response actual =
            testObj.createObject(FEDORA_OBJECT, null, null,
                                    null, "some-slug", null);
        assertNotNull(actual);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        assertTrue(actual.getEntity().toString().endsWith("some-slug"));
        verify(mockObjects).findOrCreateObject(mockSession, childPath);
        verify(mockSession).save();
    }


    @Test
    public void testDeleteObject() throws RepositoryException {
        when(mockNodes.getObject(isA(Session.class), eq(path))).thenReturn(mockObject);
        when(mockObject.getEtagValue()).thenReturn("");

        final Response actual = testObj.deleteObject();

        assertNotNull(actual);
        assertEquals(NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(mockObject).delete();
        verify(mockSession).save();
    }

    @Test
    public void testDescribeObject() throws RepositoryException {
        when(mockDataset.getDefaultModel()).thenReturn(mockModel);
        when(mockDataset.getContext()).thenReturn(mockContext);
        when(mockObject.getLastModifiedDate()).thenReturn(mockDate);
        when(mockObject.getEtagValue()).thenReturn("");
        when(mockObject.getTriples(any(IdentifierTranslator.class), eq(PropertiesRdfContext.class)))
                .thenReturn(mockRdfStream);
        when(mockObject.getTriples(any(IdentifierTranslator.class), eq(ParentRdfContext.class)))
                .thenReturn(mockRdfStream2);
        when(mockObject.getTriples(any(IdentifierTranslator.class), eq(ChildrenRdfContext.class)))
                .thenReturn(mockRdfStream2);
        when(mockObject.getTriples(any(IdentifierTranslator.class), eq(ContainerRdfContext.class)))
                .thenReturn(mockRdfStream2);
        when(mockObject.getTriples(any(IdentifierTranslator.class),
                eq(ReferencesRdfContext.class))).thenReturn(mockRdfStream3);
        when(mockNodes.getObject(isA(Session.class), isA(String.class)))
                .thenReturn(mockObject);
        when(mockObject.getChildren()).thenReturn(Collections.<FedoraResource>emptyIterator());
        final RdfStream rdfStream =
            testObj.describe(null);
        assertEquals("Got wrong triples!", mockRdfStream.concat(mockRdfStream2).concat(mockRdfStream3),
                rdfStream);
        verify(mockResponse).addHeader("Accept-Patch", "application/sparql-update");
        verify(mockResponse).addHeader("Link", "<" + LDP_NAMESPACE + "Resource>;rel=\"type\"");

    }

    @Test
    public void testDescribeObjectNoInlining() throws RepositoryException, ParseException {
        when(mockDataset.getDefaultModel()).thenReturn(mockModel);
        when(mockDataset.getContext()).thenReturn(mockContext);

        when(mockObject.getEtagValue()).thenReturn("");
        when(mockObject.getLastModifiedDate()).thenReturn(mockDate);
        when(mockObject.getTriples(any(IdentifierTranslator.class), eq(PropertiesRdfContext.class))).thenReturn(
                mockRdfStream);

        when(mockObject.getTriples(any(IdentifierTranslator.class), eq(ParentRdfContext.class)))
                .thenReturn(mockRdfStream2);
        when(mockObject.getTriples(any(IdentifierTranslator.class), eq(ContainerRdfContext.class)))
                .thenReturn(mockRdfStream2);

        when(mockObject.getTriples(any(IdentifierTranslator.class),
                eq(ReferencesRdfContext.class))).thenReturn(mockRdfStream3);
        when(mockNodes.getObject(isA(Session.class), isA(String.class)))
            .thenReturn(mockObject);
        final Prefer prefer = new Prefer("return=representation;"
                                            + "include=\"http://www.w3.org/ns/ldp#PreferMinimalContainer\"");
        final RdfStream rdfStream =
            testObj.describe(prefer);
        assertEquals("Got wrong RDF!", mockRdfStream.concat(mockRdfStream2).concat(mockRdfStream3),
                rdfStream);

    }

    @Test
    public void testSparqlUpdate() throws RepositoryException, IOException {
        final InputStream mockStream =
                new ByteArrayInputStream("my-sparql-statement".getBytes());
        when(mockNodes.getObject(mockSession, path)).thenReturn(mockObject);
        when(mockObject.updatePropertiesDataset(any(IdentifierTranslator.class), any(String.class)))
            .thenReturn(mockDataset);
        when(mockObject.getEtagValue()).thenReturn("");

        when(mockObject.getLastModifiedDate()).thenReturn(Calendar.getInstance().getTime());
        when(mockDataset.getNamedModel(PROBLEMS_MODEL_NAME))
        .thenReturn(mockModel);
        when(mockModel.isEmpty()).thenReturn(true);
        testObj.updateSparql(mockStream);

        verify(mockObject).updatePropertiesDataset(any(IdentifierTranslator.class),
                eq("my-sparql-statement"));
        verify(mockSession).save();
        verify(mockSession).logout();
    }

    @Test(expected = BadRequestException.class)
    public void testSparqlUpdateNull() throws IOException, RepositoryException {
        testObj.updateSparql(null);
    }

    @Test(expected = BadRequestException.class)
    public void testSparqlUpdateEmpty() throws IOException, RepositoryException {
        final InputStream mockStream = new ByteArrayInputStream("".getBytes());

        testObj.updateSparql(mockStream);
    }

    @Test(expected = BadRequestException.class)
    public void testSparqlUpdateError() throws IOException, RepositoryException {
        final InputStream mockStream = new ByteArrayInputStream("my-sparql-statement".getBytes());
        final Exception ex = new RuntimeException(new PathNotFoundException("expected"));
        when(mockNodes.getObject(mockSession, path)).thenThrow(ex);

        testObj.updateSparql(mockStream);
    }

    @Test
    public void testReplaceRdf() throws Exception {
        when(mockNodes.exists(mockSession, path)).thenReturn(true);
        when(mockObject.getLastModifiedDate()).thenReturn(Calendar.getInstance().getTime());
        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockObject.getEtagValue()).thenReturn("");
        when(mockNode.getPath()).thenReturn(path);

        final InputStream mockStream =
            new ByteArrayInputStream("<a> <b> <c>".getBytes());
        when(mockNodes.getObject(mockSession, path)).thenReturn(mockObject);

        testObj.createOrReplaceObjectRdf(MediaType.valueOf("application/n3"),
                                         mockStream);
        verify(mockObject).replaceProperties(any(IdentifierTranslator.class), any(Model.class), any(RdfStream.class));
    }

    @Test
    public void testCopyObject() throws RepositoryException, URISyntaxException {

        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, path)).thenReturn(true);
        when(mockObject.getPath()).thenReturn(path);

        testObj.copyObject("http://localhost/fcrepo/bar");
        verify(mockNodes).copyObject(mockSession, path, "/bar");
    }

    @Test(expected = ClientErrorException.class)
    public void testCopyMissingObject() throws RepositoryException, URISyntaxException {

        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, path)).thenReturn(false);

        testObj.copyObject("http://localhost/fcrepo/bar");
    }

    @Test(expected = ServerErrorException.class)
    public void testCopyObjectWithBadDestination() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, path)).thenReturn(true);

        testObj.copyObject("http://somewhere/else/baz");

    }

    @Test(expected = WebApplicationException.class)
    public void testCopyObjectToExistingDestination() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, path)).thenReturn(true);
        doThrow(new RepositoryRuntimeException(new ItemExistsException()))
                .when(mockNodes).copyObject(mockSession, path, "/baz");

        final Response response = testObj.copyObject("http://localhost/fcrepo/baz");

        assertEquals(PRECONDITION_FAILED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testMoveObject() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockNodes.getObject(isA(Session.class), isA(String.class)))
            .thenReturn(mockObject);
        when(mockObject.getEtagValue()).thenReturn("");
        when(mockObject.getPath()).thenReturn(path);

        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, path)).thenReturn(true);

        testObj.moveObject("http://localhost/fcrepo/bar");
        verify(mockNodes).moveObject(mockSession, path, "/bar");
    }

    @Test(expected = ClientErrorException.class)
    public void testMoveMissingObject() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, path)).thenReturn(false);
        when(mockNodes.getObject(isA(Session.class), isA(String.class)))
            .thenReturn(mockObject);
        when(mockObject.getEtagValue()).thenReturn("");

        testObj.moveObject("http://localhost/fcrepo/bar");
    }

    @Test(expected = WebApplicationException.class)
    public void testMoveObjectToExistingDestination() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, path)).thenReturn(true);
        when(mockNodes.getObject(isA(Session.class), isA(String.class)))
            .thenReturn(mockObject);
        when(mockObject.getEtagValue()).thenReturn("");
        doThrow(new RepositoryRuntimeException(new ItemExistsException()))
                .when(mockNodes).moveObject(mockSession, path, "/baz");

        final Response response = testObj.moveObject("http://localhost/fcrepo/baz");

        assertEquals(PRECONDITION_FAILED.getStatusCode(), response.getStatus());
    }

    @Test(expected = ServerErrorException.class)
    public void testMoveObjectWithBadDestination() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.getObject(isA(Session.class), isA(String.class)))
            .thenReturn(mockObject);
        when(mockNodes.exists(mockSession, path)).thenReturn(true);
        when(mockObject.getEtagValue()).thenReturn("");

        testObj.moveObject("http://somewhere/else/baz");
    }

    @Test
    public void testOptions() throws RepositoryException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockNodes.getObject(isA(Session.class), isA(String.class))).thenReturn(mockObject);
        when(mockObject.getEtagValue()).thenReturn("");
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, path)).thenReturn(true);

        final Response response = testObj.options();
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testHead() throws RepositoryException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockNodes.getObject(isA(Session.class), isA(String.class))).thenReturn(mockObject);
        when(mockObject.getEtagValue()).thenReturn("");
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, path)).thenReturn(true);

        final Response response = testObj.head();
        assertEquals(SC_OK, response.getStatus());
    }

    @Test(expected = WebApplicationException.class)
    public void testCheckJcrNamespace() {
        final List<PathSegment> pathList = createPathList("anypath");
        pathList.add(createPathList("jcr:path").get(0));
        testObj.throwIfPathIncludesJcr(pathList);
    }

}
