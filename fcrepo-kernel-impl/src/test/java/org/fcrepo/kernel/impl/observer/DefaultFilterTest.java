/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.impl.observer;

import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.Repository;

/**
 * @author ajs6f
 * @since 2013
 */
public class DefaultFilterTest {

    private DefaultFilter testObj;

    @Mock
    private Session mockSession;

    @Mock
    private Repository mockRepo;

    @Mock
    private org.modeshape.jcr.api.observation.Event mockEvent;

    @Mock
    private Node mockNode;

    @Mock
    private Property mockProperty;

    @Mock
    private NodeType fedoraResource;
    @Mock
    private NodeType fedoraContainer;
    @Mock
    private NodeType fedoraDatastream;
    @Mock
    private NodeType fedoraBinary;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new DefaultFilter();
        when(fedoraResource.getName()).thenReturn(FEDORA_RESOURCE);
        when(fedoraContainer.getName()).thenReturn(FEDORA_CONTAINER);
        when(fedoraDatastream.getName()).thenReturn(FEDORA_NON_RDF_SOURCE_DESCRIPTION);
        when(fedoraBinary.getName()).thenReturn(FEDORA_BINARY);
    }

    @Test
    public void shouldApplyToResource() throws Exception {
        when(mockEvent.getMixinNodeTypes()).thenReturn(new NodeType[] { fedoraResource });
        assertTrue(testObj.getFilter(mockSession).apply(mockEvent));
    }

    @Test
    public void shouldApplyToObject() throws Exception {
        when(mockEvent.getMixinNodeTypes()).thenReturn(new NodeType[] {fedoraContainer});
        assertTrue(testObj.getFilter(mockSession).apply(mockEvent));
    }

    @Test
    public void shouldApplyToDatastream() throws Exception {
        when(mockEvent.getMixinNodeTypes()).thenReturn(new NodeType[] { fedoraDatastream });
        assertTrue(testObj.getFilter(mockSession).apply(mockEvent));
    }

    @Test
    public void shouldApplyToBinary() throws Exception {
        when(mockEvent.getMixinNodeTypes()).thenReturn(new NodeType[] { fedoraBinary });
        assertTrue(testObj.getFilter(mockSession).apply(mockEvent));
    }


    @Test
    public void shouldNotApplyToNonFedoraNodes() throws Exception {
        when(mockEvent.getMixinNodeTypes()).thenReturn(new NodeType[] {  });
        assertFalse(testObj.getFilter(mockSession).apply(mockEvent));
    }
}
