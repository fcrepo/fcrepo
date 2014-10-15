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
package org.fcrepo.transform.http;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.http.commons.test.util.TestHelpers;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.impl.FedoraResourceImpl;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.UriInfo;

import static org.fcrepo.kernel.RdfLexicon.HAS_SPARQL_ENDPOINT;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author cbeer
 */
public class TransformResourcesTest {

    private TransformResources testObj;

    private Node mockNode;

    private FedoraResourceImpl mockResource;

    private UriInfo uriInfo;

    private DefaultIdentifierTranslator mockSubjects;

    @Mock
    private Session mockSession;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new TransformResources();
        mockNode = mock(Node.class);
        mockResource = new FedoraResourceImpl(mockNode);

        uriInfo = TestHelpers.getUriInfoImpl();
        mockSubjects = new DefaultIdentifierTranslator(mockSession);
    }

    @Test
    public void shouldDecorateModeRootNodesWithRepositoryWideLinks()
        throws RepositoryException {

        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNode.isNodeType(FedoraJcrTypes.ROOT)).thenReturn(true);
        when(mockNodeType.getName()).thenReturn("not-frozen");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getPath()).thenReturn("/");

        final Resource graphSubject = mockSubjects.reverse().convert(mockResource);

        final Model model =
            testObj.createModelForResource(mockResource, uriInfo,
                                              mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_SPARQL_ENDPOINT));
    }
}
