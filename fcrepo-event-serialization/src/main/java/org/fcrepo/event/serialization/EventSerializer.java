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

import java.io.ByteArrayInputStream;

import org.apache.jena.rdf.model.Model;
import org.fcrepo.kernel.api.observer.Event;

/**
 * A basic serialization API for Fedora events
 * @author acoburn
 * @author dbernstein
 */
public interface EventSerializer {

    /**
     * Convert an event to an Rdf Model
     * @param evt the Fedora event
     * @return an RDF model representing the event
     */
    static Model toModel(final Event evt) {
        final EventSerializer serializer = new JsonLDSerializer();
        final String json = serializer.serialize(evt);
        final Model model = createDefaultModel();
        final String baseUrl = evt.getBaseUrl();
        model.read(new ByteArrayInputStream(json.getBytes(UTF_8)), baseUrl + evt.getPath(), "JSON-LD");
        return model;
    }

    /**
     * Serialize a Event into a JSON String
     * @param evt the Fedora event
     * @return a JSON string
     */
    String serialize(final Event evt);
}
