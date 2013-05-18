/**
 *
 */
package org.fcrepo.provider;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.jena.riot.WebContent;

import com.hp.hpl.jena.rdf.model.Model;


/**
 * @author frank asseg
 *
 */
public class ModelStreamingOutput implements StreamingOutput{
    private final Model model;
    private final String contentType;

    public ModelStreamingOutput(final Model model,final MediaType mediaType) {
        super();
        this.model = model;
        this.contentType = WebContent.contentTypeToLang(mediaType.toString()).getName().toUpperCase();
    }

    @Override
    public void write(OutputStream output) throws IOException,
            WebApplicationException {
        this.model.write(output, this.contentType);
    }
}
