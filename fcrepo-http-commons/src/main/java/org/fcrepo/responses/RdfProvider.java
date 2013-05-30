
package org.fcrepo.responses;

import static com.google.common.collect.ImmutableList.of;
import static org.apache.jena.riot.WebContent.contentTypeToLang;
import static org.fcrepo.responses.RdfSerializationUtils.setCachingHeaders;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.hp.hpl.jena.query.Dataset;

@Provider
@Component
public class RdfProvider implements MessageBodyWriter<Dataset> {

    private static final Logger logger = getLogger(RdfProvider.class);

    @Override
    public void writeTo(final Dataset rdf, final Class<?> type,
            final Type genericType, final Annotation[] annotations,
            final MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders,
            final OutputStream entityStream) throws IOException,
            WebApplicationException {

        logger.debug("Writing a response for: {} with MIMEtype: {}", rdf,
                mediaType);

        // add standard headers
        httpHeaders.put("Content-type", of((Object) mediaType.toString()));
        setCachingHeaders(httpHeaders, rdf);

        new GraphStoreStreamingOutput(rdf, mediaType)
                .write(entityStream);
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType) {

        // we can return a result for any MIME type that Jena can serialize
        final Boolean appropriateMimeType =
                contentTypeToLang(mediaType.toString()) != null;
        return appropriateMimeType &&
                (Dataset.class.isAssignableFrom(type) || Dataset.class
                        .isAssignableFrom(genericType.getClass()));
    }

    @Override
    public long getSize(final Dataset rdf, final Class<?> type,
            final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        // we don't know in advance how large the result might be
        return -1;
    }

}
