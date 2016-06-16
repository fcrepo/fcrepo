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
package org.fcrepo.event.serialization.jsonld;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.fcrepo.event.serialization.jsonld.EventMessage.from;
import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import org.fcrepo.kernel.api.observer.FedoraEvent;
import org.fcrepo.event.serialization.rdf.EventSerializer;
import org.slf4j.Logger;

/**
 * Some serialization utilities for FedoraEvent objects
 * @author acoburn
 */
public class JsonLDSerializer implements EventSerializer {

    private static final Logger LOGGER = getLogger(JsonLDSerializer.class);

    /**
     * Serialize a FedoraEvent into a JSON String
     * @param evt the Fedora event
     * @return a JSON string
     */
    @Override
    public String serialize(final FedoraEvent evt) {
        final ObjectMapper mapper = new ObjectMapper();
        // newer versions of jackson rename this to `JavaTimeModule`
        mapper.registerModule(new JSR310Module());
        mapper.configure(WRITE_DATES_AS_TIMESTAMPS, false);

        try {
            return mapper.writeValueAsString(from(evt));
        } catch (final JsonProcessingException ex) {
            LOGGER.warn("Error processing JSON: " + ex.getMessage());
            return null;
        }
    }
}
