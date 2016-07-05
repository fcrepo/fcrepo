/*
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
package org.fcrepo.event.serialization;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.vocabulary.DCTerms.identifier;
import static com.hp.hpl.jena.vocabulary.DCTerms.isPartOf;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.ofEpochMilli;
import static org.fcrepo.kernel.api.RdfLexicon.EVENT_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.PROV_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.observer.OptionalValues.BASE_URL;
import static org.fcrepo.kernel.api.observer.OptionalValues.USER_AGENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import org.fcrepo.kernel.api.observer.EventType;
import org.fcrepo.kernel.api.observer.FedoraEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * <p>FedoraEventTest class.</p>
 *
 * @author acoburn
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonLdEventTest {

    private static String FOAF_NAMESPACE = "http://xmlns.com/foaf/0.1/";

    @Mock
    private FedoraEvent mockEvent;

    private String baseUrl = "http://localhost:8080/fcrepo/rest";

    private String path = "/path/to/resource";

    private Instant timestamp = ofEpochMilli(1465919304000L);

    @Before
    public void setUp() {
        final Set<EventType> typeSet = new HashSet<>();
        typeSet.add(EventType.RESOURCE_MODIFICATION);
        final Set<String> resourceTypeSet = new HashSet<>();
        resourceTypeSet.add(REPOSITORY_NAMESPACE + "Resource");
        resourceTypeSet.add(REPOSITORY_NAMESPACE + "Container");
        resourceTypeSet.add("http://example.com/SampleType");
        final Map<String, String> auxInfo = new HashMap<>();
        auxInfo.put(BASE_URL, baseUrl);
        auxInfo.put(USER_AGENT, "fcrepo-client/1.0");
        when(mockEvent.getTypes()).thenReturn(typeSet);
        when(mockEvent.getResourceTypes()).thenReturn(resourceTypeSet);
        when(mockEvent.getPath()).thenReturn(path);
        when(mockEvent.getUserID()).thenReturn("fedo raadmin");
        when(mockEvent.getDate()).thenReturn(timestamp);
        when(mockEvent.getEventID()).thenReturn("urn:uuid:some-event");
        when(mockEvent.getInfo()).thenReturn(auxInfo);
    }

    @Test
    public void testJsonSerializationAsModel() {
        final EventSerializer serializer = new JsonLDSerializer();
        final String json = serializer.serialize(mockEvent);
        final Model model = createDefaultModel();
        model.read(new ByteArrayInputStream(json.getBytes(UTF_8)), baseUrl + path, "JSON-LD");

        final Resource subject = createResource(baseUrl + path);
        final Resource blankNode = null;

        assertTrue(model.contains(subject, type, createResource(REPOSITORY_NAMESPACE + "Resource")));
        assertTrue(model.contains(subject, type, createResource(REPOSITORY_NAMESPACE + "Container")));
        assertTrue(model.contains(subject, type, createResource(PROV_NAMESPACE + "Entity")));
        assertTrue(model.contains(subject, type, createResource("http://example.com/SampleType")));
        assertTrue(model.contains(subject, isPartOf, createResource(baseUrl)));
        assertTrue(model.contains(subject, createProperty(PROV_NAMESPACE + "wasGeneratedBy")));
        assertTrue(model.contains(subject, createProperty(PROV_NAMESPACE + "wasAttributedTo")));

        final AtomicInteger activities = new AtomicInteger();
        model.listStatements(new SimpleSelector(subject, createProperty(PROV_NAMESPACE + "wasGeneratedBy"), blankNode))
            .forEachRemaining(statement -> {
                final Resource r = statement.getResource();
                assertTrue(r.hasProperty(type, createResource(EVENT_NAMESPACE + "ResourceModification")));
                assertTrue(r.hasProperty(type, createResource(PROV_NAMESPACE + "Activity")));
                assertTrue(r.hasProperty(identifier, createResource("urn:uuid:some-event")));
                activities.incrementAndGet();
            });
        assertEquals(activities.get(), 1);

        final AtomicInteger agents = new AtomicInteger();
        model.listStatements(new SimpleSelector(subject, createProperty(PROV_NAMESPACE + "wasAttributedTo"), blankNode))
            .forEachRemaining(statement -> {
                final Resource r = statement.getResource();
                if (r.hasProperty(type, createResource(PROV_NAMESPACE + "Person"))) {
                    assertTrue(r.hasProperty(type, createResource(PROV_NAMESPACE + "Person")));
                    assertTrue(r.hasProperty(createProperty(FOAF_NAMESPACE + "name"), "fedo raadmin"));
                } else {
                    assertTrue(r.hasProperty(type, createResource(PROV_NAMESPACE + "SoftwareAgent")));
                    assertTrue(r.hasProperty(createProperty(FOAF_NAMESPACE + "name"), "fcrepo-client/1.0"));
                }
                agents.incrementAndGet();
            });
        assertEquals(agents.get(), 2);
        assertEquals(1, 1);
    }

    @Test
    public void testJsonSerializationAsJson() throws IOException {
        final EventSerializer serializer = new JsonLDSerializer();
        final String json = serializer.serialize(mockEvent);

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode node = mapper.readTree(json);
        assertTrue(node.has("@context"));
        assertTrue(node.has("id"));
        assertEquals(node.get("id").textValue(), baseUrl + path);
        final List<String> types = new ArrayList<>();
        node.get("type").elements().forEachRemaining(n -> {
            types.add(n.textValue());
        });
        assertEquals(types.size(), 4);
        assertTrue(types.contains(REPOSITORY_NAMESPACE + "Resource"));
        assertTrue(types.contains(REPOSITORY_NAMESPACE + "Container"));
        assertTrue(types.contains(PROV_NAMESPACE + "Entity"));
        assertTrue(types.contains("http://example.com/SampleType"));
        assertTrue(node.has("isPartOf"));
        assertEquals(node.get("isPartOf").textValue(), baseUrl);

        // verify prov:Activity node
        assertTrue(node.has("wasGeneratedBy"));
        final JsonNode activity = node.get("wasGeneratedBy");
        final List<String> activityTypes = new ArrayList<>();
        assertTrue(activity.has("type"));
        activity.get("type").elements().forEachRemaining(n -> {
            activityTypes.add(n.textValue());
        });
        assertEquals(activityTypes.size(), 2);
        assertTrue(activityTypes.contains(EVENT_NAMESPACE + "ResourceModification"));
        assertTrue(activityTypes.contains(PROV_NAMESPACE + "Activity"));
        assertTrue(activity.has("atTime"));
        assertTrue(activity.has("identifier"));
        assertEquals(activity.get("atTime").textValue(), timestamp.toString());
        assertEquals(activity.get("identifier").textValue(), "urn:uuid:some-event");

        // verify prov:Agent node
        assertTrue(node.has("wasAttributedTo"));
        final AtomicInteger agents = new AtomicInteger();
        node.get("wasAttributedTo").elements().forEachRemaining(n -> {
            assertTrue(n.has("type"));
            assertTrue(n.has("name"));
            if (n.get("type").textValue().equals(PROV_NAMESPACE + "Person")) {
                assertEquals(n.get("type").textValue(), PROV_NAMESPACE + "Person");
                assertEquals(n.get("name").textValue(), "fedo raadmin");
            } else {
                assertEquals(n.get("type").textValue(), PROV_NAMESPACE + "SoftwareAgent");
                assertEquals(n.get("name").textValue(), "fcrepo-client/1.0");
            }
            agents.incrementAndGet();
        });
        assertEquals(agents.get(), 2);
    }
}
