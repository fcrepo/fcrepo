/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.jms.headers;

import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jms.JMSException;
import javax.jms.Message;

import org.fcrepo.jms.observer.JMSEventMessageFactory;
import org.fcrepo.kernel.observer.FedoraEvent;
import org.fcrepo.kernel.utils.EventType;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

/**
 * Generates JMS {@link Message}s composed entirely of headers, based entirely
 * on information found in the {@link FedoraEvent} that triggers publication.
 *
 * @author ajs6f
 * @since Dec 2, 2013
 */
public class DefaultMessageFactory implements JMSEventMessageFactory {

    public static final String TIMESTAMP_HEADER_NAME = REPOSITORY_NAMESPACE
            + "timestamp";

    public static final String IDENTIFIER_HEADER_NAME = REPOSITORY_NAMESPACE
            + "identifier";

    public static final String EVENT_TYPE_HEADER_NAME = REPOSITORY_NAMESPACE
            + "eventType";

    public static final String BASE_URL_HEADER_NAME = REPOSITORY_NAMESPACE
            + "baseURL";

    public static final String PROPERTIES_HEADER_NAME = REPOSITORY_NAMESPACE
            + "properties";

    private String baseURL;

    /**
     * @param baseURL indicating the repository server host/port/etc
     */
    public DefaultMessageFactory(final String baseURL) {
        this.baseURL = baseURL;
        log.debug("MessageFactory baseURL: {}", baseURL);
    }

    @Override
    public Message getMessage(final FedoraEvent jcrEvent,
        final javax.jms.Session jmsSession) throws RepositoryException,
        IOException, JMSException {
        final Message message = jmsSession.createMessage();
        message.setLongProperty(TIMESTAMP_HEADER_NAME, jcrEvent.getDate());
        message.setStringProperty(IDENTIFIER_HEADER_NAME, jcrEvent.getPath());
        message.setStringProperty(EVENT_TYPE_HEADER_NAME, getEventURIs( jcrEvent
                .getTypes()));
        message.setStringProperty(BASE_URL_HEADER_NAME, baseURL);
        message.setStringProperty(PROPERTIES_HEADER_NAME, Joiner.on(',').join(jcrEvent.getProperties()));

        log.trace("getMessage() returning: {}", message);
        return message;
    }

    private static String getEventURIs(final Set<Integer> types) {
        final String uris = Joiner.on(',').join(Iterables.transform(types, new Function<Integer, String>() {

            @Override
            public String apply(final Integer type) {
                return REPOSITORY_NAMESPACE + EventType.valueOf(type);
            }
        }));
        log.debug("Constructed event type URIs: {}", uris);
        return uris;
    }

    private static final Logger log = getLogger(DefaultMessageFactory.class);

}
