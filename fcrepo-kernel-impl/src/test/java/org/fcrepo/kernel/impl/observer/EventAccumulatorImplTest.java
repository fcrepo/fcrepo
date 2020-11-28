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

package org.fcrepo.kernel.impl.observer;

import com.google.common.eventbus.EventBus;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.observer.Event;
import org.fcrepo.kernel.api.observer.EventType;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.impl.operations.DeleteResourceOperationFactoryImpl;
import org.fcrepo.kernel.impl.operations.RdfSourceOperationFactoryImpl;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * @author pwinckles
 */
@RunWith(MockitoJUnitRunner.class)
public class EventAccumulatorImplTest {

    private static final String TX_ID = "tx1";
    private static final String BASE_URL = "http://localhost/rest";
    private static final String USER_AGENT = "user-agent";
    private static final String USER = "user";

    private static final URI CONTAINER_TYPE = uri("http://www.w3.org/ns/ldp#Container");
    private static final URI RESOURCE_TYPE = uri("http://fedora.info/definitions/v4/repository#Resource");
    private static final URI RDF_TYPE = uri("http://www.w3.org/ns/ldp#RDFSource");

    private EventAccumulatorImpl accumulator;

    @Mock
    private ResourceFactory resourceFactory;

    @Mock
    private EventBus eventBus;

    private ArgumentCaptor<Event> eventCaptor;

    @Before
    public void setup() {
        accumulator = new EventAccumulatorImpl();
        setField(accumulator, "resourceFactory", resourceFactory);
        setField(accumulator, "eventBus", eventBus);
        eventCaptor = ArgumentCaptor.forClass(Event.class);
    }

    @Test
    public void emitEventsWhenEventsOnTransactionNoMerge() throws PathNotFoundException {
        final var fId1 = FedoraId.create("/test/1");
        final var fId2 = FedoraId.create("/test/2");

        final var op1 = createOp(fId1);
        final var op2 = updateOp(fId2);

        accumulator.recordEventForOperation(TX_ID, fId1, op1);
        accumulator.recordEventForOperation(TX_ID, fId2, op2);

        expectResource(fId1, CONTAINER_TYPE);
        expectResource(fId2, CONTAINER_TYPE, RESOURCE_TYPE);

        accumulator.emitEvents(TX_ID, BASE_URL, USER_AGENT);

        verify(eventBus, times(2)).post(eventCaptor.capture());

        final var events = eventCaptor.getAllValues();

        assertThat(events, containsInAnyOrder(
                defaultEvent(fId1, Set.of(EventType.RESOURCE_CREATION), Set.of(CONTAINER_TYPE.toString())),
                defaultEvent(fId2, Set.of(EventType.RESOURCE_MODIFICATION),
                        Set.of(CONTAINER_TYPE.toString(), RESOURCE_TYPE.toString()))
        ));
    }

    @Test
    public void emitEventsWhenEventsOnTransactionWithMerge() throws PathNotFoundException {
        final var fId1 = FedoraId.create("/test/1");
        final var fId2 = FedoraId.create("/test/2");

        final var op1 = createOp(fId1);
        final var op2 = updateOp(fId2);
        final var op3 = updateOp(fId1);

        accumulator.recordEventForOperation(TX_ID, fId1, op1);
        accumulator.recordEventForOperation(TX_ID, fId2, op2);
        accumulator.recordEventForOperation(TX_ID, fId1, op3);

        expectResource(fId1, CONTAINER_TYPE, RDF_TYPE);
        expectResource(fId2, CONTAINER_TYPE, RESOURCE_TYPE);

        accumulator.emitEvents(TX_ID, BASE_URL, USER_AGENT);

        verify(eventBus, times(2)).post(eventCaptor.capture());

        final var events = eventCaptor.getAllValues();

        assertThat(events, containsInAnyOrder(
                defaultEvent(fId1, Set.of(EventType.RESOURCE_CREATION, EventType.RESOURCE_MODIFICATION),
                        Set.of(CONTAINER_TYPE.toString(), RDF_TYPE.toString())),
                defaultEvent(fId2, Set.of(EventType.RESOURCE_MODIFICATION),
                        Set.of(CONTAINER_TYPE.toString(), RESOURCE_TYPE.toString()))
        ));
    }

