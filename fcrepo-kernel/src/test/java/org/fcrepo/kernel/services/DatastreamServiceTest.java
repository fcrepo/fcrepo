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
import static java.util.Arrays.asList;
import static org.fcrepo.kernel.services.ServiceHelpers.getCheckCacheFixityFunction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.rdf.JcrRdfTools;
import org.fcrepo.kernel.services.functions.CheckCacheEntryFixity;
import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.utils.FixityResult;
import org.fcrepo.kernel.utils.LowLevelCacheEntry;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.infinispan.loaders.CacheLoaderException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.ValueFactory;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Function;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.sparql.util.Symbol;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "org.apache.xerces.*", "javax.xml.*",
        "org.xml.sax.*", "javax.management.*"})
@PrepareForTest({JcrRdfTools.class})
public class DatastreamServiceTest implements FedoraJcrTypes {

    private static final String MOCK_CONTENT_TYPE = "application/test-data";

    private static final String JCR_CONTENT = "jcr:content";

    private static final String JCR_DATA = "jcr:data";

    @Mock
    private Session mockSession;

    @Mock
    private Node mockRoot;

    @Mock
    private Node mockNode;

    @Mock
    private Node mockContent;

    private DatastreamService testObj;

    private LowLevelStorageService llStore;

    @Mock
    private ValueFactory mockValueFactory;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        testObj = new DatastreamService();
        when(mockSession.getRootNode()).thenReturn(mockRoot);
        when(mockNode.getSession()).thenReturn(mockSession);
        llStore = mock(LowLevelStorageService.class);
        testObj.setLlStoreService(llStore);
    }

    @Test
    public void testCreateDatastreamNode() throws Exception {
        final String testPath = "/foo/bar";
        final Property mockData = mock(Property.class);
        final Binary mockBinary = mock(Binary.class);
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockNode);
        when(mockNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[] {});
        when(mockData.getBinary()).thenReturn(mockBinary);

        final InputStream mockIS = mock(InputStream.class);
        when(mockContent.setProperty(JCR_DATA, mockBinary))
                .thenReturn(mockData);
        when(mockContent.getProperty(JCR_DATA)).thenReturn(mockData);
        final StoragePolicyDecisionPoint pdp = mock(StoragePolicyDecisionPoint.class);
        when(pdp.evaluatePolicies(mockNode)).thenReturn(null);
        testObj.setStoragePolicyDecisionPoint(pdp);
        when(mockNode.getSession().getValueFactory()).thenReturn(
                mockValueFactory);
        when(
                mockValueFactory.createBinary(any(InputStream.class),
                        any(String.class))).thenReturn(mockBinary);

        final Node actual =
                testObj.createDatastreamNode(mockSession, testPath,
                        MOCK_CONTENT_TYPE, mockIS);
        assertEquals(mockNode, actual);

        verify(mockContent).setProperty(JCR_DATA, mockBinary);
    }

    @Test
    public void testGetDatastreamNode() throws Exception {
        final String testPath = "/foo/bar";
        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.getName()).thenReturn(FEDORA_DATASTREAM);
        when(mockNode.getMixinNodeTypes()).thenReturn(
                new NodeType[] {mockNodeType});

        when(mockSession.getNode(testPath)).thenReturn(mockNode);
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockNode);
        final DatastreamService testObj = new DatastreamService();
        testObj.getDatastreamNode(mockSession, testPath);
        verify(mockRoot).getNode(testPath.substring(1));
    }

    @Test
    public void testGetDatastream() throws Exception {
        final String testPath = "/foo/bar";
        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.getName()).thenReturn(FEDORA_DATASTREAM);
        when(mockNode.getMixinNodeTypes()).thenReturn(
                new NodeType[] {mockNodeType});

        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockNode);
        testObj.getDatastream(mockSession, testPath);
        verify(mockRoot).getNode(testPath.substring(1));
    }

    @Test
    public void testExists() throws RepositoryException {
        testObj.exists(mockSession, "/foo/bar");
        verify(mockSession).nodeExists("/foo/bar");
    }

    @Test
    public void testGetFixityResultsModel() throws Exception {
        mockStatic(JcrRdfTools.class);
        final GraphSubjects mockSubjects = mock(GraphSubjects.class);
        final JcrRdfTools mockJcrRdfTools = mock(JcrRdfTools.class);
        when(JcrRdfTools.withContext(mockSubjects, mockSession)).thenReturn(mockJcrRdfTools);

        final FixityResult fixityResult = mock(FixityResult.class);
        when(fixityResult.matches(any(Long.class), any(URI.class))).thenReturn(
                true);

        final Collection<FixityResult> mockCollection = asList(fixityResult);
        final Datastream mockDatastream = mock(Datastream.class);

        when(mockNode.getNode(JCR_CONTENT)).thenReturn(mockContent);

        when(mockDatastream.getNode()).thenReturn(mockNode);
        when(mockDatastream.getContentDigest()).thenReturn(
                new URI("urn:sha1:abc"));

        when(
                llStore.transformLowLevelCacheEntries(eq(mockContent),
                       Matchers.<Function<LowLevelCacheEntry,FixityResult>> any())).thenReturn(mockCollection);

        when(
                mockJcrRdfTools.getJcrTriples(eq(mockNode), Matchers
                        .<Iterable<FixityResult>> any())).thenReturn(new RdfStream());

        when(mockSubjects.getGraphSubject(mockNode)).thenReturn(
                createResource("abc"));
        final Dataset fixityResultsModel =
                testObj.getFixityResultsModel(mockSubjects, mockDatastream);

        assertTrue(fixityResultsModel.getContext().isDefined(
                Symbol.create("uri")));
    }

    @Test
    public void testGetFixity() throws NoSuchAlgorithmException,
            RepositoryException {
        final URI mockUri = URI.create("sha1:foo:bar"); // can't mock final
                                                        // classes
        final long testSize = 4L;

        when(mockNode.getNode(JCR_CONTENT)).thenReturn(mockContent);

        testObj.getFixity(mockContent, mockUri, testSize);

        getCheckCacheFixityFunction(mockUri, 0L);
        final ArgumentCaptor<CheckCacheEntryFixity> argument =
                forClass(CheckCacheEntryFixity.class);
        verify(llStore).transformLowLevelCacheEntries(eq(mockContent),
                argument.capture());

        final CheckCacheEntryFixity actualFunction = argument.getValue();

        assertEquals(mockUri, actualFunction.getChecksum());
        assertEquals(4L, actualFunction.getSize());
    }

    @Test
    public void testRunFixityAndFixProblems() throws RepositoryException,
            IOException, CacheLoaderException, URISyntaxException {

        final Datastream mockDatastream = mock(Datastream.class);

        final InputStream mockIS = mock(InputStream.class);

        final LowLevelCacheEntry mockGoodEntry = mock(LowLevelCacheEntry.class);
        when(mockGoodEntry.getInputStream()).thenReturn(mockIS);
        final LowLevelCacheEntry mockBadEntry = mock(LowLevelCacheEntry.class);
        final FixityResult mockGoodResult = mock(FixityResult.class);
        when(mockGoodResult.matches(any(Long.class), any(URI.class)))
                .thenReturn(true);
        when(mockGoodResult.getEntry()).thenReturn(mockGoodEntry);
        final FixityResult mockBadResult = mock(FixityResult.class);
        when(mockBadResult.matches(any(Long.class), any(URI.class)))
                .thenReturn(false);
        when(mockBadResult.getEntry()).thenReturn(mockBadEntry);

        final FixityResult mockRepairedResult = mock(FixityResult.class);
        when(mockRepairedResult.matches(any(Long.class), any(URI.class)))
                .thenReturn(true);
        when(mockRepairedResult.getEntry()).thenReturn(mockGoodEntry);
        when(mockRepairedResult.isSuccess()).thenReturn(true);

        final Collection<FixityResult> mockFixityResults =
                asList(mockGoodResult, mockBadResult);

        when(mockNode.getNode(JCR_CONTENT)).thenReturn(mockContent);

        when(mockDatastream.getNode()).thenReturn(mockNode);
        when(mockDatastream.getContentDigest()).thenReturn(
                URI.create("urn:sha1:abc"));

        when(
                llStore.transformLowLevelCacheEntries(eq(mockContent),
                        any(CheckCacheEntryFixity.class))).thenReturn(
                mockFixityResults);

        when(mockBadEntry.checkFixity(any(URI.class), any(Long.class)))
                .thenReturn(mockRepairedResult);
        final Collection<FixityResult> fixityResults =
                testObj.runFixityAndFixProblems(mockDatastream);

        verify(mockBadResult).setRepaired();
        verify(mockBadEntry).storeValue(mockIS);

        assertTrue("expected to find good result", fixityResults
                .contains(mockGoodResult));
        assertTrue("expected to find repaired result", fixityResults
                .contains(mockBadResult));
    }
}
