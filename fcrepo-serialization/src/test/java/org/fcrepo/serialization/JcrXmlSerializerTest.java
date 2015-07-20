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
package org.fcrepo.serialization;

import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW;
import static org.fcrepo.serialization.FedoraObjectSerializer.JCR_XML;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.api.models.FedoraResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>JcrXmlSerializerTest class.</p>
 *
 * @author lsitu
 */
public class JcrXmlSerializerTest {

    @Mock
    private Session mockSession;

    @Mock
    private Node mockNode;

    @Mock
    private FedoraResource mockResource;

    private String testPath = "/path/to/node";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(mockResource.getPath()).thenReturn(testPath);
        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockNode.getSession()).thenReturn(mockSession);
    }

    @Test
    public void testSerialize() throws Exception {

        final OutputStream os = new ByteArrayOutputStream();

        new JcrXmlSerializer().serialize(mockResource, os, false, false);

        verify(mockSession).exportSystemView(testPath, os, false, !false);
    }

    @Test
    public void testSerializeWithSkipBinary() throws Exception {

        final boolean skipBinary = true;
        final OutputStream os = new ByteArrayOutputStream();

        new JcrXmlSerializer().serialize(mockResource, os, skipBinary, false);

        verify(mockSession).exportSystemView(testPath, os, skipBinary, !false);
    }

    @Test
    public void testSerializeWithOptions() throws Exception {

        final boolean skipBinary = true;
        final boolean recurse = true;
        final OutputStream os = new ByteArrayOutputStream();

        new JcrXmlSerializer().serialize(mockResource, os, skipBinary, recurse);

        verify(mockSession).exportSystemView(testPath, os, skipBinary, !recurse);
    }

    @Test
    public void testDeserialize() throws IOException, RepositoryException, InvalidSerializationFormatException {
        final InputStream is = getClass().getClassLoader().getResourceAsStream("valid-jcr-xml.xml");
        final Session mockSession = mock(Session.class);
        new JcrXmlSerializer().deserialize(mockSession, "/objects", is);
        verify(mockSession).importXML(eq("/objects"), any(InputStream.class), eq(IMPORT_UUID_COLLISION_THROW));
    }

    @Test
    public void testGetKey() {
        assertEquals(JCR_XML, new JcrXmlSerializer().getKey());
    }

    @Test
    public void testGetMediaType() {
        assertEquals("application/xml", new JcrXmlSerializer().getMediaType());
    }

    @Test
    public void testValidJCRXMLValidation() throws IOException,
            InvalidSerializationFormatException, RepositoryException {
        final Session mockSession = mock(Session.class);
        new JcrXmlSerializer().deserialize(mockSession, "/objects",
                getClass().getClassLoader().getResourceAsStream("valid-jcr-xml.xml"));
    }

    @Test (expected = InvalidSerializationFormatException.class)
    public void testInvalidJCRXMLValidation() throws IOException,
            InvalidSerializationFormatException, RepositoryException {
        final Session mockSession = mock(Session.class);
        new JcrXmlSerializer().deserialize(mockSession, "/objects",
                getClass().getClassLoader().getResourceAsStream("invalid-jcr-xml.xml"));
    }

    @Test (expected = InvalidSerializationFormatException.class)
    public void testNonRDFContentJCRXMLValidation() throws IOException,
            InvalidSerializationFormatException, RepositoryException {
        final Session mockSession = mock(Session.class);
        new JcrXmlSerializer().deserialize(mockSession, "/objects",
                getClass().getClassLoader().getResourceAsStream("invalid-jcr-xml-2.xml"));
    }

    @Test (expected = InvalidSerializationFormatException.class)
    public void testFrozenResourceJCRXMLValidation() throws IOException,
            InvalidSerializationFormatException, RepositoryException {
        final Session mockSession = mock(Session.class);
        new JcrXmlSerializer().deserialize(mockSession, "/objects",
                getClass().getClassLoader().getResourceAsStream("invalid-jcr-xml-3.xml"));
    }


}
