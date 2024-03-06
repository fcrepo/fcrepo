/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.jms;

import static jakarta.jms.Session.AUTO_ACKNOWLEDGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.google.common.eventbus.EventBus;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.fcrepo.kernel.api.observer.Event;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;

/**
 * <p>JMSTopicPublisherTest class.</p>
 *
 * @author awoods
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
abstract class AbstractJMSPublisherTest {

    protected abstract AbstractJMSPublisher getPublisher();

    private AbstractJMSPublisher testJMSPublisher;

    @Mock
    private JMSEventMessageFactory mockEventFactory;

    @Mock
    private MessageProducer mockProducer;

    @Mock
    private ActiveMQConnectionFactory mockConnections;

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
