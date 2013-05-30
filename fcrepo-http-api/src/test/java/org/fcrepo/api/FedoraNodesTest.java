
package org.fcrepo.api;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.fcrepo.FedoraObject;
import org.fcrepo.FedoraResource;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.identifiers.UUIDPidMinter;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.services.NodeService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.test.util.TestHelpers;
import org.fcrepo.utils.FedoraJcrTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.modeshape.jcr.api.Repository;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;

public class FedoraNodesTest {

    FedoraNodes testObj;

    ObjectService mockObjects;

    NodeService mockNodes;

    DatastreamService mockDatastreams;

    Session mockSession;

    private UriInfo uriInfo;

    @Before
    public void setUp() throws LoginException, RepositoryException {
        mockObjects = mock(ObjectService.class);
        mockDatastreams = mock(DatastreamService.class);
        mockNodes = mock(NodeService.class);
        testObj = new FedoraNodes();
        mockSession = TestHelpers.mockSession(testObj);
        testObj.setObjectService(mockObjects);
        testObj.setNodeService(mockNodes);
        testObj.setDatastreamService(mockDatastreams);
        uriInfo = TestHelpers.getUriInfoImpl();
        testObj.setUriInfo(uriInfo);
        testObj.setPidMinter(new UUIDPidMinter());
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testIngestAndMint() throws RepositoryException {
        //final Response actual = testObj.ingestAndMint(createPathList("objects"));
        //assertNotNull(actual);
        //assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
        //verify(mockSession).save();
    }

    @Test
    public void testModify() throws RepositoryException, IOException, InvalidChecksumException {
        final String pid = "testObject";

        final FedoraResource mockResource = mock(FedoraResource.class);
        when(mockResource.isNew()).thenReturn(false);
        when(mockResource.getLastModifiedDate()).thenReturn(new Date());
        when(mockNodes.findOrCreateObject(mockSession, "/testObject")).thenReturn(mockResource);
        final Request mockRequest = mock(Request.class);
        when(mockRequest.evaluatePreconditions(any(Date.class))).thenReturn(null);
        final Response actual =
                testObj.modifyObject(createPathList(pid), TestHelpers
                        .getUriInfoImpl(), new ByteArrayInputStream("".getBytes()), null, mockRequest);
        assertNotNull(actual);
        assertEquals(Status.NO_CONTENT.getStatusCode(), actual.getStatus());
        // this verify will fail when modify is actually implemented, thus encouraging the unit test to be updated appropriately.
        // HA!
        // verifyNoMoreInteractions(mockObjects);
        verify(mockSession).save();
    }

    @Test
    public void testCreateObject() throws RepositoryException, IOException,
            InvalidChecksumException {
        final String pid = "testObject";
        final String path = "/" + pid;
        final Response actual =
                testObj.createObject(createPathList(pid),
                        FedoraJcrTypes.FEDORA_OBJECT, null, null, null,
                        TestHelpers.getUriInfoImpl(), null);
        assertNotNull(actual);
        assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
        assertTrue(actual.getEntity().toString().endsWith(pid));
        verify(mockNodes).exists(mockSession, path);
        verify(mockObjects).createObject(mockSession, path);
        verify(mockSession).save();
    }

    @Test
    public void testCreateDatastream() throws RepositoryException, IOException,
            InvalidChecksumException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId = "testDS";
        final String dsContent = "asdf";
        final String dsPath = "/" + pid + "/" + dsId;
        final InputStream dsContentStream = IOUtils.toInputStream(dsContent);
        final Node mockNode = mock(Node.class);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(
                mockDatastreams.createDatastreamNode(any(Session.class),
                        eq(dsPath), anyString(), eq(dsContentStream),
                        anyString(), anyString())).thenReturn(mockNode);
        final Response actual =
                testObj.createObject(createPathList(pid, dsId),
                        FedoraJcrTypes.FEDORA_DATASTREAM, null, null, null,
                        TestHelpers.getUriInfoImpl(), dsContentStream);
        assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
        verify(mockDatastreams).createDatastreamNode(any(Session.class),
                eq(dsPath), anyString(), any(InputStream.class), anyString(),
                anyString());
        verify(mockSession).save();
    }

    @Test
    public void testDeleteObject() throws RepositoryException {
        final String pid = "testObject";
        final String path = "/" + pid;
        final Response actual = testObj.deleteObject(createPathList(pid));
        assertNotNull(actual);
        assertEquals(Status.NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(mockNodes).deleteObject(mockSession, path);
        verify(mockSession).save();
    }

    @Test
    public void testDescribeObject() throws RepositoryException, IOException {
        final String pid = "FedoraObjectsRdfTest1";
        final String path = "/" + pid;

        final FedoraObject mockObject = mock(FedoraObject.class);
        final Dataset mockDataset = mock(Dataset.class);
        final Model mockModel = mock(Model.class);
        when(mockDataset.getDefaultModel()).thenReturn(mockModel);

        when(mockObject.getLastModifiedDate()).thenReturn(null);
        when(mockObject.getPropertiesDataset(any(GraphSubjects.class))).thenReturn(mockDataset);
        when(
                mockNodes.getObject(Mockito.isA(Session.class), Mockito
                        .isA(String.class))).thenReturn(mockObject);
        final Request mockRequest = mock(Request.class);
        final Dataset dataset =
                testObj.describe(createPathList(path), mockRequest, uriInfo);
        assertNotNull(dataset.getDefaultModel());

    }

    @Test
    public void testSparqlUpdate() throws RepositoryException, IOException {
        final String pid = "FedoraObjectsRdfTest1";
        final String path = "/" + pid;

        final FedoraObject mockObject = mock(FedoraObject.class);

        when(mockObject.getDatasetProblems()).thenReturn(null);
        final InputStream mockStream =
                new ByteArrayInputStream("my-sparql-statement".getBytes());
        when(mockNodes.getObject(mockSession, path)).thenReturn(mockObject);

        testObj.updateSparql(createPathList(pid), TestHelpers.getUriInfoImpl(),
                mockStream);

        verify(mockObject).updatePropertiesDataset(any(GraphSubjects.class),
                                                          eq("my-sparql-statement"));
        verify(mockSession).save();
        verify(mockSession).logout();
    }

}