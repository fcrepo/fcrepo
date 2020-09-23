/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.jms;

import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import com.google.common.eventbus.AllowConcurrentEvents;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.fcrepo.kernel.api.observer.Event;
import org.slf4j.Logger;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Machinery to publish JMS messages when an EventBus
 * message is received.
 *
 * @author barmintor
 * @author awoods
 * @author acoburn
 */
abstract class AbstractJMSPublisher {

    @Inject
    private EventBus eventBus;

    @Inject
    private ActiveMQConnectionFactory connectionFactory;

    @Inject
    private JMSEventMessageFactory eventFactory;

    private Connection connection;

    protected Session jmsSession;

    private MessageProducer producer;

    private static final Logger LOGGER = getLogger(AbstractJMSPublisher.class);

    protected abstract Destination createDestination() throws JMSException;

    /**
     * When an EventBus message is received, map it to our JMS
     * message payload and push it onto the queue.
     *
     * @param event the fedora event
     * @throws JMSException if JMS exception occurred
     */
    @Subscribe
    @AllowConcurrentEvents
    public void publishJCREvent(final Event event) throws JMSException {
        LOGGER.debug("Received an event from the internal bus. {}", event);
        final Message tm =
                eventFactory.getMessage(event, jmsSession);
        LOGGER.trace("Transformed the event to a JMS message.");
        producer.send(tm);

        LOGGER.debug("Put event: {} onto JMS.", tm.getJMSMessageID());
    }

    /**
     * Connect to JCR Repository and JMS queue
     *
     * @throws JMSException if JMS Exception occurred
     */
    @PostConstruct
    public void acquireConnections() throws JMSException {
        LOGGER.debug("Initializing: {}", this.getClass().getCanonicalName());

        connection = connectionFactory.createConnection();
        connection.start();
        jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        producer = jmsSession.createProducer(createDestination());
        eventBus.register(this);
    }

    /**
     * Close external connections
     *
     * @throws JMSException if JMS exception occurred
     */
    @PreDestroy
    public void releaseConnections() throws JMSException {
        LOGGER.debug("Tearing down: {}", this.getClass().getCanonicalName());

        producer.close();
        jmsSession.close();
        connection.close();
        eventBus.unregister(this);
    }
}
