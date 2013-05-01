package org.fcrepo.generator;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;

import org.apache.any23.writer.TripleHandlerException;
import org.fcrepo.Datastream;
import org.fcrepo.FedoraObject;
import org.fcrepo.TestHelpers;
import org.fcrepo.generator.rdf.TripleSource;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.test.util.PathSegmentImpl;
import org.junit.Before;
import org.junit.Test;


public class FedoraRdfGeneratorTest {

    private FedoraRdfGenerator testObj;
    
    private ObjectService mockObjects;
    
    private DatastreamService mockDatastreams;
    
    private UriInfo mockURIs;
	private Session mockSession;

	@Before
    public void setUp() throws IllegalArgumentException, UriBuilderException, URISyntaxException, RepositoryException {
        testObj = new FedoraRdfGenerator();
		mockSession = TestHelpers.mockSession(testObj);
        mockObjects = mock(ObjectService.class);
        mockDatastreams = mock(DatastreamService.class);
        mockURIs = mock(UriInfo.class);
        UriBuilder mockBuilder = mock(UriBuilder.class);
        when(mockBuilder.build(any())).thenReturn(new URI("http://fcrepo.tv/"));
        when(mockURIs.getBaseUriBuilder()).thenReturn(mockBuilder);
        testObj.setObjectService(mockObjects);
        testObj.setDatastreamService(mockDatastreams);
        testObj.setUriInfo(mockURIs);
    }
    
    @Test
    public void testSetObjectGenerators() throws IOException, RepositoryException, TripleHandlerException {
        @SuppressWarnings("unchecked")
        List<TripleSource<FedoraObject>> mockList = mock(List.class);
        testObj.setObjectGenerators(mockList);
        @SuppressWarnings("unchecked")
        List<TripleSource<FedoraObject>> actual = (List<TripleSource<FedoraObject>>) getMember("objectGenerators");
        assertEquals(mockList, actual);
    }

    @Test
    public void testSetDatastreamGenerators() throws IOException, RepositoryException, TripleHandlerException {
        @SuppressWarnings("unchecked")
        List<TripleSource<Datastream>> mockList = mock(List.class);
        testObj.setDatastreamGenerators(mockList);
        @SuppressWarnings("unchecked")
        List<TripleSource<Datastream>> actual = (List<TripleSource<Datastream>>) getMember("datastreamGenerators");
        assertEquals(mockList, actual);
    }

    @Test
    public void testGetRdfXml() throws IOException, RepositoryException, TripleHandlerException {
        List<PathSegment> pathList = PathSegmentImpl.createPathList("objects", "foo");
        FedoraObject mockObj = mock(FedoraObject.class);
        Workspace mockWS = mock(Workspace.class);
        NamespaceRegistry mockNS = mock(NamespaceRegistry.class);
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        when(mockWS.getNamespaceRegistry()).thenReturn(mockNS);
        when(mockNS.getPrefixes()).thenReturn(new String[]{});
        Node mockNode = mock(Node.class);
        when(mockObj.getNode()).thenReturn(mockNode);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockObjects.getObject(mockSession, "/objects/foo")).thenReturn(mockObj);

        @SuppressWarnings("unchecked")
        TripleSource<FedoraObject>[] gens = new TripleSource[0];
        List<TripleSource<FedoraObject>> mockList = Arrays.asList(gens);
        testObj.setObjectGenerators(mockList);

        
        testObj.getRdfXml(pathList, TEXT_PLAIN);
    }

    private Object getMember(String name) {
        try {
            Field field = FedoraRdfGenerator.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(testObj);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
