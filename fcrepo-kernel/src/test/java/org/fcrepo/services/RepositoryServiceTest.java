/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.services;

import static org.fcrepo.services.RepositoryService.getRepositoryNamespaces;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;

import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.utils.FedoraJcrTypes;
import org.fcrepo.utils.FedoraTypesUtils;
import org.fcrepo.utils.JcrRdfTools;
import org.fcrepo.utils.NamespaceTools;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;


/**
 * @todo Add Documentation.
 * @author Chris Beer
 * @date Mar 11, 2013
 */
@RunWith(PowerMockRunner.class)
// PowerMock needs to ignore some packages to prevent class-cast errors
@PowerMockIgnore({"org.slf4j.*", "org.apache.xerces.*", "javax.xml.*",
                         "org.xml.sax.*", "javax.management.*"})
@PrepareForTest({NamespaceTools.class, JcrRdfTools.class,
                        FedoraTypesUtils.class})
public class RepositoryServiceTest implements FedoraJcrTypes {

    String testPid = "testObj";

    String testDsId = "testDs";

    String mockPrefix = "mock1";

    RepositoryService testObj;

    Repository mockRepo;

    Session mockSession;

    Node mockRootNode;

    Node mockDsNode;

    Long expectedSize = 5L;

    Long expectedCount = 0L;

    NodeTypeIterator mockNTI;

    Map<String, String> expectedNS;

    /**
     * @todo Add Documentation.
     */
    @Before
    public void setUp() {
        final String relPath = "/" + testPid + "/" + testDsId;
        final String[] mockPrefixes = {mockPrefix};
        expectedNS = new HashMap<String, String>();

        mockSession = mock(Session.class);
        mockRootNode = mock(Node.class);
        mockDsNode = mock(Node.class);
        mockRepo = mock(Repository.class);
        final NodeIterator mockNI = mock(NodeIterator.class);
        final Property mockProp = mock(Property.class);

        try {
            when(mockSession.getRootNode()).thenReturn(mockRootNode);
            when(mockRootNode.getNode(relPath)).thenReturn(mockDsNode);
            when(mockRootNode.getProperty("fedora:size")).thenReturn(mockProp);
            when(mockProp.getLong()).thenReturn(expectedSize);
            when(mockRepo.login()).thenReturn(mockSession);

            final Workspace mockWorkspace = mock(Workspace.class);
            final QueryManager mockQueryManager = mock(QueryManager.class);
            final QueryResult mockQueryResult = mock(QueryResult.class);
            final Query mockQuery = mock(Query.class);
            final RowIterator mockRI = mock(RowIterator.class);
            final Value mockValue = mock(Value.class);
            final Row mockRow = mock(Row.class);
            final NodeTypeManager mockNodeTypeManager =
                    mock(NodeTypeManager.class);
            final NamespaceRegistry mockNamespaceRegistry =
                    mock(NamespaceRegistry.class);

            mockNTI = mock(NodeTypeIterator.class);
            testObj = new RepositoryService();
            testObj.setRepository(mockRepo);

            when(mockSession.getNode("/objects")).thenReturn(mockRootNode);
            when(mockRootNode.getNodes()).thenReturn(mockNI);
            when(mockNI.getSize()).thenReturn(expectedSize);
            when(testObj.findOrCreateNode(mockSession, "/objects")).thenReturn(
                    mockRootNode);
            when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
            when(mockWorkspace.getQueryManager()).thenReturn(mockQueryManager);
            when(mockWorkspace.getNodeTypeManager()).thenReturn(
                    mockNodeTypeManager);
            when(mockWorkspace.getNamespaceRegistry()).thenReturn(
                    mockNamespaceRegistry);
            when(mockNamespaceRegistry.getPrefixes()).thenReturn(mockPrefixes);
            when(mockNodeTypeManager.getAllNodeTypes()).thenReturn(mockNTI);
            when(mockQueryManager.createQuery(anyString(), eq(Query.JCR_SQL2)))
                    .thenReturn(mockQuery);
            when(mockQuery.execute()).thenReturn(mockQueryResult);
            when(mockQueryResult.getRows()).thenReturn(mockRI);
            when(mockRI.hasNext()).thenReturn(true, false);
            when(mockRI.nextRow()).thenReturn(mockRow);
            when(mockRow.getValue(CONTENT_SIZE)).thenReturn(mockValue);
            when(mockValue.getLong()).thenReturn(expectedSize);

            expectedNS
                    .put(mockPrefix, mockNamespaceRegistry.getURI(mockPrefix));

        } catch (final RepositoryException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * @todo Add Documentation.
     */
    @After
    public void tearDown() {

    }


    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetRepositorySize() throws RepositoryException {
        final Long actual = testObj.getRepositorySize();
        assertEquals(expectedSize, actual);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetRepositoryObjectCount() {
        final Long actual = testObj.getRepositoryObjectCount();
        assertEquals(expectedCount, actual);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetAllNodeTypes() throws RepositoryException {
        final NodeTypeIterator actual = testObj.getAllNodeTypes(mockSession);
        assertEquals(mockNTI, actual);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetRepositoryNamespaces() throws RepositoryException {
        final Map<String, String> actual = getRepositoryNamespaces(mockSession);
        assertEquals(expectedNS, actual);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testExists() throws RepositoryException {
        final String existsPath = "/foo/bar/exists";
        when(mockSession.nodeExists(existsPath)).thenReturn(true);
        assertEquals(true, testObj.exists(mockSession, existsPath));
        assertEquals(false, testObj.exists(mockSession, "/foo/bar"));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testSearchRepository() throws RepositoryException {

        PowerMockito.mockStatic(JcrRdfTools.class);

        Resource subject =
            ResourceFactory.createResource("info:fedora/search/request");
        Workspace mockWorkspace = mock(Workspace.class);
        QueryManager mockQueryManager = mock(QueryManager.class);
        QueryObjectModelFactory mockQOMFactory =
            mock(QueryObjectModelFactory.class);
        QueryObjectModel mockQuery = mock(QueryObjectModel.class);
        QueryResult mockQueryResults = mock(QueryResult.class);
        NodeIterator mockIterator = mock(NodeIterator.class);
        GraphSubjects mockSubjectFactory = mock(GraphSubjects.class);


        ValueFactory mockFactory = mock(ValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockFactory);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getQueryManager()).thenReturn(mockQueryManager);
        when(mockQueryManager.getQOMFactory()).thenReturn(mockQOMFactory);
        when(mockQOMFactory.createQuery(null, null, null, null))
            .thenReturn(mockQuery);

        when(mockQuery.execute()).thenReturn(mockQueryResults);
        when(mockQueryResults.getNodes()).thenReturn(mockIterator);
        when(mockIterator.getSize()).thenReturn(500L);
        when(mockIterator.next()).thenReturn("");
        when(JcrRdfTools.getJcrNodeIteratorModel(eq(mockSubjectFactory),
                                                 any(Iterator.class),
                                                 eq(subject)))
            .thenReturn(ModelFactory.createDefaultModel());

        testObj.searchRepository(mockSubjectFactory, subject, mockSession,
                                 "search terms", 10, 0L);

        // n+1
        verify(mockQuery).setLimit(11);
        verify(mockQuery).setOffset(0);
        verify(mockQuery).execute();


    }
}
