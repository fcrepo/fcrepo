package org.fcrepo.generator.rdf;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.OutputStream;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.any23.writer.NTriplesWriter;
import org.apache.any23.writer.RDFXMLWriter;
import org.apache.any23.writer.TripleHandler;
import org.apache.any23.writer.TurtleWriter;
import org.junit.Test;

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
        Workspace mockWS = mock(Workspace.class);
        NamespaceRegistry mockNSR = mock(NamespaceRegistry.class);
        when(mockProp.getName()).thenReturn("foo:bar");
        when(mockProp.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        when(mockWS.getNamespaceRegistry()).thenReturn(mockNSR);
        when(mockNSR.getURI("foo")).thenReturn("http://foo.gov/");
        String actual = Utils.expandJCRNamespace(mockProp);
        String expected = "http://foo.gov/bar";
        assertEquals(expected, actual);
    }
}
