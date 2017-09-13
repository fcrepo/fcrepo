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
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.fcrepo.kernel.api.observer.FedoraEvent;
import org.slf4j.Logger;

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

    public static final String USER_AGENT_BASE_URI_PROPERTY = "fcrepo.auth.webac.userAgent.baseUri";
    static class Object {

        @JsonProperty("type")
        public List<String> type;

        @JsonProperty("id")
        public String id;

        public Object(final String id, final List<String> type) {
            this.type = type;
            this.id = id;
        }
    }

    @JsonProperty("id")
    public String id;

    @JsonProperty("type")
    public List<String> type;

    @JsonProperty("name")
    public String name;

    @JsonProperty("actor")
    public String actor;

    @JsonProperty("object")
    public Object object;

    @JsonProperty("@context")
    public String context;

    /**
     * Populate a JsonLDEventMessage from a FedoraEvent
     * 
     * @param evt The Fedora event
     * @return a JsonLDEventMessage
     */
    public static JsonLDEventMessage from(final FedoraEvent evt) {
        final JsonLDEventMessage msg = new JsonLDEventMessage();
        msg.context = ACTIVITYSTREAMS_NS;
        final String baseUrl = evt.getInfo().get(BASE_URL);
        final String userAgent = evt.getUserID();
        msg.id = evt.getEventID();
        final String userAgentBaseUri = System.getProperty(USER_AGENT_BASE_URI_PROPERTY, "#");
        msg.actor = userAgentBaseUri  + userAgent;

        final List<String> types = evt.getTypes()
                .stream()
                .map(rdfType -> rdfType.getType())
                .collect(toList());
        msg.type = types;

        final String name = String.join(", ", evt.getTypes()
                .stream()
                .map(rdfType -> rdfType.getName())
                .collect(toList()));
        msg.name = name;
        final List<String> resourceTypes = new ArrayList<>(evt.getResourceTypes());
        if (!resourceTypes.contains(PROV_NAMESPACE + "Entity")) {
            resourceTypes.add(PROV_NAMESPACE + "Entity");
        }
        final String objectId = baseUrl + evt.getPath();
        msg.object = new Object(objectId, resourceTypes);
        return msg;
    }
}
