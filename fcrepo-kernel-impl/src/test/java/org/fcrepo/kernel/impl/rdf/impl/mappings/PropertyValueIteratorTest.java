/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.impl.rdf.impl.mappings;

import com.google.common.collect.ImmutableList;

import org.fcrepo.kernel.impl.testutilities.TestPropertyIterator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author cabeer
 */
public class PropertyValueIteratorTest {

    private PropertyValueIterator testObj;

    @Mock
    private Property mockProperty;

    @Mock
    private Property mockMultivaluedProperty;

    @Mock
    private Value value1;

    @Mock
    private Value value2;

    @Mock
    private Value value3;


    private PropertyIterator propertyIterator;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockProperty.getValue()).thenReturn(value1);
        when(mockMultivaluedProperty.isMultiple()).thenReturn(true);
        when(mockMultivaluedProperty.getValues()).thenReturn(new Value[] { value2, value3 });
        propertyIterator = new TestPropertyIterator(mockProperty, mockMultivaluedProperty);
    }

    @Test
    public void testSingleValueSingleProperty() {
        testObj = new PropertyValueIterator(mockProperty);
        final List<Value> values = newArrayList(testObj);
        assertTrue(values.contains(value1));
    }

    @Test
    public void testMultiValueSingleProperty() {
        testObj = new PropertyValueIterator(mockMultivaluedProperty);
        final List<Value> values = newArrayList(testObj);
        assertTrue(values.containsAll(ImmutableList.of(value2, value3)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSingleValuePropertyIterator() {
        testObj = new PropertyValueIterator(propertyIterator);
        final List<Value> values = newArrayList(testObj);
        assertTrue(values.containsAll(ImmutableList.of(value1, value2, value3)));
    }
}