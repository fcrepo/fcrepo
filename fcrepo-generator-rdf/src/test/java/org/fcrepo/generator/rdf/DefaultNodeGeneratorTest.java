package org.fcrepo.generator.rdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.Datastream;
import org.fcrepo.generator.rdf.TripleSource.Triple;
import org.junit.Before;
import org.junit.Test;

public class DefaultNodeGeneratorTest {
	
	@Test
	public void testGetTriples() throws RepositoryException {
        String path = "/testing/fake/object";
        UriInfo mockUris = mock(UriInfo.class);
        UriBuilder mockBuilder = mock(UriBuilder.class);
        when(mockBuilder.path(anyString())).thenReturn(mockBuilder);
        URI mockUri = URI.create("info:fedora/mock/test/uri");
        when(mockBuilder.build()).thenReturn(mockUri);
        when(mockUris.getBaseUriBuilder()).thenReturn(mockBuilder);
        Node mockNode = mock(Node.class);
        when(mockNode.getPath()).thenReturn(path);
        Property mockProp = mock(Property.class);
        when(mockProp.getParent()).thenReturn(mockNode);
        when(mockProp.getString()).thenReturn("mockValue");
        PropertyIterator mockProps = mock(PropertyIterator.class);
		when(mockNode.getProperties()).thenReturn(mockProps);
		NodeType mockType = mock(NodeType.class);
		when(mockType.getName()).thenReturn("mock:nodeType");
		when(mockNode.getPrimaryNodeType()).thenReturn(mockType);
		NodeType mockMixin = mock(NodeType.class);
		when(mockMixin.getName()).thenReturn("mock:mixinType");
		NodeType[] mockMixins = new NodeType[]{mockMixin};
		when(mockNode.getMixinNodeTypes()).thenReturn(mockMixins);
		List<Triple> actual = DefaultNodeGenerator.getTriples(mockNode, mockUris);
		assertTrue(actual.size() > 0);
		for (Triple triple: actual) {
			assertEquals(mockUri.toString(), triple.subject);
		}
	}
}
