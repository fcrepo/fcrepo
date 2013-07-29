/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.utils;

import static java.util.Arrays.asList;
import static org.fcrepo.utils.FedoraTypesUtils.getDefinitionForPropertyName;
import static org.fcrepo.utils.NodePropertiesTools.appendOrReplaceNodeProperty;
import static org.fcrepo.utils.NodePropertiesTools.removeNodeProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

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
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({FedoraTypesUtils.class})
public class NodePropertiesToolsTest {

    @Mock
    private PropertyDefinition mockDefinition;

    @Mock
    private Value mockValue;

    @Mock
    private Node mockNode;

    @Mock
    private Property mockProperty;

    @Mock
    private Value previousValue;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        mockStatic(FedoraTypesUtils.class);
        when(getDefinitionForPropertyName(mockNode, "mockPropertyName"))
                .thenReturn(mockDefinition);
        when(mockNode.getProperty("mockPropertyName")).thenReturn(mockProperty);
    }

    @Test
    public void addNewSingleValuedProperty() throws RepositoryException {
        when(mockDefinition.isMultiple()).thenReturn(false);
        when(mockNode.hasProperty("mockPropertyName")).thenReturn(false);
        appendOrReplaceNodeProperty(mockNode, "mockPropertyName", mockValue);
        verify(mockNode).setProperty("mockPropertyName", mockValue, 0);
    }

    @Test
    public void addNewMultiValuedProperty() throws RepositoryException {
        when(mockDefinition.isMultiple()).thenReturn(true);
        when(mockNode.hasProperty("mockPropertyName")).thenReturn(false);
        appendOrReplaceNodeProperty(mockNode, "mockPropertyName", mockValue);
        verify(mockNode).setProperty("mockPropertyName",
                asList(mockValue).toArray(new Value[0]), 0);
    }

    @Test
    public void replaceExistingSingleValuedPropertyWithValue()
            throws RepositoryException {
        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockNode.hasProperty("mockPropertyName")).thenReturn(true);
        appendOrReplaceNodeProperty(mockNode, "mockPropertyName", mockValue);
        verify(mockProperty).setValue(mockValue);
    }

    @Test
    public void appendValueToExistingMultivaluedProperty()
            throws RepositoryException {
        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockNode.hasProperty("mockPropertyName")).thenReturn(true);
        final Value[] values = new Value[] {previousValue};
        when(mockProperty.getValues()).thenReturn(values);
        appendOrReplaceNodeProperty(mockNode, "mockPropertyName", mockValue);
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
        when(mockNode.hasProperty("mockPropertyName")).thenReturn(true);
        final Value[] values = new Value[] {previousValue};
        when(mockProperty.getValues()).thenReturn(values);
        appendOrReplaceNodeProperty(mockNode, "mockPropertyName", previousValue);

        verify(mockProperty, never()).setValue(any(Value[].class));
    }

    @Test
    public void shouldBeANoopWhenRemovingPropertyThatDoesntExist()
            throws RepositoryException {
        when(mockNode.hasProperty("mockPropertyName")).thenReturn(false);
        removeNodeProperty(mockNode, "mockPropertyName", mockValue);
        verify(mockNode).hasProperty("mockPropertyName");
        verifyNoMoreInteractions(mockNode);
        verifyZeroInteractions(mockProperty);
    }

    @Test
    public void shouldRemoveASingleValuedProperty() throws RepositoryException {
        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockNode.hasProperty("mockPropertyName")).thenReturn(true);
        when(mockProperty.getValue()).thenReturn(mockValue);
        when(mockValue.getString()).thenReturn("");
        removeNodeProperty(mockNode, "mockPropertyName", mockValue);
        verify(mockProperty).setValue((Value) null);

    }

    @Test
    public void shouldRemoveAMultiValuedProperty() throws RepositoryException {
        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockNode.hasProperty("mockPropertyName")).thenReturn(true);
        final Value[] values = new Value[] {mockValue};
        when(mockProperty.getValues()).thenReturn(values);
        removeNodeProperty(mockNode, "mockPropertyName", mockValue);
        verify(mockProperty).setValue((Value[]) null);

    }

    @Test
    public void shouldRemoveAValueFromMultiValuedProperty()
            throws RepositoryException {
        when(mockProperty.isMultiple()).thenReturn(true);
        when(mockNode.hasProperty("mockPropertyName")).thenReturn(true);
        final Value[] values = new Value[] {previousValue, mockValue};
        when(mockProperty.getValues()).thenReturn(values);
        removeNodeProperty(mockNode, "mockPropertyName", mockValue);
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
        when(mockNode.hasProperty("mockPropertyName")).thenReturn(true);
        final Value[] values = new Value[] {mockValue, mockValue};
        when(mockProperty.getValues()).thenReturn(values);
        removeNodeProperty(mockNode, "mockPropertyName", mockValue);
        verify(mockProperty).setValue((Value[]) null);
    }
}
