
package org.fcrepo.observer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import javax.jcr.Repository;
import javax.jcr.observation.Event;
import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageProducer;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.google.common.eventbus.EventBus;

public class JMSTopicPublisherTest {

    @Mock
    JMSTopicPublisher testObj;

    @Mock
    JMSEventMessageFactory mockEvents;

    @Mock
    MessageProducer mockProducer;

    @Mock
    ActiveMQConnectionFactory mockConnections;

    @Mock
    Repository mockRepo;

    @Mock
    EventBus mockBus;

    @Before
    public void setUp() throws Exception {
        testObj = new JMSTopicPublisher();
        mockEvents = mock(JMSEventMessageFactory.class);
        mockProducer = mock(MessageProducer.class);
        mockConnections = mock(ActiveMQConnectionFactory.class);
        mockRepo = mock(Repository.class);
        mockBus = mock(EventBus.class);
        Field setField =
                JMSTopicPublisher.class.getDeclaredField("eventFactory");
        setField.setAccessible(true);
        setField.set(testObj, mockEvents);
        setField = JMSTopicPublisher.class.getDeclaredField("producer");
        setField.setAccessible(true);
        setField.set(testObj, mockProducer);
        setField =
                JMSTopicPublisher.class.getDeclaredField("connectionFactory");
        setField.setAccessible(true);
        setField.set(testObj, mockConnections);
        setField = JMSTopicPublisher.class.getDeclaredField("repo");
        setField.setAccessible(true);
        setField.set(testObj, mockRepo);
        setField = JMSTopicPublisher.class.getDeclaredField("eventBus");
        setField.setAccessible(true);
        setField.set(testObj, mockBus);

    }

    @Test
    public void testAcquireConnections() throws Exception {
        Connection mockConn = mock(Connection.class);
        javax.jms.Session mockSession = mock(javax.jms.Session.class);
        when(mockConnections.createConnection()).thenReturn(mockConn);
        when(mockConn.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE))
                .thenReturn(mockSession);
        testObj.acquireConnections();
        verify(mockBus).register(any());
        verify(mockRepo).login();
    }

    @Test
    public void testPublishJCREvent() throws Exception {
        Message mockMsg = mock(Message.class);
        Event mockEvent = mock(Event.class);
        when(
                mockEvents.getMessage(eq(mockEvent),
                        any(javax.jcr.Session.class),
                        any(javax.jms.Session.class))).thenReturn(mockMsg);
        testObj.publishJCREvent(mockEvent);
    }

    @Test
    public void testReleaseConnections() throws Exception {
        Connection mockConn = mock(Connection.class);
        javax.jms.Session mockJmsSession = mock(javax.jms.Session.class);
        javax.jcr.Session mockJcrSession = mock(javax.jcr.Session.class);
        Field setField = JMSTopicPublisher.class.getDeclaredField("connection");
        setField.setAccessible(true);
        setField.set(testObj, mockConn);
        setField = JMSTopicPublisher.class.getDeclaredField("session");
        setField.setAccessible(true);
        setField.set(testObj, mockJcrSession);
        setField = JMSTopicPublisher.class.getDeclaredField("jmsSession");
        setField.setAccessible(true);
        setField.set(testObj, mockJmsSession);
        testObj.releaseConnections();
        verify(mockJcrSession).logout();
    }
}
