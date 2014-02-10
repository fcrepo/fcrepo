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

package org.fcrepo.http.api.url;

import static com.google.common.collect.ImmutableSet.of;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.fcrepo.jcr.FedoraJcrTypes.ROOT;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_SERVICE;
import static org.fcrepo.kernel.RdfLexicon.HAS_NAMESPACE_SERVICE;
import static org.fcrepo.kernel.RdfLexicon.HAS_SEARCH_SERVICE;
import static org.fcrepo.kernel.RdfLexicon.HAS_SERIALIZATION;
import static org.fcrepo.kernel.RdfLexicon.HAS_SITEMAP;
import static org.fcrepo.kernel.RdfLexicon.HAS_TRANSACTION_SERVICE;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION_HISTORY;
import static org.fcrepo.kernel.RdfLexicon.HAS_WORKSPACE_SERVICE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

import java.util.HashSet;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.http.api.FedoraNodes;
import org.fcrepo.http.commons.api.rdf.HttpGraphSubjects;
import org.fcrepo.kernel.FedoraResourceImpl;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.serialization.SerializerUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class HttpApiResourcesTest {

    private HttpApiResources testObj;

    @Mock
    private Node mockNode;

    private FedoraResourceImpl mockResource;

    private UriInfo uriInfo;

    @Mock
    private NodeType mockNodeType;

    private GraphSubjects mockSubjects;

    @Mock
    private SerializerUtil mockSerializers;

    @Mock
    private Session mockSession;

    @Mock
    private Repository mockRepository;

    @Before
    public void setUp() throws NoSuchFieldException {
        initMocks(this);
        testObj = new HttpApiResources();
        mockResource = new FedoraResourceImpl(mockNode);
        uriInfo = getUriInfoImpl();
        when(mockSession.getRepository()).thenReturn(mockRepository);
        mockSubjects =
            new HttpGraphSubjects(mockSession, FedoraNodes.class,
                    uriInfo);
        setField(testObj, "serializers", mockSerializers);
    }

    @Test
    public void shouldDecorateModeRootNodesWithRepositoryWideLinks()
        throws RepositoryException {
        when(mockNodeType.isNodeType(ROOT)).thenReturn(true);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getPath()).thenReturn("/");

        final Resource graphSubject = mockSubjects.getGraphSubject(mockNode);

        final Model model =
            testObj.createModelForResource(mockResource, uriInfo, mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_SEARCH_SERVICE));
        assertTrue(model.contains(graphSubject, HAS_SITEMAP));
        assertTrue(model.contains(graphSubject, HAS_TRANSACTION_SERVICE));
        assertTrue(model.contains(graphSubject, HAS_NAMESPACE_SERVICE));
        assertTrue(model.contains(graphSubject, HAS_WORKSPACE_SERVICE));
    }

    @Test
    public void shouldDecorateNodesWithLinksToVersionsAndExport()
        throws RepositoryException {

        when(mockNode.getPrimaryNodeType()).thenReturn(mock(NodeType.class));
        when(mockNode.getPath()).thenReturn("/some/path/to/object");

        when(mockSerializers.keySet()).thenReturn(of("a", "b"));
        final Resource graphSubject = mockSubjects.getGraphSubject(mockNode);

        final Model model =
            testObj.createModelForResource(mockResource, uriInfo, mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_VERSION_HISTORY));
        assertEquals(2, model.listObjectsOfProperty(graphSubject,
                HAS_SERIALIZATION).toSet().size());
    }

    @Test
    public void shouldDecorateDatastreamsWithLinksToFixityChecks()
        throws RepositoryException {
        when(mockNode.hasNode(JCR_CONTENT)).thenReturn(true);
        when(mockNode.getPrimaryNodeType()).thenReturn(mock(NodeType.class));
        when(mockNode.getPath()).thenReturn("/some/path/to/datastream");
        when(mockSerializers.keySet()).thenReturn(new HashSet<String>());
        final Resource graphSubject = mockSubjects.getGraphSubject(mockNode);

        final Model model =
            testObj.createModelForResource(mockResource, uriInfo, mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_FIXITY_SERVICE));
    }

}
