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
package org.fcrepo.kernel.modeshape.identifiers;

import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.Tombstone;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_TOMBSTONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author cabeer
 */
public class NodeResourceConverterTest {

    private NodeResourceConverter testObj;

    @Mock
    private FedoraResource mockResource;

    @Mock
    private Node mockNode;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = NodeResourceConverter.nodeConverter;
        when(mockResource.getNode()).thenReturn(mockNode);
    }


    @Test
    public void testForwardObject() {
        final FedoraResource actual = testObj.convert(mockNode);
        assertTrue(actual instanceof Container);
        assertEquals(mockNode, actual.getNode());
    }

    @Test
    public void testForwardDatastream() throws RepositoryException {
        when(mockNode.isNodeType(FEDORA_NON_RDF_SOURCE_DESCRIPTION)).thenReturn(true);
        final FedoraResource actual = testObj.convert(mockNode);
        assertTrue(actual instanceof NonRdfSourceDescription);
        assertEquals(mockNode, actual.getNode());

    }

    @Test
    public void testForwardBinary() throws RepositoryException {
        when(mockNode.isNodeType(FEDORA_BINARY)).thenReturn(true);
        final FedoraResource actual = testObj.convert(mockNode);
        assertTrue(actual instanceof FedoraBinary);
        assertEquals(mockNode, actual.getNode());
    }

    @Test
    public void testForwardTombstone() throws RepositoryException {
        when(mockNode.isNodeType(FEDORA_TOMBSTONE)).thenReturn(true);
        final FedoraResource actual = testObj.convert(mockNode);
        assertTrue(actual instanceof Tombstone);
        assertEquals(mockNode, actual.getNode());
    }

    @Test
    public void testBackward() {
        final Node actual = testObj.reverse().convert(mockResource);
        assertEquals(mockNode, actual);
    }

}
