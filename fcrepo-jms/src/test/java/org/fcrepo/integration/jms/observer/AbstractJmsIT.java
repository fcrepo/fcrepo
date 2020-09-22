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
package org.fcrepo.integration.jms.observer;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.services.CreateResourceService;
import org.fcrepo.kernel.api.services.DeleteResourceService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.kernel.api.services.ReplaceBinariesService;
import org.fcrepo.kernel.api.services.ReplacePropertiesService;
import org.fcrepo.kernel.api.services.UpdatePropertiesService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import static com.google.common.base.Strings.nullToEmpty;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static java.util.UUID.randomUUID;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.fcrepo.jms.DefaultMessageFactory.BASE_URL_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.EVENT_TYPE_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.IDENTIFIER_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.RESOURCE_TYPE_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.TIMESTAMP_HEADER_NAME;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.observer.EventType.INBOUND_REFERENCE;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_CREATION;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_DELETION;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_MODIFICATION;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>
 * AbstractJmsIT class.
 * </p>
 *
 * @author ajs6f
 */
abstract class AbstractJmsIT implements MessageListener {

    /**
     * Time to wait for a set of test messages, in milliseconds.
     */
    private static final long TIMEOUT = 20000;

    private String testIngested = "/testMessageFromIngestion-" + randomUUID();

    private String testRemoved = "/testMessageFromRemoval-" + randomUUID();

    private String testFile = "/testMessageFromFile-" + randomUUID() + "/file1";

    private String testMeta = "/testMessageFromMetadata-" + randomUUID();

    private static final String USER = "fedoraAdmin";
    private static final String TEST_USER_AGENT = "FedoraClient/1.0";
    private static final String TEST_BASE_URL = "http://localhost:8080/rest";

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    private TransactionManager txMananger;

    @Inject
    private CreateResourceService createResourceService;

    @Inject
    private ReplaceBinariesService replaceBinariesService;

    @Inject
    private UpdatePropertiesService updatePropertiesService;

    @Inject
    private ReplacePropertiesService replacePropertiesService;

    @Inject
    private DeleteResourceService deleteResourceService;

    @Inject
    private ActiveMQConnectionFactory connectionFactory;

    @Inject
    private ReferenceService referenceService;

    private Connection connection;

    protected Session jmsSession;

    private MessageConsumer consumer;

    private final Set<Message> messages = new CopyOnWriteArraySet<>();

    private static final Logger LOGGER = getLogger(AbstractJmsIT.class);

    protected abstract Destination createDestination() throws JMSException;

    @Test(timeout = TIMEOUT)
    public void testIngestion() {

        LOGGER.debug("Expecting a {} event", RESOURCE_CREATION.getType());

        doInTx(tx -> {
            createResourceService.perform(tx.getId(), USER, FedoraId.create(testIngested),
                    null, ModelFactory.createDefaultModel());
            tx.commit();
            awaitMessageOrFail(testIngested, RESOURCE_CREATION.getType(), null);
        });
    }

    @Test(timeout = TIMEOUT)
    public void testFileEvents() throws InvalidChecksumException {
        final var fedoraId = FedoraId.create(testFile);

        doInTx(tx -> {
            createResourceService.perform(tx.getId(), USER, fedoraId,
                    "text/plain", "file.txt", 3L,
                    List.of(), null, stream("foo"), null);
            tx.commit();
            awaitMessageOrFail(testFile, RESOURCE_CREATION.getType(), NON_RDF_SOURCE.toString());
        });

        doInTx(tx -> {
            replaceBinariesService.perform(tx.getId(), USER, fedoraId,
                    "file.txt", "text/plain", null,
                    stream("barney"), 6L, null);
            tx.commit();
            awaitMessageOrFail(testFile, RESOURCE_MODIFICATION.getType(), NON_RDF_SOURCE.toString());
        });

        doInTx(tx -> {
            final FedoraResource binaryResource = getResource(fedoraId);
            deleteResourceService.perform(tx, binaryResource, USER);
            tx.commit();
            awaitMessageOrFail(testFile, RESOURCE_DELETION.getType(), null);
        });
    }

