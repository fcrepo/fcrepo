package org.fcrepo.generator;

import static org.mockito.Mockito.*;
import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.fcrepo.generator.dublincore.DCGenerator;
import org.fcrepo.services.ObjectService;
import org.junit.Before;
import org.junit.Test;

public class DublinCoreGeneratorTest {

    DublinCoreGenerator testObj;
    ObjectService mockObjects;
    DCGenerator mockGenerator;
    
    @Before
    public void setUp() {
        testObj = new DublinCoreGenerator();
        mockGenerator = mock(DCGenerator.class);
        mockObjects = mock(ObjectService.class);
        testObj.objectService = mockObjects;
        testObj.dcgenerators = Arrays.asList(new DCGenerator[]{mockGenerator});
    }
    
    @Test
    public void testGetObjectAsDublinCore() throws RepositoryException {
        testObj.dcgenerators = Arrays.asList(new DCGenerator[]{mockGenerator});
        InputStream mockIS = mock(InputStream.class);
        when(mockGenerator.getStream(any(Node.class))).thenReturn(mockIS);
        testObj.getObjectAsDublinCore(createPathList("objects","foo"));
        
    }
    
    @Test
    public void testNoGenerators() {
        testObj.dcgenerators = Arrays.asList(new DCGenerator[0]);
        try {
            testObj.getObjectAsDublinCore(createPathList("objects","foo"));
            fail("Should have failed without a generator configured!");
        } catch (PathNotFoundException ex) {
            // this is what we expect
        } catch (RepositoryException e) {
            fail("unexpected RepositoryException: " + e.getMessage());
        }
    }
}
