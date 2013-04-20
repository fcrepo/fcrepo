
package org.fcrepo.generator.rdf;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.io.OutputStream;

import org.apache.any23.writer.NTriplesWriter;
import org.apache.any23.writer.RDFXMLWriter;
import org.apache.any23.writer.TripleHandler;
import org.apache.any23.writer.TurtleWriter;

public abstract class Utils {

    public static TripleHandler selectWriter(final String mimeType,
            final OutputStream out) {
        switch (mimeType) {
            case "text/turtle":
                return new TurtleWriter(out);
            case TEXT_PLAIN:
                return new NTriplesWriter(out);
            default:
                return new RDFXMLWriter(out);
        }
    }

}
