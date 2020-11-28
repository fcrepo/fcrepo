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

package org.fcrepo.event.serialization;

import static java.time.Instant.ofEpochMilli;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.ACTIVITY_STREAMS_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.PROV_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.fcrepo.kernel.api.observer.Event;
import org.fcrepo.kernel.api.observer.EventType;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * <p>
 * JsonLdEventTest class.
 * </p>
 *
 * @author acoburn
 * @author dbernstein
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class EventSerializerTestBase {

    @Mock
    protected Event mockEvent;

    protected final String baseUrl = "http://localhost:8080/fcrepo/rest";

    protected final String path = "/path/to/resource";

    protected final String eventResourceId = "urn:uuid:some-event";

    private final String username = "fedoraadmin";

    protected final Instant timestamp = ofEpochMilli(1465919304000L);

    private final String userAgentBaseUri = "https://example.com/agents/";

    protected final String softwareAgent = "fcrepo-java-client";

    private void mockEventCommon(final String path) {
        final Set<EventType> typeSet = new HashSet<>();
        typeSet.add(EventType.RESOURCE_MODIFICATION);
        final Set<String> resourceTypeSet = new HashSet<>();
        resourceTypeSet.add(FEDORA_RESOURCE.getURI());
        resourceTypeSet.add(FEDORA_CONTAINER.getURI());
        resourceTypeSet.add("http://example.com/SampleType");

        when(mockEvent.getTypes()).thenReturn(typeSet);
        when(mockEvent.getResourceTypes()).thenReturn(resourceTypeSet);
        when(mockEvent.getPath()).thenReturn(path);
        when(mockEvent.getUserID()).thenReturn(username);
        when(mockEvent.getUserURI()).thenReturn(URI.create(getAgentURI()));

        when(mockEvent.getDate()).thenReturn(timestamp);
        when(mockEvent.getEventID()).thenReturn(eventResourceId);
        when(mockEvent.getUserAgent()).thenReturn(softwareAgent);
        when(mockEvent.getBaseUrl()).thenReturn(baseUrl);
    }

    protected void mockEvent(final String path) {
        mockEventCommon(path);
        when(mockEvent.getUserID()).thenReturn(username);
        when(mockEvent.getUserURI()).thenReturn(URI.create(getAgentURI()));
    }

    protected void mockEventNullUser(final String path) {
        mockEventCommon(path);
        when(mockEvent.getUserID()).thenReturn(null);
        when(mockEvent.getUserURI()).thenReturn(null);
    }

    protected void testModel(final Model model) {
        final Resource resourceSubject = createResource(baseUrl + path);
        final Resource eventSubject = createResource(eventResourceId);

        final Resource blankNode = null;

        assertTrue(model.contains(resourceSubject, type, FEDORA_RESOURCE));
        assertTrue(model.contains(resourceSubject, type, FEDORA_CONTAINER));
        assertTrue(model.contains(resourceSubject, type, createResource(PROV_NAMESPACE + "Entity")));
        assertTrue(model.contains(resourceSubject, type, createResource("http://example.com/SampleType")));
        assertTrue(model.contains(eventSubject, type, createResource(EventType.RESOURCE_MODIFICATION.getType())));
        assertTrue(model.contains(eventSubject, createProperty(ACTIVITY_STREAMS_NAMESPACE + "object"),
                resourceSubject));

        assertTrue(model.contains(eventSubject, createProperty(ACTIVITY_STREAMS_NAMESPACE + "published"),
                    (RDFNode) null));

        final AtomicInteger actors = new AtomicInteger();
        model.listStatements(new SimpleSelector(eventSubject, createProperty(ACTIVITY_STREAMS_NAMESPACE + "actor"),
                blankNode))
                .forEachRemaining(statement -> {
                    final Resource r = statement.getResource();
                    if (r.hasProperty(type, createResource(ACTIVITY_STREAMS_NAMESPACE + "Person"))) {
                        assertTrue(r.hasProperty(type, createResource(ACTIVITY_STREAMS_NAMESPACE + "Person")));
                        assertEquals(getAgentURI(), r.toString());
                    } else {
                        assertTrue(r.hasProperty(type, createResource(ACTIVITY_STREAMS_NAMESPACE + "Application")));
                        assertTrue(r.hasProperty(createProperty(ACTIVITY_STREAMS_NAMESPACE + "name"), softwareAgent));
                    }
                    actors.incrementAndGet();
                });
        assertEquals(actors.get(), 2);

        final AtomicInteger eventName = new AtomicInteger();
        model.listStatements(new SimpleSelector(eventSubject, createProperty(ACTIVITY_STREAMS_NAMESPACE + "name"),
                blankNode))
                .forEachRemaining(statement -> {
                    assertEquals(EventType.RESOURCE_MODIFICATION.getName(), statement.getString());
                    eventName.incrementAndGet();
                });
        assertEquals(1, eventName.get());
    }

    protected String getAgentURI() {
        return userAgentBaseUri + username;
    }
}