    @Test
    public void onlyEmitEventsForSameSpecifiedTransaction() throws PathNotFoundException {
        final var fId1 = FedoraId.create("/test/1");
        final var fId2 = FedoraId.create("/test/2");

        final var op1 = createOp(fId1);
        final var op2 = updateOp(fId2);

        accumulator.recordEventForOperation(TX_ID, fId1, op1);
        accumulator.recordEventForOperation("tx2", fId2, op2);

        expectResource(fId2, CONTAINER_TYPE);

        accumulator.emitEvents("tx2", BASE_URL, USER_AGENT);

        verify(eventBus, times(1)).post(eventCaptor.capture());

        final var events = eventCaptor.getAllValues();

        assertThat(events, contains(
                defaultEvent(fId2, Set.of(EventType.RESOURCE_MODIFICATION),
                        Set.of(CONTAINER_TYPE.toString()))
        ));
    }

    @Test
    public void doNothingWhenTransactionHasNoEvents() throws PathNotFoundException {
        final var fId1 = FedoraId.create("/test/1");
        final var fId2 = FedoraId.create("/test/2");

        final var op1 = createOp(fId1);
        final var op2 = updateOp(fId2);

        accumulator.recordEventForOperation(TX_ID, fId1, op1);
        accumulator.recordEventForOperation("tx2", fId2, op2);

        expectResource(fId2, CONTAINER_TYPE);

        accumulator.emitEvents("tx3", BASE_URL, USER_AGENT);

        verify(eventBus, times(0)).post(eventCaptor.capture());

        final var events = eventCaptor.getAllValues();

        assertEquals(0, events.size());
    }

    @Test
    public void clearTransactionEventsWhenCleared() throws PathNotFoundException {
        final var fId1 = FedoraId.create("/test/1");
        final var fId2 = FedoraId.create("/test/2");

        final var op1 = createOp(fId1);
        final var op2 = updateOp(fId2);
        final var op3 = updateOp(fId1);

        accumulator.recordEventForOperation(TX_ID, fId1, op1);
        accumulator.recordEventForOperation(TX_ID, fId2, op2);
        accumulator.recordEventForOperation(TX_ID, fId1, op3);

        expectResource(fId1, CONTAINER_TYPE, RDF_TYPE);
        expectResource(fId2, CONTAINER_TYPE, RESOURCE_TYPE);

        accumulator.clearEvents(TX_ID);

        accumulator.emitEvents(TX_ID, BASE_URL, USER_AGENT);

        verify(eventBus, times(0)).post(eventCaptor.capture());

        final var events = eventCaptor.getAllValues();
        assertEquals(0, events.size());
    }

    @Test
    public void nonDefaultValues() throws PathNotFoundException {
        final var tx = "tx4";
        final var url = "http://example.com/rest";
        final var agent = "me";

        final var fId1 = FedoraId.create("/example/1");
        final var fId2 = FedoraId.create("/example/2");

        final var op1 = createOp(fId1);
        final var op2 = updateOp(fId2);
        final var op3 = deleteOp(fId1);

        accumulator.recordEventForOperation(tx, fId1, op1);
        accumulator.recordEventForOperation(tx, fId2, op2);
        accumulator.recordEventForOperation(tx, fId1, op3);

        expectResource(fId1, RDF_TYPE);
        expectResource(fId2, RESOURCE_TYPE);

        accumulator.emitEvents(tx, url, agent);

        verify(eventBus, times(2)).post(eventCaptor.capture());

        final var events = eventCaptor.getAllValues();

        assertThat(events, containsInAnyOrder(
                event(fId1, Set.of(EventType.RESOURCE_CREATION, EventType.RESOURCE_DELETION),
                        Set.of(RDF_TYPE.toString()), url, agent),
                event(fId2, Set.of(EventType.RESOURCE_MODIFICATION),
                        Set.of(RESOURCE_TYPE.toString()), url, agent)
        ));
    }

