/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.kernel.services;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.jcr.query.Query.JCR_SQL2;
import static org.fcrepo.kernel.RdfLexicon.RESTAPI_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_PATH;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.HashMap;
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
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;

import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.rdf.JcrRdfTools;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Resource;

@RunWith(PowerMockRunner.class)
// PowerMock needs to ignore some packages to prevent class-cast errors
@PowerMockIgnore({"org.slf4j.*", "org.apache.xerces.*", "javax.xml.*",
        "org.xml.sax.*", "javax.management.*"})
@PrepareForTest({JcrRdfTools.class})
public class RepositoryServiceImplTest implements FedoraJcrTypes {

    private static final String TESTPID = "testObj";

    private static final String TESTDSID = "testDs";

    private static final String MOCKPREFIX = "mock1";

    private static final Long EXPECTED_SIZE = 5L;

    private static final Long EXPECTED_COUNT = 0L;

    private RepositoryService testObj;

    @Mock
    private Repository mockRepo;

    @Mock
    private Session mockSession;

    @Mock
    private Node mockRootNode;

    @Mock
    private Node mockDsNode;

    @Mock
    private Property mockProp;

    @Mock
    private NodeIterator mockNI;

    @Mock
    private RowIterator mockRowIterator;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private QueryManager mockQueryManager;

    @Mock
    private QueryObjectModel mockQueryOM;

    @Mock
    private QueryResult mockQueryResult;

    @Mock
    private Query mockQuery;

    @Mock
    private RowIterator mockRI;

    @Mock
    private Value mockValue;

    @Mock
    private Row mockRow;

    @Mock
    private GraphSubjects mockSubjectFactory;

    @Mock
    private QueryObjectModelFactory mockQOMFactory;

    @Mock
    private ValueFactory mockFactory;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    private Map<String, String> expectedNS;

    private Resource subject;

