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
package org.fcrepo.serialization;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>SerializerUtilTest class.</p>
 *
 * @author cbeer
 */
public class SerializerUtilTest {

    private ApplicationContext mockContext;

    private SerializerUtil testObj;

    @Before
    public void setUp() {
        mockContext = mock(ApplicationContext.class);

        testObj = new SerializerUtil();
        testObj.setApplicationContext(mockContext);

        final FedoraObjectSerializer mockA = mock(FedoraObjectSerializer.class);
        when(mockA.getKey()).thenReturn("a");
        final FedoraObjectSerializer mockB = mock(FedoraObjectSerializer.class);
        when(mockB.getKey()).thenReturn("b");

        when(mockContext.getBeansOfType(FedoraObjectSerializer.class))
                .thenReturn(ImmutableMap.of("mockA", mockA, "mockB", mockB));

        testObj.buildFedoraObjectSerializersMap();

    }

    @Test
    public void shouldProvideSetOfFormatsItCanSerialize() {
        assertEquals(ImmutableSet.of("a", "b"), testObj.keySet());
    }

    @Test
    public void shouldRetrieveSerializerByKey() {
        final FedoraObjectSerializer mockA = mock(FedoraObjectSerializer.class);
        when(mockA.getKey()).thenReturn("a");
        final FedoraObjectSerializer mockB = mock(FedoraObjectSerializer.class);
        when(mockB.getKey()).thenReturn("b");

        when(mockContext.getBeansOfType(FedoraObjectSerializer.class))
                .thenReturn(ImmutableMap.of("mockA", mockA, "mockB", mockB));

        testObj.buildFedoraObjectSerializersMap();

        assertEquals(mockA, testObj.getSerializer("a"));
    }

    @Test
    public void shouldListAllSerializers() {
        final FedoraObjectSerializer mockA = mock(FedoraObjectSerializer.class);
        when(mockA.getKey()).thenReturn("a");
        final FedoraObjectSerializer mockB = mock(FedoraObjectSerializer.class);
        when(mockB.getKey()).thenReturn("b");

        when(mockContext.getBeansOfType(FedoraObjectSerializer.class))
                .thenReturn(ImmutableMap.of("mockA", mockA, "mockB", mockB));

        testObj.buildFedoraObjectSerializersMap();

        assertEquals(ImmutableMap.of("a", mockA, "b", mockB), testObj
                .getFedoraObjectSerializers());

    }
}
