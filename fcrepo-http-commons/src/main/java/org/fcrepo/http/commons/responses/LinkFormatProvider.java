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
package org.fcrepo.http.commons.responses;

import static org.fcrepo.http.commons.domain.RDFMediaType.APPLICATION_LINK_FORMAT;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

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

        final PrintWriter writer = new PrintWriter(entityStream);
        links.getStream().forEach(l -> {
            writer.println(l.toString() + ",");
        });
        writer.close();
    }

}
