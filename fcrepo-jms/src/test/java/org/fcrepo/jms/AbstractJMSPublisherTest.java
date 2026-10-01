/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.jms;

import static jakarta.jms.Session.AUTO_ACKNOWLEDGE;
import static java.time.Instant.ofEpochMilli;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;

import org.apache.activemq.command.ActiveMQTextMessage;

import org.fcrepo.kernel.api.observer.Event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.eventbus.EventBus;

/**
 * Shared unit tests for {@link AbstractJMSPublisher} subclasses. Mocks the JMS API directly
 * so the tests are broker-agnostic and apply equally to ActiveMQ Classic and Artemis; the
 * concrete broker behaviours are verified separately by the integration tests.
 *
 * @author awoods
 */
@ExtendWith(MockitoExtension.class)
abstract class AbstractJMSPublisherTest {

    protected abstract AbstractJMSPublisher getPublisher();

    private AbstractJMSPublisher testJMSPublisher;

    @Mock
    private JMSEventMessageFactory mockEventFactory;

    @Mock
    private MessageProducer mockProducer;

    @Mock
    private ConnectionFactory mockConnections;

    @Mock
    private EventBus mockBus;

    @Mock
    private jakarta.jms.Session mockJmsSession;

    @Mock
    private Connection mockConn;

    @BeforeEach
    public void setUp() {
        testJMSPublisher = getPublisher();
        setField(testJMSPublisher, "eventFactory", mockEventFactory);
        setField(testJMSPublisher, "producer", mockProducer);
        setField(testJMSPublisher, "connectionFactory", mockConnections);
        setField(testJMSPublisher, "eventBus", mockBus);
    }

    @Test
    public void testAcquireConnections() throws JMSException {
        when(mockConnections.createConnection()).thenReturn(mockConn);
        when(mockConn.createSession(false, AUTO_ACKNOWLEDGE))
                .thenReturn(mockJmsSession);
        testJMSPublisher.acquireConnections();
        verify(mockBus).register(any());
    }

    @Test
    public void testPublishJCREvent() throws JMSException {
        final Message mockMsg = mock(Message.class);
        final Event mockEvent = mock(Event.class);
        when(mockEventFactory.getMessage(eq(mockEvent), isNull())).thenReturn(mockMsg);
        testJMSPublisher.publishJCREvent(mockEvent);
        verify(mockProducer).send(mockMsg);
    }

    /**
     * Mirrors what {@code JmsConfig.messageFactory(FedoraPropsConfig)} does in production: instantiate a
     * {@link DefaultMessageFactory} and seed its provider from config. Verifies that an Artemis-configured wiring
     * results in published messages whose property names use the underscore namespace Artemis requires.
     */
    @Test
    public void testPublishUsesArtemisNamespaceWhenFactoryConfiguredForArtemis() throws JMSException {
        final var artemisFactory = new DefaultMessageFactory();
        artemisFactory.setJmsProvider("artemis");
        setField(testJMSPublisher, "eventFactory", artemisFactory);
        setField(testJMSPublisher, "jmsSession", mockJmsSession);

        final var sentMessage = publishMinimalEvent();

        assertEquals("/r", sentMessage.getStringProperty("org_fcrepo_jms_identifier"),
                "Artemis-configured publisher must use the underscore namespace");
        assertNull(sentMessage.getStringProperty("org.fcrepo.jms.identifier"),
                "Artemis-configured publisher must not emit the dotted namespace");
    }

    /**
     * Mirrors what {@code JmsConfig.messageFactory(FedoraPropsConfig)} does in production for the ActiveMQ Classic
     * provider — verifies the dotted namespace is preserved on published messages so existing downstream consumers
     * keep working.
     */
    @Test
    public void testPublishUsesDottedNamespaceWhenFactoryConfiguredForActiveMq() throws JMSException {
        final var activeMqFactory = new DefaultMessageFactory();
        activeMqFactory.setJmsProvider("activemq");
        setField(testJMSPublisher, "eventFactory", activeMqFactory);
        setField(testJMSPublisher, "jmsSession", mockJmsSession);

        final var sentMessage = publishMinimalEvent();

        assertEquals("/r", sentMessage.getStringProperty("org.fcrepo.jms.identifier"),
                "ActiveMQ-configured publisher must use the dotted namespace");
        assertNull(sentMessage.getStringProperty("org_fcrepo_jms_identifier"),
                "ActiveMQ-configured publisher must not emit the underscore namespace");
    }

    private ActiveMQTextMessage publishMinimalEvent() throws JMSException {
        final var sentMessage = new ActiveMQTextMessage();
        when(mockJmsSession.createTextMessage(anyString())).thenReturn(sentMessage);

        final Event mockEvent = mock(Event.class);
        when(mockEvent.getDate()).thenReturn(ofEpochMilli(0L));
        when(mockEvent.getBaseUrl()).thenReturn("http://example/");
        when(mockEvent.getPath()).thenReturn("/r");
        when(mockEvent.getTypes()).thenReturn(emptySet());
        when(mockEvent.getResourceTypes()).thenReturn(emptySet());
        when(mockEvent.getUserID()).thenReturn("u");
        when(mockEvent.getEventID()).thenReturn("e");

        testJMSPublisher.publishJCREvent(mockEvent);
        verify(mockProducer).send(sentMessage);
        return sentMessage;
    }

    @Test
    public void testReleaseConnections() throws JMSException  {
        setField(testJMSPublisher, "connection", mockConn);
        setField(testJMSPublisher, "jmsSession", mockJmsSession);
        testJMSPublisher.releaseConnections();
        verify(mockProducer).close();
        verify(mockJmsSession).close();
        verify(mockConn).close();
        verify(mockBus).unregister(testJMSPublisher);
    }

}