    @Test
    public void doNotSetResourceTypesWhenCannotLoad() throws PathNotFoundException {
        final var fId1 = FedoraId.create("/test/1");
        final var fId2 = FedoraId.create("/test/2");
        final var fId3 = FedoraId.create("/test/3");

        final var op1 = createOp(fId1);
        final var op2 = deleteOp(fId2);
        final var op3 = updateOp(fId3);

        accumulator.recordEventForOperation(TX_ID, fId1, op1);
        accumulator.recordEventForOperation(TX_ID, fId2, op2);
        accumulator.recordEventForOperation(TX_ID, fId3, op3);

        expectResource(fId1, CONTAINER_TYPE);
        when(resourceFactory.getResource(fId2)).thenThrow(new PathNotFoundException("not found"));
        expectResource(fId3, RESOURCE_TYPE);

        accumulator.emitEvents(TX_ID, BASE_URL, USER_AGENT);

        verify(eventBus, times(3)).post(eventCaptor.capture());

        final var events = eventCaptor.getAllValues();

        assertThat(events, containsInAnyOrder(
                defaultEvent(fId1, Set.of(EventType.RESOURCE_CREATION),
                        Set.of(CONTAINER_TYPE.toString())),
                defaultEvent(fId2, Set.of(EventType.RESOURCE_DELETION),
                        Set.of()),
                defaultEvent(fId3, Set.of(EventType.RESOURCE_MODIFICATION),
                        Set.of(RESOURCE_TYPE.toString()))
        ));
    }

    private ResourceOperation createOp(final FedoraId fedoraId) {
        return new RdfSourceOperationFactoryImpl().createBuilder(fedoraId, RDF_SOURCE.toString())
                .userPrincipal(USER)
                .build();
    }

    private ResourceOperation updateOp(final FedoraId fedoraId) {
        return new RdfSourceOperationFactoryImpl().updateBuilder(fedoraId)
                .userPrincipal(USER)
                .build();
    }

    private ResourceOperation deleteOp(final FedoraId fedoraId) {
        return new DeleteResourceOperationFactoryImpl().deleteBuilder(fedoraId)
                .userPrincipal(USER)
                .build();
    }

    private void expectResource(final FedoraId fedoraId, final URI... types) throws PathNotFoundException {
        final var resource = mockResource(types);
        when(resourceFactory.getResource(fedoraId)).thenReturn(resource);
    }

    private FedoraResource mockResource(final URI... types) {
        final var resource = Mockito.mock(FedoraResource.class);
        when(resource.getTypes()).thenReturn(Arrays.asList(types));
        return resource;
    }

    private static URI uri(final String uri) {
        try {
            return new URI(uri);
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static Matcher<Event> defaultEvent(final FedoraId fedoraId,
                                               final Set<EventType> eventTypes,
                                               final Set<String> resourceTypes) {
        return event(fedoraId, eventTypes, resourceTypes, BASE_URL, USER_AGENT);
    }

    private static Matcher<Event> event(final FedoraId fedoraId,
                                        final Set<EventType> eventTypes,
                                        final Set<String> resourceTypes,
                                        final String baseUrl,
                                        final String userAgent) {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(final Event item) {
                if (! Objects.equals(item.getFedoraId(), fedoraId)) {
                    return false;
                } else if (! Objects.equals(item.getTypes(), eventTypes)) {
                    return false;
                } else if (! Objects.equals(item.getResourceTypes(), resourceTypes)) {
                    return false;
                } else if (! Objects.equals(item.getBaseUrl(), baseUrl)) {
                    return false;
                } else if (! Objects.equals(item.getUserAgent(), userAgent)) {
                    return false;
                }
                return true;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("fedoraId=").appendValue(fedoraId)
                        .appendText(", eventTypes=").appendValue(eventTypes)
                        .appendText(", resourceTyps=").appendValue(resourceTypes)
                        .appendText(", baseUrl=").appendValue(baseUrl)
                        .appendText(", userAgent=").appendValue(userAgent);
            }
        };
    }

}
