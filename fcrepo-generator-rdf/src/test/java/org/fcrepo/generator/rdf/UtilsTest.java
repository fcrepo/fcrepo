package org.fcrepo.generator.rdf;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.OutputStream;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.any23.writer.NTriplesWriter;
import org.apache.any23.writer.RDFXMLWriter;
import org.apache.any23.writer.TripleHandler;
import org.apache.any23.writer.TurtleWriter;
import org.fcrepo.utils.NamespaceTools;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({NamespaceTools.class})
public class UtilsTest {

    @Test
    public void testSelectWriter() {
        OutputStream mockOut = mock(OutputStream.class);
        TripleHandler actual;
        actual = Utils.selectWriter("text/turtle", mockOut);
        assertEquals(TurtleWriter.class, actual.getClass());
        actual = Utils.selectWriter("text/plain", mockOut);
        assertEquals(NTriplesWriter.class, actual.getClass());
        actual = Utils.selectWriter("foo/bar", mockOut);
        assertEquals(RDFXMLWriter.class, actual.getClass());
    }
    
    @Test
    public void testExpandJcrNamespace() throws RepositoryException {
        Property mockProp = mock(Property.class);
        Session mockSession = mock(Session.class);
        NamespaceRegistry mockNSR = mock(NamespaceRegistry.class);
        when(mockProp.getName()).thenReturn("foo:bar");
        when(mockProp.getSession()).thenReturn(mockSession);
        when(mockNSR.getURI("foo")).thenReturn("http://foo.gov/");
        mockStatic(NamespaceTools.class);
        when(NamespaceTools.getNamespaceRegistry(mockSession)).thenReturn(mockNSR);
        String actual = Utils.expandJCRNamespace(mockProp);
        String expected = "http://foo.gov/bar";
        assertEquals(expected, actual);
    }
}
