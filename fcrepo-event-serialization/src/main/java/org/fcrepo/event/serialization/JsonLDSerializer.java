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

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.fcrepo.event.serialization.JsonLDEventMessage.from;
import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.fcrepo.kernel.api.observer.Event;
import org.slf4j.Logger;

/**
 * Some serialization utilities for Event objects
 * @author acoburn
 */
public class JsonLDSerializer implements EventSerializer {

    private static final Logger LOGGER = getLogger(JsonLDSerializer.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Create a new JSON-LD Event Serializer
     */
    public JsonLDSerializer() {
        // newer versions of jackson rename this to `JavaTimeModule`
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.configure(WRITE_DATES_AS_TIMESTAMPS, false);
    }

    /**
     * Serialize a Event into a JSON String
     * @param evt the Fedora event
     * @return a JSON string
     */
    @Override
    public String serialize(final Event evt) {
        try {
            return MAPPER.writeValueAsString(from(evt));
        } catch (final JsonProcessingException ex) {
            LOGGER.error("Error processing JSON: {}", ex.getMessage());
            return null;
        }
    }
}
