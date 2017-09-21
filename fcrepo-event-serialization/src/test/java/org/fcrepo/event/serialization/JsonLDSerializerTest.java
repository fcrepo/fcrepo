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
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.api.RdfLexicon.PROV_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.fcrepo.kernel.api.observer.EventType;

import org.apache.jena.rdf.model.Model;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>
 * JsonLDSerializerTest class.
 * </p>
 *
 * @author acoburn
 * @author dbernstein
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonLDSerializerTest extends FedoraEventSerializerTestBase {

    @Test
    public void testJsonSerializationAsModel() {
        final EventSerializer serializer = new JsonLDSerializer();
        final String json = serializer.serialize(mockEvent);
        final Model model = createDefaultModel();
        model.read(new ByteArrayInputStream(json.getBytes(UTF_8)), baseUrl + path, "JSON-LD");
        testModel(model);
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
        assertTrue(node.has("published"));

        assertEquals(eventResourceId, node.get("id").textValue());
        assertEquals(EventType.RESOURCE_MODIFICATION.getName(), node.get("name").textValue());
        assertEquals(EventType.RESOURCE_MODIFICATION.getTypeAbbreviated(), node.get("type").get(0).asText());
        assertEquals(getAgentIRI(), node.get("actor").get(0).get("id").asText());
        assertEquals("Person", node.get("actor").get(0).get("type").get(0).asText());
        assertEquals(softwareAgent, node.get("actor").get(1).get("name").asText());
        assertEquals("Application", node.get("actor").get(1).get("type").get(0).asText());
        assertEquals(node.get("published").textValue(), timestamp.toString());
        final List<String> types = new ArrayList<>();
        final JsonNode objectNode = node.get("object");
        assertEquals(baseUrl + path, objectNode.get("id").asText());
        assertEquals(objectNode.get("isPartOf").textValue(), baseUrl);
        objectNode.get("type").elements().forEachRemaining(n -> {
            types.add(n.textValue());
        });
        assertEquals(types.size(), 4);
        assertTrue(types.contains(REPOSITORY_NAMESPACE + "Resource"));
        assertTrue(types.contains(REPOSITORY_NAMESPACE + "Container"));
        assertTrue(types.contains(PROV_NAMESPACE + "Entity"));
        assertTrue(types.contains("http://example.com/SampleType"));
    }

}
