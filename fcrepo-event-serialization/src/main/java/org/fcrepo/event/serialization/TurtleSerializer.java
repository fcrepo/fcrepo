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

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.jena.rdf.model.Model;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.observer.Event;

/**
 * Serialize a Event as Turtle
 * @author acoburn
 * @since 6/16/16
 */
public class TurtleSerializer implements EventSerializer {

    /**
     * Serialize a Event in RDF using Turtle syntax
     * @param evt the Fedora event
     * @return a string of RDF, using Turtle syntax
     */
    @Override
    public String serialize(final Event evt) {
        try {
            final Model model = EventSerializer.toModel(evt);
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            model.write(out, "TTL");
            return out.toString("UTF-8");
        } catch (final UnsupportedEncodingException ex) {
            throw new RepositoryRuntimeException(ex.getMessage(), ex);
        }
    }
}
