package org.fcrepo.generator.dublincore;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.InputStream;

import javax.jcr.Node;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.fcrepo.generator.util.OaiDublinCore;
import org.junit.Before;
import org.junit.Test;


public class WorstCaseGeneratorTest {

    private WorstCaseGenerator testObj;
    
    private JAXBContext context;
    
    @Before
    public void setUp() throws JAXBException {
        testObj = new WorstCaseGenerator();
        context =
                JAXBContext.newInstance(OaiDublinCore.class);

    }
    
    @Test
    public void testGetStream() throws Exception {
        Node mockNode = mock(Node.class);
        InputStream out = testObj.getStream(mockNode);
        OaiDublinCore actual = (OaiDublinCore)context.createUnmarshaller().unmarshal(out);
        assertTrue(actual != null);
    }
}
