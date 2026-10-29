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
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;

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

    public static String TIMESTAMP_HEADER_NAME = "timestamp";
    public static String IDENTIFIER_HEADER_NAME = "identifier";
    public static String EVENT_TYPE_HEADER_NAME = "eventType";
    public static String BASE_URL_HEADER_NAME = "baseURL";
    public static String RESOURCE_TYPE_HEADER_NAME = "resourceType";
    public static String USER_HEADER_NAME = "user";
    public static String USER_AGENT_HEADER_NAME = "userAgent";
    public static String EVENT_ID_HEADER_NAME = "eventID";

    private String jmsProvider = "activemq";
    private String jmsNamespace;

    public void setJmsProvider(final String jmsProvider) {
        this.jmsProvider = jmsProvider;
        this.jmsNamespace = null;
    }

    private String getJmsNamespace() {
        if (jmsNamespace == null) {
            // Artemis enforces the JMS spec rule that property names must be valid Java identifiers.
            jmsNamespace = "artemis".equalsIgnoreCase(jmsProvider) ? "org_fcrepo_jms_" : "org.fcrepo.jms.";
        }
        return jmsNamespace;
    }

    public String getTimestampHeaderName() {
        return getJmsNamespace() + TIMESTAMP_HEADER_NAME;
    }

    public String getIdentifierHeaderName() {
        return getJmsNamespace() + IDENTIFIER_HEADER_NAME;
    }

    public String getEventTypeHeaderName() {
        return getJmsNamespace() + EVENT_TYPE_HEADER_NAME;
    }

    public String getBaseUrlHeaderName() {
        return getJmsNamespace() + BASE_URL_HEADER_NAME;
    }

    public String getResourceTypeHeaderName() {
        return getJmsNamespace() + RESOURCE_TYPE_HEADER_NAME;
    }

    public String getUserHeaderName() {
        return getJmsNamespace() + USER_HEADER_NAME;
    }

    public String getUserAgentHeaderName() {
        return getJmsNamespace() + USER_AGENT_HEADER_NAME;
    }

    public String getEventIdHeaderName() {
        return getJmsNamespace() + EVENT_ID_HEADER_NAME;
    }

    @Override
    public Message getMessage(final Event event, final Session jmsSession)
            throws JMSException {

        final EventSerializer serializer = new JsonLDSerializer();
        final String body = serializer.serialize(event);
        final Message message = jmsSession.createTextMessage(body);

        message.setLongProperty(getTimestampHeaderName(), event.getDate().toEpochMilli());
        message.setStringProperty(getBaseUrlHeaderName(), event.getBaseUrl());

        if (event.getUserAgent() != null) {
            message.setStringProperty(getUserAgentHeaderName(), event.getUserAgent());
        }

        message.setStringProperty(getIdentifierHeaderName(), event.getPath());
        message.setStringProperty(getEventTypeHeaderName(), getEventURIs(event.getTypes()));
        message.setStringProperty(getUserHeaderName(), event.getUserID());
        message.setStringProperty(getResourceTypeHeaderName(), join(",", event.getResourceTypes()));
        message.setStringProperty(getEventIdHeaderName(), event.getEventID());

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
