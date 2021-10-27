/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.jms;

import static java.lang.String.join;
import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.fcrepo.kernel.api.observer.Event;
import org.fcrepo.kernel.api.observer.EventType;
import org.fcrepo.event.serialization.EventSerializer;
import org.fcrepo.event.serialization.JsonLDSerializer;

import org.slf4j.Logger;

/**
 * Generates JMS {@link Message}s composed entirely of headers, based entirely
 * on information found in the {@link Event} that triggers publication.
 *
 * @author ajs6f
 * @author escowles
 * @since Dec 2, 2013
 */
public class DefaultMessageFactory implements JMSEventMessageFactory {

    private static final String JMS_NAMESPACE = "org.fcrepo.jms.";

    public static final String TIMESTAMP_HEADER_NAME = JMS_NAMESPACE
            + "timestamp";

    public static final String IDENTIFIER_HEADER_NAME = JMS_NAMESPACE
            + "identifier";

    public static final String EVENT_TYPE_HEADER_NAME = JMS_NAMESPACE
            + "eventType";

    public static final String BASE_URL_HEADER_NAME = JMS_NAMESPACE
            + "baseURL";

    public static final String RESOURCE_TYPE_HEADER_NAME = JMS_NAMESPACE + "resourceType";

    public static final String USER_HEADER_NAME = JMS_NAMESPACE + "user";
    public static final String USER_AGENT_HEADER_NAME = JMS_NAMESPACE + "userAgent";
    public static final String EVENT_ID_HEADER_NAME = JMS_NAMESPACE + "eventID";

    @Override
    public Message getMessage(final Event event, final Session jmsSession)
            throws JMSException {

        final EventSerializer serializer = new JsonLDSerializer();
        final String body = serializer.serialize(event);
        final Message message = jmsSession.createTextMessage(body);

        message.setLongProperty(TIMESTAMP_HEADER_NAME, event.getDate().toEpochMilli());
        message.setStringProperty(BASE_URL_HEADER_NAME, event.getBaseUrl());

        if (event.getUserAgent() != null) {
            message.setStringProperty(USER_AGENT_HEADER_NAME, event.getUserAgent());
        }

        message.setStringProperty(IDENTIFIER_HEADER_NAME, event.getPath());
        message.setStringProperty(EVENT_TYPE_HEADER_NAME, getEventURIs(event.getTypes()));
        message.setStringProperty(USER_HEADER_NAME, event.getUserID());
        message.setStringProperty(RESOURCE_TYPE_HEADER_NAME, join(",", event.getResourceTypes()));
        message.setStringProperty(EVENT_ID_HEADER_NAME, event.getEventID());

        LOGGER.trace("getMessage() returning: {}", message);
        return message;
    }

    private static String getEventURIs(final Set<EventType> types) {
        final String uris = types.stream()
                                 .map(EventType::getType)
                                 .collect(joining(","));

        LOGGER.debug("Constructed event type URIs: {}", uris);
        return uris;
    }

    private static final Logger LOGGER = getLogger(DefaultMessageFactory.class);

}
