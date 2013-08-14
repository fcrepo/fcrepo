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

package org.fcrepo.kernel;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.FedoraResource.hasMixin;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.fcrepo.jcr.FedoraJcrTypes.JCR_CREATED;
import static org.fcrepo.jcr.FedoraJcrTypes.JCR_LASTMODIFIED;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getBaseVersion;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getVersionHistory;
import static org.fcrepo.kernel.utils.JcrRdfTools.getProblemsModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.rdf.impl.DefaultGraphSubjects;
import org.fcrepo.kernel.utils.FedoraTypesUtils;
import org.fcrepo.kernel.utils.JcrRdfTools;
import org.fcrepo.kernel.utils.NamespaceTools;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.util.Symbol;

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
@PrepareForTest({NamespaceTools.class, JcrRdfTools.class,
        FedoraTypesUtils.class})
public class FedoraResourceTest {

    private FedoraResource testObj;

    @Mock
    private Node mockNode;

    @Mock
    private Node mockRoot;

    @Mock
    private Session mockSession;

    @Mock
    private Property mockProp;

    @Mock
    private JcrRdfTools mockJcrRdfTools;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockNode.getSession()).thenReturn(mockSession);
        testObj = new FedoraResource(mockNode);
        assertEquals(mockNode, testObj.getNode());
    }

    @Test
    public void testPathConstructor() throws RepositoryException {
        when(mockSession.getRootNode()).thenReturn(mockRoot);
        when(mockRoot.getNode("foo/bar")).thenReturn(mockNode);
        when(mockNode.isNew()).thenReturn(true);
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[] {});
        new FedoraResource(mockSession, "/foo/bar", null);
    }

    @Test
    public void testHasMixin() throws RepositoryException {
        boolean actual;
        final NodeType mockType = mock(NodeType.class);
        final NodeType[] mockTypes = new NodeType[] {mockType};
        when(mockNode.getMixinNodeTypes()).thenReturn(mockTypes);
        actual = hasMixin(mockNode);
        assertEquals(false, actual);
        when(mockType.getName()).thenReturn(FEDORA_RESOURCE);
        actual = hasMixin(mockNode);
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
        final GraphSubjects mockSubjects = mock(GraphSubjects.class);

        when(JcrRdfTools.withContext(mockSubjects, mockSession)).thenReturn(mockJcrRdfTools);

        final Resource mockResource =
                new DummyURIResource(RdfLexicon.RESTAPI_NAMESPACE + "xyz");
        when(mockSubjects.getGraphSubject(mockNode)).thenReturn(mockResource);

        final Model propertiesModel = createDefaultModel();
        when(mockJcrRdfTools.getJcrPropertiesModel(mockNode)).thenReturn(
                propertiesModel);
        final Model treeModel = createDefaultModel();
        when(mockJcrRdfTools.getJcrTreeModel(mockNode, 0, -1)).thenReturn(
                treeModel);
        final Model problemsModel = createDefaultModel();
        when(getProblemsModel()).thenReturn(problemsModel);
        final Dataset dataset =
                testObj.getPropertiesDataset(mockSubjects, 0, -1);

        assertTrue(dataset.containsNamedModel("tree"));
        assertEquals(treeModel, dataset.getNamedModel("tree"));

        assertEquals(propertiesModel, dataset.getDefaultModel());
        assertEquals(RdfLexicon.RESTAPI_NAMESPACE + "xyz",
                dataset.getContext().get(Symbol.create("uri")));
    }

    @Test
    public void testGetPropertiesDatasetDefaultLimits()
        throws Exception {

        mockStatic(JcrRdfTools.class);
        final GraphSubjects mockSubjects = mock(GraphSubjects.class);
        when(JcrRdfTools.withContext(mockSubjects, mockSession)).thenReturn(mockJcrRdfTools);
        final Resource mockResource =
                new DummyURIResource(RdfLexicon.RESTAPI_NAMESPACE + "xyz");
        when(mockSubjects.getGraphSubject(mockNode)).thenReturn(mockResource);

        final Model propertiesModel = createDefaultModel();
        when(mockJcrRdfTools.getJcrPropertiesModel(mockNode)).thenReturn(
                                                                            propertiesModel);
        final Model treeModel = createDefaultModel();
        when(mockJcrRdfTools.getJcrTreeModel(mockNode, 0, -1)).thenReturn(
                                                                             treeModel);
        final Model problemsModel = createDefaultModel();
        when(getProblemsModel()).thenReturn(problemsModel);
        final Dataset dataset = testObj.getPropertiesDataset(mockSubjects);

        assertTrue(dataset.containsNamedModel("tree"));
        assertEquals(treeModel, dataset.getNamedModel("tree"));

        assertEquals(propertiesModel, dataset.getDefaultModel());
        assertEquals(RdfLexicon.RESTAPI_NAMESPACE + "xyz",
                dataset.getContext().get(Symbol.create("uri")));
    }

    @Test
    public void testGetVersionDataset() throws Exception {

        mockStatic(FedoraTypesUtils.class);
        when(FedoraTypesUtils.getVersionHistory(mockNode)).thenReturn(mock(VersionHistory.class));

        mockStatic(JcrRdfTools.class);
        final GraphSubjects mockSubjects = mock(GraphSubjects.class);

        when(JcrRdfTools.withContext(mockSubjects, mockSession)).thenReturn(mockJcrRdfTools);
        final Resource mockResource =
                new DummyURIResource(RdfLexicon.RESTAPI_NAMESPACE + "xyz");
        when(mockSubjects.getGraphSubject(mockNode)).thenReturn(mockResource);

        final Model versionsModel = createDefaultModel();
        when(mockJcrRdfTools.getJcrPropertiesModel(any(VersionHistory.class), eq(mockResource))).thenReturn(
                                                                                                               versionsModel);
        final Dataset dataset = testObj.getVersionDataset(mockSubjects);

        assertEquals(versionsModel, dataset.getDefaultModel());
        assertEquals(RdfLexicon.RESTAPI_NAMESPACE + "xyz",
                dataset.getContext().get(
                Symbol.create("uri")));
    }

    @Test
    public void testAddVersionLabel() throws RepositoryException {

        mockStatic(FedoraTypesUtils.class);
        final VersionHistory mockVersionHistory = mock(VersionHistory.class);
        final Version mockVersion = mock(Version.class);
        when(mockVersion.getName()).thenReturn("uuid");
        when(getBaseVersion(mockNode)).thenReturn(mockVersion);
        when(getVersionHistory(mockNode)).thenReturn(mockVersionHistory);

        testObj.addVersionLabel("v1.0.0");
        verify(mockVersionHistory).addVersionLabel("uuid", "v1.0.0", true);
    }

    @Test
    public void testIsNew() throws RepositoryException {
        when(mockNode.isNew()).thenReturn(true);
        assertTrue("resource state should be the same as the node state",
                testObj.isNew());
    }

    @Test
    public void testIsNotNew() throws RepositoryException {
        when(mockNode.isNew()).thenReturn(false);
        assertFalse("resource state should be the same as the node state",
                testObj.isNew());
    }

    @Test
    public void testReplacePropertiesDataset() throws RepositoryException {

        mockStatic(JcrRdfTools.class);
        final DefaultGraphSubjects defaultGraphSubjects = new DefaultGraphSubjects(mockSession);
        when(JcrRdfTools.withContext(defaultGraphSubjects, mockSession)).thenReturn(mockJcrRdfTools);

        when(mockNode.getPath()).thenReturn("/xyz");

        final Model propertiesModel = ModelFactory.createDefaultModel();
        propertiesModel.add(propertiesModel.createResource("a"),
                               propertiesModel.createProperty("b"),
                               "c");


        propertiesModel.add(propertiesModel.createResource("i"),
                               propertiesModel.createProperty("j"),
                               "k");

        propertiesModel.add(propertiesModel.createResource("x"),
                               propertiesModel.createProperty("y"),
                               "z");
        when(mockJcrRdfTools.getJcrPropertiesModel(mockNode)).thenReturn(propertiesModel);

        final Model treeModel = createDefaultModel();
        when(mockJcrRdfTools.getJcrTreeModel(mockNode, 0, -2)).thenReturn(
                                                                             treeModel);
        final Model problemsModel = createDefaultModel();
        when(getProblemsModel()).thenReturn(problemsModel);

        final Model replacementModel = ModelFactory.createDefaultModel();

        replacementModel.add(replacementModel.createResource("a"),
                                replacementModel.createProperty("b"),
                               "n");


        replacementModel.add(replacementModel.createResource("i"),
                                replacementModel.createProperty("j"),
                               "k");

        testObj.replacePropertiesDataset(defaultGraphSubjects, replacementModel);

        assertTrue(propertiesModel.containsAll(replacementModel));

        assertFalse(problemsModel.contains(propertiesModel.createResource("x"),
                                              propertiesModel.createProperty("y"),
                                              "z"));
    }
    @Test
    public void shouldGetEtagForAnObject() throws RepositoryException {
        final Property mockMod = mock(Property.class);
        final Calendar modDate = Calendar.getInstance();
        modDate.set(2013, Calendar.JULY, 30, 0, 0, 0);
        when(mockNode.getPath()).thenReturn("some-path");
        when(mockNode.hasProperty(JCR_LASTMODIFIED)).thenReturn(true);
        when(mockNode.getProperty(JCR_LASTMODIFIED)).thenReturn(mockMod);
        when(mockMod.getDate()).thenReturn(modDate);

        assertEquals(DigestUtils.shaHex("some-path" + testObj.getLastModifiedDate().toString()), testObj.getEtagValue());
    }

}
