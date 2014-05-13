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
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_OK;
import static org.fcrepo.http.commons.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionManager;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.http.commons.domain.Prefer;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.FedoraResourceImpl;
import org.fcrepo.kernel.identifiers.PidMinter;
import org.fcrepo.kernel.rdf.HierarchyRdfContextOptions;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.services.VersionService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.util.Context;


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

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new FedoraNodes();
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
        when(mockObject.getEtagValue()).thenReturn("XYZ");
        when(mockNodeType.getName()).thenReturn("nt:folder");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);

    }

    @Ignore/*(expected = WebApplicationException.class)*/
    public void testCreateObjectWithBadPath() throws Exception {
        final String path = "/does/not/exist";
        when(mockNodes.exists(mockSession, path)).thenReturn(false);
        testObj.createObject(createPathList(path), null, null, null, null, null, mockResponse, getUriInfoImpl(), null);
    }

    @Test
    public void testCreateChildObject() throws Exception {

        setField(testObj, "pidMinter", mockPidMinter);
        final String pid = "testCreateChildObject";
        final String path = "/" + pid + "/a";
        when(mockNodes.exists(mockSession, "/" + pid)).thenReturn(true);
        when(mockPidMinter.mintPid()).thenReturn("a");
        when(mockObjects.createObject(mockSession, path)).thenReturn(mockObject);
        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn(path);
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);
        when(mockValueFactory.createValue("a", PATH)).thenReturn(mockValue);
        when(mockNodes.getObject(mockSession, "/" + pid)).thenReturn(mockResource);
        when(mockResource.hasContent()).thenReturn(false);

        final Response actual =
            testObj.createObject(createPathList(pid), FEDORA_OBJECT, null, null,
                                    null, null, mockResponse, getUriInfoImpl(), null);
        assertNotNull(actual);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        assertTrue(actual.getEntity().toString().endsWith("a"));
        verify(mockObjects).createObject(mockSession, path);
        verify(mockSession).save();
    }

    @Test
    public void testCreateObjectWithSparqlUpdate() throws Exception {

        setField(testObj, "pidMinter", mockPidMinter);
        final String pid = "testCreateObjectWithSparqlUpdate";
        final String path = "/" + pid + "/a";
        when(mockNodes.exists(mockSession, "/" + pid)).thenReturn(true);
        when(mockPidMinter.mintPid()).thenReturn("a");
        when(mockObjects.createObject(mockSession, path)).thenReturn(mockObject);
        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn(path);
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);
        when(mockValueFactory.createValue("a", PATH)).thenReturn(mockValue);
        when(mockNodes.getObject(mockSession, "/" + pid)).thenReturn(mockResource);
        when(mockResource.hasContent()).thenReturn(false);

        final InputStream mockStream =
                new ByteArrayInputStream("my-sparql-statement".getBytes());
        final Response actual = testObj.createObject(createPathList(pid), FEDORA_OBJECT, null, null,
                MediaType.valueOf("application/sparql-update"), null, mockResponse, getUriInfoImpl(), mockStream);
        assertNotNull(actual);
        assertEquals(NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(mockObjects).createObject(mockSession, path);
        verify(mockSession).save();
    }

    @Test
    public void testCreateObjectWithRDF() throws Exception {

        setField(testObj, "pidMinter", mockPidMinter);
        final String pid = "testCreateObjectWithSparqlUpdate";
        final String path = "/" + pid + "/a";
        when(mockNodes.exists(mockSession, "/" + pid)).thenReturn(true);
        when(mockPidMinter.mintPid()).thenReturn("a");
        when(mockObjects.createObject(mockSession, path)).thenReturn(mockObject);
        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn(path);
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);
        when(mockValueFactory.createValue("a", PATH)).thenReturn(mockValue);
        when(mockNodes.getObject(mockSession, "/" + pid)).thenReturn(mockResource);
        when(mockResource.hasContent()).thenReturn(false);

        final InputStream mockStream =
                new ByteArrayInputStream("<> <http://purl.org/dc/elements/1.1/title> 'foo'".getBytes());
        final Response actual = testObj.createObject(createPathList(pid), FEDORA_OBJECT, null, null,
                MediaType.valueOf("text/n3"), null, mockResponse, getUriInfoImpl(), mockStream);
        assertNotNull(actual);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockObjects).createObject(mockSession, path);
        verify(mockSession).save();
    }

    @Test
    public void testCreateObjectWithDatastream() throws Exception {

        setField(testObj, "pidMinter", mockPidMinter);
        final String pid = "testCreateObjectWithSparqlUpdate";
        final String path = "/" + pid + "/a";
        when(mockNodes.exists(mockSession, "/" + pid)).thenReturn(true);
        when(mockPidMinter.mintPid()).thenReturn("a");
        final Node contentNode = mock(Node.class);
        final Datastream mockDatastream = mock(Datastream.class);
        when(mockDatastreams.createDatastream(mockSession, path)).thenReturn(mockDatastream);
        when(mockDatastream.getNode()).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn(path);
        when(mockDatastream.getContentNode()).thenReturn(contentNode);
        when(contentNode.getPath()).thenReturn(path + "/fcr:content");
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);
        when(mockValueFactory.createValue("a", PATH)).thenReturn(mockValue);
        when(mockNodes.getObject(mockSession, "/" + pid)).thenReturn(mockResource);
        when(mockResource.hasContent()).thenReturn(true);
        when(mockDatastream.getEtagValue()).thenReturn("");

        final InputStream mockStream =
                new ByteArrayInputStream("random-image-bytes".getBytes());
        final Response actual = testObj.createObject(createPathList(pid), FEDORA_DATASTREAM,
                "urn:sha1:ebd0438cfbab7365669a7f8a64379e93c8112490", "inline; filename=foo.tiff",
                MediaType.valueOf("image/tiff"), null, mockResponse, getUriInfoImpl(), mockStream);
        assertNotNull(actual);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockDatastreams).createDatastream(mockSession, path);
        verify(mockSession).save();
    }

    @Test
    public void testCreateChildObjectWithSlug() throws Exception {
        setField(testObj, "pidMinter", mockPidMinter);

        final String pid = "testCreateChildObjectWithSlug";
        final String path = "/" + pid + "/some-slug";
        when(mockNodes.exists(mockSession, "/" + pid)).thenReturn(true);
        when(mockObjects.createObject(mockSession, path)).thenReturn(mockObject);
        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn(path);
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);
        when(mockValueFactory.createValue("a", PATH)).thenReturn(mockValue);
        when(mockNodes.getObject(mockSession, "/" + pid)).thenReturn(mockResource);
        when(mockResource.hasContent()).thenReturn(false);

        final Response actual =
            testObj.createObject(createPathList(pid), FEDORA_OBJECT, null, null,
                                    null, "some-slug", mockResponse, getUriInfoImpl(), null);
        assertNotNull(actual);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        assertTrue(actual.getEntity().toString().endsWith("some-slug"));
        verify(mockObjects).createObject(mockSession, path);
        verify(mockSession).save();
    }


    @Ignore
    public void testDeleteObject() throws RepositoryException {
        final String pid = "testObject";
        final String path = "/" + pid;
        when(mockNodes.getObject(isA(Session.class), isA(String.class)))
            .thenReturn(mockObject);
        when(mockObject.getEtagValue()).thenReturn("");

        final Response actual = testObj.deleteObject(createPathList(pid), mockRequest);

        assertNotNull(actual);
        assertEquals(NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(mockNodes).deleteObject(mockSession, path);
        verify(mockSession).save();
    }

    @Ignore
    public void testDescribeObject() throws RepositoryException {
        final String pid = "FedoraObjectsRdfTest1";
        final String path = "/" + pid;

        when(mockDataset.getDefaultModel()).thenReturn(mockModel);
        when(mockDataset.getContext()).thenReturn(mockContext);
        when(mockObject.getLastModifiedDate()).thenReturn(mockDate);
        when(mockObject.getEtagValue()).thenReturn("");
        when(mockObject.getTriples(any(IdentifierTranslator.class))).thenReturn(
                mockRdfStream);
        when(mockObject.getHierarchyTriples(any(IdentifierTranslator.class),
                                               any(HierarchyRdfContextOptions.class))).thenReturn(
                mockRdfStream2);
        when(mockNodes.getObject(isA(Session.class), isA(String.class)))
                .thenReturn(mockObject);
        final Request mockRequest = mock(Request.class);
        final RdfStream rdfStream =
            testObj.describe(createPathList(path), 0, -2, null, mockRequest,
                    mockResponse, mockUriInfo);
        assertEquals("Got wrong triples!", mockRdfStream.concat(mockRdfStream2),
                rdfStream);
        verify(mockResponse).addHeader("Accept-Patch", "application/sparql-update");
        verify(mockResponse).addHeader("Link", "<" + LDP_NAMESPACE + "Resource>;rel=\"type\"");

    }

    @Ignore
    public void testDescribeObjectNoInlining() throws RepositoryException, ParseException {
        final String pid = "FedoraObjectsRdfTest1";
        final String path = "/" + pid;

        when(mockDataset.getDefaultModel()).thenReturn(mockModel);
        when(mockDataset.getContext()).thenReturn(mockContext);

        when(mockObject.getEtagValue()).thenReturn("");
        when(mockObject.getLastModifiedDate()).thenReturn(mockDate);
        when(mockObject.getTriples(any(IdentifierTranslator.class))).thenReturn(
                mockRdfStream);
        when(mockObject.getHierarchyTriples(any(IdentifierTranslator.class),
                                               any(HierarchyRdfContextOptions.class))).thenReturn(mockRdfStream2);
        when(mockNodes.getObject(isA(Session.class), isA(String.class)))
            .thenReturn(mockObject);
        final Request mockRequest = mock(Request.class);
        final Prefer prefer = new Prefer("return=representation;"
                                            + "include=\"http://www.w3.org/ns/ldp#PreferEmptyContainer\"");
        final RdfStream rdfStream =
            testObj.describe(createPathList(path), 0, -1, prefer, mockRequest, mockResponse, mockUriInfo);
        assertEquals("Got wrong RDF!", mockRdfStream.concat(mockRdfStream2),
                rdfStream);

    }

    @Ignore
    public void testSparqlUpdate() throws RepositoryException, IOException {
        final String pid = "FedoraObjectsRdfTest1";
        final String path = "/" + pid;
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
        testObj.updateSparql(createPathList(pid), getUriInfoImpl(), mockStream, mockRequest, mockResponse);

        verify(mockObject).updatePropertiesDataset(any(IdentifierTranslator.class),
                eq("my-sparql-statement"));
        verify(mockSession).save();
        verify(mockSession).logout();
    }

    @Ignore
    public void testReplaceRdf() throws Exception {
        final String pid = "FedoraObjectsRdfTest1";
        final String path = "/" + pid;
        when(mockNodes.exists(mockSession, path)).thenReturn(true);
        when(mockObject.getLastModifiedDate()).thenReturn(Calendar.getInstance().getTime());
        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockObject.getEtagValue()).thenReturn("");
        when(mockNode.getPath()).thenReturn(path);

        final InputStream mockStream =
            new ByteArrayInputStream("<a> <b> <c>".getBytes());
        when(mockNodes.getObject(mockSession, path)).thenReturn(mockObject);

        testObj.createOrReplaceObjectRdf(createPathList(pid), getUriInfoImpl(), MediaType.valueOf("application/n3"), mockStream, mockRequest, mockResponse);
        verify(mockObject).replaceProperties(any(IdentifierTranslator.class), any(Model.class));
    }

    @Test
    public void testCopyObject() throws RepositoryException, URISyntaxException {

        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, "/foo")).thenReturn(true);

        final String pid = "foo";

        testObj.copyObject(createPathList(pid), "http://localhost/fcrepo/bar");
        verify(mockNodes).copyObject(mockSession, "/foo", "/bar");
    }

    @Test
    public void testCopyMissingObject() throws RepositoryException, URISyntaxException {

        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, "/foo")).thenReturn(false);

        final String pid = "foo";

        final Response response = testObj.copyObject(createPathList(pid), "http://localhost/fcrepo/bar");

        assertEquals(CONFLICT.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCopyObjectWithBadDestination() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, "/foo")).thenReturn(true);

        final String pid = "foo";

        final Response response = testObj.copyObject(createPathList(pid), "http://somewhere/else/baz");

        // BAD GATEWAY
        assertEquals(SC_BAD_GATEWAY, response.getStatus());
    }

    @Test(expected = WebApplicationException.class)
    public void testCopyObjectToExistingDestination() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, "/foo")).thenReturn(true);
        doThrow(new ItemExistsException()).when(mockNodes).copyObject(mockSession, "/foo", "/baz");

        final String pid = "foo";

        final Response response = testObj.copyObject(createPathList(pid), "http://localhost/fcrepo/baz");

        assertEquals(PRECONDITION_FAILED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testMoveObject() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockNodes.getObject(isA(Session.class), isA(String.class)))
            .thenReturn(mockObject);
        when(mockObject.getEtagValue()).thenReturn("");

        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, "/foo")).thenReturn(true);

        final String pid = "foo";

        testObj.moveObject(createPathList(pid), "http://localhost/fcrepo/bar", mockRequest);
        verify(mockNodes).moveObject(mockSession, "/foo", "/bar");
    }

    @Test
    public void testMoveMissingObject() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, "/foo")).thenReturn(false);
        when(mockNodes.getObject(isA(Session.class), isA(String.class)))
            .thenReturn(mockObject);
        when(mockObject.getEtagValue()).thenReturn("");

        final String pid = "foo";

        final Response response = testObj.moveObject(createPathList(pid), "http://localhost/fcrepo/bar", mockRequest);
        assertEquals(CONFLICT.getStatusCode(), response.getStatus());
    }

    @Test(expected = WebApplicationException.class)
    public void testMoveObjectToExistingDestination() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, "/foo")).thenReturn(true);
        when(mockNodes.getObject(isA(Session.class), isA(String.class)))
            .thenReturn(mockObject);
        when(mockObject.getEtagValue()).thenReturn("");
        doThrow(new ItemExistsException()).when(mockNodes).moveObject(mockSession, "/foo", "/baz");

        final String pid = "foo";

        final Response response = testObj.moveObject(createPathList(pid), "http://localhost/fcrepo/baz", mockRequest);

        assertEquals(PRECONDITION_FAILED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testMoveObjectWithBadDestination() throws RepositoryException, URISyntaxException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.getObject(isA(Session.class), isA(String.class)))
            .thenReturn(mockObject);
        when(mockNodes.exists(mockSession, "/foo")).thenReturn(true);
        when(mockObject.getEtagValue()).thenReturn("");

        final String pid = "foo";

        final Response response = testObj.moveObject(createPathList(pid), "http://somewhere/else/baz", mockRequest);

        // BAD GATEWAY
        assertEquals(SC_BAD_GATEWAY, response.getStatus());
    }

    @Test
    public void testOptions() throws RepositoryException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockNodes.getObject(isA(Session.class), isA(String.class))).thenReturn(mockObject);
        when(mockObject.getEtagValue()).thenReturn("");
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, "/foo")).thenReturn(true);
        final String pid = "foo";

        final Response response = testObj.options( createPathList(pid), mockResponse);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testHead() throws RepositoryException {
        final ValueFactory mockVF = mock(ValueFactory.class);
        when(mockNodes.getObject(isA(Session.class), isA(String.class))).thenReturn(mockObject);
        when(mockObject.getEtagValue()).thenReturn("");
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        when(mockNodes.exists(mockSession, "/foo")).thenReturn(true);
        final String pid = "foo";

        final Response response = testObj.head( createPathList(pid), mockRequest, mockResponse, mockUriInfo);
        assertEquals(SC_OK, response.getStatus());
    }
}
