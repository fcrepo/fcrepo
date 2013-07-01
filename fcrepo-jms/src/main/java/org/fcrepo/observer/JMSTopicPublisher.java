/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.observer;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Machinery to publish JMS messages when an EventBus
 * message is received.
 */
public class JMSTopicPublisher {

    @Inject
    private EventBus eventBus;

    @Inject
    private Repository repo;

    @Inject
    private ActiveMQConnectionFactory connectionFactory;

    @Inject
    private JMSEventMessageFactory eventFactory;

    private Connection connection;

    private Session jmsSession;

    private MessageProducer producer;

    private final Logger LOGGER = getLogger(JMSTopicPublisher.class);

    private javax.jcr.Session session;

    /**
     * When an EventBus mesage is received, map it to our JMS
     * message payload and push it onto the queue.
     * 
     * @param fedoraEvent
     * @throws JMSException
     * @throws RepositoryException
     * @throws IOException
     */
    @Subscribe
    public void publishJCREvent(final Event fedoraEvent) throws JMSException,
        RepositoryException, IOException {
        LOGGER.debug("Received an event from the internal bus.");
        final Message tm =
                eventFactory.getMessage(fedoraEvent, session, jmsSession);
        LOGGER.debug("Transformed the event to a JMS message.");
        producer.send(tm);

        LOGGER.debug("Put event: \n{}\n onto JMS.", tm.getJMSMessageID());
    }

    /**
     * Connect to JCR Repostory and JMS queue
     * 
     * @throws JMSException
     * @throws RepositoryException
     */
    @PostConstruct
    public void acquireConnections() throws JMSException, RepositoryException {
        LOGGER.debug("Initializing: " + this.getClass().getCanonicalName());

        connection = connectionFactory.createConnection();
        connection.start();
        jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        producer = jmsSession.createProducer(jmsSession.createTopic("fedora"));
        eventBus.register(this);

        session = repo.login();
    }

    /**
     * Close external connections
     * 
     * @throws JMSException
     */
    @PreDestroy
    public void releaseConnections() throws JMSException {
        LOGGER.debug("Tearing down: " + this.getClass().getCanonicalName());

        producer.close();
        jmsSession.close();
        connection.close();
        eventBus.unregister(this);
        session.logout();
    }
}
