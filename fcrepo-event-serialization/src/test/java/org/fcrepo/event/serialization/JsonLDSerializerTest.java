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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.rdf.model.Model;
import org.fcrepo.kernel.api.observer.EventType;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.PROV_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * <p>
 * JsonLDSerializerTest class.
 * </p>
 *
 * @author acoburn
 * @author dbernstein
 */
public class JsonLDSerializerTest extends EventSerializerTestBase {

    @Test
    public void testJsonSerializationAsModel() {
        mockEvent(path);
        final EventSerializer serializer = new JsonLDSerializer();
        final String json = serializer.serialize(mockEvent);
        final Model model = createDefaultModel();
        model.read(new ByteArrayInputStream(json.getBytes(UTF_8)), baseUrl + path, "JSON-LD");
        testModel(model);
    }

    @Test
    public void testJsonSerializationAsJson() throws IOException {
        mockEvent(path);
        testJsonSerializationAsJson(path, getAgentURI());
    }

    @Test
    public void testJsonSerializationAsJsonWithNullUser() throws IOException {
        mockEventNullUser(path);
        testJsonSerializationAsJson(path, "null");
    }

    private void testJsonSerializationAsJson(final String outputPath, final String user) throws IOException {
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
        assertTrue(node.has("published"));

        assertEquals(eventResourceId, node.get("id").textValue());
        assertEquals(EventType.RESOURCE_MODIFICATION.getName(), node.get("name").textValue());
        assertEquals(EventType.RESOURCE_MODIFICATION.getTypeAbbreviated(), node.get("type").get(0).asText());
        assertEquals(user, node.get("actor").get(0).get("id").asText());
        assertEquals("Person", node.get("actor").get(0).get("type").asText());
        assertEquals(softwareAgent, node.get("actor").get(1).get("name").asText());
        assertEquals("Application", node.get("actor").get(1).get("type").asText());
        assertEquals(node.get("published").textValue(), timestamp.toString());
        final List<String> types = new ArrayList<>();
        final JsonNode objectNode = node.get("object");
        assertEquals(baseUrl + outputPath, objectNode.get("id").asText());
        assertEquals(objectNode.get("isPartOf").textValue(), baseUrl);
        objectNode.get("type").elements().forEachRemaining(n -> {
            types.add(n.textValue());
        });
        assertEquals(types.size(), 4);
        assertTrue(types.contains(FEDORA_RESOURCE.getURI()));
        assertTrue(types.contains(FEDORA_CONTAINER.getURI()));
        assertTrue(types.contains(PROV_NAMESPACE + "Entity"));
        assertTrue(types.contains("http://example.com/SampleType"));
    }

}