    @Before
    public void setUp() {
        initMocks(this);
        final String relPath = "/" + TESTPID + "/" + TESTDSID;
        final String[] mockPrefixes = {MOCKPREFIX};
        expectedNS = new HashMap<>();
        try {
            when(mockSession.getRootNode()).thenReturn(mockRootNode);
            when(mockRootNode.getNode(relPath)).thenReturn(mockDsNode);
            when(mockRootNode.getProperty("fedora:size")).thenReturn(mockProp);
            when(mockProp.getLong()).thenReturn(EXPECTED_SIZE);
            when(mockRepo.login()).thenReturn(mockSession);

            testObj = new RepositoryServiceImpl();
            testObj.setRepository(mockRepo);

            when(mockSession.getNode("/objects")).thenReturn(mockRootNode);
            when(mockRootNode.getNodes()).thenReturn(mockNI);
            when(mockNI.getSize()).thenReturn(EXPECTED_SIZE);
            when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
            when(mockWorkspace.getQueryManager()).thenReturn(mockQueryManager);
            when(mockWorkspace.getNamespaceRegistry()).thenReturn(
                    mockNamespaceRegistry);
            when(mockNamespaceRegistry.getPrefixes()).thenReturn(mockPrefixes);

            when(mockQueryManager.createQuery(anyString(), eq(JCR_SQL2)))
                    .thenReturn(mockQuery);
            when(mockQuery.execute()).thenReturn(mockQueryResult);
            when(mockQueryResult.getRows()).thenReturn(mockRI);
            when(mockRI.hasNext()).thenReturn(true, false);
            when(mockRI.nextRow()).thenReturn(mockRow);
            when(mockRow.getValue(CONTENT_SIZE)).thenReturn(mockValue);
            when(mockValue.getLong()).thenReturn(EXPECTED_SIZE);

            expectedNS
                    .put(MOCKPREFIX, mockNamespaceRegistry.getURI(MOCKPREFIX));

        } catch (final RepositoryException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetRepositorySize() {
        final Long actual = testObj.getRepositorySize();
        assertEquals(EXPECTED_SIZE, actual);
    }

    @Test
    public void testGetRepositoryObjectCount() {
        final Long actual = testObj.getRepositoryObjectCount();
        assertEquals(EXPECTED_COUNT, actual);
    }

    @Test
    public void testGetRepositoryNamespaces() throws RepositoryException {
        final Map<String, String> actual = testObj.getRepositoryNamespaces(mockSession);
        assertEquals(expectedNS, actual);
    }

    private void setupSearchRepository() throws RepositoryException {
        mockStatic(JcrRdfTools.class);
        final JcrRdfTools mockJcrRdfTools = mock(JcrRdfTools.class);
        when(JcrRdfTools.withContext(mockSubjectFactory, mockSession)).thenReturn(mockJcrRdfTools);

        subject = createResource(RESTAPI_NAMESPACE + "search/request");

        when(mockSession.getValueFactory()).thenReturn(mockFactory);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getQueryManager()).thenReturn(mockQueryManager);
        when(mockQueryManager.getQOMFactory()).thenReturn(mockQOMFactory);
        when(mockQOMFactory.createQuery(null, null, null, null)).thenReturn(
                mockQueryOM);
        when(mockQueryOM.execute()).thenReturn(mockQueryResult);
        when(mockQueryResult.getNodes()).thenReturn(mockNI);
        when(mockNI.getSize()).thenReturn(500L);
        when(mockNI.next()).thenReturn("");
        when(
                mockJcrRdfTools.getJcrPropertiesModel(any(org.fcrepo.kernel.utils.iterators.NodeIterator.class), eq(subject)))
                .thenReturn(new RdfStream());
    }

    @Test
    public void testSearchRepository() throws RepositoryException {

        setupSearchRepository();

        final Dataset dataset =
                testObj.searchRepository(mockSubjectFactory, subject,
                        mockSession,
                "search terms", 10, 0L);

        // n+1
        verify(mockQueryOM).setLimit(11);
        verify(mockQueryOM).setOffset(0);
        verify(mockQueryOM).execute();

        assertFalse("Dataset graph should contain results", dataset
                .getDefaultModel().getGraph().isEmpty());

    }

    @Test
    public void testSearchRepositoryNullSearchTerms()
            throws RepositoryException {
        setupSearchRepository();

        final Dataset dataset = testObj.searchRepository(mockSubjectFactory, subject, mockSession,
                null, 10, 0L);

        // No query should have been run
        verify(mockQueryManager, never()).getQOMFactory();
        verify(mockQueryOM, never()).execute();

        assertTrue("Null search terms should return an empty result graph",
                dataset.getDefaultModel().getGraph().isEmpty());
    }

    @Test
    public void testSearchRepositoryNoSearchTerms() throws RepositoryException {
        setupSearchRepository();

        final Dataset dataset =
                testObj.searchRepository(mockSubjectFactory, subject,
                        mockSession, "", 10, 0L);

        // No query should have been run
        verify(mockQueryManager, never()).getQOMFactory();
        verify(mockQueryOM, never()).execute();

        assertTrue("Blank query should return an empty result graph", dataset
                .getDefaultModel().getGraph().isEmpty());
    }

    @Test
    public void testGetObjectSize() throws RepositoryException {

        when(mockRepo.login()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getQueryManager()).thenReturn(mockQueryManager);
        when(
                mockQueryManager.createQuery("SELECT [" + CONTENT_SIZE +
                        "] FROM [" + FEDORA_BINARY + "]", JCR_SQL2))
                .thenReturn(mockQuery);
        when(mockQuery.execute()).thenReturn(mockQueryResult);
        when(mockQueryResult.getRows()).thenReturn(mockRowIterator);

        when(mockRowIterator.hasNext()).thenReturn(true, true, true, false);
        when(mockRowIterator.nextRow()).thenReturn(mockRow, mockRow, mockRow);

        when(mockRow.getValue(CONTENT_SIZE)).thenReturn(mockValue);
        when(mockValue.getLong()).thenReturn(5L, 10L, 1L);

        final long count = testObj.getRepositorySize();
        assertEquals("Got wrong count!", 16L, count);
        verify(mockSession).logout();
        verify(mockSession, never()).save();
    }

    @Test
    public void testGetObjectCount() throws RepositoryException {
        when(mockRepo.login()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getQueryManager()).thenReturn(mockQueryManager);
        when(
                mockQueryManager.createQuery("SELECT [" + JCR_PATH +
                        "] FROM [" + FEDORA_OBJECT + "]", JCR_SQL2))
                .thenReturn(mockQuery);
        when(mockQuery.execute()).thenReturn(mockQueryResult);
        when(mockQueryResult.getRows()).thenReturn(mockRowIterator);
        when(mockRowIterator.getSize()).thenReturn(3L);

        final long count = testObj.getRepositoryObjectCount();
        assertEquals(3L, count);
        verify(mockSession).logout();
        verify(mockSession, never()).save();
    }
}
