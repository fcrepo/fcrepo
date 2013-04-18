
package org.fcrepo.api;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.tika.io.IOUtils;
import org.modeshape.jcr.api.Repository;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;

import com.sun.jersey.api.uri.UriBuilderImpl;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.StreamDataBodyPart;

public abstract class TestHelpers {

    static String MOCK_PREFIX = "mockPrefix";

    static String MOCK_URI_STRING = "mock.namespace.org";

    public static UriInfo getUriInfoImpl() {
        //        UriInfo ui = mock(UriInfo.class,withSettings().verboseLogging());
        final UriInfo ui = mock(UriInfo.class);
        final UriBuilder ub = new UriBuilderImpl();
        ub.scheme("http");
        ub.host("locahost");
        ub.path("/fcrepo");

        when(ui.getRequestUri()).thenReturn(
                URI.create("http://localhost/fcrepo"));
        when(ui.getBaseUri()).thenReturn(URI.create("http://localhost/"));
        when(ui.getBaseUriBuilder()).thenReturn(ub);
        when(ui.getAbsolutePathBuilder()).thenReturn(ub);

        return ui;
    }

    public static MultiPart getStringsAsMultipart(
            final Map<String, String> contents) {
        final MultiPart multipart = new MultiPart();
        for (final Entry<String, String> e : contents.entrySet()) {
            final String id = e.getKey();
            final String content = e.getValue();
            final InputStream src = IOUtils.toInputStream(content);
            final StreamDataBodyPart part = new StreamDataBodyPart(id, src);
            try {
                final FormDataContentDisposition cd =
                        new FormDataContentDisposition("form-data;name=" + id +
                                ";filename=" + id + ".txt");
                part.contentDisposition(cd);
            } catch (final ParseException ex) {
                ex.printStackTrace();
            }
            multipart.bodyPart(part);
        }
        return multipart;
    }

    @SuppressWarnings("unchecked")
    public static Query getQueryMock() {
        final Query mockQ = mock(Query.class);
        final QueryResult mockResults = mock(QueryResult.class);
        final NodeIterator mockNodes = mock(NodeIterator.class);
        when(mockNodes.getSize()).thenReturn(2L);
        when(mockNodes.hasNext()).thenReturn(true, true, false);
        final Node node1 = mock(Node.class);
        final Node node2 = mock(Node.class);
        try {
            when(node1.getName()).thenReturn("node1");
            when(node2.getName()).thenReturn("node2");
        } catch (final RepositoryException e) {
            e.printStackTrace();
        }
        when(mockNodes.nextNode()).thenReturn(node1, node2).thenThrow(
                IndexOutOfBoundsException.class);
        try {
            when(mockResults.getNodes()).thenReturn(mockNodes);
            when(mockQ.execute()).thenReturn(mockResults);
        } catch (final RepositoryException e) {
            e.printStackTrace();
        }
        return mockQ;
    }

    public static Session getQuerySessionMock() {
        final Session mock = mock(Session.class);
        final Workspace mockWS = mock(Workspace.class);
        final QueryManager mockQM = mock(QueryManager.class);
        try {
            final Query mockQ = getQueryMock();
            when(mockQM.createQuery(anyString(), anyString()))
                    .thenReturn(mockQ);
            when(mockWS.getQueryManager()).thenReturn(mockQM);
        } catch (final RepositoryException e) {
            e.printStackTrace();
        }
        when(mock.getWorkspace()).thenReturn(mockWS);
        final ValueFactory mockVF = mock(ValueFactory.class);
        try {
            when(mock.getValueFactory()).thenReturn(mockVF);
        } catch (final RepositoryException e) {
            e.printStackTrace();
        }
        return mock;
    }

    @SuppressWarnings("unchecked")
    public static Session getSessionMock() throws RepositoryException {
        final String[] mockPrefixes = {MOCK_PREFIX};
        final Session mockSession = mock(Session.class);
        final Workspace mockWorkspace = mock(Workspace.class);
        final NamespaceRegistry mockNameReg = mock(NamespaceRegistry.class);
        final NodeTypeManager mockNTM = mock(NodeTypeManager.class);
        final NodeTypeIterator mockNTI = mock(NodeTypeIterator.class);
        final NodeType mockNodeType = mock(NodeType.class);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNameReg);
        when(mockNameReg.getPrefixes()).thenReturn(mockPrefixes);
        when(mockNameReg.getURI(MOCK_PREFIX)).thenReturn(MOCK_URI_STRING);
        when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNTM);
        when(mockNodeType.getName()).thenReturn("mockName");
        when(mockNodeType.toString()).thenReturn("mockString");
        when(mockNTM.getAllNodeTypes()).thenReturn(mockNTI);
        when(mockNTI.hasNext()).thenReturn(true, false);
        when(mockNTI.nextNodeType()).thenReturn(mockNodeType).thenThrow(
                ArrayIndexOutOfBoundsException.class);
        return mockSession;
    }

    public static Repository createMockRepo() throws RepositoryException {
        final Repository mockRepo = mock(Repository.class);
        final Session mockSession = getSessionMock();

        when(mockRepo.getDescriptor("jcr.repository.name")).thenReturn(
                "Mock Repository");
        final String[] mockKeys = {MOCK_PREFIX};

        when(mockRepo.login()).thenReturn(mockSession);
        when(mockRepo.getDescriptorKeys()).thenReturn(mockKeys);

        return mockRepo;
    }

}
