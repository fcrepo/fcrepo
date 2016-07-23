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

import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDdateTime;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.apache.jena.vocabulary.DCTerms.identifier;
import static org.apache.jena.vocabulary.DCTerms.isPartOf;
import static org.apache.jena.vocabulary.RDF.type;
import static java.time.Instant.ofEpochMilli;
import static org.fcrepo.kernel.api.RdfLexicon.EVENT_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.PROV_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.observer.OptionalValues.BASE_URL;
import static org.fcrepo.kernel.api.observer.OptionalValues.USER_AGENT;
import static org.fcrepo.event.serialization.EventSerializer.toModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
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
public class FedoraEventTest {

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
    public void testModel() {
        final Model model = toModel(mockEvent);
        final Resource subject = createResource(baseUrl + path);
        final Resource blankNode = null;

        assertTrue(model.contains(subject, type, createResource(REPOSITORY_NAMESPACE + "Resource")));
        assertTrue(model.contains(subject, type, createResource(REPOSITORY_NAMESPACE + "Container")));
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
                assertTrue(r.hasProperty(createProperty(PROV_NAMESPACE + "atTime"),
                        createTypedLiteral(timestamp.toString(), XSDdateTime)));
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
    }

    @Test
    public void testTurtle() {
        final EventSerializer serializer = new TurtleSerializer();
        final String ttl = serializer.serialize(mockEvent);
        assertTrue(ttl.contains("<http://localhost:8080/fcrepo/rest/path/to/resource>"));
    }
}
