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

package org.fcrepo.webhooks;

import static org.fcrepo.kernel.RdfLexicon.HAS_SUBSCRIPTION_SERVICE;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.FedoraResourceImpl;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.rdf.impl.DefaultGraphSubjects;
import org.fcrepo.http.commons.test.util.TestHelpers;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class WebhooksResourcesTest {

    private WebhooksResources testObj;

    private Node mockNode;

    private FedoraResourceImpl mockResource;

    private UriInfo uriInfo;

    private GraphSubjects mockSubjects;

    @Before
    public void setUp() {
        testObj = new WebhooksResources();
        mockNode = mock(Node.class);
        mockResource = new FedoraResourceImpl(mockNode);

        uriInfo = TestHelpers.getUriInfoImpl();
        mockSubjects = new DefaultGraphSubjects(mock(Session.class));
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

        assertTrue(model.contains(graphSubject, HAS_SUBSCRIPTION_SERVICE));
    }
}
