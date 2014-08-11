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
package org.fcrepo.jms.observer;

import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.fcrepo.kernel.impl.utils.TestHelpers.setField;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.fcrepo.kernel.observer.FedoraEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.google.common.eventbus.EventBus;

/**
 * <p>JMSTopicPublisherTest class.</p>
 *
 * @author awoods
 */
public class JMSTopicPublisherTest {

    private JMSTopicPublisher testObj;

    @Mock
    private JMSEventMessageFactory mockEvents;

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
        testObj = new JMSTopicPublisher();
        initMocks(this);
        setField(testObj, "eventFactory", mockEvents);
        setField(testObj, "producer", mockProducer);
        setField(testObj, "connectionFactory", mockConnections);
        setField(testObj, "eventBus", mockBus);
    }

    @Test
    public void testAcquireConnections() throws JMSException {
        when(mockConnections.createConnection()).thenReturn(mockConn);
        when(mockConn.createSession(false, AUTO_ACKNOWLEDGE))
                .thenReturn(mockJmsSession);
        testObj.acquireConnections();
        verify(mockBus).register(any());
    }

    @Test
    public void testPublishJCREvent() throws RepositoryException, IOException, JMSException {
        final Message mockMsg = mock(Message.class);
        final FedoraEvent mockEvent = mock(FedoraEvent.class);
        when(mockEvents.getMessage(eq(mockEvent), any(javax.jms.Session.class))).thenReturn(mockMsg);
        testObj.publishJCREvent(mockEvent);
    }

    @Test
    public void testReleaseConnections() throws JMSException  {
        setField(testObj, "connection", mockConn);
        setField(testObj, "jmsSession", mockJmsSession);
        testObj.releaseConnections();
        verify(mockProducer).close();
        verify(mockJmsSession).close();
        verify(mockConn).close();
        verify(mockBus).unregister(testObj);
    }
}
