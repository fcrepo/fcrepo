/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.event.serialization;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static java.time.Instant.ofEpochMilli;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.ACTIVITY_STREAMS_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.PROV_NAMESPACE;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.observer.Event;
import org.fcrepo.kernel.api.observer.EventType;
import org.mockito.Mock;

/**
 * <p>
 * JsonLdEventTest class.
 * </p>
 *
 * @author acoburn
 * @author dbernstein
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
        model.listStatements(eventSubject, createProperty(ACTIVITY_STREAMS_NAMESPACE + "actor"),
                        (RDFNode) null)
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
        assertEquals(2, actors.get());

        final AtomicInteger eventName = new AtomicInteger();
        model.listStatements(eventSubject, createProperty(ACTIVITY_STREAMS_NAMESPACE + "name"),
                        (RDFNode) null)
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
