package org.fcrepo;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.StringBufferInputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.JcrConstants;

public class TestHelpers {
    public static NodeIterator mockDatastreamNode(String dsId, String content) throws RepositoryException, IOException {
    	Node mockDsNode = mock(Node.class);
        Node mockContent = mock(Node.class);
        Property mockData = mock(Property.class);
        Binary mockBinary = mock(Binary.class);
        when(mockDsNode.getName()).thenReturn(dsId);
        when(mockDsNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(mockContent);
        when(mockContent.getProperty(JcrConstants.JCR_DATA)).thenReturn(mockData);
        when(mockData.getBinary()).thenReturn(mockBinary);
        when(mockBinary.getMimeType()).thenReturn("binary/octet-stream");
        when(mockBinary.getStream()).thenReturn(new StringBufferInputStream(content));
    	NodeIterator mockIter = mock(NodeIterator.class);
        when(mockIter.hasNext()).thenReturn(true, false);
        when(mockIter.next()).thenReturn(mockDsNode);
        when(mockIter.nextNode()).thenReturn(mockDsNode);
        return mockIter;
    }
}
