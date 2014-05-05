/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.serialization.jcrxml;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Session;

import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.serialization.JcrXmlSerializer;
import org.junit.Test;

public class JcrXmlSerializerTest {

    @Test
    public void testSerialize() throws Exception {
        final Session mockSession = mock(Session.class);
        final Node mockNode = mock(Node.class);
        final FedoraObject mockObject = mock(FedoraObject.class);
        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockNode.getPath()).thenReturn("/path/to/node");

        final OutputStream os = new ByteArrayOutputStream();

        new JcrXmlSerializer().serialize(mockObject, os);

        verify(mockSession).exportSystemView("/path/to/node", os, false, false);
    }

    @Test
    public void testDeserialize() throws Exception {
        final Session mockSession = mock(Session.class);
        final InputStream mockIS = mock(InputStream.class);

        new JcrXmlSerializer().deserialize(mockSession, "/objects", mockIS);
        verify(mockSession).importXML("/objects", mockIS,
                ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

    }

    @Test
    public void testGetKey() {
        assertEquals("jcr/xml", new JcrXmlSerializer().getKey());
    }

    @Test
    public void testGetMediaType() {
        assertEquals("application/xml", new JcrXmlSerializer().getMediaType());
    }
}
