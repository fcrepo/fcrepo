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

import static java.util.stream.Collectors.toList;
import static org.fcrepo.kernel.api.RdfLexicon.PROV_NAMESPACE;
import static org.fcrepo.kernel.api.observer.OptionalValues.BASE_URL;
import static org.fcrepo.kernel.api.observer.OptionalValues.USER_AGENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fcrepo.kernel.api.observer.FedoraEvent;

import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A structure used for serializing a FedoraEvent into JSON
 * 
 * @author acoburn
 * @author dbernstein
 */
class JsonLDEventMessage {

    @JsonIgnore
    private static final Logger LOGGER = getLogger(JsonLDEventMessage.class);

    public static final String ACTIVITYSTREAMS_NS = "https://www.w3.org/ns/activitystreams";

    static class ContextElement {

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

    static class Context {

        public final String prov = "http://www.w3.org/ns/prov#";

        public final String dcterms = "http://purl.org/dc/terms/";

        public final String type = "@type";

        public final String id = "@id";

        public final ContextElement isPartOf = new ContextElement("dcterms:isPartOf");

    }

    static class Object {

        @JsonProperty("type")
        public List<String> type;

        @JsonProperty("id")
        public String id;

        @JsonProperty("isPartOf")
        public String isPartOf;

        public Object(final String id, final List<String> type, final String isPartOf) {
            this.type = type;
            this.id = id;
            this.isPartOf = isPartOf;
        }
    }

    static class Actor {

        @JsonProperty("type")
        public List<String> type;

        public Actor(final List<String> type) {
            this.type = type;
        }
    }

    static class Application extends Actor {

        @JsonProperty("name")
        public String name;

        public Application(final String name, final List<String> type) {
            super(type);
            this.name = name;
        }
    }

    static class Person extends Actor {

        @JsonProperty("id")
        public String id;

        public Person(final String id, final List<String> type) {
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

    /**
     * Populate a JsonLDEventMessage from a FedoraEvent
     * 
     * @param evt The Fedora event
     * @return a JsonLDEventMessage
     */
    public static JsonLDEventMessage from(final FedoraEvent evt) {

        final String baseUrl = evt.getInfo().get(BASE_URL);

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
        final List<Actor> actor = new ArrayList();
        actor.add(new Person(evt.getUserURI().toString(), Arrays.asList("Person")));
        final String softwareAgent = evt.getInfo().get(USER_AGENT);
        if (softwareAgent != null) {
            actor.add(new Application(softwareAgent, Arrays.asList("Application")));
        }

        final JsonLDEventMessage msg = new JsonLDEventMessage();

        msg.id = evt.getEventID();
        msg.context = Arrays.asList(ACTIVITYSTREAMS_NS, new Context());
        msg.actor = actor;
        msg.published = evt.getDate();
        msg.type = types;
        msg.name = name;
        msg.object = new Object(objectId, resourceTypes, baseUrl);
        return msg;
    }
}
