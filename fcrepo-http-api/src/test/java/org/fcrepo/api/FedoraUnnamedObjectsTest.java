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

package org.fcrepo.api;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.fcrepo.FedoraObject;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.identifiers.UUIDPidMinter;
import org.fcrepo.services.NodeService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.test.util.TestHelpers;
import org.fcrepo.utils.FedoraJcrTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FedoraUnnamedObjectsTest {

    FedoraUnnamedObjects testObj;

    Session mockSession;

    ObjectService mockObjects;

    NodeService mockNodeService;

    @Before
    public void setUp() throws RepositoryException {
        mockObjects = mock(ObjectService.class);
        mockNodeService = mock(NodeService.class);
        testObj = new FedoraUnnamedObjects();
        mockSession = TestHelpers.mockSession(testObj);
        testObj.setNodeService(mockNodeService);
        testObj.setObjectService(mockObjects);
        testObj.setSession(mockSession);
        testObj.setUriInfo(TestHelpers.getUriInfoImpl());
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testIngestAndMint() throws RepositoryException, IOException,
        InvalidChecksumException, URISyntaxException {
        final UUIDPidMinter mockMint = mock(UUIDPidMinter.class);
        testObj.setPidMinter(mockMint);
        when(mockMint.mintPid()).thenReturn("uuid-123");

        final FedoraObject mockObject = mock(FedoraObject.class);
        final String path = "/objects/uuid-123";
        when(mockObject.getPath()).thenReturn(path);
        Node mockNode = mock(Node.class);
        when(mockNode.getPath()).thenReturn(path);
        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockObjects.createObject(mockSession, path))
                .thenReturn(mockObject);
        final Response actual =
                testObj.ingestAndMint(createPathList("objects"),
                        FedoraJcrTypes.FEDORA_OBJECT, null, null, null, null,
                        TestHelpers.getUriInfoImpl());
        assertNotNull(actual);
        assertEquals("http://localhost/fcrepo/objects/uuid-123", actual
                .getMetadata().getFirst("Location").toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), actual
                .getStatus());
        assertTrue(actual.getEntity().toString().endsWith("uuid-123"));
        verify(mockObjects).createObject(mockSession, path);
        verify(mockNodeService).exists(mockSession, path);
        verify(mockSession).save();

    }

}
