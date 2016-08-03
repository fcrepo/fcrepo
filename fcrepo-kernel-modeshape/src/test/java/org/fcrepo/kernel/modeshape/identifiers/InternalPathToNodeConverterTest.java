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
package org.fcrepo.kernel.modeshape.identifiers;

import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.FROZEN_NODE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.api.FedoraTypes;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * 
 * @author barmintor
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class InternalPathToNodeConverterTest implements FedoraTypes {

    private static final String JCR_FROZEN_UUID = "jcr:frozenUuid";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private Node mockNode;

    @Mock
    private Node mockBinaryNode;

    @Mock
    private Session mockSession;

    private InternalPathToNodeConverter testObj;

    private String simplePath = "/some/path";

    private String metadataPath = "/some/path/fcr:metadata";

    private String tombstonePath = "/some/path/fcr:tombstone";

    private String versionsPath = "/some/path/fcr:versions";

    @Before
    public void setUp() {
        testObj = new InternalPathToNodeConverter(mockSession);
    }

    @Test
    public void testSimple() throws PathNotFoundException, RepositoryException {
        when(mockSession.getNode(simplePath)).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn(simplePath);
        assertEquals(mockNode, testObj.apply(simplePath));
        assertEquals(simplePath, testObj.toDomain(mockNode));
    }

    @Test
    public void testMetadata() throws PathNotFoundException, RepositoryException {
        when(mockSession.getNode(simplePath)).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn(simplePath);
        when(mockNode.isNodeType(FEDORA_NON_RDF_SOURCE_DESCRIPTION)).thenReturn(true);
        assertEquals(mockNode, testObj.apply(metadataPath));
        assertEquals(metadataPath, testObj.toDomain(mockNode));
    }

    @Test
    public void testBinaryNode() throws PathNotFoundException, RepositoryException {
        when(mockSession.getNode(simplePath)).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn(simplePath);
        when(mockNode.getNode("jcr:content")).thenReturn(mockBinaryNode);
        when(mockBinaryNode.getPath()).thenReturn(simplePath + "/jcr:content");
        when(mockNode.isNodeType(FEDORA_NON_RDF_SOURCE_DESCRIPTION)).thenReturn(true);
        when(mockBinaryNode.isNodeType(FEDORA_BINARY)).thenReturn(true);
        assertEquals(mockBinaryNode, testObj.apply(simplePath));
        assertEquals(simplePath, testObj.toDomain(mockBinaryNode));
    }

    /**
     * Throwing TombstoneException is done in the resource
     * see also org.fcrepo.http.api.FedoraBaseResource#getResourceFromPath
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    @Test
    public void testDeletedNode() throws PathNotFoundException, RepositoryException {
        when(mockSession.getNode(simplePath)).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn(simplePath);
        when(mockNode.isNodeType(FEDORA_TOMBSTONE)).thenReturn(true);
        assertEquals(tombstonePath, testObj.toDomain(mockNode));
        assertEquals(mockNode, testObj.apply(simplePath));
    }

    @Test
    public void testTombstone() throws PathNotFoundException, RepositoryException {
        when(mockSession.getNode(simplePath)).thenReturn(mockNode);
        when(mockNode.isNodeType(FEDORA_TOMBSTONE)).thenReturn(true);
        assertEquals(mockNode, testObj.apply(tombstonePath));
    }

    //TODO implement a test here for versions node
    @Ignore
    public void testVersionsNode() throws PathNotFoundException, RepositoryException {
        when(mockSession.getNode(simplePath)).thenReturn(mockNode);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockNode.isNodeType(FROZEN_NODE)).thenReturn(true);
        final Property mockProperty = mock(Property.class);
        when(mockNode.hasProperty(JCR_FROZEN_UUID)).thenReturn(true);
        when(mockNode.getProperty(JCR_FROZEN_UUID)).thenReturn(mockProperty);
        when(mockProperty.getString()).thenReturn("mockProp");
        final Node mockVersions = mock(Node.class);
        when(mockSession.getNodeByIdentifier("mockProp")).thenReturn(mockVersions);
        assertEquals(versionsPath, testObj.toDomain(mockNode));
        testObj.apply(versionsPath);
    }
}
