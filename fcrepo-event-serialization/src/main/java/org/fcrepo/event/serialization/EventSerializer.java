/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
