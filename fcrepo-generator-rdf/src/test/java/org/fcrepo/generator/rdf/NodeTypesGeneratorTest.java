package org.fcrepo.generator.rdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.concat;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.Datastream;
import org.slf4j.Logger;
import com.google.common.base.Function;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.generator.rdf.TripleSource.Triple;
import org.junit.Before;
import org.junit.Test;

public class NodeTypesGeneratorTest {

    private NodeTypesGenerator testObj;
    
    @Before
    public void setUp() {
        testObj = new NodeTypesGenerator();
    }
    
    @Test
    public void testGetTriples() throws RepositoryException {
        UriInfo mockUris = mock(UriInfo.class);
        Node mockNode = mock(Node.class);
        NodeType mockType = mock(NodeType.class);
        NodeType mockMixin = mock(NodeType.class);
        when(mockType.getName()).thenReturn("foo:primary");
        when(mockMixin.getName()).thenReturn("foo:mixin");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockType);
        NodeType[] mixins = new NodeType[]{mockMixin};
        when(mockNode.getMixinNodeTypes()).thenReturn(mixins);
        List<Triple> triples = testObj.getTriples(mockNode, mockUris);
        assertEquals(2, triples.size());
        List<String> types = new ArrayList<String>(Arrays.asList(new String[]{"foo:primary", "foo:mixin"}));
        for (Triple t: triples) {
            assertEquals(NodeTypesGenerator.TYPE_PREDICATE, t.predicate);
            assertTrue("Unexpected rdf type object: " + t.object, types.remove(t.object));
        }
        assertEquals("Expected type values were not found (" + types.size() + " values)", 0, types.size());
    }
}
