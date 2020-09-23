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

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.stream.Collectors.toList;

import static org.fcrepo.kernel.api.RdfLexicon.ACTIVITY_STREAMS_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.PROV_NAMESPACE;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.fcrepo.kernel.api.observer.Event;

import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A structure used for serializing a Event into JSON
 * 
 * @author acoburn
 * @author dbernstein
 * @author whikloj
 */
public class JsonLDEventMessage {

    @JsonIgnore
    private static final Logger LOGGER = getLogger(JsonLDEventMessage.class);

    public static class ContextElement {

        @JsonProperty("@id")
        public final String id;

        @JsonProperty("@type")
        public final String type;

        public ContextElement(final String id) {
            this.id = id;
            this.type = "@id";
        }

        public ContextElement(final String id, final String type) {
            this.id = id;
            this.type = type;
        }
    }

    public static class Context {

        public final String prov = "http://www.w3.org/ns/prov#";

        public final String dcterms = "http://purl.org/dc/terms/";

        public final String type = "@type";

        public final String id = "@id";

        public final ContextElement isPartOf = new ContextElement("dcterms:isPartOf");

    }

    public static class Object {

        @JsonProperty("type")
        public List<String> type;

        @JsonProperty("id")
        public String id;

        @JsonProperty("isPartOf")
        public String isPartOf;

        public Object() {
            // Needed to deserialize class.
        }

        public Object(final String id, final List<String> type, final String isPartOf) {
            this.type = type;
            this.id = id;
            this.isPartOf = isPartOf;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Application.class, name = "Application"),
            @JsonSubTypes.Type(value = Person.class, name = "Person") }
    )
    public static class Actor {
        @JsonIgnore
        public String type;

        public Actor(final String type) {
            this.type = type;
        }
    }

    public static class Application extends Actor {

        @JsonProperty("name")
        public String name;

        public Application() {
            super("Application");
        }

        public Application(final String name, final String type) {
            super(type);
            this.name = name;
        }
    }

    public static class Person extends Actor {

        @JsonProperty("id")
        public String id;

        public Person() {
            super("Person");
        }

        public Person(final String id, final String type) {
            super(type);
            this.id = id;
        }
    }

    @JsonProperty("id")
    public String id;

    @JsonProperty("type")
    public List<String> type;

    @JsonProperty("name")
    public String name;

    @JsonProperty("published")
    public Instant published;


    @JsonProperty("actor")
    public List<Actor> actor;

    @JsonProperty("object")
    public Object object;

    @JsonProperty("@context")
    public List<java.lang.Object> context;

    public void setPublished(final String published) {
        this.published = Instant.from(ISO_INSTANT.parse(published));
    }

    /**
     * Populate a JsonLDEventMessage from a Event
     * 
     * @param evt The Fedora event
     * @return a JsonLDEventMessage
     */
    public static JsonLDEventMessage from(final Event evt) {

        final String baseUrl = evt.getBaseUrl();

        // build objectId
        final String objectId = baseUrl + evt.getPath();
        // build event types list
        final List<String> types = evt.getTypes()
                .stream()
                .map(rdfType -> rdfType.getTypeAbbreviated())
                .collect(toList());
        // comma-separated list for names of events (since name requires string rather than array)
        final String name = String.join(", ", evt.getTypes()
                .stream()
                .map(rdfType -> rdfType.getName())
                .collect(toList()));
        // build resource types list
        final List<String> resourceTypes = new ArrayList<>(evt.getResourceTypes());
        if (!resourceTypes.contains(PROV_NAMESPACE + "Entity")) {
            resourceTypes.add(PROV_NAMESPACE + "Entity");
        }

        // build actors list
        final List<Actor> actor = new ArrayList<>();
        actor.add(new Person(Objects.toString(evt.getUserURI()), "Person"));
        final String softwareAgent = evt.getUserAgent();
        if (softwareAgent != null) {
            actor.add(new Application(softwareAgent, "Application"));
        }

        final JsonLDEventMessage msg = new JsonLDEventMessage();

        msg.id = evt.getEventID();
        msg.context = Arrays.asList(ACTIVITY_STREAMS_NAMESPACE, new Context());
        msg.actor = actor;
        msg.published = evt.getDate();
        msg.type = types;
        msg.name = name;
        msg.object = new Object(objectId, resourceTypes, baseUrl);
        return msg;
    }
}
