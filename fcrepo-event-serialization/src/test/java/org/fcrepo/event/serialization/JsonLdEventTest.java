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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.ofEpochMilli;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.ACTIVITY_STREAMS_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.PROV_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.observer.OptionalValues.BASE_URL;
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

import org.fcrepo.kernel.api.observer.EventType;
import org.fcrepo.kernel.api.observer.FedoraEvent;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>
 * JsonLdEventTest class.
 * </p>
 *
 * @author acoburn
 * @author dbernstein
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonLdEventTest {

    @Mock
    private FedoraEvent mockEvent;

    private String baseUrl = "http://localhost:8080/fcrepo/rest";

    private String path = "/path/to/resource";

    private String eventResourceId = "urn:uuid:some-event";

    private String username = "fedoraadmin";

    private Instant timestamp = ofEpochMilli(1465919304000L);

    private String userAgentBaseUri = "https://example.com/agents/";

    @Before
    public void setUp() {
        System.setProperty(JsonLDEventMessage.USER_AGENT_BASE_URI_PROPERTY, userAgentBaseUri);
        final Set<EventType> typeSet = new HashSet<>();
        typeSet.add(EventType.RESOURCE_MODIFICATION);
        final Set<String> resourceTypeSet = new HashSet<>();
        resourceTypeSet.add(REPOSITORY_NAMESPACE + "Resource");
        resourceTypeSet.add(REPOSITORY_NAMESPACE + "Container");
        resourceTypeSet.add("http://example.com/SampleType");
        final Map<String, String> auxInfo = new HashMap<>();
        auxInfo.put(BASE_URL, baseUrl);
        when(mockEvent.getTypes()).thenReturn(typeSet);
        when(mockEvent.getResourceTypes()).thenReturn(resourceTypeSet);
        when(mockEvent.getPath()).thenReturn(path);
        when(mockEvent.getUserID()).thenReturn(username);
        when(mockEvent.getDate()).thenReturn(timestamp);
        when(mockEvent.getEventID()).thenReturn(eventResourceId);
        when(mockEvent.getInfo()).thenReturn(auxInfo);
    }

    @Test
    public void testJsonSerializationAsModel() {
        final EventSerializer serializer = new JsonLDSerializer();
        final String json = serializer.serialize(mockEvent);
        final Model model = createDefaultModel();
        model.read(new ByteArrayInputStream(json.getBytes(UTF_8)), baseUrl + path, "JSON-LD");

        final Resource resourceSubject = createResource(baseUrl + path);
        final Resource eventSubject = createResource(eventResourceId);

        final Resource blankNode = null;

        assertTrue(model.contains(resourceSubject, type, createResource(REPOSITORY_NAMESPACE + "Resource")));
        assertTrue(model.contains(resourceSubject, type, createResource(REPOSITORY_NAMESPACE + "Container")));
        assertTrue(model.contains(resourceSubject, type, createResource(PROV_NAMESPACE + "Entity")));
        assertTrue(model.contains(resourceSubject, type, createResource("http://example.com/SampleType")));
        assertTrue(model.contains(eventSubject, type, createResource(EventType.RESOURCE_MODIFICATION.getType())));
        assertTrue(model.contains(eventSubject, createProperty(ACTIVITY_STREAMS_NAMESPACE + "actor"), createResource(
                getAgentIRI())));
        assertTrue(model.contains(eventSubject, createProperty(ACTIVITY_STREAMS_NAMESPACE + "object"),
                resourceSubject));

        final AtomicInteger name = new AtomicInteger();
        model.listStatements(new SimpleSelector(null, createProperty(ACTIVITY_STREAMS_NAMESPACE + "name"), blankNode))
                .forEachRemaining(statement -> {
                    assertEquals(EventType.RESOURCE_MODIFICATION.getName(), statement.getString());
                    assertEquals(eventSubject.toString(), statement.asTriple().getSubject().toString());
                    name.incrementAndGet();
                });
        assertEquals(name.get(), 1);
    }

    @Test
    public void testJsonSerializationAsJson() throws IOException {
        final EventSerializer serializer = new JsonLDSerializer();
        final String json = serializer.serialize(mockEvent);

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode node = mapper.readTree(json);
        assertTrue(node.has("@context"));
        assertTrue(node.has("id"));
        assertTrue(node.has("name"));
        assertTrue(node.has("type"));
        assertTrue(node.has("object"));
        assertTrue(node.has("actor"));

        assertEquals(eventResourceId, node.get("id").textValue());
        assertEquals(EventType.RESOURCE_MODIFICATION.getName(), node.get("name").textValue());
        assertEquals(EventType.RESOURCE_MODIFICATION.getType(), node.get("type").get(0).asText());
        assertEquals(getAgentIRI(), node.get("actor").asText());

        final List<String> types = new ArrayList<>();
        final JsonNode objectNode = node.get("object");
        assertEquals(baseUrl + path, objectNode.get("id").asText());
        objectNode.get("type").elements().forEachRemaining(n -> {
            types.add(n.textValue());
        });
        assertEquals(types.size(), 4);
        assertTrue(types.contains(REPOSITORY_NAMESPACE + "Resource"));
        assertTrue(types.contains(REPOSITORY_NAMESPACE + "Container"));
        assertTrue(types.contains(PROV_NAMESPACE + "Entity"));
        assertTrue(types.contains("http://example.com/SampleType"));
    }

    private String getAgentIRI() {
        return userAgentBaseUri + username;
    }
}
