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
package org.fcrepo.jms.observer;

import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.IOException;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;

import org.apache.activemq.ActiveMQConnectionFactory;

import org.fcrepo.kernel.observer.FedoraEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.eventbus.EventBus;

/**
 * <p>JMSTopicPublisherTest class.</p>
 *
 * @author awoods
 */
@RunWith(MockitoJUnitRunner.class)
public class JMSTopicPublisherTest {

    private JMSTopicPublisher testJMSTopicPublisher;

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

    @Before
    public void setUp() {
        testJMSTopicPublisher = new JMSTopicPublisher();
        setField(testJMSTopicPublisher, "eventFactory", mockEventFactory);
        setField(testJMSTopicPublisher, "producer", mockProducer);
        setField(testJMSTopicPublisher, "connectionFactory", mockConnections);
        setField(testJMSTopicPublisher, "eventBus", mockBus);
    }

    @Test
    public void testAcquireConnections() throws JMSException {
        when(mockConnections.createConnection()).thenReturn(mockConn);
        when(mockConn.createSession(false, AUTO_ACKNOWLEDGE))
                .thenReturn(mockJmsSession);
        testJMSTopicPublisher.acquireConnections();
        verify(mockBus).register(any());
    }

    @Test
    public void testPublishJCREvent() throws IOException, JMSException {
        final Message mockMsg = mock(Message.class);
        final FedoraEvent mockEvent = mock(FedoraEvent.class);
        when(mockEventFactory.getMessage(eq(mockEvent), any(javax.jms.Session.class))).thenReturn(mockMsg);
        testJMSTopicPublisher.publishJCREvent(mockEvent);
        verify(mockProducer).send(mockMsg);
    }

    @Test
    public void testReleaseConnections() throws JMSException  {
        setField(testJMSTopicPublisher, "connection", mockConn);
        setField(testJMSTopicPublisher, "jmsSession", mockJmsSession);
        testJMSTopicPublisher.releaseConnections();
        verify(mockProducer).close();
        verify(mockJmsSession).close();
        verify(mockConn).close();
        verify(mockBus).unregister(testJMSTopicPublisher);
    }
}
