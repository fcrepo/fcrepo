/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import com.google.common.base.Function;
import com.hp.hpl.jena.rdf.model.Model;
import org.fcrepo.Datastream;
import org.fcrepo.binary.PolicyDecisionPoint;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.services.functions.CheckCacheEntryFixity;
import org.fcrepo.utils.FedoraJcrTypes;
import org.fcrepo.utils.FedoraTypesUtils;
import org.fcrepo.utils.FixityResult;
import org.fcrepo.utils.JcrRdfTools;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.infinispan.loaders.CacheLoaderException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.JcrConstants;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @todo Add Documentation.
 * @a Chris Beeruthor
 * @date Feb 12, 2013
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "org.apache.xerces.*", "javax.xml.*",
                         "org.xml.sax.*", "javax.management.*"})
@PrepareForTest({FedoraTypesUtils.class,
        ServiceHelpers.class, JcrRdfTools.class})
public class DatastreamServiceTest implements FedoraJcrTypes {

    private static final String MOCK_CONTENT_TYPE = "application/test-data";

    private static final String JCR_CONTENT = "jcr:content";

    private static final String JCR_DATA = "jcr:data";

    private Session mockSession;

    private Node mockRoot;

    private DatastreamService testObj;
    private LowLevelStorageService llStore;

    /**
     * @todo Add Documentation.
     */
    @Before
    public void setUp() throws RepositoryException {
        testObj = new DatastreamService();
        mockSession = mock(Session.class);
        mockRoot = mock(Node.class);
        when(mockSession.getRootNode()).thenReturn(mockRoot);
        llStore = mock(LowLevelStorageService.class);
        testObj.setLlStoreService(llStore);
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
    public void testCreateDatastreamNode() throws Exception {
        final String testPath = "/foo/bar";
        final Node mockNode = mock(Node.class);
        final Node mockContent = mock(Node.class);
        Property mockData = mock(Property.class);
        Binary mockBinary = mock(Binary.class);
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockNode);
        when(mockNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        when(mockData.getBinary()).thenReturn(mockBinary);
        final InputStream mockIS = mock(InputStream.class);
        when(mockContent.setProperty(JCR_DATA, mockBinary)).thenReturn(mockData);
        when(mockContent.getProperty(JCR_DATA)).thenReturn(mockData);
        final PolicyDecisionPoint pdp = mock(PolicyDecisionPoint.class);
        when(pdp.evaluatePolicies(mockNode)).thenReturn(null);
        testObj.setStoragePolicyDecisionPoint(pdp);
        mockStatic(FedoraTypesUtils.class);
        when(FedoraTypesUtils.getBinary(eq(mockNode), eq(mockIS), any(String.class))).thenReturn(mockBinary);

        final Node actual =
                testObj.createDatastreamNode(mockSession, testPath,
                        MOCK_CONTENT_TYPE, mockIS);
        assertEquals(mockNode, actual);

        verify(mockContent).setProperty(JCR_DATA, mockBinary);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetDatastreamNode() throws Exception {
        final String testPath = "/foo/bar";
        final Node mockNode = mock(Node.class);

        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.getName()).thenReturn(FEDORA_DATASTREAM);
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[] { mockNodeType });

        when(mockSession.getNode(testPath)).thenReturn(mockNode);
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockNode);
        final DatastreamService testObj = new DatastreamService();
        testObj.getDatastreamNode(mockSession, testPath);
        verify(mockRoot).getNode(testPath.substring(1));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetDatastream() throws Exception {
        final String testPath = "/foo/bar";
        final Node mockNode = mock(Node.class);

        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.getName()).thenReturn(FEDORA_DATASTREAM);
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[] { mockNodeType });

        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockNode);
        testObj.getDatastream(mockSession, testPath);
        verify(mockRoot).getNode(testPath.substring(1));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testExists() throws RepositoryException {
        final Session mockSession = mock(Session.class);
        testObj.exists(mockSession, "/foo/bar");
        verify(mockSession).nodeExists("/foo/bar");
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetFixityResultsModel() throws RepositoryException, URISyntaxException {
        mockStatic(JcrRdfTools.class);

        final FixityResult fixityResult = mock(FixityResult.class);
        when(fixityResult.matches(any(Long.class), any(URI.class))).thenReturn(true);

        Collection<FixityResult> mockCollection = Arrays.asList(fixityResult);
        final Datastream mockDatastream = mock(Datastream.class);
        final Node mockNode = mock(Node.class);
        final Node mockContent = mock(Node.class);

        when(mockNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(mockContent);

        when(mockDatastream.getNode()).thenReturn(mockNode);
        when(mockDatastream.getContentDigest()).thenReturn(new URI("urn:sha1:abc"));

        when(llStore.transformLowLevelCacheEntries(eq(mockContent), any(Function.class))).thenReturn(mockCollection);


        GraphSubjects mockSubjects = mock(GraphSubjects.class);
        Model mockModel = mock(Model.class);
        when(JcrRdfTools.getFixityResultsModel(eq(mockSubjects), eq(mockNode), any(Collection.class))).thenReturn(mockModel);

        testObj.getFixityResultsModel(mockSubjects, mockDatastream);
    }


    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetFixity() throws RepositoryException {
        final Node mockNode = mock(Node.class);
        final Node mockContent = mock(Node.class);

        final MessageDigest mockDigest = mock(MessageDigest.class);
        final URI mockUri = URI.create("urn:foo:bar"); // can't mock final classes
        final long testSize = 4L;

        when(mockNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(mockContent);

        testObj.getFixity(mockContent, mockDigest, mockUri, testSize );

        ServiceHelpers.getCheckCacheFixityFunction(null, null, 0L);
        ArgumentCaptor<CheckCacheEntryFixity> argument = ArgumentCaptor.forClass(CheckCacheEntryFixity.class);
        verify(llStore).transformLowLevelCacheEntries(eq(mockContent), argument.capture());

        final CheckCacheEntryFixity actualFunction = argument.getValue();

        assertEquals(mockDigest, actualFunction.getDigest());
        assertEquals(mockUri, actualFunction.getChecksum());
        assertEquals(4L, actualFunction.getSize());
    }

    /**
     * @todo Add Documentation.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testRunFixityAndFixProblems() throws RepositoryException,
                                                                 IOException, CacheLoaderException, URISyntaxException {

        final Datastream mockDatastream = mock(Datastream.class);
        final Node mockNode = mock(Node.class);
        final Node mockContent = mock(Node.class);

        InputStream mockIS = mock(InputStream.class);

        LowLevelCacheEntry mockGoodEntry = mock(LowLevelCacheEntry.class);
        when(mockGoodEntry.getInputStream()).thenReturn(mockIS);
        LowLevelCacheEntry mockBadEntry = mock(LowLevelCacheEntry.class);
        final FixityResult mockGoodResult = mock(FixityResult.class);
        when(mockGoodResult.matches(any(Long.class), any(URI.class))).thenReturn(true);
        when(mockGoodResult.getEntry()).thenReturn(mockGoodEntry);
        final FixityResult mockBadResult = mock(FixityResult.class);
        when(mockBadResult.matches(any(Long.class), any(URI.class))).thenReturn(false);
        when(mockBadResult.getEntry()).thenReturn(mockBadEntry);

        final FixityResult mockRepairedResult = mock(FixityResult.class);
        when(mockRepairedResult.matches(any(Long.class), any(URI.class))).thenReturn(true);
        when(mockRepairedResult.getEntry()).thenReturn(mockGoodEntry);
        when(mockRepairedResult.isSuccess()).thenReturn(true);

        Collection<FixityResult> mockFixityResults = Arrays.asList(mockGoodResult, mockBadResult);

        when(mockNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(mockContent);

        when(mockDatastream.getNode()).thenReturn(mockNode);
        when(mockDatastream.getContentDigest()).thenReturn(new URI("urn:sha1:abc"));

        when(llStore.transformLowLevelCacheEntries(eq(mockContent), any(CheckCacheEntryFixity.class))).thenReturn(mockFixityResults);

        when(mockBadEntry.checkFixity(any(URI.class), any(Long.class), any(MessageDigest.class))).thenReturn(mockRepairedResult);
        final Collection<FixityResult> fixityResults = testObj.runFixityAndFixProblems(mockDatastream);

        verify(mockBadResult).setRepaired();
        verify(mockBadEntry).storeValue(mockIS);

        assertTrue("expected to find good result", fixityResults.contains(mockGoodResult));
        assertTrue("expected to find repaired result", fixityResults.contains(mockBadResult));
    }
}
