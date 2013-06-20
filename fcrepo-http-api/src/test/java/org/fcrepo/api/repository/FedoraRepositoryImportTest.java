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

package org.fcrepo.api.repository;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Session;

import org.fcrepo.serialization.FedoraObjectSerializer;
import org.fcrepo.serialization.SerializerUtil;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

public class FedoraRepositoryImportTest {

    FedoraRepositoryImport testObj;

    Session mockSession;

    SerializerUtil mockSerializers;

    @Before
    public void setUp() throws Exception {

        testObj = new FedoraRepositoryImport();
        mockSession = TestHelpers.mockSession(testObj);
        mockSerializers = mock(SerializerUtil.class);

        TestHelpers.setField(testObj, "serializers", mockSerializers);
        TestHelpers.setField(testObj, "uriInfo", TestHelpers.getUriInfoImpl());
        TestHelpers.setField(testObj, "session", mockSession);
    }

    @Test
    public void testImportObject() throws Exception {
        final InputStream mockInputStream = mock(InputStream.class);
        final FedoraObjectSerializer mockSerializer =
                mock(FedoraObjectSerializer.class);
        when(mockSerializers.getSerializer("fake-format")).thenReturn(
                mockSerializer);

        Node mockNode = mock(Node.class);
        when(mockNode.getPath()).thenReturn("/");
        when(mockSession.getNode("/")).thenReturn(mockNode);

        testObj.importObject(createPathList(), "fake-format", mockInputStream);
        verify(mockSerializer).deserialize(mockSession, "/", mockInputStream);

    }
}
