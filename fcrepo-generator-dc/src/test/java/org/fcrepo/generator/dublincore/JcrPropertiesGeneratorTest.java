package org.fcrepo.generator.dublincore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
public class JcrPropertiesGeneratorTest {


    Session mockSession;

    Node mockNode;


    @Before
    public void setUp(){
        mockSession = mock(Session.class);
        mockNode = mock(Node.class);
        try{
            when(mockNode.getName()).thenReturn("id");
            when(mockNode.getSession()).thenReturn(mockSession);
        } catch(RepositoryException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetStreamNoProperties() throws Exception {

        PropertyIterator mockIterator = mock(PropertyIterator.class);
        when(mockIterator.hasNext()).thenReturn(false);
        when(mockNode.getProperties(JcrPropertiesGenerator.SALIENT_DC_PROPERTY_NAMESPACES)).thenReturn(mockIterator);

        final InputStream inputStream = new JcrPropertiesGenerator().getStream(mockNode);

        final String actual = IOUtils.toString(inputStream);
        final String expected = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n</oai_dc:dc>";

        assertEquals(expected, actual);
    }


    @Test
    public void testGetStreamSingleValuedProperties() throws Exception {

        // Modeshape PropertyIterator is private to modeshape.
        // Too lazy to build out our own implementation for testing, so here's
        // a bunch of mocks
        PropertyIterator mockIterator = mock(PropertyIterator.class);
        when(mockIterator.hasNext()).thenReturn(true, true, false);

        Property mockProperty1 = mock(Property.class);
        when(mockProperty1.isMultiple()).thenReturn(false);
        when(mockProperty1.getName()).thenReturn("dc:title");
        Value mockValue1 = mock(Value.class);
        when(mockValue1.getString()).thenReturn("title");
        when(mockProperty1.getValue()).thenReturn(mockValue1);


        Property mockProperty2 = mock(Property.class);
        when(mockProperty2.getName()).thenReturn("dc:identifier");
        Value mockValue2 = mock(Value.class);
        when(mockValue2.getString()).thenReturn("identifier");
        when(mockProperty2.getValue()).thenReturn(mockValue2);

        when(mockIterator.nextProperty()).thenReturn(mockProperty1, mockProperty2);

        when(mockNode.getProperties(JcrPropertiesGenerator.SALIENT_DC_PROPERTY_NAMESPACES)).thenReturn(mockIterator);


        final InputStream inputStream = new JcrPropertiesGenerator().getStream(mockNode);

        final String actual = IOUtils.toString(inputStream);
        final String expected = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" +
                "\t<dc:title>title</dc:title>\n" +
                "\t<dc:identifier>identifier</dc:identifier>\n" +
                "</oai_dc:dc>";

        assertEquals(expected, actual);
    }

    @Test
    public void testGetStreamMultivaluedProperties() throws Exception {

        // Modeshape PropertyIterator is private to modeshape.
        // Too lazy to build out our own implementation for testing, so here's
        // a bunch of mocks
        PropertyIterator mockIterator = mock(PropertyIterator.class);
        when(mockIterator.hasNext()).thenReturn(true, false);

        Property mockProperty1 = mock(Property.class);
        when(mockProperty1.isMultiple()).thenReturn(true);

        when(mockProperty1.getName()).thenReturn("dc:title");
        Value mockValue1 = mock(Value.class);
        when(mockValue1.getString()).thenReturn("title");

        Value mockValue2 = mock(Value.class);
        when(mockValue2.getString()).thenReturn("title2");

        Value[] values = new Value[] { mockValue1, mockValue2 };
        when(mockProperty1.getValues()).thenReturn(values);

        when(mockIterator.nextProperty()).thenReturn(mockProperty1);

        when(mockNode.getProperties(JcrPropertiesGenerator.SALIENT_DC_PROPERTY_NAMESPACES)).thenReturn(mockIterator);


        final InputStream inputStream = new JcrPropertiesGenerator().getStream(mockNode);

        final String actual = IOUtils.toString(inputStream);
        final String expected = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" +
                "\t<dc:title>title</dc:title>\n" +
                "\t<dc:title>title2</dc:title>\n" +
                "</oai_dc:dc>";

        assertEquals(expected, actual);
    }
}
