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
package org.fcrepo.http.api;

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.serialization.FedoraObjectSerializer;
import org.fcrepo.serialization.SerializerUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>FedoraImportTest class.</p>
 *
 * @author awoods
 */
public class FedoraImportTest {

    FedoraImport testObj;

    Session mockSession;

    @Mock
    private SerializerUtil mockSerializers;

    @Mock
    private FedoraObjectSerializer mockSerializer;

    @Mock
    private InputStream mockInputStream;

    @Mock
    private Node mockNode;

    @Mock
    private NodeType mockNodeType;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new FedoraImport();

        mockSession = mockSession(testObj);
        when(mockSerializers.getSerializer("fake-format")).thenReturn(
                mockSerializer);
        setField(testObj, "serializers", mockSerializers);
        setField(testObj, "uriInfo", getUriInfoImpl());
        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);
        when(mockNodeType.getName()).thenReturn("nt:folder");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
    }

    @Test
    public void testImportObject() throws Exception {
        when(mockNode.getPath()).thenReturn("/test/object");
        when(mockSession.getNode("/test/object")).thenReturn(mockNode);
        testObj.importObject("test/object", "fake-format",
                mockInputStream);
        verify(mockSerializer).deserialize(mockSession, "/test/object",
                mockInputStream);
    }

    @Test
    public void testImportObjectAtRoot() throws Exception {
        when(mockSerializers.getSerializer("fake-format")).thenReturn(
                mockSerializer);
        when(mockNode.getPath()).thenReturn("/");
        when(mockSession.getNode("/")).thenReturn(mockNode);
        testObj.importObject(null, "fake-format", mockInputStream);
        verify(mockSerializer).deserialize(mockSession, "/", mockInputStream);

    }
}
