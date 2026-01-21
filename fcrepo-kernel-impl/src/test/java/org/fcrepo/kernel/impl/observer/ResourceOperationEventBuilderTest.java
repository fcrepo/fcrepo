/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.impl.observer;

import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.observer.Event;
import org.fcrepo.kernel.api.observer.EventType;
import org.fcrepo.kernel.impl.operations.DeleteResourceOperationFactoryImpl;
import org.fcrepo.kernel.impl.operations.NonRdfSourceOperationFactoryImpl;
import org.fcrepo.kernel.impl.operations.RdfSourceOperationFactoryImpl;
import org.fcrepo.kernel.impl.operations.VersionResourceOperationFactoryImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.util.Set;

import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * @author pwinckles
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ResourceOperationEventBuilderTest {

    private static final FedoraId FEDORA_ID = FedoraId.create("/test");
    private static final String USER = "user1";
    private static final String BASE_URL = "http://locahost/rest";

    @Mock
    private Transaction transaction;

    @BeforeEach
    public void setup() {
        when(transaction.getId()).thenReturn("tx-123");
    }

    @Test
    public void buildCreateEventFromCreateRdfOperation() {
        final var operation = new RdfSourceOperationFactoryImpl()
                .createBuilder(transaction, FEDORA_ID, RDF_SOURCE.toString(), ServerManagedPropsMode.RELAXED)
                .userPrincipal(USER)
                .build();

        final var event = ResourceOperationEventBuilder.fromResourceOperation(FEDORA_ID, operation, null)
                .withBaseUrl(BASE_URL)
                .build();

        assertDefaultEvent(event, EventType.RESOURCE_CREATION);
    }

    @Test
    public void buildCreateEventFromCreateNonRdfOperation() {
        final var fedoraId = FedoraId.create("/test/ab/c");
        final var user = "user2";
        final var operation = new NonRdfSourceOperationFactoryImpl()
                .createInternalBinaryBuilder(transaction, fedoraId, new ByteArrayInputStream(new byte[]{}))
                .userPrincipal(user)
                .build();

        final var event = ResourceOperationEventBuilder.fromResourceOperation(fedoraId, operation, null)
                .withBaseUrl(BASE_URL)
                .build();

        assertEquals(fedoraId, event.getFedoraId());
        assertEquals(fedoraId.getFullIdPath(), event.getPath());
        assertEquals(user, event.getUserID());
        assertThat(event.getTypes(), contains(EventType.RESOURCE_CREATION));
        assertNotNull(event.getEventID());
        assertNotNull(event.getDate());
    }

    @Test
    public void buildCreateEventFromVersionOperation() {
        final var operation = new VersionResourceOperationFactoryImpl().createBuilder(transaction, FEDORA_ID)
                .userPrincipal(USER)
                .build();

        final var event = ResourceOperationEventBuilder.fromResourceOperation(FEDORA_ID, operation, null)
                .withBaseUrl(BASE_URL)
                .build();

        assertDefaultEvent(event, EventType.RESOURCE_MODIFICATION);
    }

    @Test
    public void buildDeleteEventFromDeleteOperation() {
        final var operation = new DeleteResourceOperationFactoryImpl().deleteBuilder(transaction, FEDORA_ID)
                .userPrincipal(USER)
                .build();

        final var event = ResourceOperationEventBuilder.fromResourceOperation(FEDORA_ID, operation, null)
                .withBaseUrl(BASE_URL)
                .build();

        assertDefaultEvent(event, EventType.RESOURCE_DELETION);
    }

    @Test
    public void buildUpdateEventFromUpdateRdfOperation() {
        final var operation = new RdfSourceOperationFactoryImpl()
                .updateBuilder(transaction, FEDORA_ID, ServerManagedPropsMode.RELAXED)
                .userPrincipal(USER)
                .build();

        final var event = ResourceOperationEventBuilder.fromResourceOperation(FEDORA_ID, operation, null)
                .withBaseUrl(BASE_URL)
                .build();

        assertDefaultEvent(event, EventType.RESOURCE_MODIFICATION);
    }

    @Test
    public void buildUpdateEventFromUpdateNonRdfOperation() {
        final var operation = new NonRdfSourceOperationFactoryImpl()
                .updateInternalBinaryBuilder(transaction, FEDORA_ID, new ByteArrayInputStream(new byte[]{}))
                .userPrincipal(USER)
                .build();

        final var event = ResourceOperationEventBuilder.fromResourceOperation(FEDORA_ID, operation, null)
                .withBaseUrl(BASE_URL)
                .build();

        assertDefaultEvent(event, EventType.RESOURCE_MODIFICATION);
    }

    @Test
    public void mergeValidObjects() {
        final var createOperation = new RdfSourceOperationFactoryImpl()
                .createBuilder(transaction, FEDORA_ID, RDF_SOURCE.toString(), ServerManagedPropsMode.RELAXED)
                .userPrincipal(USER)
                .build();

        final var createEventBuilder = ResourceOperationEventBuilder
                .fromResourceOperation(FEDORA_ID, createOperation, null)
                .withBaseUrl(BASE_URL);

        final var updateOperation = new NonRdfSourceOperationFactoryImpl()
                .updateInternalBinaryBuilder(transaction, FEDORA_ID, new ByteArrayInputStream(new byte[]{}))
                .userPrincipal(USER)
                .build();

        final var updateEventBuilder = ResourceOperationEventBuilder
                .fromResourceOperation(FEDORA_ID, updateOperation, null)
                .withBaseUrl(BASE_URL);
        final var updateEvent = updateEventBuilder.build();

        final var merged = createEventBuilder.merge(updateEventBuilder).build();

        assertEquals(FEDORA_ID, merged.getFedoraId());
        assertEquals(FEDORA_ID.getFullIdPath(), merged.getPath());
        assertEquals(USER, merged.getUserID());
        assertThat(merged.getTypes(), containsInAnyOrder(EventType.RESOURCE_CREATION, EventType.RESOURCE_MODIFICATION));
        assertEquals(updateEvent.getDate(), merged.getDate());
    }

    @Test
    public void populateOtherEventFields() {
        final var operation = new NonRdfSourceOperationFactoryImpl()
                .updateInternalBinaryBuilder(transaction, FEDORA_ID, new ByteArrayInputStream(new byte[]{}))
                .userPrincipal(USER)
                .build();

        final var baseUrl = "http://localhost/rest";
        final var userAgent = "user-agent";
        final var resourceTypes = Set.of("resource-type");

        final var event = ResourceOperationEventBuilder.fromResourceOperation(FEDORA_ID, operation, null)
                .withBaseUrl(baseUrl)
                .withUserAgent(userAgent)
                .withResourceTypes(resourceTypes)
                .build();

        assertEquals(baseUrl, event.getBaseUrl());
        assertEquals(userAgent, event.getUserAgent());
        assertEquals(resourceTypes, event.getResourceTypes());
    }

    @Test
    public void populateOtherEventFieldsWithNullUserAgent() {
        final var operation = new NonRdfSourceOperationFactoryImpl()
                .updateInternalBinaryBuilder(transaction, FEDORA_ID, new ByteArrayInputStream(new byte[]{}))
                .userPrincipal(USER)
                .build();

        final var baseUrl = "http://localhost/rest";
        final String userAgent = null;
        final var resourceTypes = Set.of("resource-type");

        final var event = ResourceOperationEventBuilder.fromResourceOperation(FEDORA_ID, operation, null)
                .withBaseUrl(baseUrl)
                .withUserAgent(userAgent)
                .withResourceTypes(resourceTypes)
                .build();

        assertEquals(baseUrl, event.getBaseUrl());
        assertEquals(userAgent, event.getUserAgent());
        assertEquals(resourceTypes, event.getResourceTypes());
    }


    private void assertDefaultEvent(final Event event, final EventType type) {
        assertEquals(FEDORA_ID, event.getFedoraId());
        assertEquals(FEDORA_ID.getFullIdPath(), event.getPath());
        assertEquals(USER, event.getUserID());
        assertThat(event.getTypes(), contains(type));
        assertNotNull(event.getEventID());
        assertNotNull(event.getDate());
    }

}
