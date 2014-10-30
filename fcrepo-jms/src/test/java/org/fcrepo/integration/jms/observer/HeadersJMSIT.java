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
package org.fcrepo.integration.jms.observer;

import static com.google.common.base.Throwables.propagate;
import static java.lang.System.currentTimeMillis;
import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_CONTAINER;
import static org.fcrepo.jms.headers.DefaultMessageFactory.BASE_URL_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.EVENT_TYPE_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.IDENTIFIER_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.PROPERTIES_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.TIMESTAMP_HEADER_NAME;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.fcrepo.kernel.utils.EventType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * <p>HeadersJMSIT class.</p>
 *
 * @author ajs6f
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/headers-jms.xml", "/spring-test/repo.xml",
        "/spring-test/eventing.xml"})
@DirtiesContext
public class HeadersJMSIT implements MessageListener {

    @Inject
    private Repository repository;

    @Inject
    private ActiveMQConnectionFactory connectionFactory;

    private Connection connection;

    private javax.jms.Session session;

    private MessageConsumer consumer;

    private volatile Set<Message> messages;

    private JcrTools jcrTools = new JcrTools(true);

    private static final Logger LOGGER = getLogger(HeadersJMSIT.class);

    /**
     * Time to wait for a set of test messages.
     */
    private static final long TIMEOUT = 2000;

    @Test
    public void testIngestion() throws RepositoryException,
                               InterruptedException, JMSException {

        final String pid = "/testIngestion";
        final String expectedEventType =
            REPOSITORY_NAMESPACE + EventType.valueOf(NODE_ADDED).toString();
        LOGGER.debug("Expecting a {} event", expectedEventType);
        Boolean success = false;

        final Session session = repository.login();
        try {
            final Node node = jcrTools.findOrCreateNode(session, pid);
            node.addMixin(FEDORA_CONTAINER);
            session.save();

            final Long start = currentTimeMillis();
            synchronized (this) {
                while ((currentTimeMillis() - start < TIMEOUT) && (!success)) {
                    for (final Message message : messages) {
                        if (getIdentifier(message).equals(pid)) {
                            if (getEventTypes(message).contains(expectedEventType)) {
                                success = true;
                            }
                        }
                    }
                    LOGGER.debug("Waiting for next message...");
                    wait(1000);
                }
            }
        } finally {
            session.logout();
        }
        assertTrue(
                "Found no message with correct identifer and correct event type!",
                success);
    }

    @Test
    public void testRemoval() throws RepositoryException, InterruptedException,
                             JMSException {

        final String pid = "/testRemoval";
        final String expectedEventType =
            REPOSITORY_NAMESPACE + EventType.valueOf(NODE_REMOVED).toString();
        LOGGER.debug("Expecting a {} event", expectedEventType);
        Boolean success = false;

        final Session session = repository.login();
        try {
            final Node node = jcrTools.findOrCreateNode(session, pid);
            node.addMixin(FEDORA_CONTAINER);
            session.save();
            node.remove();
            session.save();

            final Long start = currentTimeMillis();
            synchronized (this) {
                while ((currentTimeMillis() - start < TIMEOUT) && (!success)) {
                    for (final Message message : messages) {
                        if (getIdentifier(message).equals(pid)) {
                            if (getEventTypes(message).contains(expectedEventType)) {
                                success = true;
                            }
                        }
                    }
                    LOGGER.debug("Waiting for next message...");
                    wait(1000);
                }
            }
        } finally {
            session.logout();
        }
        assertTrue(
                "Found no message with correct identifer and correct event type!",
                success);
    }

    @Override
    public void onMessage(final Message message) {
        try {
            LOGGER.debug( "Received JMS message: {} with identifier: {}, timestamp: {}, event type: {}, properties: {},"
                + " and baseURL: {}", message.getJMSMessageID(), getIdentifier(message), getTimestamp(message),
                getEventTypes(message), getProperties(message), getBaseURL(message));
        } catch (final JMSException e) {
            propagate(e);
        }
        messages.add(message);
        synchronized (this) {
            this.notifyAll();
        }
    }

    @Before
    public void acquireConnection() throws JMSException {
        LOGGER.debug(this.getClass().getName() + " acquiring JMS connection.");
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(false, AUTO_ACKNOWLEDGE);
        consumer = session.createConsumer(session.createTopic("fedora"));
        messages = new HashSet<>();
        consumer.setMessageListener(this);
    }

    @After
    public void releaseConnection() throws JMSException {
        // ignore any remaining or queued messages
        consumer.setMessageListener(new NoopListener());
        // and shut the listening machinery down
        LOGGER.debug(this.getClass().getName() + " releasing JMS connection.");
        consumer.close();
        session.close();
        connection.close();
    }

    private static String getIdentifier(final Message msg) throws JMSException {
        final String id = msg.getStringProperty(IDENTIFIER_HEADER_NAME);
        LOGGER.debug("Processing an event with identifier: {}", id);
        return id;
    }

    private static String getEventTypes(final Message msg) throws JMSException {
        final String type = msg.getStringProperty(EVENT_TYPE_HEADER_NAME);
        LOGGER.debug("Processing an event with type: {}", type);
        return type;
    }

    private static Long getTimestamp(final Message msg) throws JMSException {
        return msg.getLongProperty(TIMESTAMP_HEADER_NAME);
    }

    private static String getBaseURL(final Message msg) throws JMSException {
        return msg.getStringProperty(BASE_URL_HEADER_NAME);
    }

    private static String getProperties(final Message msg) throws JMSException {
        return msg.getStringProperty(PROPERTIES_HEADER_NAME);
    }

}
