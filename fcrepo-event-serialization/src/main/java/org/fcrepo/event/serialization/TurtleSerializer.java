/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.event.serialization;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.jena.rdf.model.Model;
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
        final Model model = EventSerializer.toModel(evt);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        model.write(out, "TTL");
        return out.toString(StandardCharsets.UTF_8);
    }
}