    @Test(timeout = TIMEOUT)
    @Ignore("updatePropertiesService is not implemented")
    public void testMetadataEvents() {
        final IdentifierConverter<Resource,FedoraResource> subjects = null;

        doInTx(tx -> {
            // final FedoraResource resource1 = containerService.findOrCreate(tx, testMeta);
            final String sparql1 = "insert data { <> <http://foo.com/prop> \"foo\" . }";
            // updatePropertiesService.updateProperties(resource1, sparql1, resource1.getTriples(subjects,
            // PROPERTIES));
            tx.commit();
            awaitMessageOrFail(testMeta, RESOURCE_MODIFICATION.getType(), REPOSITORY_NAMESPACE + "Container");
        });

        doInTx(tx -> {
            // final FedoraResource resource2 = containerService.findOrCreate(tx, testMeta);
            final String sparql2 = " delete { <> <http://foo.com/prop> \"foo\" . } "
                    + "insert { <> <http://foo.com/prop> \"bar\" . } where {}";
            // updatePropertiesService.updateProperties(resource2, sparql2, resource2.getTriples(subjects,
            // PROPERTIES));
            tx.commit();
            awaitMessageOrFail(testMeta, RESOURCE_MODIFICATION.getType(), REPOSITORY_NAMESPACE + "Resource");
        });
    }

    @Test(timeout = TIMEOUT)
    public void testRemoval() throws PathNotFoundException {
        final var fedoraId = FedoraId.create(testRemoved);

        LOGGER.debug("Expecting a {} event", RESOURCE_DELETION.getType());

        doInTx(tx -> {
            createResourceService.perform(tx.getId(), USER, fedoraId,
                    null, ModelFactory.createDefaultModel());
            tx.commit();
        });

        doInTx(tx -> {
            final var resource = getResource(fedoraId);
            deleteResourceService.perform(tx, resource, USER);
            tx.commit();
            awaitMessageOrFail(testRemoved, RESOURCE_DELETION.getType(), null);
        });
    }

    @Test(timeout = TIMEOUT)
    @Ignore("updatePropertiesService is not implemented")
    public void testInboundReference() {
        final String uri1 = "/testInboundReference-" + randomUUID().toString();
        final String uri2 = "/testInboundReference-" + randomUUID().toString();

        final IdentifierConverter<Resource,FedoraResource> subjects = null;

        doInTx(tx -> {
            // final Container resource = containerService.findOrCreate(tx, uri1);
            // final Container resource2 = containerService.findOrCreate(tx, uri2);
            tx.commit();
        });

        doInTx(tx -> {
            // final Resource subject2 = subjects.reverse().convert(resource2);
            // final String sparql = "insert { <> <http://foo.com/prop> <" + subject2 + "> . } where {}";
            // updatePropertiesService.updateProperties(resource, sparql, resource.getTriples(subjects, PROPERTIES));
            tx.commit();
            awaitMessageOrFail(uri2, INBOUND_REFERENCE.getType(), null);
        });
    }

    @Override
    public void onMessage(final Message message) {
        try {
            LOGGER.debug(
                    "Received JMS message: {} with path: {}, timestamp: {}, event type: {}, properties: {},"
                            + " and baseURL: {}", message.getJMSMessageID(), getPath(message), getTimestamp(message),
                            getEventTypes(message), getResourceTypes(message), getBaseURL(message));
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
        messages.add(message);
    }

    @Before
    public void acquireConnection() throws JMSException {
        LOGGER.debug(this.getClass().getName() + " acquiring JMS connection.");
        connection = connectionFactory.createConnection();
        connection.start();
        jmsSession = connection.createSession(false, AUTO_ACKNOWLEDGE);
        consumer = jmsSession.createConsumer(createDestination());
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
        jmsSession.close();
        connection.close();
    }

    private void awaitMessageOrFail(final String id, final String eventType, final String type) {
        await().pollInterval(ONE_HUNDRED_MILLISECONDS).until(() -> messages.stream().anyMatch(msg -> {
            try {
                LOGGER.debug("Received msg: {}", msg);
                return getPath(msg).equals(id) && getEventTypes(msg).contains(eventType)
                        && getResourceTypes(msg).contains(nullToEmpty(type));
            } catch (final JMSException e) {
                throw new RuntimeException(e);
            }
        }));
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

    private static String getResourceTypes(final Message msg) throws JMSException {
        return msg.getStringProperty(RESOURCE_TYPE_HEADER_NAME);
    }

    private void doInTx(final Consumer<Transaction> consumer) {
        final var tx = newTransaction();
        try {
            consumer.accept(tx);
        } finally {
            tx.expire();
        }
    }

    private Transaction newTransaction() {
        final var tx = txMananger.create();
        tx.setBaseUri(TEST_BASE_URL);
        tx.setUserAgent(TEST_USER_AGENT);
        return tx;
    }

    private InputStream stream(final String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    private FedoraResource getResource(final FedoraId fedoraId) {
        try {
            return resourceFactory.getResource(fedoraId);
        } catch (final PathNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
