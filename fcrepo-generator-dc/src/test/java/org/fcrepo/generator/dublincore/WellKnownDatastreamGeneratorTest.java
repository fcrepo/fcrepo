package org.fcrepo.generator.dublincore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
        Node mockNode = mock(Node.class);
        InputStream actual = testObj.getStream(mockNode);
        assertTrue(actual == null);
    }
    
    @Test
    public void testGetStreamPresent() throws Exception {
        String dsid = "foo";
        testObj.setWellKnownDsid(dsid);
        Node mockNode = mock(Node.class);
        Node mockDS = mock(Node.class);
        Node mockCN = mock(Node.class);
        Binary mockB = mock(Binary.class);
        Property mockD = mock(Property.class);
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
        Field field = WellKnownDatastreamGenerator.class.getDeclaredField("wellKnownDsid");
        field.setAccessible(true);
        String actual = (String) field.get(testObj);
        assertEquals("foo", actual);
    }
}
