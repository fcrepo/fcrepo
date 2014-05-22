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
package org.fcrepo.kernel;
import static com.hp.hpl.jena.graph.NodeFactory.createAnon;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.util.Calendar.JULY;
import static org.apache.commons.codec.digest.DigestUtils.shaHex;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.fcrepo.jcr.FedoraJcrTypes.JCR_CREATED;
import static org.fcrepo.jcr.FedoraJcrTypes.JCR_LASTMODIFIED;
import static org.fcrepo.kernel.rdf.JcrRdfTools.getProblemsModel;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isFedoraResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.fcrepo.kernel.rdf.HierarchyRdfContextOptions;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.rdf.JcrRdfTools;
import org.fcrepo.kernel.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.utils.FedoraTypesUtils;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.util.Symbol;

/**
 * <p>FedoraResourceImplTest class.</p>
 *
 * @author ajs6f
 */
@RunWith(PowerMockRunner.class)
//PowerMock needs to ignore some packages to prevent class-cast errors
//PowerMock needs to ignore unnecessary packages to keep from running out of heap
@PowerMockIgnore({
  "org.slf4j.*",
  "org.apache.xerces.*",
  "javax.xml.*",
  "org.xml.sax.*",
  "javax.management.*",
  "com.google.common.*",
  "com.hp.hpl.jena.*",
  "com.codahale.metrics.*"
  })
@PrepareForTest({JcrRdfTools.class, FedoraTypesUtils.class})
public class FedoraResourceImplTest {

    private FedoraResource testObj;

    @Mock
    private Node mockNode;

    @Mock
    private Node mockRoot;

    @Mock
    private Session mockSession;

    private Resource mockResource = createResource();

    @Mock
    private Property mockProp;

    @Mock
    NodeType mockNodeType;

    private Triple mockTriple =
        create(createAnon(), createAnon(), createAnon());

