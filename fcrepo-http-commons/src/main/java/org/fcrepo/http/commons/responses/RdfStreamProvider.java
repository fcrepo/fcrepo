/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static com.google.common.util.concurrent.Futures.addCallback;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.fcrepo.kernel.utils.LogoutCallback;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.springframework.stereotype.Component;


/**
 * Provides serialization for streaming RDF results.
 *
 * @author ajs6f
 * @date Nov 19, 2013
 */
@Provider
@Component
public class RdfStreamProvider implements MessageBodyWriter<RdfStream> {

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType) {
        return RdfStream.class.isAssignableFrom(type) ;
    }

    @Override
    public long getSize(final RdfStream t, final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType) {
        // We do not know how long the stream is
        return -1;
    }

    @Override
    public void writeTo(final RdfStream rdfStream, final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders,
            final OutputStream entityStream) throws IOException,
                                      WebApplicationException {
        final RdfStreamStreamingOutput streamOutput =
            new RdfStreamStreamingOutput(rdfStream, mediaType);
        addCallback(streamOutput, new LogoutCallback(rdfStream.session()));
        streamOutput.write(entityStream);

    }

}
