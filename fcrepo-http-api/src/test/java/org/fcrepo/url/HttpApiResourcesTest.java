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

package org.fcrepo.url;

import static org.fcrepo.RdfLexicon.HAS_FIXITY_SERVICE;
import static org.fcrepo.RdfLexicon.HAS_NAMESPACE_SERVICE;
import static org.fcrepo.RdfLexicon.HAS_SEARCH_SERVICE;
import static org.fcrepo.RdfLexicon.HAS_SERIALIZATION;
import static org.fcrepo.RdfLexicon.HAS_SITEMAP;
import static org.fcrepo.RdfLexicon.HAS_TRANSACTION_SERVICE;
import static org.fcrepo.RdfLexicon.HAS_VERSION_HISTORY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.FedoraResource;
import org.fcrepo.api.FedoraNodes;
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.serialization.SerializerUtil;
import org.fcrepo.test.util.TestHelpers;
import org.fcrepo.utils.FedoraJcrTypes;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;

import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class HttpApiResourcesTest {

    private HttpApiResources testObj;

    private Node mockNode;

    private FedoraResource mockResource;

    private UriInfo uriInfo;

    private GraphSubjects mockSubjects;

    private SerializerUtil mockSerializers;

    @Before
    public void setUp() throws NoSuchFieldException {
        testObj = new HttpApiResources();
        mockNode = mock(Node.class);
        mockResource = new FedoraResource(mockNode);

        uriInfo = TestHelpers.getUriInfoImpl();
        mockSubjects = new HttpGraphSubjects(FedoraNodes.class, uriInfo);

        mockSerializers = mock(SerializerUtil.class);
        TestHelpers.setField(testObj, "serializers", mockSerializers);
    }

    @Test
    public void shouldDecorateModeRootNodesWithRepositoryWideLinks()
        throws RepositoryException {

        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.isNodeType(FedoraJcrTypes.ROOT)).thenReturn(true);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getPath()).thenReturn("/");

        Resource graphSubject = mockSubjects.getGraphSubject(mockNode);

        final Model model =
                testObj.createModelForResource(mockResource, uriInfo,
                        mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_SEARCH_SERVICE));
        assertTrue(model.contains(graphSubject, HAS_SITEMAP));
        assertTrue(model.contains(graphSubject, HAS_TRANSACTION_SERVICE));
        assertTrue(model.contains(graphSubject, HAS_NAMESPACE_SERVICE));
    }

    @Test
    public void shouldDecorateNodesWithLinksToVersionsAndExport()
        throws RepositoryException {

        when(mockNode.getPrimaryNodeType()).thenReturn(mock(NodeType.class));
        when(mockNode.getPath()).thenReturn("/some/path/to/object");

        when(mockSerializers.keySet()).thenReturn(ImmutableSet.of("a", "b"));
        Resource graphSubject = mockSubjects.getGraphSubject(mockNode);

        final Model model =
                testObj.createModelForResource(mockResource, uriInfo,
                        mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_VERSION_HISTORY));
        assertEquals(2, model.listObjectsOfProperty(graphSubject,
                HAS_SERIALIZATION).toSet().size());
    }

    @Test
    public void shouldDecorateDatastreamsWithLinksToFixityChecks()
        throws RepositoryException {
        when(mockNode.hasNode(JcrConstants.JCR_CONTENT)).thenReturn(true);
        when(mockNode.getPrimaryNodeType()).thenReturn(mock(NodeType.class));
        when(mockNode.getPath()).thenReturn("/some/path/to/datastream");
        when(mockSerializers.keySet()).thenReturn(new HashSet<String>());
        Resource graphSubject = mockSubjects.getGraphSubject(mockNode);

        final Model model =
                testObj.createModelForResource(mockResource, uriInfo,
                        mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_FIXITY_SERVICE));
    }

}
