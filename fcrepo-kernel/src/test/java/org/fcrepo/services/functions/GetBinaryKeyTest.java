/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.services.functions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.junit.Test;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * @todo Add Documentation.
 * @author Benjamin Armintor
 * @date Apr 3, 2013
 */
public class GetBinaryKeyTest {

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testApply() throws LoginException, RepositoryException {
        final Node mockNode = mock(Node.class);
        final Node mockContent = mock(Node.class);
        final Property mockProp = mock(Property.class);
        final BinaryValue mockBin = mock(BinaryValue.class);
        final BinaryKey binaryKey = new BinaryKey("abc");
        when(mockBin.getKey()).thenReturn(binaryKey);
        when(mockProp.getBinary()).thenReturn(mockBin);
        when(mockContent.getProperty(JCR_DATA)).thenReturn(mockProp);
        when(mockNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        final GetBinaryKey testObj = new GetBinaryKey();
        assertEquals(binaryKey, testObj.apply(mockProp));
    }

}
