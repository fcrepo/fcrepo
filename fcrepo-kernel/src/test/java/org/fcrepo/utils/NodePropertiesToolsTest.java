/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


/**
 * @todo Add Documentation.
 * @author Chris Beer
 * @date May 10, 2013
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*" })
@PrepareForTest({FedoraTypesUtils.class })
public class NodePropertiesToolsTest {

    private PropertyDefinition mockDefinition;
    private Value mockValue;
    private Node mockNode;
    private Property mockProperty;

    /**
     * @todo Add Documentation.
     */
    @Before
    public void setUp() throws RepositoryException {
        PowerMockito.mockStatic(FedoraTypesUtils.class);
        mockDefinition = mock(PropertyDefinition.class);
        mockProperty = mock(Property.class);
        mockValue = mock(Value.class);

        mockNode = mock(Node.class);
        when(FedoraTypesUtils
             .getDefinitionForPropertyName(mockNode, "mockPropertyName"))
            .thenReturn(mockDefinition);

        when(mockNode.getProperty("mockPropertyName")).thenReturn(mockProperty);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void addNewSingleValuedProperty() throws RepositoryException {

        when(mockDefinition.isMultiple()).thenReturn(false);

        when(mockNode.hasProperty("mockPropertyName")).thenReturn(false);

        NodePropertiesTools
            .appendOrReplaceNodeProperty(mockNode, "mockPropertyName",
                                         mockValue);

        verify(mockNode).setProperty("mockPropertyName", mockValue);
    }


    /**
     * @todo Add Documentation.
     */
    @Test
    public void addNewMultiValuedProperty() throws RepositoryException {
        when(mockDefinition.isMultiple()).thenReturn(true);

        when(mockNode.hasProperty("mockPropertyName")).thenReturn(false);

        NodePropertiesTools
            .appendOrReplaceNodeProperty(mockNode, "mockPropertyName",
                                         mockValue);

        verify(mockNode)
            .setProperty("mockPropertyName",
                         Arrays.asList(mockValue).toArray(new Value[0]));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void replaceExistingSingleValuedPropertyWithValue()
        throws RepositoryException {

        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockNode.hasProperty("mockPropertyName")).thenReturn(true);

        NodePropertiesTools.appendOrReplaceNodeProperty(mockNode,
                                                        "mockPropertyName",
                                                        mockValue);

        verify(mockProperty).setValue(mockValue);

    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void appendValueToExistingMultivaluedProperty()
        throws RepositoryException {

        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockNode.hasProperty("mockPropertyName")).thenReturn(true);
        final Value previousValue = mock(Value.class);
        Value[] values = new Value[]{previousValue};
        when(mockProperty.getValues()).thenReturn(values);

        NodePropertiesTools.appendOrReplaceNodeProperty(mockNode,
                                                        "mockPropertyName",
                                                        mockValue);

        ArgumentCaptor<Value[]> valuesCaptor =
            ArgumentCaptor.forClass(Value[].class);


        verify(mockProperty).setValue(valuesCaptor.capture());

        List<Value> actualValues = Arrays.asList(valuesCaptor.getValue());

        assertEquals(2, actualValues.size());
        assertTrue("actual values missing previous value",
                   actualValues.contains(previousValue));
        assertTrue("actual values missing value we were adding",
                   actualValues.contains(mockValue));

    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void addMultiValuedPropertyWithSameValueAsExistingProperty()
        throws RepositoryException {

        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockNode.hasProperty("mockPropertyName")).thenReturn(true);
        final Value previousValue = mockValue;
        Value[] values = new Value[]{previousValue};
        when(mockProperty.getValues()).thenReturn(values);

        NodePropertiesTools.appendOrReplaceNodeProperty(mockNode,
                                                        "mockPropertyName",
                                                        mockValue);

        verify(mockProperty, never()).setValue(any(Value[].class));

    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void shouldBeANoopWhenRemovingPropertyThatDoesntExist()
        throws RepositoryException {
        when(mockNode.hasProperty("mockPropertyName")).thenReturn(false);

        NodePropertiesTools.removeNodeProperty(mockNode, "mockPropertyName",
                                               mockValue);

        verify(mockNode).hasProperty("mockPropertyName");
        verifyNoMoreInteractions(mockNode);
        verifyZeroInteractions(mockProperty);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void shouldRemoveASingleValuedProperty() throws RepositoryException {

        when(mockProperty.isMultiple()).thenReturn(false);

        when(mockNode.hasProperty("mockPropertyName")).thenReturn(true);

        when(mockProperty.getValue()).thenReturn(mockValue);
        when(mockValue.getString()).thenReturn("");

        NodePropertiesTools.removeNodeProperty(mockNode, "mockPropertyName",
                                               mockValue);

        verify(mockProperty).setValue((Value)null);

    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void shouldRemoveAMultiValuedProperty() throws RepositoryException {

        when(mockProperty.isMultiple()).thenReturn(true);

        when(mockNode.hasProperty("mockPropertyName")).thenReturn(true);
        Value[] values = new Value[]{mockValue};
        when(mockProperty.getValues()).thenReturn(values);

        NodePropertiesTools.removeNodeProperty(mockNode, "mockPropertyName",
                                               mockValue);

        verify(mockProperty).setValue((Value[])null);

    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void shouldRemoveAValueFromMultiValuedProperty()
        throws RepositoryException {

        when(mockProperty.isMultiple()).thenReturn(true);

        when(mockNode.hasProperty("mockPropertyName")).thenReturn(true);

        final Value previousValue = mock(Value.class);
        Value[] values = new Value[]{previousValue, mockValue};

        when(mockProperty.getValues()).thenReturn(values);

        NodePropertiesTools.removeNodeProperty(mockNode, "mockPropertyName",
                                               mockValue);

        ArgumentCaptor<Value[]> valuesCaptor =
            ArgumentCaptor.forClass(Value[].class);

        verify(mockProperty).setValue(valuesCaptor.capture());

        Value[] actualValues = valuesCaptor.getValue();

        assertEquals(1, actualValues.length);
        assertTrue("removed the wrong value",
                   previousValue.equals(actualValues[0]));
        assertTrue("found the value we were removing",
                   !mockValue.equals(actualValues[0]));

    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void shouldRemoveAllMatchingValuesFromAMultivaluedProperty()
        throws RepositoryException {

        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockNode.hasProperty("mockPropertyName")).thenReturn(true);
        Value[] values = new Value[]{mockValue, mockValue};

        when(mockProperty.getValues()).thenReturn(values);

        NodePropertiesTools.removeNodeProperty(mockNode, "mockPropertyName",
                                               mockValue);

        verify(mockProperty).setValue((Value[])null);
    }
}
