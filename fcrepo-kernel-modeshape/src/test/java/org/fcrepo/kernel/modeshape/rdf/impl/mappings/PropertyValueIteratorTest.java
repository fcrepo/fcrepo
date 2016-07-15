/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.rdf.impl.mappings;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterators.contains;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author cabeer
 * @author ajs6f
 */
public class PropertyValueIteratorTest {

    private PropertyValueIterator testObj;

    @Mock
    private Property mockProperty;

    @Mock
    private Property mockMultivaluedProperty;


    @Mock
    private Property mockMultivaluedEmptyProperty;

    @Mock
    private Value value1;

    @Mock
    private Value value2;

    @Mock
    private Value value3;

    private Iterator<Property> propertyIterator;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockProperty.getValue()).thenReturn(value1);
        when(mockMultivaluedProperty.isMultiple()).thenReturn(true);
        when(mockMultivaluedProperty.getValues()).thenReturn(new Value[] { value2, value3 });
        propertyIterator = of(mockProperty, mockMultivaluedProperty).iterator();

        when(mockMultivaluedEmptyProperty.isMultiple()).thenReturn(true);
        when(mockMultivaluedEmptyProperty.getValues()).thenReturn(new Value[] {});
    }

    @Test
    public void testSingleValueSingleProperty() {
        testObj = new PropertyValueIterator(mockProperty);
        assertTrue(contains(testObj, value1));
    }

    @Test
    public void testMultiValueSingleProperty() {
        testObj = new PropertyValueIterator(mockMultivaluedProperty);
        final List<Value> values = newArrayList(testObj);
        assertTrue(values.containsAll(of(value2, value3)));
    }

    @Test
    public void testMultiValueSingleEmptyProperty() {
        testObj = new PropertyValueIterator(mockMultivaluedEmptyProperty);
        final List<Value> values = newArrayList(testObj);
        assertTrue(values.isEmpty());
    }

    @Test
    public void testSingleValuePropertyIterator() {
        testObj = new PropertyValueIterator(propertyIterator);
        final List<Value> values = newArrayList(testObj);
        assertTrue(values.containsAll(of(value1, value2, value3)));
    }
}
