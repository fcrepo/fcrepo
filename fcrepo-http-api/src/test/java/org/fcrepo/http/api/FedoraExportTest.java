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

import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import javax.jcr.Session;
import javax.ws.rs.core.StreamingOutput;

import org.fcrepo.http.commons.test.util.TestHelpers;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.serialization.FedoraObjectSerializer;
import org.fcrepo.serialization.JcrXmlSerializer;
import org.fcrepo.serialization.SerializerUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>FedoraExportTest class.</p>
 *
 * @author awoods
 */
public class FedoraExportTest {

    FedoraExport testObj;

    private Session mockSession;

    @Mock
    private SerializerUtil mockSerializers;

    @Mock
    private FedoraObjectSerializer mockSerializer;

    @Mock
    private JcrXmlSerializer mockJcrXmlSerializer;

    @Mock
    private ContainerService mockContainerService;

    @Mock
    private FedoraResource mockResource;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = spy(new FedoraExport());
        mockSession = mockSession(testObj);
        when(mockSerializers.getSerializer("fake-format")).thenReturn(
                mockSerializer);
        setField(testObj, "containerService", mockContainerService);
        setField(testObj, "serializers", mockSerializers);
        setField(testObj, "uriInfo", TestHelpers.getUriInfoImpl());
        setField(testObj, "session", mockSession);
    }

    @Test
    public void testExportObject() throws Exception {
        doReturn(mockResource).when(testObj).getResourceFromPath("test/object");
        ((StreamingOutput) testObj.exportObject("test/object", "fake-format",
                    "false", "false").getEntity()).write(new ByteArrayOutputStream());
        verify(mockSerializer).serialize(eq(mockResource), any(OutputStream.class),
                eq(Boolean.valueOf("false")), eq(Boolean.valueOf("false")));

    }

    @Test
    public void testExportObjectSkipBinary() throws Exception {
        final String skipBinary = "true";
        when(mockSerializers.getSerializer(FedoraObjectSerializer.JCR_XML)).thenReturn(
                mockJcrXmlSerializer);
        doReturn(mockResource).when(testObj).getResourceFromPath("test/object");
        ((StreamingOutput) testObj.exportObject("test/object", FedoraObjectSerializer.JCR_XML,
                    "false", skipBinary).getEntity()).write(new ByteArrayOutputStream());
        verify(mockJcrXmlSerializer).serialize(eq(mockResource), any(OutputStream.class),
                eq(Boolean.valueOf("false")), eq(Boolean.valueOf(skipBinary)));
    }

    @Test
    public void testExportObjectNoRecurse() throws Exception {
        final String noRecurse = "true";
        when(mockSerializers.getSerializer(FedoraObjectSerializer.JCR_XML)).thenReturn(
                mockJcrXmlSerializer);
        doReturn(mockResource).when(testObj).getResourceFromPath("test/object");
        ((StreamingOutput) testObj.exportObject("test/object", FedoraObjectSerializer.JCR_XML,
                    noRecurse, "false").getEntity()).write(new ByteArrayOutputStream());
        verify(mockJcrXmlSerializer).serialize(eq(mockResource),
                any(OutputStream.class), eq(Boolean.valueOf(noRecurse)),
                    eq(Boolean.valueOf("false")));
    }
}
