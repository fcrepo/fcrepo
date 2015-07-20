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
package org.fcrepo.integration.jms.observer;

import static com.google.common.base.Throwables.propagate;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_SECOND;
import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.fcrepo.jms.headers.DefaultMessageFactory.BASE_URL_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.EVENT_TYPE_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.IDENTIFIER_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.PROPERTIES_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.TIMESTAMP_HEADER_NAME;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SIZE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.jgroups.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

import org.apache.activemq.ActiveMQConnectionFactory;

import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.utils.EventType;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.modeshape.rdf.impl.PropertiesRdfContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * <p>
 * HeadersJMSIT class.
 * </p>
 *
 * @author ajs6f
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/spring-test/headers-jms.xml", "/spring-test/repo.xml",
    "/spring-test/eventing.xml" })
@DirtiesContext
public class HeadersJMSIT implements MessageListener {

    /**
     * Time to wait for a set of test messages, in milliseconds.
     */
    private static final long TIMEOUT = 20000;

    private static final String testIngested = "/testMessageFromIngestion-" + randomUUID();

    private static final String testRemoved = "/testMessageFromRemoval-" + randomUUID();

    private static final String testFile = "/testMessageFromFile-" + randomUUID() + "/file1";

    private static final String testMeta = "/testMessageFromMetadata-" + randomUUID();

    private static final String NODE_ADDED_EVENT_TYPE
            = REPOSITORY_NAMESPACE + EventType.valueOf(NODE_ADDED).toString();
    private static final String NODE_REMOVED_EVENT_TYPE
            = REPOSITORY_NAMESPACE + EventType.valueOf(NODE_REMOVED).toString();
    private static final String PROP_ADDED_EVENT_TYPE
            = REPOSITORY_NAMESPACE + EventType.valueOf(PROPERTY_ADDED).toString();
    private static final String PROP_CHANGED_EVENT_TYPE
            = REPOSITORY_NAMESPACE + EventType.valueOf(PROPERTY_CHANGED).toString();
    private static final String PROP_REMOVED_EVENT_TYPE
            = REPOSITORY_NAMESPACE + EventType.valueOf(PROPERTY_REMOVED).toString();

    @Inject
    private Repository repository;

    @Inject
    private BinaryService binaryService;

    @Inject
    private ContainerService containerService;

    @Inject
    private ActiveMQConnectionFactory connectionFactory;

    private Connection connection;

    private javax.jms.Session session;

    private MessageConsumer consumer;

    private volatile Set<Message> messages = new HashSet<>();

    private static final Logger LOGGER = getLogger(HeadersJMSIT.class);

    @Test(timeout = TIMEOUT)
    public void testIngestion() throws RepositoryException {

        LOGGER.debug("Expecting a {} event", NODE_ADDED_EVENT_TYPE);

        final Session session = repository.login();
        try {
            containerService.findOrCreate(session, testIngested);
            session.save();
            awaitMessageOrFail(testIngested, NODE_ADDED_EVENT_TYPE, null);
        } finally {
            session.logout();
        }
    }

    @Test(timeout = TIMEOUT)
    public void testFileEvents() throws InvalidChecksumException, RepositoryException {

        final Session session = repository.login();

        try {
            binaryService.findOrCreate(session, testFile)
                .setContent(new ByteArrayInputStream("foo".getBytes()), "text/plain", null, null, null);
            session.save();
            awaitMessageOrFail(testFile, NODE_ADDED_EVENT_TYPE, HAS_SIZE.toString());

            binaryService.find(session, testFile)
                .setContent(new ByteArrayInputStream("bar".getBytes()), "text/plain", null, null, null);
            session.save();
            awaitMessageOrFail(testFile, PROP_CHANGED_EVENT_TYPE, HAS_SIZE.toString());

            binaryService.find(session, testFile).delete();
            session.save();
            awaitMessageOrFail(testFile, NODE_REMOVED_EVENT_TYPE, null);
        } finally {
            session.logout();
        }
    }

    @Test(timeout = TIMEOUT)
    public void testMetadataEvents() throws RepositoryException {

        final Session session = repository.login();
        final DefaultIdentifierTranslator subjects = new DefaultIdentifierTranslator(session);

        try {
            final FedoraResource resource1 = containerService.findOrCreate(session, testMeta);
            final String sparql1 = "insert data { <> <http://foo.com/prop> \"foo\" . }";
            resource1.updateProperties(subjects, sparql1, resource1.getTriples(subjects, PropertiesRdfContext.class));
            session.save();
            awaitMessageOrFail(testMeta, PROP_ADDED_EVENT_TYPE, "http://foo.com/prop");

            final FedoraResource resource2 = containerService.findOrCreate(session, testMeta);
            final String sparql2 = " delete { <> <http://foo.com/prop> \"foo\" . } "
                + "insert { <> <http://foo.com/prop> \"bar\" . } where {}";
            resource2.updateProperties(subjects, sparql2, resource2.getTriples(subjects, PropertiesRdfContext.class));
            session.save();
            awaitMessageOrFail(testMeta, PROP_CHANGED_EVENT_TYPE, "http://foo.com/prop");
        } finally {
            session.logout();
        }
    }

    private void awaitMessageOrFail(final String id, final String eventType, final String property) {
        await().pollInterval(ONE_SECOND).until(() -> messages.stream().anyMatch(msg -> {
            try {
                return getPath(msg).equals(id) && getEventTypes(msg).contains(eventType)
                        && (property == null || getProperties(msg).contains(property));
            } catch (final JMSException e) {
                throw propagate(e);
            }
        }));
    }

    @Test(timeout = TIMEOUT)
    public void testRemoval() throws RepositoryException {

        LOGGER.debug("Expecting a {} event", NODE_REMOVED_EVENT_TYPE);
        final Session session = repository.login();
        try {
            final Container resource = containerService.findOrCreate(session, testRemoved);
            session.save();
            resource.delete();
            session.save();
            awaitMessageOrFail(testRemoved, NODE_REMOVED_EVENT_TYPE, null);
        } finally {
            session.logout();
        }
    }

    @Override
    public void onMessage(final Message message) {
        try {
            LOGGER.debug(
                    "Received JMS message: {} with path: {}, timestamp: {}, event type: {}, properties: {},"
                            + " and baseURL: {}", message.getJMSMessageID(), getPath(message), getTimestamp(message),
                            getEventTypes(message), getProperties(message), getBaseURL(message));
        } catch (final JMSException e) {
            propagate(e);
        }
        messages.add(message);
    }

    @Before
    public void acquireConnection() throws JMSException {
        LOGGER.debug(this.getClass().getName() + " acquiring JMS connection.");
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(false, AUTO_ACKNOWLEDGE);
        consumer = session.createConsumer(session.createTopic("fedora"));
        messages.clear();
        consumer.setMessageListener(this);
    }

    @After
    public void releaseConnection() throws JMSException {
        // ignore any remaining or queued messages
        consumer.setMessageListener(msg -> { });
        // and shut the listening machinery down
        LOGGER.debug(this.getClass().getName() + " releasing JMS connection.");
        consumer.close();
        session.close();
        connection.close();
    }

    private static String getPath(final Message msg) throws JMSException {
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
