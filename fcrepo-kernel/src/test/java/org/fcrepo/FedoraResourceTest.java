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

package org.fcrepo;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.FedoraResource.DEFAULT_SUBJECT_FACTORY;
import static org.fcrepo.FedoraResource.hasMixin;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.fcrepo.utils.FedoraJcrTypes.JCR_CREATED;
import static org.fcrepo.utils.FedoraJcrTypes.JCR_LASTMODIFIED;
import static org.fcrepo.utils.FedoraTypesUtils.getBaseVersion;
import static org.fcrepo.utils.FedoraTypesUtils.getVersionHistory;
import static org.fcrepo.utils.JcrRdfTools.getGraphSubject;
import static org.fcrepo.utils.JcrRdfTools.getJcrPropertiesModel;
import static org.fcrepo.utils.JcrRdfTools.getJcrTreeModel;
import static org.fcrepo.utils.JcrRdfTools.getJcrVersionsModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.utils.FedoraTypesUtils;
import org.fcrepo.utils.JcrPropertyStatementListener;
import org.fcrepo.utils.JcrRdfTools;
import org.fcrepo.utils.NamespaceTools;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.modeshape.common.collection.Problems;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.util.Symbol;

@RunWith(PowerMockRunner.class)
// PowerMock needs to ignore some packages to prevent class-cast errors
@PowerMockIgnore({"org.slf4j.*", "org.apache.xerces.*", "javax.xml.*",
        "org.xml.sax.*", "javax.management.*"})
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

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new FedoraResource(mockNode);
        assertEquals(mockNode, testObj.getNode());
    }

    @Test
    public void testPathConstructor() throws RepositoryException {
        when(mockSession.getRootNode()).thenReturn(mockRoot);
        when(mockRoot.getNode("foo/bar")).thenReturn(mockNode);
        when(mockNode.isNew()).thenReturn(true);
        when(mockNode.getSession()).thenReturn(mockSession);
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
    public void testGetPropertiesDataset() throws RepositoryException {

        mockStatic(JcrRdfTools.class);
        final GraphSubjects mockSubjects = mock(GraphSubjects.class);
        final Resource mockResource = mock(Resource.class);
        when(mockResource.getURI()).thenReturn("info:fedora/xyz");
        when(getGraphSubject(mockSubjects, mockNode)).thenReturn(mockResource);

        final Model propertiesModel = createDefaultModel();
        when(getJcrPropertiesModel(mockSubjects, mockNode)).thenReturn(
                propertiesModel);
        final Model treeModel = createDefaultModel();
        when(getJcrTreeModel(mockSubjects, mockNode, 0, -1)).thenReturn(
                treeModel);
        final Dataset dataset =
                testObj.getPropertiesDataset(mockSubjects, 0, -1);

        assertTrue(dataset.containsNamedModel("tree"));
        assertEquals(treeModel, dataset.getNamedModel("tree"));

        assertEquals(propertiesModel, dataset.getDefaultModel());
        assertEquals("info:fedora/xyz", dataset.getContext().get(
                Symbol.create("uri")));
    }

    @Test
    public void testGetPropertiesDatasetDefaultLimits()
            throws RepositoryException {

        mockStatic(JcrRdfTools.class);
        final GraphSubjects mockSubjects = mock(GraphSubjects.class);
        final Resource mockResource = mock(Resource.class);
        when(mockResource.getURI()).thenReturn("info:fedora/xyz");
        when(getGraphSubject(mockSubjects, mockNode)).thenReturn(mockResource);

        final Model propertiesModel = createDefaultModel();
        when(getJcrPropertiesModel(mockSubjects, mockNode)).thenReturn(
                propertiesModel);
        final Model treeModel = createDefaultModel();
        when(getJcrTreeModel(mockSubjects, mockNode, 0, -1)).thenReturn(
                treeModel);
        final Dataset dataset = testObj.getPropertiesDataset(mockSubjects);

        assertTrue(dataset.containsNamedModel("tree"));
        assertEquals(treeModel, dataset.getNamedModel("tree"));

        assertEquals(propertiesModel, dataset.getDefaultModel());
        assertEquals("info:fedora/xyz", dataset.getContext().get(
                Symbol.create("uri")));
    }

    @Test
    public void testGetPropertiesDatasetDefaults() throws RepositoryException {

        mockStatic(JcrRdfTools.class);
        final Resource mockResource = mock(Resource.class);
        when(mockResource.getURI()).thenReturn("info:fedora/xyz");
        when(getGraphSubject(DEFAULT_SUBJECT_FACTORY, mockNode)).thenReturn(
                mockResource);

        final Model propertiesModel = createDefaultModel();
        when(getJcrPropertiesModel(DEFAULT_SUBJECT_FACTORY, mockNode))
                .thenReturn(propertiesModel);
        final Model treeModel = createDefaultModel();
        when(getJcrTreeModel(DEFAULT_SUBJECT_FACTORY, mockNode, 0, -1))
                .thenReturn(treeModel);
        final Dataset dataset = testObj.getPropertiesDataset();

        assertTrue(dataset.containsNamedModel("tree"));
        assertEquals(treeModel, dataset.getNamedModel("tree"));

        assertEquals(propertiesModel, dataset.getDefaultModel());
        assertEquals("info:fedora/xyz", dataset.getContext().get(
                Symbol.create("uri")));
    }

    @Test
    public void testGetVersionDataset() throws RepositoryException {

        mockStatic(JcrRdfTools.class);
        final GraphSubjects mockSubjects = mock(GraphSubjects.class);
        final Resource mockResource = mock(Resource.class);
        when(mockResource.getURI()).thenReturn("info:fedora/xyz");
        when(getGraphSubject(mockSubjects, mockNode)).thenReturn(mockResource);

        final Model versionsModel = createDefaultModel();
        when(getJcrVersionsModel(mockSubjects, mockNode)).thenReturn(
                versionsModel);
        final Dataset dataset = testObj.getVersionDataset(mockSubjects);

        assertEquals(versionsModel, dataset.getDefaultModel());
        assertEquals("info:fedora/xyz", dataset.getContext().get(
                Symbol.create("uri")));
    }

    @Test
    public void testGetVersionDatasetDefaultSubject()
            throws RepositoryException {

        mockStatic(JcrRdfTools.class);
        final Resource mockResource = mock(Resource.class);
        when(mockResource.getURI()).thenReturn("info:fedora/xyz");
        when(getGraphSubject(DEFAULT_SUBJECT_FACTORY, mockNode)).thenReturn(
                mockResource);

        final Model versionsModel = createDefaultModel();
        when(getJcrVersionsModel(DEFAULT_SUBJECT_FACTORY, mockNode))
                .thenReturn(versionsModel);
        final Dataset dataset = testObj.getVersionDataset();

        assertEquals(versionsModel, dataset.getDefaultModel());
        assertEquals("info:fedora/xyz", dataset.getContext().get(
                Symbol.create("uri")));
    }

    @Test
    public void testGetGraphProblems() throws RepositoryException {
        final Problems actual = testObj.getDatasetProblems();
        assertEquals(null, actual);
        final JcrPropertyStatementListener mockListener =
                mock(JcrPropertyStatementListener.class);
        setField("listener", FedoraResource.class, mockListener, testObj);
        testObj.getDatasetProblems();
        verify(mockListener).getProblems();
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

    private static void setField(final String name, final Class<?> clazz,
            final Object value, final Object object) {
        try {
            final Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            field.set(object, value);
        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }
}
