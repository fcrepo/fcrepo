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
import static org.fcrepo.test.util.TestHelpers.mockSession;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import javax.jcr.Session;
import javax.ws.rs.core.StreamingOutput;

import org.fcrepo.FedoraObject;
import org.fcrepo.serialization.FedoraObjectSerializer;
import org.fcrepo.serialization.SerializerUtil;
import org.fcrepo.services.ObjectService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

public class FedoraExportTest {

    FedoraExport testObj;

    Session mockSession;

    SerializerUtil mockSerializers;

    FedoraObjectSerializer mockSerializer;

    ObjectService mockObjects;

    @Before
    public void setUp() throws Exception {

        testObj = new FedoraExport();

        mockSession = mockSession(testObj);
        mockSerializers = mock(SerializerUtil.class);
        mockSerializer = mock(FedoraObjectSerializer.class);
        when(mockSerializers.getSerializer("fake-format")).thenReturn(
                mockSerializer);
        mockObjects = mock(ObjectService.class);
        TestHelpers.setField(testObj, "objectService", mockObjects);
        TestHelpers.setField(testObj, "serializers", mockSerializers);
        TestHelpers.setField(testObj, "uriInfo", TestHelpers.getUriInfoImpl());
        mockSession = TestHelpers.mockSession(testObj);
        TestHelpers.setField(testObj, "session", mockSession);
    }

    @Test
    public void testExportObject() throws Exception {
        FedoraObject mockObject = mock(FedoraObject.class);
        when(mockObjects.getObject(mockSession, "/test/object")).thenReturn(
                mockObject);

        ((StreamingOutput) testObj.exportObject(
                createPathList("test", "object"), "fake-format").getEntity())
                .write(new ByteArrayOutputStream());
        verify(mockSerializer).serialize(eq(mockObject),
                any(OutputStream.class));

    }

}
