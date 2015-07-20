/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.utils;

import static java.util.Arrays.asList;
import static javax.jcr.PropertyType.URI;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * <p>NodePropertiesToolsTest class.</p>
 *
 * @author awoods
 */
@Ignore
public class NodePropertiesToolsTest {

    private final NodePropertiesTools testNodePropertiesTools = new NodePropertiesTools();

    @Mock
    private PropertyDefinition mockDefinition;

    @Mock
    private Value mockValue;

    @Mock
    private Value mockRefValue;

    @Mock
    private Node mockNode;

    @Mock
    private Property mockProperty;

    @Mock
    private Property mockRefProperty;

    @Mock
    private Value previousValue;

    @Mock
    private IdentifierConverter<Resource, FedoraResource> idTranslator;

    @Mock
    private Session mockSession;

    @Mock
    private ValueFactory mockValueFactory;

    private final String mockPropertyName = "mockPropertyName";

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);

        final PropertyDefinition[] mockPropertyDefinitions = new PropertyDefinition[] {mockDefinition};
        when(mockNodeType.getPropertyDefinitions()).thenReturn(mockPropertyDefinitions);
        when(mockDefinition.getName()).thenReturn(mockPropertyName);
        when(mockNode.getPath()).thenReturn("/some/path");
        when(mockNode.getProperty(mockPropertyName)).thenReturn(mockProperty);

        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[] { });

        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);

        idTranslator = new DefaultIdentifierTranslator(mockSession);
    }


    @Test
    public void addNewSingleValuedProperty() throws RepositoryException {
        when(mockDefinition.isMultiple()).thenReturn(false);
        when(mockNode.hasProperty(mockPropertyName)).thenReturn(false);
        when(mockNode.setProperty(mockPropertyName, mockValue, 0)).thenReturn(mockProperty);
        testNodePropertiesTools.appendOrReplaceNodeProperty(mockNode, mockPropertyName, mockValue);
        verify(mockNode).setProperty(mockPropertyName, mockValue, 0);
    }

    @Test
    public void addNewSingleValuedUriRefProperty() throws RepositoryException {
        when(mockProperty.getType()).thenReturn(URI);
        when(mockProperty.getName()).thenReturn(mockPropertyName);
        when(mockDefinition.isMultiple()).thenReturn(false);
        when(mockNode.hasProperty(mockPropertyName)).thenReturn(false);
        when(mockNode.setProperty(mockPropertyName, mockValue, 0)).thenReturn(mockProperty);
        when(mockValue.getString()).thenReturn("xyz");
        when(mockValueFactory.createValue(mockNode, true)).thenReturn(mockRefValue);
        when(mockRefValue.getType()).thenReturn(WEAKREFERENCE);
        when(mockNode.setProperty("mockPropertyName_ref", asList(mockRefValue).toArray(new Value[0]), WEAKREFERENCE))
                .thenReturn(mockRefProperty);
        testNodePropertiesTools.appendOrReplaceNodeProperty(mockNode,
                mockPropertyName, mockValue);
        verify(mockNode).setProperty(mockPropertyName, mockValue, 0);

        verify(mockNode, times(1)).setProperty("mockPropertyName_ref",
                                        asList(mockRefValue).toArray(new Value[0]), WEAKREFERENCE);
    }

    @Test
    public void addNewMultiValuedProperty() throws RepositoryException {
        when(mockDefinition.isMultiple()).thenReturn(true);
        when(mockNode.hasProperty(mockPropertyName)).thenReturn(false);
        when(mockNode.setProperty(mockPropertyName, asList(mockValue).toArray(new Value[0]), 0))
                .thenReturn(mockProperty);
        testNodePropertiesTools.appendOrReplaceNodeProperty(mockNode,
                mockPropertyName, mockValue);
        verify(mockNode).setProperty(mockPropertyName,
                asList(mockValue).toArray(new Value[0]), 0);
    }

    @Test
    public void addNewMultiValuedUriRefProperty() throws RepositoryException {
        when(mockProperty.getType()).thenReturn(URI);
        when(mockProperty.getName()).thenReturn(mockPropertyName);

        when(mockValue.getString()).thenReturn("xyz");
        when(mockValueFactory.createValue(mockNode, true)).thenReturn(mockRefValue);
        when(mockRefValue.getType()).thenReturn(WEAKREFERENCE);

        when(mockDefinition.isMultiple()).thenReturn(true);
        when(mockNode.hasProperty(mockPropertyName)).thenReturn(false);
        when(mockNode.setProperty(mockPropertyName, asList(mockValue).toArray(new Value[0]), 0))
                .thenReturn(mockProperty);
        when(mockNode.setProperty("mockPropertyName_ref", asList(mockRefValue).toArray(new Value[0]), WEAKREFERENCE))
                .thenReturn(mockRefProperty);
        testNodePropertiesTools.appendOrReplaceNodeProperty(mockNode, mockPropertyName, mockValue);
        verify(mockNode).setProperty(mockPropertyName,
                                        asList(mockValue).toArray(new Value[0]), 0);
        verify(mockNode).setProperty("mockPropertyName_ref",
                                        asList(mockRefValue).toArray(new Value[0]), WEAKREFERENCE);
    }

    @Test
    public void replaceExistingSingleValuedPropertyWithValue()
            throws RepositoryException {
        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockNode.hasProperty(mockPropertyName)).thenReturn(true);
        testNodePropertiesTools.appendOrReplaceNodeProperty(mockNode, mockPropertyName, mockValue);
        verify(mockProperty).setValue(mockValue);
    }


    @Test
    public void replaceExistingSingleValuedUriPropertyWithValue()
        throws RepositoryException {
        when(mockProperty.getType()).thenReturn(URI);
        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockProperty.getName()).thenReturn(mockPropertyName);
        when(mockNode.hasProperty(mockPropertyName)).thenReturn(true);
        when(mockNode.hasProperty("mockPropertyName_ref")).thenReturn(true,false);
        when(mockValue.getString()).thenReturn("xyz");
        when(mockValueFactory.createValue(mockNode, true)).thenReturn(mockRefValue);
        when(mockRefValue.getType()).thenReturn(WEAKREFERENCE);
        when(mockNode.setProperty("mockPropertyName_ref", asList(mockRefValue).toArray(new Value[0]), WEAKREFERENCE))
                .thenReturn(mockRefProperty);
        testNodePropertiesTools.appendOrReplaceNodeProperty(mockNode, mockPropertyName, mockValue);
        verify(mockProperty).setValue(mockValue);
        final InOrder inOrder = Mockito.inOrder(mockNode, mockNode);
        inOrder.verify(mockNode).setProperty("mockPropertyName_ref", (Value[])null);
        inOrder.verify(mockNode, times(1)).setProperty("mockPropertyName_ref",
                                                       asList(mockRefValue).toArray(new Value[0]), WEAKREFERENCE);
    }


    @Test
    public void appendValueToExistingMultivaluedProperty()
            throws RepositoryException {
        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockNode.hasProperty(mockPropertyName)).thenReturn(true);
        final Value[] values = new Value[] {previousValue};
        when(mockProperty.getValues()).thenReturn(values);
        testNodePropertiesTools.appendOrReplaceNodeProperty(mockNode, mockPropertyName, mockValue);
        final ArgumentCaptor<Value[]> valuesCaptor =
                ArgumentCaptor.forClass(Value[].class);
        verify(mockProperty).setValue(valuesCaptor.capture());
        final List<Value> actualValues = asList(valuesCaptor.getValue());

        assertEquals(2, actualValues.size());
        assertTrue("actual values missing previous value", actualValues
                .contains(previousValue));
        assertTrue("actual values missing value we were adding", actualValues
                .contains(mockValue));
    }

    @Test
    public void appendValueToExistingMultivaluedUriProperty()
        throws RepositoryException {
        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockProperty.getType()).thenReturn(URI);
        when(mockProperty.getName()).thenReturn(mockPropertyName);

        when(mockValue.getString()).thenReturn("xyz");
        when(mockValueFactory.createValue(mockNode, true)).thenReturn(mockRefValue);
        when(mockRefValue.getType()).thenReturn(WEAKREFERENCE);

        when(mockDefinition.isMultiple()).thenReturn(true);
        when(mockNode.hasProperty(mockPropertyName)).thenReturn(false);
        when(mockNode.hasProperty("mockPropertyName_ref")).thenReturn(false);
        when(mockNode.setProperty(mockPropertyName, asList(mockValue).toArray(new Value[0]), 0))
                .thenReturn(mockProperty);
        when(mockNode.setProperty("mockPropertyName_ref", asList(mockRefValue).toArray(new Value[0]), WEAKREFERENCE))
                .thenReturn(mockRefProperty);

        when(mockNode.hasProperty(mockPropertyName)).thenReturn(true);
        final Value[] values = new Value[] {previousValue};
        when(mockProperty.getValues()).thenReturn(values);
        testNodePropertiesTools.appendOrReplaceNodeProperty(mockNode, mockPropertyName, mockValue);
        final ArgumentCaptor<Value[]> valuesCaptor =
            ArgumentCaptor.forClass(Value[].class);
        verify(mockProperty).setValue(valuesCaptor.capture());
        final List<Value> actualValues = asList(valuesCaptor.getValue());

        assertEquals(2, actualValues.size());
        assertTrue("actual values missing previous value", actualValues
                                                               .contains(previousValue));
        assertTrue("actual values missing value we were adding", actualValues
                                                                     .contains(mockValue));

    }

    @Test
    public void addMultiValuedPropertyWithSameValueAsExistingProperty()
            throws RepositoryException {

        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockNode.hasProperty(mockPropertyName)).thenReturn(true);
        final Value[] values = new Value[] {previousValue};
        when(mockProperty.getValues()).thenReturn(values);
        testNodePropertiesTools.appendOrReplaceNodeProperty(mockNode, mockPropertyName, previousValue);

        verify(mockProperty, never()).setValue(any(Value[].class));
    }

    @Test
    public void shouldBeANoopWhenRemovingPropertyThatDoesntExist()
            throws RepositoryException {
        when(mockNode.hasProperty(mockPropertyName)).thenReturn(false);
        testNodePropertiesTools.removeNodeProperty(mockNode, mockPropertyName, mockValue);
        verify(mockNode).hasProperty(mockPropertyName);
        verifyNoMoreInteractions(mockNode);
        verifyZeroInteractions(mockProperty);
    }

    @Test
    public void shouldRemoveASingleValuedProperty() throws RepositoryException {
        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockNode.hasProperty(mockPropertyName)).thenReturn(true);
        when(mockProperty.getValue()).thenReturn(mockValue);
        when(mockValue.getString()).thenReturn("");
        testNodePropertiesTools.removeNodeProperty(mockNode, mockPropertyName, mockValue);
        verify(mockProperty).setValue((Value) null);

    }

    @Test
    public void shouldRemoveASingleValuedUriProperty() throws RepositoryException {
        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockProperty.getType()).thenReturn(URI);
        when(mockProperty.getName()).thenReturn(mockPropertyName);
        when(mockValue.getString()).thenReturn("xyz");
        when(mockNode.hasProperty(mockPropertyName)).thenReturn(true);
        when(mockNode.hasProperty("mockPropertyName_ref")).thenReturn(true);
        when(mockProperty.getValue()).thenReturn(mockValue);
        testNodePropertiesTools.removeNodeProperty(mockNode, mockPropertyName, mockValue);
        verify(mockProperty).setValue((Value) null);
        verify(mockNode).setProperty("mockPropertyName_ref", (Value[])null);

    }

    @Test
    public void shouldRemoveAMultiValuedProperty() throws RepositoryException {
        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockNode.hasProperty(mockPropertyName)).thenReturn(true);
        final Value[] values = new Value[] {mockValue};
        when(mockProperty.getValues()).thenReturn(values);
        testNodePropertiesTools.removeNodeProperty(mockNode, mockPropertyName, mockValue);
        verify(mockProperty).setValue((Value[]) null);
    }

    @Test
    public void shouldRemoveAMultiValuedUriProperty() throws RepositoryException {
        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockProperty.getType()).thenReturn(URI);
        when(mockProperty.getName()).thenReturn(mockPropertyName);
        when(mockNode.hasProperty(mockPropertyName)).thenReturn(true);
        when(mockNode.hasProperty("mockPropertyName_ref")).thenReturn(true);
        when(mockNode.getProperty("mockPropertyName_ref")).thenReturn(mockRefProperty);
        when(mockRefProperty.isMultiple()).thenReturn(true);
        when(mockRefProperty.getValues()).thenReturn(new Value[] { mockRefValue });
        when(mockValue.getString()).thenReturn("xyz");
        when(mockValueFactory.createValue(mockNode, true)).thenReturn(mockRefValue);
        final Value[] values = new Value[] {mockValue};
        when(mockProperty.getValues()).thenReturn(values);
        testNodePropertiesTools.removeNodeProperty(mockNode, mockPropertyName, mockValue);
        verify(mockProperty).setValue((Value[]) null);
        verify(mockRefProperty).setValue((Value[])null);
    }

    @Test
    public void shouldRemoveAValueFromMultiValuedProperty()
            throws RepositoryException {
        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockNode.hasProperty(mockPropertyName)).thenReturn(true);
        final Value[] values = new Value[] {previousValue, mockValue};
        when(mockProperty.getValues()).thenReturn(values);
        testNodePropertiesTools.removeNodeProperty(mockNode, mockPropertyName, mockValue);
        final ArgumentCaptor<Value[]> valuesCaptor =
                ArgumentCaptor.forClass(Value[].class);
        verify(mockProperty).setValue(valuesCaptor.capture());
        final Value[] actualValues = valuesCaptor.getValue();
        assertEquals(1, actualValues.length);
        assertTrue("removed the wrong value", previousValue
                .equals(actualValues[0]));
        assertTrue("found the value we were removing", !mockValue
                .equals(actualValues[0]));

    }

    @Test
    public void shouldRemoveAllMatchingValuesFromAMultivaluedProperty()
            throws RepositoryException {

        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockNode.hasProperty(mockPropertyName)).thenReturn(true);
        final Value[] values = new Value[] {mockValue, mockValue};
        when(mockProperty.getValues()).thenReturn(values);
        testNodePropertiesTools.removeNodeProperty(mockNode, mockPropertyName, mockValue);
        verify(mockProperty).setValue((Value[]) null);
    }
}
