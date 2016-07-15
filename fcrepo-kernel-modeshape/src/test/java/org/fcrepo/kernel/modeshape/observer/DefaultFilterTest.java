/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.observer;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.ROOT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.modeshape.jcr.api.Repository;
import org.modeshape.jcr.api.observation.Event;

/**
 * @author ajs6f
 * @since 2013
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultFilterTest {

    private DefaultFilter testObj;

    @Mock
    private Session mockSession;

    @Mock
    private Repository mockRepo;

    @Mock
    private Event mockEvent;

    @Mock
    private Node mockNode;

    @Mock
    private Property mockProperty;

    @Mock
    private NodeType fedoraResource, fedoraContainer, fedoraDatastream, fedoraBinary, modeshapeRootType,
            modeshapeFolderType;

    @Before
    public void setUp() {
        testObj = new DefaultFilter();
        when(fedoraResource.getName()).thenReturn(FEDORA_RESOURCE);
        when(fedoraContainer.getName()).thenReturn(FEDORA_CONTAINER);
        when(fedoraDatastream.getName()).thenReturn(FEDORA_NON_RDF_SOURCE_DESCRIPTION);
        when(fedoraBinary.getName()).thenReturn(FEDORA_BINARY);
        when(modeshapeRootType.getName()).thenReturn(ROOT);
        when(modeshapeFolderType.getName()).thenReturn("nt:folder");
    }

    @Test
    public void shouldApplyToResource() throws RepositoryException {
        when(mockEvent.getPrimaryNodeType()).thenReturn(modeshapeFolderType);
        when(mockEvent.getMixinNodeTypes()).thenReturn(new NodeType[] { fedoraResource });
        assertTrue(testObj.test(mockEvent));
    }

    @Test
    public void shouldApplyToObject() throws RepositoryException {
        when(mockEvent.getPrimaryNodeType()).thenReturn(modeshapeFolderType);
        when(mockEvent.getMixinNodeTypes()).thenReturn(new NodeType[] {fedoraContainer});
        assertTrue(testObj.test(mockEvent));
    }

    @Test
    public void shouldNotApplyToDatastream() throws RepositoryException {
        when(mockEvent.getPrimaryNodeType()).thenReturn(modeshapeFolderType);
        when(mockEvent.getMixinNodeTypes()).thenReturn(new NodeType[] { fedoraDatastream });
        assertFalse(testObj.test(mockEvent));
    }

    @Test
    public void shouldApplyToBinary() throws RepositoryException {
        when(mockEvent.getPrimaryNodeType()).thenReturn(modeshapeFolderType);
        when(mockEvent.getMixinNodeTypes()).thenReturn(new NodeType[] { fedoraBinary });
        assertTrue(testObj.test(mockEvent));
    }

    @Test
    public void shouldApplyToRoot() throws RepositoryException {
        when(mockEvent.getPrimaryNodeType()).thenReturn(modeshapeRootType);
        when(mockEvent.getMixinNodeTypes()).thenReturn(new NodeType[] { });
        assertTrue(testObj.test(mockEvent));
    }

    @Test
    public void shouldNotApplyToNonFedoraNodes() throws RepositoryException {
        when(mockEvent.getPrimaryNodeType()).thenReturn(modeshapeFolderType);
        when(mockEvent.getMixinNodeTypes()).thenReturn(new NodeType[] {  });
        assertFalse(testObj.test(mockEvent));
    }
}
