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

package org.fcrepo.generator.dublincore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.InputStream;
import java.lang.reflect.Field;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;

import org.junit.Before;
import org.junit.Test;

public class WellKnownDatastreamGeneratorTest {

    private WellKnownDatastreamGenerator testObj;

    @Before
    public void setUp() {
        testObj = new WellKnownDatastreamGenerator();
    }

    @Test
    public void testGetStreamAbsent() {
        final Node mockNode = mock(Node.class);
        final InputStream actual = testObj.getStream(mockNode);
        assertNull(actual);
    }

    @Test
    public void testGetStreamPresent() throws Exception {
        final String dsid = "foo";
        testObj.setWellKnownDsid(dsid);
        final Node mockNode = mock(Node.class);
        final Node mockDS = mock(Node.class);
        final Node mockCN = mock(Node.class);
        final Binary mockB = mock(Binary.class);
        final Property mockD = mock(Property.class);
        when(mockNode.hasNode(dsid)).thenReturn(true);
        when(mockNode.getNode(dsid)).thenReturn(mockDS);
        when(mockDS.getNode(JCR_CONTENT)).thenReturn(mockCN);
        when(mockCN.getProperty(JCR_DATA)).thenReturn(mockD);
        when(mockD.getBinary()).thenReturn(mockB);
        testObj.getStream(mockNode);
        verify(mockNode).getNode(dsid);
    }

    @Test
    public void testSetWellKnownDsid() throws Exception {
        testObj.setWellKnownDsid("foo");
        final Field field =
                WellKnownDatastreamGenerator.class
                        .getDeclaredField("wellKnownDsid");
        field.setAccessible(true);
        final String actual = (String) field.get(testObj);
        assertEquals("foo", actual);
    }
}
