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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.fcrepo.kernel.api.observer.FedoraEvent;
import org.slf4j.Logger;

/**
 * A structure used for serializing a FedoraEvent into JSON
 * @author acoburn
 */
class JsonLDEventMessage {

    @JsonIgnore
    private static final Logger LOGGER = getLogger(JsonLDEventMessage.class);

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

        public final String foaf = "http://xmlns.com/foaf/0.1/";

        public final String dcterms = "http://purl.org/dc/terms/";

        public final String xsd = "http://www.w3.org/2001/XMLSchema#";

        public final String type = "@type";

        public final String id = "@id";

        public final ContextElement name = new ContextElement("foaf:name", "xsd:string");

        public final ContextElement identifier = new ContextElement("dcterms:identifier");

        public final ContextElement isPartOf = new ContextElement("dcterms:isPartOf");

        public final ContextElement atTime = new ContextElement("prov:atTime", "xsd:dateTime");

        public final ContextElement wasAttributedTo = new ContextElement("prov:wasAttributedTo");

        public final ContextElement wasGeneratedBy = new ContextElement("prov:wasGeneratedBy");
    }

    static class Activity {
        @JsonProperty("type")
        public List<String> type;

        @JsonProperty("identifier")
        public String identifier;

        @JsonProperty("atTime")
        public Instant atTime;

        public Activity(final String identifier, final Instant atTime, final List<String> type) {
            this.type = type;
            this.identifier = identifier;
            this.atTime = atTime;
        }
    }

    static class Agent {
        @JsonProperty("type")
        public String type;

        @JsonProperty("name")
        public String name;

        public Agent(final String type, final String name) {
            this.type = type;
            this.name = name;
        }
    }

    @JsonProperty("id")
    public String id;

    @JsonProperty("type")
    public List<String> type;

    @JsonProperty("isPartOf")
    public String isPartOf;

    @JsonProperty("wasGeneratedBy")
    public Activity wasGeneratedBy;

    @JsonProperty("wasAttributedTo")
    public List<Agent> wasAttributedTo;

    @JsonProperty("@context")
    public Context context = new Context();

    /**
     * Populate a JsonLDEventMessage from a FedoraEvent
     * @param evt The Fedora event
     * @return a JsonLDEventMessage
     */
    public static JsonLDEventMessage from(final FedoraEvent evt) {
        final JsonLDEventMessage msg = new JsonLDEventMessage();

        final String baseUrl = evt.getInfo().get(BASE_URL);
        final String userAgent = evt.getInfo().get(USER_AGENT);

        final List<Agent> agents = new ArrayList<>();
        agents.add(new Agent(PROV_NAMESPACE + "Person", evt.getUserID()));
        if (userAgent != null) {
            agents.add(new Agent(PROV_NAMESPACE + "SoftwareAgent", userAgent));
        }
        msg.wasAttributedTo = agents;

        final List<String> types = evt.getTypes().stream().map(rdfType -> rdfType.getType()).collect(toList());
        if (!types.contains(PROV_NAMESPACE + "Activity")) {
            types.add(PROV_NAMESPACE + "Activity");
        }
        msg.wasGeneratedBy = new Activity(evt.getEventID(), evt.getDate(), types);

        if (baseUrl == null) {
            msg.id = "info:fedora" + evt.getPath();
        } else {
            msg.isPartOf = baseUrl;
            msg.id = baseUrl + evt.getPath();
        }
        final List<String> resourceTypes = new ArrayList<>(evt.getResourceTypes());
        if (!resourceTypes.contains(PROV_NAMESPACE + "Entity")) {
            resourceTypes.add(PROV_NAMESPACE + "Entity");
        }
        msg.type = resourceTypes;
        return msg;
    }
}
