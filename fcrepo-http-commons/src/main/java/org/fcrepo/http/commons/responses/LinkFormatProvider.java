/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.responses;

import static org.fcrepo.http.commons.domain.RDFMediaType.APPLICATION_LINK_FORMAT;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

/**
 * Writer for application/link-format bodies.
 *
 * @author whikloj
 * @since 2017-10-25
 */
@Provider
@Produces(APPLICATION_LINK_FORMAT)
public class LinkFormatProvider implements MessageBodyWriter<LinkFormatStream> {

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
        final MediaType mediaType) {
        return LinkFormatStream.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(final LinkFormatStream links, final Class<?> type, final Type genericType,
        final Annotation[] annotations, final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final LinkFormatStream links, final Class<?> type, final Type genericType,
        final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
        final OutputStream entityStream)
            throws WebApplicationException {

        final PrintWriter writer = new PrintWriter(entityStream, false, StandardCharsets.UTF_8);
        links.getStream().forEach(l -> {
            writer.println(l.toString() + ",");
        });
        writer.close();
    }

}
