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
package org.fcrepo.kernel.impl;
import static com.hp.hpl.jena.graph.NodeFactory.createAnon;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static java.util.Calendar.JULY;
import static org.apache.commons.codec.digest.DigestUtils.shaHex;
import static org.fcrepo.jcr.FedoraJcrTypes.JCR_CREATED;
import static org.fcrepo.jcr.FedoraJcrTypes.JCR_LASTMODIFIED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.rdf.model.ResIterator;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.testutilities.TestTriplesContext;
import org.fcrepo.kernel.impl.rdf.JcrRdfTools;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * <p>FedoraResourceImplTest class.</p>
 *
 * @author ajs6f
 */
public class FedoraResourceImplTest {

    private FedoraResource testObj;

    @Mock
    private Node mockNode;

    @Mock
    private Node mockRoot;

    @Mock
    private Session mockSession;

    @Mock
    private Property mockProp;

    private final Triple mockTriple =
        create(createAnon(), createAnon(), createAnon());

    @Mock
    private JcrRdfTools mockJcrRdfTools;

    private IdentifierConverter mockSubjects;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockNode.getPath()).thenReturn("/some/path");
        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.getName()).thenReturn("nt:folder");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        testObj = new FedoraResourceImpl(mockNode);
        assertEquals(mockNode, testObj.getNode());

        mockSubjects = new DefaultIdentifierTranslator(mockSession);
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
    public void testGetLastModifiedDate() {
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
    public void testGetTriples() {
        final IdentifierConverter mockSubjects = mock(IdentifierConverter.class);

        final RdfStream triples = testObj.getTriples(mockSubjects, TestTriplesContext.class);

        final Model model = triples.asModel();

        final ResIterator resIterator = model.listSubjects();

        final ImmutableSet<String> resources = ImmutableSet.copyOf(
                Iterators.transform(resIterator,
                        new Function<Resource, String>() {
                            @Override
                            public String apply(final Resource resource) {
                                return resource.getURI();
                            }
                        }));

        assertTrue(resources.contains("MockTriplesContextClass"));
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

        final DefaultIdentifierTranslator defaultGraphSubjects = new DefaultIdentifierTranslator(mockSession);

        when(mockNode.getPath()).thenReturn("/xyz");
        when(mockSession.getNode("/xyz")).thenReturn(mockNode);

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

        final Model replacementModel = createDefaultModel();

        replacementModel.add(replacementModel.createResource("a"),
                                replacementModel.createProperty("b"),
                               "n");


        replacementModel.add(replacementModel.createResource("i"),
                                replacementModel.createProperty("j"),
                               "k");

        final Model replacements = testObj.replaceProperties(defaultGraphSubjects,
                replacementModel,
                propertiesStream).asModel();

        assertTrue(replacements.containsAll(replacementModel));
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
                + testObj.getLastModifiedDate().getTime()), testObj
                .getEtagValue());
    }

}
