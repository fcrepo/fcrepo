package org.fcrepo.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.junit.Test;
import org.modeshape.jcr.JcrValueFactory;
import org.modeshape.jcr.api.Namespaced;

import com.google.common.base.Predicate;

public class FedoraTypesUtilsTest {
    
    // unfortunately, we need to be able to cast to two interfaces to perform some tests
    // this testing interface allows mocks to do that
    static interface PropertyMock extends Property, Namespaced {};
    
    @Test
    public void testIsMultipleValuedProperty() throws RepositoryException {
        Property mockYes = mock(Property.class);
        when(mockYes.isMultiple()).thenReturn(true);
        Property mockNo = mock(Property.class);
        Predicate<Property> test = FedoraTypesUtils.isMultipleValuedProperty;
        try {
            test.apply(null);
            fail("Null values should throw an IllegalArgumentException");
        } catch (IllegalArgumentException e) {}
        boolean actual = test.apply(mockYes);
        assertEquals(true, actual);
        actual = test.apply(mockNo);
        assertEquals(false, actual);
    }
    
    @Test
    public void testGetValueFactory() throws RepositoryException {
        Node mockNode = mock(Node.class);
        Session mockSession = mock(Session.class);
        ValueFactory mockVF = mock(ValueFactory.class);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        ValueFactory actual = FedoraTypesUtils.getValueFactory.apply(mockNode);
        assertEquals(mockVF, actual);
    }

    @Test
    public void testGetPredicateForProperty() {
        PropertyMock mockProp = mock(PropertyMock.class);
        com.hp.hpl.jena.rdf.model.Property actual = 
                FedoraTypesUtils.getPredicateForProperty.apply(mockProp);
    }
    
    @Test
    public void testGetBinary() throws RepositoryException {
        Node mockNode = mock(Node.class);
        Session mockSession = mock(Session.class);
        ValueFactory mockVF = mock(ValueFactory.class);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getValueFactory()).thenReturn(mockVF);
        InputStream mockInput = mock(InputStream.class);
        FedoraTypesUtils.getBinary(mockNode, mockInput);
        verify(mockVF).createBinary(mockInput);
        // try it with hints
        JcrValueFactory mockJVF = mock(JcrValueFactory.class);
        when(mockSession.getValueFactory()).thenReturn(mockJVF);
        String mockHint = "storage-hint";
        FedoraTypesUtils.getBinary(mockNode, mockInput, mockHint);
        verify(mockJVF).createBinary(mockInput, mockHint);
    }
    
    @Test
    public void testGetDefinitionForPropertyName() throws RepositoryException {
        Node mockNode = mock(Node.class);
        String mockPropertyName = "mock:property";
        Session mockSession = mock(Session.class);
        when(mockNode.getSession()).thenReturn(mockSession);
        Workspace mockWS = mock(Workspace.class);
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        NodeTypeManager mockNTM = mock(NodeTypeManager.class);
        when(mockWS.getNodeTypeManager()).thenReturn(mockNTM);
        NodeType mockType = mock(NodeType.class);
        when(mockNTM.getNodeType(anyString())).thenReturn(mockType);
        PropertyDefinition mockPD = mock(PropertyDefinition.class);
        when(mockPD.getName()).thenReturn(mockPropertyName);
        PropertyDefinition[] PDs = new PropertyDefinition[]{mockPD};
        when(mockType.getPropertyDefinitions()).thenReturn(PDs);
        PropertyDefinition actual =
                FedoraTypesUtils.getDefinitionForPropertyName(mockNode, mockPropertyName);
        assertEquals(mockPD, actual);
        actual =
                FedoraTypesUtils.getDefinitionForPropertyName(mockNode, mockPropertyName + ":fail");
        assertEquals(null, actual);
        
    }
}
