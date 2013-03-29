
package org.fcrepo;

import static org.fcrepo.utils.FedoraJcrTypes.DC_TITLE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

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