    @Mock
    private JcrRdfTools mockJcrRdfTools;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockNode.getSession()).thenReturn(mockSession);
        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.getName()).thenReturn("nt:folder");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        testObj = new FedoraResourceImpl(mockNode);
        assertEquals(mockNode, testObj.getNode());
    }

    @Test
    public void testPathConstructor() throws RepositoryException {
        when(mockSession.getRootNode()).thenReturn(mockRoot);
        when(mockRoot.getNode("foo/bar")).thenReturn(mockNode);
        when(mockNode.isNew()).thenReturn(true);
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[] {});
        new FedoraResourceImpl(mockSession, "/foo/bar", null);
    }

    @Test
    public void testHasMixin() throws RepositoryException {
        boolean actual;
        final NodeType mockType = mock(NodeType.class);
        final NodeType[] mockTypes = new NodeType[] {mockType};
        when(mockNode.getMixinNodeTypes()).thenReturn(mockTypes);
        actual = isFedoraResource.apply(mockNode);
        assertEquals(false, actual);
        when(mockType.getName()).thenReturn(FEDORA_RESOURCE);
        actual = isFedoraResource.apply(mockNode);
        assertEquals(true, actual);
    }

    @Test
    public void testGetPath() throws RepositoryException {
        testObj.getPath();
        verify(mockNode).getPath();
    }

    @Test
    public void testHasContent() throws RepositoryException {
        testObj.hasContent();
        verify(mockNode).hasNode(JCR_CONTENT);
    }

    @Test
    public void testGetCreatedDate() throws RepositoryException {
        final Calendar someDate = Calendar.getInstance();
        when(mockProp.getDate()).thenReturn(someDate);
        when(mockNode.hasProperty(JCR_CREATED)).thenReturn(true);
        when(mockNode.getProperty(JCR_CREATED)).thenReturn(mockProp);
        assertEquals(someDate.getTimeInMillis(), testObj.getCreatedDate()
                                                     .getTime());
    }

    @Test
    public void testGetLastModifiedDateDefault() throws RepositoryException {
        // test missing JCR_LASTMODIFIED
        final Calendar someDate = Calendar.getInstance();
        someDate.add(Calendar.DATE, -1);
        try {
            when(mockNode.hasProperty(JCR_LASTMODIFIED)).thenReturn(false);
            when(mockProp.getDate()).thenReturn(someDate);
            when(mockNode.hasProperty(JCR_CREATED)).thenReturn(true);
            when(mockNode.getProperty(JCR_CREATED)).thenReturn(mockProp);
            when(mockNode.getSession()).thenReturn(mockSession);
        } catch (final RepositoryException e) {
            e.printStackTrace();
        }
        final Date actual = testObj.getLastModifiedDate();
        assertEquals(someDate.getTimeInMillis(), actual.getTime());
        // this is a read operation, it must not persist the session
        verify(mockSession, never()).save();
    }

    @Test
    public void testGetLastModifiedDate() throws RepositoryException {
        // test existing JCR_LASTMODIFIED
        final Calendar someDate = Calendar.getInstance();
        someDate.add(Calendar.DATE, -1);
        try {
            when(mockProp.getDate()).thenReturn(someDate);
            when(mockNode.hasProperty(JCR_CREATED)).thenReturn(true);
            when(mockNode.getProperty(JCR_CREATED)).thenReturn(mockProp);
            when(mockNode.getSession()).thenReturn(mockSession);
        } catch (final RepositoryException e) {
            e.printStackTrace();
        }
        final Property mockMod = mock(Property.class);
        final Calendar modDate = Calendar.getInstance();
        try {
            when(mockNode.hasProperty(JCR_LASTMODIFIED)).thenReturn(true);
            when(mockNode.getProperty(JCR_LASTMODIFIED)).thenReturn(mockMod);
            when(mockMod.getDate()).thenReturn(modDate);
        } catch (final RepositoryException e) {
            System.err.println("What are we doing in the second test?");
            e.printStackTrace();
        }
        final Date actual = testObj.getLastModifiedDate();
        assertEquals(modDate.getTimeInMillis(), actual.getTime());
    }

    @Test
    public void testGetPropertiesDataset() throws Exception {

        mockStatic(JcrRdfTools.class);
        final IdentifierTranslator mockSubjects = mock(IdentifierTranslator.class);

        when(JcrRdfTools.withContext(mockSubjects, mockSession)).thenReturn(mockJcrRdfTools);

        when(mockSubjects.getSubject(mockNode.getPath())).thenReturn(mockResource);

        final RdfStream propertiesStream = new RdfStream(mockTriple);
        when(mockJcrRdfTools.getJcrTriples(mockNode)).thenReturn(
                propertiesStream);
        final RdfStream treeStream = new RdfStream(mockTriple);
        when(mockJcrRdfTools.getTreeTriples(eq(mockNode),
                                               any(HierarchyRdfContextOptions.class))).thenReturn(treeStream);
        final Model problemsModel = new RdfStream().asModel();
        when(getProblemsModel()).thenReturn(problemsModel);
        final Dataset dataset =
                testObj.getPropertiesDataset(mockSubjects, 0, -1);

        assertTrue(dataset.getDefaultModel().containsAll(
                propertiesStream.asModel()));
        assertTrue(dataset.getDefaultModel().containsAll(
                propertiesStream.asModel()));
        assertEquals(mockResource, dataset.getContext().get(
                Symbol.create("uri")));
    }

    @Test
    public void testGetPropertiesDatasetDefaultLimits()
        throws Exception {

        mockStatic(JcrRdfTools.class);
        final IdentifierTranslator mockSubjects = mock(IdentifierTranslator.class);
        when(JcrRdfTools.withContext(mockSubjects, mockSession)).thenReturn(mockJcrRdfTools);
        when(mockSubjects.getSubject(mockNode.getPath())).thenReturn(mockResource);

        final RdfStream propertiesStream = new RdfStream(mockTriple);
        when(mockJcrRdfTools.getJcrTriples(mockNode)).thenReturn(propertiesStream);
        final RdfStream treeStream = new RdfStream(mockTriple);
        when(mockJcrRdfTools.getTreeTriples(eq(mockNode),
                                               any(HierarchyRdfContextOptions.class))).thenReturn(treeStream);
        final Model problemsModel = createDefaultModel();
        when(getProblemsModel()).thenReturn(problemsModel);
        final Dataset dataset = testObj.getPropertiesDataset(mockSubjects);

        assertTrue(dataset.getDefaultModel().containsAll(treeStream.asModel()));

        assertTrue(dataset.getDefaultModel().containsAll(
                propertiesStream.asModel()));
        assertEquals(mockResource, dataset.getContext().get(Symbol.create("uri")));
    }

    @Test
    public void testGetVersionDataset() throws Exception {

        mockStatic(JcrRdfTools.class);
        final IdentifierTranslator mockSubjects = mock(IdentifierTranslator.class);

        when(JcrRdfTools.withContext(mockSubjects, mockSession)).thenReturn(mockJcrRdfTools);
        when(mockSubjects.getSubject(mockNode.getPath())).thenReturn(mockResource);

        final RdfStream versionsStream = new RdfStream();
        when(mockJcrRdfTools.getVersionTriples(any(Node.class)))
                .thenReturn(versionsStream);
        final RdfStream result = testObj.getVersionTriples(mockSubjects);
        assertEquals(versionsStream, result);
    }

    @Test
    public void testAddVersionLabel() throws RepositoryException {

        final VersionHistory mockVersionHistory = mock(VersionHistory.class);
        final Version mockVersion = mock(Version.class);
        when(mockVersion.getName()).thenReturn("uuid");
        final Workspace mockWorkspace = mock(Workspace.class);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        final VersionManager mockVersionManager = mock(VersionManager.class);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);

        when(mockVersionManager.getBaseVersion(anyString())).thenReturn(
                mockVersion);

        when(mockVersionManager.getVersionHistory(anyString())).thenReturn(
                mockVersionHistory);

        testObj.addVersionLabel("v1.0.0");
        verify(mockVersionHistory).addVersionLabel("uuid", "v1.0.0", true);
    }

    @Test
    public void testGetBaseVersion() throws RepositoryException {

        final Version mockVersion = mock(Version.class);
        when(mockVersion.getName()).thenReturn("uuid");
        final Workspace mockWorkspace = mock(Workspace.class);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        final VersionManager mockVersionManager = mock(VersionManager.class);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);

        when(mockVersionManager.getBaseVersion(anyString())).thenReturn(
                mockVersion);

        testObj.getBaseVersion();

        verify(mockVersionManager).getBaseVersion(anyString());
    }

    @Test
    public void testGetVersionHistory() throws RepositoryException {

        final VersionHistory mockVersionHistory = mock(VersionHistory.class);
        final Version mockVersion = mock(Version.class);
        when(mockVersion.getName()).thenReturn("uuid");
        final Workspace mockWorkspace = mock(Workspace.class);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        final VersionManager mockVersionManager = mock(VersionManager.class);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);

        when(mockVersionManager.getVersionHistory(anyString())).thenReturn(
                mockVersionHistory);

        testObj.getVersionHistory();

        verify(mockVersionManager).getVersionHistory(anyString());
    }

    @Test
    public void testIsNew() {
        when(mockNode.isNew()).thenReturn(true);
        assertTrue("resource state should be the same as the node state",
                testObj.isNew());
    }

    @Test
    public void testIsNotNew() {
        when(mockNode.isNew()).thenReturn(false);
        assertFalse("resource state should be the same as the node state",
                testObj.isNew());
    }

    @Test
    public void testReplacePropertiesDataset() throws Exception {

        mockStatic(JcrRdfTools.class);
        final DefaultIdentifierTranslator defaultGraphSubjects = new DefaultIdentifierTranslator();
        when(JcrRdfTools.withContext(defaultGraphSubjects, mockSession)).thenReturn(mockJcrRdfTools);

        when(mockNode.getPath()).thenReturn("/xyz");

        final Model propertiesModel = createDefaultModel();
        propertiesModel.add(propertiesModel.createResource("a"),
                               propertiesModel.createProperty("b"),
                               "c");


        propertiesModel.add(propertiesModel.createResource("i"),
                               propertiesModel.createProperty("j"),
                               "k");

        propertiesModel.add(propertiesModel.createResource("x"),
                               propertiesModel.createProperty("y"),
                               "z");
        final RdfStream propertiesStream = RdfStream.fromModel(propertiesModel);
        when(mockJcrRdfTools.getJcrTriples(mockNode)).thenReturn(propertiesStream);

        final RdfStream treeStream = new RdfStream();
        when(mockJcrRdfTools.getTreeTriples(mockNode)).thenReturn(treeStream);
        final Model problemsModel = createDefaultModel();
        when(getProblemsModel()).thenReturn(problemsModel);

        final Model replacementModel = createDefaultModel();

        replacementModel.add(replacementModel.createResource("a"),
                                replacementModel.createProperty("b"),
                               "n");


        replacementModel.add(replacementModel.createResource("i"),
                                replacementModel.createProperty("j"),
                               "k");

        final Model replacements = testObj.replaceProperties(defaultGraphSubjects, replacementModel).asModel();

        assertTrue(replacements.containsAll(replacementModel));

        assertFalse(problemsModel.contains(propertiesModel.createResource("x"),
                                              propertiesModel.createProperty("y"),
                                              "z"));
    }
    @Test
    public void shouldGetEtagForAnObject() throws RepositoryException {
        final Property mockMod = mock(Property.class);
        final Calendar modDate = Calendar.getInstance();
        modDate.set(2013, JULY, 30, 0, 0, 0);
        when(mockNode.getPath()).thenReturn("some-path");
        when(mockNode.hasProperty(JCR_LASTMODIFIED)).thenReturn(true);
        when(mockNode.getProperty(JCR_LASTMODIFIED)).thenReturn(mockMod);
        when(mockMod.getDate()).thenReturn(modDate);

        assertEquals(shaHex("some-path"
                + testObj.getLastModifiedDate().toString()), testObj
                .getEtagValue());
    }

    @Test
    public void testStaticFindChildren() throws Exception {
        final Node mockChildNode = mock(Node.class);
        final IdentifierTranslator mockConverter = mock(IdentifierTranslator.class);
        final NodeIterator mockNodeIterator = mock(NodeIterator.class);
        when(mockNode.getNodes()).thenReturn(mockNodeIterator);
        when(mockNodeIterator.hasNext()).thenReturn(true, false);
        when(mockNodeIterator.nextNode()).thenReturn(mockChildNode);
        when( mockChildNode.getPath() ).thenReturn("/foo/bar");
        when( mockChildNode.getName() ).thenReturn("bar");
        when( mockChildNode.getPrimaryNodeType() ).thenReturn(mockNodeType);
        when( mockNodeType.isNodeType(anyString()) ).thenReturn(false);
        final Iterator<Node> result = FedoraResourceImpl.getChildren(mockNode, mockConverter);
        assertEquals( "Child not found", mockChildNode, result.next());
    }

    @Test
    public void testFindChildren() throws Exception {
        final Node mockChildNode = mock(Node.class);
        final IdentifierTranslator mockConverter = mock(IdentifierTranslator.class);
        final NodeIterator mockNodeIterator = mock(NodeIterator.class);
        when(mockNode.getNodes()).thenReturn(mockNodeIterator);
        when(mockNodeIterator.hasNext()).thenReturn(true, false);
        when(mockNodeIterator.nextNode()).thenReturn(mockChildNode);
        when( mockChildNode.getPath() ).thenReturn("/foo/bar");
        when( mockChildNode.getName() ).thenReturn("bar");
        when( mockChildNode.getPrimaryNodeType() ).thenReturn(mockNodeType);
        when( mockNodeType.isNodeType(anyString()) ).thenReturn(false);
        final Iterator<Node> result = testObj.getChildren(mockConverter);
        assertEquals( "Child not found", mockChildNode, result.next());
    }

}
