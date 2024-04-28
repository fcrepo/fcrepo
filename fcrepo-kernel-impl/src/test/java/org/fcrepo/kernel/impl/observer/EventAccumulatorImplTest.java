/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.impl.observer;

import com.google.common.eventbus.EventBus;

import org.fcrepo.config.AuthPropsConfig;
import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.Transaction;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private Transaction transaction;

    private ArgumentCaptor<Event> eventCaptor;

    private AuthPropsConfig authPropsConfig;

    @Before
    public void setup() {
        authPropsConfig = new AuthPropsConfig();
        accumulator = new EventAccumulatorImpl();
        transaction = mockTransaction(TX_ID);
        setField(accumulator, "resourceFactory", resourceFactory);
        setField(accumulator, "eventBus", eventBus);
        setField(accumulator, "authPropsConfig", authPropsConfig);
        eventCaptor = ArgumentCaptor.forClass(Event.class);
    }

    @Test
    public void emitEventsWhenEventsOnTransactionNoMerge() throws PathNotFoundException {
        final var fId1 = FedoraId.create("/test/1");
        final var fId2 = FedoraId.create("/test/2");

        final var op1 = createOp(fId1);
        final var op2 = updateOp(fId2);

        accumulator.recordEventForOperation(transaction, fId1, op1);
        accumulator.recordEventForOperation(transaction, fId2, op2);

        expectResource(fId1, CONTAINER_TYPE);
        expectResource(fId2, CONTAINER_TYPE, RESOURCE_TYPE);

        accumulator.emitEvents(transaction, BASE_URL, USER_AGENT);

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

        accumulator.recordEventForOperation(transaction, fId1, op1);
        accumulator.recordEventForOperation(transaction, fId2, op2);
        accumulator.recordEventForOperation(transaction, fId1, op3);

        expectResource(fId1, CONTAINER_TYPE, RDF_TYPE);
        expectResource(fId2, CONTAINER_TYPE, RESOURCE_TYPE);

        accumulator.emitEvents(transaction, BASE_URL, USER_AGENT);

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

        final Transaction transaction2 = mockTransaction("tx2");

        accumulator.recordEventForOperation(transaction, fId1, op1);
        accumulator.recordEventForOperation(transaction2, fId2, op2);

        expectResource(fId2, CONTAINER_TYPE);

        accumulator.emitEvents(transaction2, BASE_URL, USER_AGENT);

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

        final Transaction transaction2 = mockTransaction("tx2");
        final Transaction transaction3 = mockTransaction("tx3");

        accumulator.recordEventForOperation(transaction, fId1, op1);
        accumulator.recordEventForOperation(transaction2, fId2, op2);

        expectResource(fId2, CONTAINER_TYPE);

        accumulator.emitEvents(transaction3, BASE_URL, USER_AGENT);

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

        accumulator.recordEventForOperation(transaction, fId1, op1);
        accumulator.recordEventForOperation(transaction, fId2, op2);
        accumulator.recordEventForOperation(transaction, fId1, op3);

        expectResource(fId1, CONTAINER_TYPE, RDF_TYPE);
        expectResource(fId2, CONTAINER_TYPE, RESOURCE_TYPE);

        accumulator.clearEvents(transaction);

        accumulator.emitEvents(transaction, BASE_URL, USER_AGENT);

        verify(eventBus, times(0)).post(eventCaptor.capture());

        final var events = eventCaptor.getAllValues();
        assertEquals(0, events.size());
    }

    @Test
    public void nonDefaultValues() throws PathNotFoundException {
        final var tx = "tx4";
        final Transaction transaction4 = mockTransaction(tx);
        final var url = "http://example.com/rest";
        final var agent = "me";

        final var fId1 = FedoraId.create("/example/1");
        final var fId2 = FedoraId.create("/example/2");

        final var op1 = createOp(fId1);
        final var op2 = updateOp(fId2);
        final var op3 = deleteOp(fId1);

        accumulator.recordEventForOperation(transaction4, fId1, op1);
        accumulator.recordEventForOperation(transaction4, fId2, op2);
        accumulator.recordEventForOperation(transaction4, fId1, op3);

        expectResource(fId1, RDF_TYPE);
        expectResource(fId2, RESOURCE_TYPE);

        accumulator.emitEvents(transaction4, url, agent);

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

        accumulator.recordEventForOperation(transaction, fId1, op1);
        accumulator.recordEventForOperation(transaction, fId2, op2);
        accumulator.recordEventForOperation(transaction, fId3, op3);

        expectResource(fId1, CONTAINER_TYPE);
        when(resourceFactory.getResource(any(Transaction.class), eq(fId2))).thenThrow(new PathNotFoundException("not " +
                "found"));
        expectResource(fId3, RESOURCE_TYPE);

        accumulator.emitEvents(transaction, BASE_URL, USER_AGENT);

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

    @Test
    public void testUserAgentWithSpace() throws PathNotFoundException{
        final var fId1 = FedoraId.create("/test/1");
        final var fId2 = FedoraId.create("/test/2");
        final var op1 = createOp(fId1, "user name");
        final var op2 = updateOp(fId2, "user name");

        accumulator.recordEventForOperation(transaction, fId1, op1);
        accumulator.recordEventForOperation(transaction, fId2, op2);

        expectResource(fId1, CONTAINER_TYPE);
        expectResource(fId2, RESOURCE_TYPE);

        accumulator.emitEvents(transaction, BASE_URL, USER_AGENT);

        verify(eventBus, times(2)).post(eventCaptor.capture());

        final var events = eventCaptor.getAllValues();

        assertThat(events, containsInAnyOrder(
                defaultEvent(fId1, Set.of(EventType.RESOURCE_CREATION),
                        Set.of(CONTAINER_TYPE.toString())),
                defaultEvent(fId2, Set.of(EventType.RESOURCE_MODIFICATION),
                        Set.of(RESOURCE_TYPE.toString()))
        ));
    }

    private ResourceOperation createOp(final FedoraId fedoraId) {
        return createOp(fedoraId, null);
    }

    private ResourceOperation createOp(final FedoraId fedoraId, final String user) {
        final var agent = user == null ? USER : user;
        return new RdfSourceOperationFactoryImpl().createBuilder(transaction, fedoraId, RDF_SOURCE.toString(),
                ServerManagedPropsMode.RELAXED)
                .userPrincipal(agent)
                .build();
    }

    private ResourceOperation updateOp(final FedoraId fedoraId) {
        return updateOp(fedoraId, null);
    }

    private ResourceOperation updateOp(final FedoraId fedoraId, final String user) {
        final var agent = user == null ? USER : user;
        return new RdfSourceOperationFactoryImpl().updateBuilder(transaction, fedoraId, ServerManagedPropsMode.RELAXED)
                .userPrincipal(agent)
                .build();
    }

    private ResourceOperation deleteOp(final FedoraId fedoraId) {
        return new DeleteResourceOperationFactoryImpl().deleteBuilder(transaction, fedoraId)
                .userPrincipal(USER)
                .build();
    }

    private void expectResource(final FedoraId fedoraId, final URI... types) throws PathNotFoundException {
        final var resource = mockResource(types);
        when(resourceFactory.getResource(any(Transaction.class), eq(fedoraId))).thenReturn(resource);
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

    /**
     * Create a mock transaction.
     * @param transactionId the id of the transaction
     * @return the mock transaction.
     */
    private static Transaction mockTransaction(final String transactionId) {
        final var transaction = Mockito.mock(Transaction.class);
        when(transaction.getId()).thenReturn(transactionId);
        return transaction;
    }
}
