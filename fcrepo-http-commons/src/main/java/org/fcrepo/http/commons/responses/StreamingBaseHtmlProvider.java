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

import static org.fcrepo.kernel.rdf.SerializationUtils.subjectKey;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;

/**
 * Simple HTML provider that delegates to {@link BaseHtmlProvider}
 *
 * @author ajs6f
 * @date Nov 19, 2013
 */
@Provider
public class StreamingBaseHtmlProvider implements MessageBodyWriter<RdfStream>,
        ApplicationContextAware {

    private BaseHtmlProvider delegate;

    private ApplicationContext applicationContext;

    private static final Logger LOGGER =
        getLogger(StreamingBaseHtmlProvider.class);

    @PostConstruct
    void init() throws IOException, RepositoryException {
        delegate = applicationContext.getBean(BaseHtmlProvider.class);
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType) {
        if (!RdfStream.class.isAssignableFrom(type)) {
            return false;
        }
        LOGGER.debug(
                "Checking to see if type: {} is serializable to mimeType: {}",
                type.getName(), mediaType);
        return delegate.isWriteable(Dataset.class, genericType, annotations,
                mediaType);
    }

    @Override
    public long getSize(final RdfStream rdfStream, final Class<?> type,
            final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        // we do not know how long the stream is
        return -1;
    }

    @Override
    public void writeTo(final RdfStream rdfStream, final Class<?> type,
            final Type genericType, final Annotation[] annotations,
            final MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders,
            final OutputStream entityStream) throws IOException,
                                            WebApplicationException {
        final Dataset dataset = DatasetFactory.create(rdfStream.asModel());
        dataset.getContext().set(subjectKey, rdfStream.topic());
        delegate.writeTo(dataset, type, genericType, annotations, mediaType,
                httpHeaders, entityStream);
    }

    @Override
    public void setApplicationContext(
            final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;

    }

}
