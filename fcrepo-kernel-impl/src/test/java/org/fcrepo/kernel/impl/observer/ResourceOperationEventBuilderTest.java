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

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.observer.Event;
import org.fcrepo.kernel.api.observer.EventType;
import org.fcrepo.kernel.impl.operations.DeleteResourceOperationFactoryImpl;
import org.fcrepo.kernel.impl.operations.NonRdfSourceOperationFactoryImpl;
import org.fcrepo.kernel.impl.operations.RdfSourceOperationFactoryImpl;
import org.fcrepo.kernel.impl.operations.VersionResourceOperationFactoryImpl;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Set;

import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author pwinckles
 */
public class ResourceOperationEventBuilderTest {

    private static final FedoraId FEDORA_ID = FedoraId.create("/test");
    private static final String USER = "user1";
    private static final String BASE_URL = "http://locahost/rest";

    @Test
    public void buildCreateEventFromCreateRdfOperation() {
        final var operation = new RdfSourceOperationFactoryImpl()
                .createBuilder(FEDORA_ID, RDF_SOURCE.toString())
                .userPrincipal(USER)
                .build();

        final var event = ResourceOperationEventBuilder.fromResourceOperation(FEDORA_ID, operation)
                .withBaseUrl(BASE_URL)
                .build();

        assertDefaultEvent(event, EventType.RESOURCE_CREATION);
    }

    @Test
    public void buildCreateEventFromCreateNonRdfOperation() {
        final var fedoraId = FedoraId.create("/test/ab/c");
        final var user = "user2";
        final var operation = new NonRdfSourceOperationFactoryImpl()
                .createInternalBinaryBuilder(fedoraId, new ByteArrayInputStream(new byte[]{}))
                .userPrincipal(user)
                .build();

        final var event = ResourceOperationEventBuilder.fromResourceOperation(fedoraId, operation)
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
        final var operation = new VersionResourceOperationFactoryImpl().createBuilder(FEDORA_ID)
                .userPrincipal(USER)
                .build();

        final var event = ResourceOperationEventBuilder.fromResourceOperation(FEDORA_ID, operation)
                .withBaseUrl(BASE_URL)
                .build();

        assertDefaultEvent(event, EventType.RESOURCE_MODIFICATION);
    }

    @Test
    public void buildDeleteEventFromDeleteOperation() {
        final var operation = new DeleteResourceOperationFactoryImpl().deleteBuilder(FEDORA_ID)
                .userPrincipal(USER)
                .build();

        final var event = ResourceOperationEventBuilder.fromResourceOperation(FEDORA_ID, operation)
                .withBaseUrl(BASE_URL)
                .build();

        assertDefaultEvent(event, EventType.RESOURCE_DELETION);
    }

    @Test
    public void buildUpdateEventFromUpdateRdfOperation() {
        final var operation = new RdfSourceOperationFactoryImpl().updateBuilder(FEDORA_ID)
                .userPrincipal(USER)
                .build();

        final var event = ResourceOperationEventBuilder.fromResourceOperation(FEDORA_ID, operation)
                .withBaseUrl(BASE_URL)
                .build();

        assertDefaultEvent(event, EventType.RESOURCE_MODIFICATION);
    }

    @Test
    public void buildUpdateEventFromUpdateNonRdfOperation() {
        final var operation = new NonRdfSourceOperationFactoryImpl()
                .updateInternalBinaryBuilder(FEDORA_ID, new ByteArrayInputStream(new byte[]{}))
                .userPrincipal(USER)
                .build();

        final var event = ResourceOperationEventBuilder.fromResourceOperation(FEDORA_ID, operation)
                .withBaseUrl(BASE_URL)
                .build();

        assertDefaultEvent(event, EventType.RESOURCE_MODIFICATION);
    }

    @Test
    public void mergeValidObjects() {
        final var createOperation = new RdfSourceOperationFactoryImpl()
                .createBuilder(FEDORA_ID, RDF_SOURCE.toString())
                .userPrincipal(USER)
                .build();

        final var createEventBuilder = ResourceOperationEventBuilder.fromResourceOperation(FEDORA_ID, createOperation)
                .withBaseUrl(BASE_URL);

        final var updateOperation = new NonRdfSourceOperationFactoryImpl()
                .updateInternalBinaryBuilder(FEDORA_ID, new ByteArrayInputStream(new byte[]{}))
                .userPrincipal(USER)
                .build();

        final var updateEventBuilder = ResourceOperationEventBuilder.fromResourceOperation(FEDORA_ID, updateOperation)
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
                .updateInternalBinaryBuilder(FEDORA_ID, new ByteArrayInputStream(new byte[]{}))
                .userPrincipal(USER)
                .build();

        final var baseUrl = "http://localhost/rest";
        final var userAgent = "user-agent";
        final var resourceTypes = Set.of("resource-type");

        final var event = ResourceOperationEventBuilder.fromResourceOperation(FEDORA_ID, operation)
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
