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
package org.fcrepo.http.api.url;

import static com.google.common.collect.ImmutableSet.of;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.jcr.FedoraJcrTypes.ROOT;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_SERVICE;
import static org.fcrepo.kernel.RdfLexicon.HAS_SERIALIZATION;
import static org.fcrepo.kernel.RdfLexicon.HAS_TRANSACTION_SERVICE;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION_HISTORY;
import static org.jgroups.util.Util.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.http.commons.api.rdf.UriAwareIdentifierConverter;
import org.fcrepo.kernel.impl.FedoraResourceImpl;
import org.fcrepo.serialization.SerializerUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.util.HashSet;

/**
 * <p>HttpApiResourcesTest class.</p>
 *
 * @author awoods
 */
public class HttpApiResourcesTest {

    private HttpApiResources testObj;

    @Mock
    private Node mockNode;

    private FedoraResourceImpl mockResource;

    private UriInfo uriInfo;

    @Mock
    private NodeType mockNodeType;

    private UriAwareIdentifierConverter mockSubjects;

    @Mock
    private SerializerUtil mockSerializers;

    @Mock
    private Session mockSession;

    @Mock
    private Repository mockRepository;

    @Mock
    private Workspace mockWorkspace;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new HttpApiResources();
        mockResource = new FedoraResourceImpl(mockNode);
        uriInfo = getUriInfoImpl();
        when(mockSession.getRepository()).thenReturn(mockRepository);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getName()).thenReturn("default");
        mockSubjects = new UriAwareIdentifierConverter(mockSession, UriBuilder.fromUri("http://localhost/{path: .*}"));
        setField(testObj, "serializers", mockSerializers);
    }

    @Test
    public void shouldDecorateModeRootNodesWithRepositoryWideLinks()
        throws RepositoryException {
        when(mockNodeType.isNodeType(ROOT)).thenReturn(true);
        when(mockNodeType.getName()).thenReturn("nt:root");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getPath()).thenReturn("/");

        final Resource graphSubject = mockSubjects.reverse().convert(mockNode);

        final Model model =
            testObj.createModelForResource(mockResource, uriInfo, mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_TRANSACTION_SERVICE));
    }

    @Test
    public void shouldDecorateNodesWithLinksToVersionsAndExport()
        throws RepositoryException {

        when(mockNodeType.getName()).thenReturn("nt:folder");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.isNodeType(NodeType.MIX_VERSIONABLE)).thenReturn(true);
        when(mockNode.getPath()).thenReturn("/some/path/to/object");

        when(mockSerializers.keySet()).thenReturn(of("a", "b"));
        final Resource graphSubject = mockSubjects.reverse().convert(mockNode);

        final Model model =
            testObj.createModelForResource(mockResource, uriInfo, mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_VERSION_HISTORY));
        assertEquals(2, model.listObjectsOfProperty(graphSubject,
                HAS_SERIALIZATION).toSet().size());
    }

    @Test
    public void shouldNotDecorateNodesWithLinksToVersionsUnlessVersionable()
            throws RepositoryException {

        when(mockNodeType.getName()).thenReturn("nt:folder");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.isNodeType(NodeType.MIX_VERSIONABLE)).thenReturn(false);
        when(mockNode.getPath()).thenReturn("/some/path/to/object");

        when(mockSerializers.keySet()).thenReturn(of("a", "b"));
        final Resource graphSubject = mockSubjects.reverse().convert(mockNode);

        final Model model =
                testObj.createModelForResource(mockResource, uriInfo, mockSubjects);

        assertFalse(model.contains(graphSubject, HAS_VERSION_HISTORY));
    }

    @Test
    public void shouldDecorateDatastreamsWithLinksToFixityChecks()
        throws RepositoryException {
        when(mockNode.hasNode(JCR_CONTENT)).thenReturn(true);
        when(mockNodeType.getName()).thenReturn("nt:file");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getPath()).thenReturn("/some/path/to/datastream");
        when(mockSerializers.keySet()).thenReturn(new HashSet<String>());
        final Resource graphSubject = mockSubjects.reverse().convert(mockNode);

        final Model model =
            testObj.createModelForResource(mockResource, uriInfo, mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_FIXITY_SERVICE));
    }

    @Test
    public void shouldDecorateRootNodeWithCorrectResourceURI()
            throws RepositoryException {
        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.isNodeType(ROOT)).thenReturn(true);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);

        when(mockSerializers.keySet()).thenReturn(of("a"));
        when(mockNode.getPath()).thenReturn("/");

        final Resource graphSubject = mockSubjects.reverse().convert(mockNode);
        final Model model =
                testObj.createModelForResource(mockResource, uriInfo,
                        mockSubjects);
        assertEquals("http://localhost/fcrepo/fcr:export?format=a", model
                .getProperty(graphSubject, HAS_SERIALIZATION).getResource()
                .getURI());
    }

    @Test
    public void shouldDecorateOtherNodesWithCorrectResourceURI()
            throws RepositoryException {
        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.isNodeType(ROOT)).thenReturn(false);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getName()).thenReturn("not-frozen");

        when(mockSerializers.keySet()).thenReturn(of("a"));
        when(mockNode.getPath()).thenReturn("/some/path/to/object");

        final Resource graphSubject = mockSubjects.reverse().convert(mockNode);
        final Model model =
                testObj.createModelForResource(mockResource, uriInfo,
                        mockSubjects);
        assertEquals(
                "http://localhost/fcrepo/some/path/to/object/fcr:export?format=a",
                model.getProperty(graphSubject, HAS_SERIALIZATION)
                        .getResource().getURI());
    }

}
