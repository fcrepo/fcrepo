
package org.fcrepo.api;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Variant;

import org.apache.commons.io.IOUtils;
import org.fcrepo.FedoraObject;
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
import org.modeshape.jcr.api.Repository;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.GraphStore;

public class FedoraNodesTest {

    FedoraNodes testObj;

    ObjectService mockObjects;

    NodeService mockNodes;

    DatastreamService mockDatastreams;

    Repository mockRepo;

    Session mockSession;

    LowLevelStorageService mockLow;

    Request mockRequest;

    @Before
    public void setUp() throws LoginException, RepositoryException {
        mockRequest = mock(Request.class);
        mockObjects = mock(ObjectService.class);
        mockDatastreams = mock(DatastreamService.class);
        mockNodes = mock(NodeService.class);
        mockLow = mock(LowLevelStorageService.class);
        testObj = new FedoraNodes();
        mockSession = TestHelpers.mockSession(testObj);
        testObj.setObjectService(mockObjects);
        testObj.setNodeService(mockNodes);
        testObj.setDatastreamService(mockDatastreams);
        testObj.setLlStoreService(mockLow);
        mockRepo = mock(Repository.class);
        testObj.setUriInfo(TestHelpers.getUriInfoImpl());
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
    public void testModify() throws RepositoryException, IOException {
        final String pid = "testObject";
        final Response actual = testObj.modifyObject(createPathList(pid), TestHelpers.getUriInfoImpl(), null);
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
                        FedoraJcrTypes.FEDORA_OBJECT, null, null, null, TestHelpers.getUriInfoImpl(), null);
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

    @SuppressWarnings("unchecked")
    @Test
    public void testDescribeRdfObject() throws RepositoryException, IOException {
        final String pid = "FedoraObjectsRdfTest1";
        final String path = "/" + pid;

        final FedoraObject mockObject = mock(FedoraObject.class);

        final GraphStore mockStore = mock(GraphStore.class);
        final Dataset mockDataset = mock(Dataset.class);
        final Model mockModel = mock(Model.class);
        when(mockStore.toDataset()).thenReturn(mockDataset);
        when(mockDataset.getDefaultModel()).thenReturn(mockModel);

        when(mockObject.getLastModifiedDate()).thenReturn(new Date());
        when(mockObject.getGraphStore(any(GraphSubjects.class))).thenReturn(mockStore);
        when(mockNodes.getObject(mockSession, path)).thenReturn(mockObject);
        final Request mockRequest = mock(Request.class);

        when(mockRequest.selectVariant(any(List.class))).thenReturn(
                new Variant(MediaType.valueOf("application/n-triples"), null,
                        null));

        final OutputStream mockStream = mock(OutputStream.class);
        ((StreamingOutput) testObj
                .describeRdf(createPathList(pid), mockRequest, TestHelpers.getUriInfoImpl()).getEntity())
                .write(mockStream);

        verify(mockModel).write(mockStream, "N-TRIPLES");

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDescribeRdfCached() throws RepositoryException, IOException {
        final String pid = "FedoraObjectsRdfTest2";
        final String path = "/" + pid;
        final FedoraObject mockObject = mock(FedoraObject.class);

        final GraphStore mockStore = mock(GraphStore.class);
        final Dataset mockDataset = mock(Dataset.class);
        final Model mockModel = mock(Model.class);
        when(mockStore.toDataset()).thenReturn(mockDataset);
        when(mockDataset.getDefaultModel()).thenReturn(mockModel);

        when(mockObject.getLastModifiedDate()).thenReturn(new Date());
        when(mockObject.getGraphStore()).thenReturn(mockStore);
        when(mockNodes.getObject(mockSession, path)).thenReturn(mockObject);
        final Request mockRequest = mock(Request.class);

        when(mockRequest.selectVariant(any(List.class))).thenReturn(
                new Variant(MediaType.valueOf("application/n-triples"), null,
                        null));

        when(mockRequest.evaluatePreconditions(any(Date.class))).thenReturn(
                Response.notModified());
        final Response actual =
                testObj.describeRdf(createPathList(pid), mockRequest, TestHelpers.getUriInfoImpl());
        assertNotNull(actual);
        assertEquals(Status.NOT_MODIFIED.getStatusCode(), actual.getStatus());
        verify(mockSession, never()).save();
    }

    @Test
    public void testSparqlUpdate() throws RepositoryException, IOException {
        final String pid = "FedoraObjectsRdfTest1";
        final String path = "/" + pid;

        final FedoraObject mockObject = mock(FedoraObject.class);

        when(mockObject.getGraphProblems()).thenReturn(null);
        final InputStream mockStream =
                new ByteArrayInputStream("my-sparql-statement".getBytes());
        when(mockNodes.getObject(mockSession, path)).thenReturn(mockObject);

        testObj.updateSparql(createPathList(pid), TestHelpers.getUriInfoImpl(), mockStream);

        verify(mockObject).updateGraph(any(GraphSubjects.class), eq("my-sparql-statement"));
        verify(mockSession).save();
        verify(mockSession).logout();
    }

}