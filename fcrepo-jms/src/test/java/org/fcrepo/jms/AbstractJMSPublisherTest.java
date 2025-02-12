/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.jms;

import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;

import org.apache.activemq.ActiveMQConnectionFactory;

import org.fcrepo.kernel.api.observer.Event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.eventbus.EventBus;

/**
 * <p>JMSTopicPublisherTest class.</p>
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
    private ActiveMQConnectionFactory mockConnections;

    @Mock
    private EventBus mockBus;

    @Mock
    private javax.jms.Session mockJmsSession;

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
