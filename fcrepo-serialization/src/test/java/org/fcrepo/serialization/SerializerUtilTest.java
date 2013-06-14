package org.fcrepo.serialization;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SerializerUtilTest {

    private ApplicationContext mockContext;
    private SerializerUtil testObj;

    @Before
    public void setUp() {
        mockContext = mock(ApplicationContext.class);

        testObj = new SerializerUtil();
        testObj.setApplicationContext(mockContext);


        FedoraObjectSerializer mockA = mock(FedoraObjectSerializer.class);
        when(mockA.getKey()).thenReturn("a");
        FedoraObjectSerializer mockB = mock(FedoraObjectSerializer.class);
        when(mockB.getKey()).thenReturn("b");

        when(mockContext.getBeansOfType(FedoraObjectSerializer.class)).thenReturn(ImmutableMap.of("mockA", mockA, "mockB", mockB));

        testObj.buildFedoraObjectSerializersMap();

    }

    @Test
    public void shouldProvideSetOfFormatsItCanSerialize() {
        assertEquals(ImmutableSet.of("a", "b"), testObj.keySet());
    }

    @Test
    public void shouldRetrieveSerializerByKey() {
        FedoraObjectSerializer mockA = mock(FedoraObjectSerializer.class);
        when(mockA.getKey()).thenReturn("a");
        FedoraObjectSerializer mockB = mock(FedoraObjectSerializer.class);
        when(mockB.getKey()).thenReturn("b");

        when(mockContext.getBeansOfType(FedoraObjectSerializer.class)).thenReturn(ImmutableMap.of("mockA", mockA, "mockB", mockB));

        testObj.buildFedoraObjectSerializersMap();

        assertEquals(mockA, testObj.getSerializer("a"));
    }

    @Test
    public void shouldListAllSerializers() {
        FedoraObjectSerializer mockA = mock(FedoraObjectSerializer.class);
        when(mockA.getKey()).thenReturn("a");
        FedoraObjectSerializer mockB = mock(FedoraObjectSerializer.class);
        when(mockB.getKey()).thenReturn("b");

        when(mockContext.getBeansOfType(FedoraObjectSerializer.class)).thenReturn(ImmutableMap.of("mockA", mockA, "mockB", mockB));

        testObj.buildFedoraObjectSerializersMap();

        assertEquals(ImmutableMap.of("a", mockA, "b", mockB), testObj.getFedoraObjectSerializers());

    }
}
