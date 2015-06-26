/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.http.commons.domain;

import com.google.common.annotations.VisibleForTesting;

import org.fcrepo.kernel.services.ExternalContentService;

import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import static java.util.Arrays.stream;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * Provide an InputStream either from the POST/PUT body, or by resolving a Content-Location URI
 * @author cabeer
 */
@Provider
public class ContentLocationMessageBodyReader implements MessageBodyReader<InputStream> {

    /**
     * The fcrepo node service
     */
    @Autowired
    private ExternalContentService contentService;

    private static final Class<ContentLocation> contentLocationClass = ContentLocation.class;

    @Override
    public boolean isReadable(final Class<?> type,
                              final Type genericType,
                              final Annotation[] annotations,
                              final MediaType mediaType) {
        return InputStream.class.isAssignableFrom(type) &&
                    stream(annotations).map(Annotation::annotationType).anyMatch(contentLocationClass::equals);
    }

    @Override
    public InputStream readFrom(final Class<InputStream> type,
                                final Type genericType,
                                final Annotation[] annotations,
                                final MediaType mediaType,
                                final MultivaluedMap<String, String> httpHeaders,
                                final InputStream entityStream) throws IOException {

        if (httpHeaders.containsKey("Content-Location")) {
            final String location = httpHeaders.getFirst("Content-Location");

            try {
                return contentService.retrieveExternalContent(new URI(location));
            } catch (final URISyntaxException e) {
                throw new WebApplicationException(e, BAD_REQUEST);
            }

        }
        return entityStream;
    }

    @VisibleForTesting
    protected void setContentService(final ExternalContentService externalContentService) {
        this.contentService = externalContentService;
    }
}
