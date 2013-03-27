
package org.fcrepo;

import static org.fcrepo.utils.FedoraJcrTypes.DC_TITLE;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;

public class ObjectTest {
	
	FedoraObject testObj;
	
	Node mockNode;
	
	@Before
	public void setUp(){
		mockNode = mock(Node.class);
		testObj = new FedoraObject(mockNode);
	}


    @Test
    public void testLabel() throws RepositoryException, IOException {
        when(mockNode.getName()).thenReturn("testObject");
        testObj.setLabel("Best object ever!");
        verify(mockNode).setProperty(DC_TITLE, "Best object ever!");
    }

    @Test
    public void testDoubleLabel() throws RepositoryException, IOException {
        testObj.setLabel("Worst object ever!");

        testObj.setLabel("Best object ever!");
        verify(mockNode).setProperty(DC_TITLE, "Worst object ever!");
        verify(mockNode).setProperty(DC_TITLE, "Best object ever!");
    }

}
