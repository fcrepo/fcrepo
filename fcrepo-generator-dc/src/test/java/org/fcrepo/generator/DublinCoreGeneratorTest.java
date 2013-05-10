package org.fcrepo.generator;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.FedoraResource;
import org.fcrepo.generator.dublincore.DCGenerator;
import org.fcrepo.services.NodeService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

public class DublinCoreGeneratorTest {

    DublinCoreGenerator testObj;
	NodeService mockNodeService;
    DCGenerator mockGenerator;
	Session mockSession;
    
    @Before
    public void setUp() throws RepositoryException {
		mockNodeService = mock(NodeService.class);
        testObj = new DublinCoreGenerator();
		testObj.setNodeService(mockNodeService);

		mockSession = TestHelpers.mockSession(testObj);
        mockGenerator = mock(DCGenerator.class);
        testObj.dcgenerators = Arrays.asList(new DCGenerator[]{mockGenerator});
    }
    
    @Test
    public void testGetObjectAsDublinCore() throws RepositoryException {
        testObj.dcgenerators = Arrays.asList(new DCGenerator[]{mockGenerator});
        InputStream mockIS = mock(InputStream.class);
		FedoraResource mockResource = mock(FedoraResource.class);
		when(mockResource.getNode()).thenReturn(mock(Node.class));
		when(mockNodeService.getObject(mockSession, "/objects/foo")).thenReturn(mockResource);
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
