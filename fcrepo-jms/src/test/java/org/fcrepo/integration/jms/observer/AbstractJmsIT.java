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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionTimeoutException;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.event.serialization.JsonLDEventMessage;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
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
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.ws.rs.core.UriBuilder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;

import static javax.jms.Session.AUTO_ACKNOWLEDGE;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.observer.EventType.INBOUND_REFERENCE;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_CREATION;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_DELETION;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_MODIFICATION;
import static org.junit.Assert.fail;
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

    private final String testIngested = "/testMessageFromIngestion-" + randomUUID();

    private final String testRemoved = "/testMessageFromRemoval-" + randomUUID();

    private final String testFile = "/testMessageFromFile-" + randomUUID() + "/file1";

    private final String testMeta = "/testMessageFromMetadata-" + randomUUID();

    private static final String USER = "fedoraAdmin";
    private static final String TEST_USER_AGENT = "FedoraClient/1.0";
    private static final String TEST_BASE_URL = "http://localhost:8080/rest";

    private static final ObjectMapper objectMapper = new ObjectMapper();

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

    @Autowired
    @Qualifier("referenceService")
    private ReferenceService referenceService;

    private Connection connection;

    protected Session jmsSession;

    private MessageConsumer consumer;

    private final Set<Message> messages = new CopyOnWriteArraySet<>();

    private static final Logger LOGGER = getLogger(AbstractJmsIT.class);

    protected abstract Destination createDestination() throws JMSException;

    private final HttpIdentifierConverter identifierConverter = new HttpIdentifierConverter(
            UriBuilder.fromUri(TEST_BASE_URL + "/{path: .*}"));

    @Test(timeout = TIMEOUT)
    public void testIngestion() {

        LOGGER.debug("Expecting a {} event", RESOURCE_CREATION.getType());
        final FedoraId fedoraId = FedoraId.create(testIngested);
        final String externalUri = identifierConverter.toExternalId(fedoraId.getFullId());

        doInTx(tx -> {
            createResourceService.perform(tx, USER, fedoraId,
                    null, createDefaultModel());
            tx.commit();
            awaitMessageOrFail(externalUri, RESOURCE_CREATION.getType(), null);
        });
    }

    @Test(timeout = TIMEOUT)
    public void testFileEvents() throws InvalidChecksumException {
        final var fedoraId = FedoraId.create(testFile);
        final var externalId = identifierConverter.toExternalId(fedoraId.getFullId());

        doInTx(tx -> {
            createResourceService.perform(tx, USER, fedoraId,
                    "text/plain", "file.txt", 3L,
                    List.of(), null, stream("foo"), null);
            tx.commit();
            awaitMessageOrFail(externalId, RESOURCE_CREATION.getType(), NON_RDF_SOURCE.toString());
        });

        doInTx(tx -> {
            replaceBinariesService.perform(tx, USER, fedoraId,
                    "file.txt", "text/plain", null,
                    stream("barney"), 6L, null);
            tx.commit();
            awaitMessageOrFail(externalId, RESOURCE_MODIFICATION.getType(), NON_RDF_SOURCE.toString());
        });

        doInTx(tx -> {
            final FedoraResource binaryResource = getResource(fedoraId);
            deleteResourceService.perform(tx, binaryResource, USER);
            tx.commit();
            awaitMessageOrFail(externalId, RESOURCE_DELETION.getType(), null);
        });
    }

    @Test(timeout = TIMEOUT)
    public void testMetadataEvents() {
        final var fedoraId = FedoraId.create(testMeta);
        final var externalId = identifierConverter.toExternalId(fedoraId.getFullId());

        doInTx(tx -> {
            createResourceService.perform(tx, USER, fedoraId, List.of(), createDefaultModel());
            final String sparql1 = "insert data { <> <http://foo.com/prop> \"foo\" . }";
            updatePropertiesService.updateProperties(tx, USER, fedoraId, sparql1);
            tx.commit();
            awaitMessageOrFail(externalId, RESOURCE_MODIFICATION.getType(), FEDORA_CONTAINER.getURI());
        });

        doInTx(tx -> {
            final String sparql2 = " delete { <> <http://foo.com/prop> \"foo\" . } "
                    + "insert { <> <http://foo.com/prop> \"bar\" . } where {}";
            updatePropertiesService.updateProperties(tx, USER, fedoraId, sparql2);
            tx.commit();
            awaitMessageOrFail(externalId, RESOURCE_MODIFICATION.getType(), FEDORA_RESOURCE.getURI());
        });
    }

    @Test(timeout = TIMEOUT)
    public void testRemoval() throws PathNotFoundException {
        final var fedoraId = FedoraId.create(testRemoved);
        final var externalId = identifierConverter.toExternalId(fedoraId.getFullId());

        LOGGER.debug("Expecting a {} event", RESOURCE_DELETION.getType());

        doInTx(tx -> {
            createResourceService.perform(tx, USER, fedoraId,
                    null, createDefaultModel());
            tx.commit();
        });

        doInTx(tx -> {
            final var resource = getResource(fedoraId);
            deleteResourceService.perform(tx, resource, USER);
            tx.commit();
            awaitMessageOrFail(externalId, RESOURCE_DELETION.getType(), null);
        });
    }

    @Test(timeout = TIMEOUT)
    public void testInboundReference() {
        final var id1 = FedoraId.create("/testInboundReference-" + randomUUID().toString());
        final var id2 = FedoraId.create("/testInboundReference-" + randomUUID().toString());
        final var externalId2 = identifierConverter.toExternalId(id2.getFullId());

        doInTx(tx -> {
            createResourceService.perform(tx, USER, id1, List.of(), createDefaultModel());
            createResourceService.perform(tx, USER, id2, List.of(), createDefaultModel());
            tx.commit();
        });

        doInTx(tx -> {
            final String sparql = "insert { <> <http://foo.com/prop> <" + id2.getFullId() + "> . } where {}";
            updatePropertiesService.updateProperties(tx, USER, id1, sparql);
            tx.commit();
            awaitMessageOrFail(externalId2, INBOUND_REFERENCE.getType(), null);
        });
    }

    @Test
    public void testInboundReferenceNoMessage() {
        final var id1 = FedoraId.create("/testInboundReference-" + randomUUID().toString());
        final var id2 = FedoraId.create("/testInboundReference-" + randomUUID().toString());
        final var externalId2 = identifierConverter.toExternalId(id2.getFullId());

        doInTx(tx -> {
            createResourceService.perform(tx, USER, id1, List.of(), createDefaultModel());
            tx.commit();
        });

        doInTx(tx -> {
            final String sparql = "insert { <> <http://foo.com/prop> <" + id2.getFullId() + "> . } where {}";
            updatePropertiesService.updateProperties(tx, USER, id1, sparql);
            tx.commit();
            awaitNoMessageOrFail(externalId2, INBOUND_REFERENCE.getType(), null);
        });
    }

    @Override
    public void onMessage(final Message message) {
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

    private void awaitMessageOrFail(final String id, final String eventType, final String resourceType) {
        await().pollInterval(ONE_HUNDRED_MILLISECONDS).until(() -> messages.stream().anyMatch(msg -> {
            try {
                return checkForMatchingMessage(msg, id, eventType, resourceType);
            } catch (final JMSException | JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    private void awaitNoMessageOrFail(final String id, final String eventType, final String resourceType) {
        try {
            await().atMost(new Duration(TIMEOUT, TimeUnit.MILLISECONDS)).until(() -> messages.stream().anyMatch(msg -> {
                try {
                    return checkForMatchingMessage(msg, id, eventType, resourceType);
                } catch (final JMSException | JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }));
            fail("Should not match the message");
        } catch (final ConditionTimeoutException exc) {
            // We don't want to match so this is a pass
        }
    }

    private static boolean checkForMatchingMessage(final Message msg, final String id, final String eventType,
                                                   final String resourceType)
            throws JMSException, JsonProcessingException {
        LOGGER.debug("Received msg: {}", msg);
        final String msgBody = ((TextMessage)msg).getText();
        final JsonLDEventMessage eventMsg = objectMapper.readValue(msgBody, JsonLDEventMessage.class);
        final Model model = decodeModel(eventMsg.object.id, msgBody);
        final String eventId = eventMsg.id;
        final String resourceId = eventMsg.object.id;
        final Resource expectedResource = createResource(id);
        final boolean resourceTypeComparison;
        if (resourceType != null) {
            resourceTypeComparison = model.contains(expectedResource, RDF.type, createResource(resourceType));
        } else {
            resourceTypeComparison = true;
        }
        return resourceId.equals(id) &&
                model.contains(createResource(eventId), RDF.type, createResource(eventType)) &&
                resourceTypeComparison;
    }

    /**
     * Decode message body into a graph.
     * @param id the base id of the graph.
     * @param msgBody the message body
     * @return the model from the message.
     */
    private static Model decodeModel(final String id, final String msgBody) {
        final Model model = createDefaultModel();
        model.read(new ByteArrayInputStream(msgBody.getBytes(UTF_8)), id, "JSON-LD");
        return model;
    }

    private void doInTx(final Consumer<Transaction> consumer) {
        final var tx = newTransaction();
        tx.setShortLived(true);
        try {
            consumer.accept(tx);
        } finally {
            tx.releaseResourceLocksIfShortLived();
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
        return new ByteArrayInputStream(value.getBytes(UTF_8));
    }

    private FedoraResource getResource(final FedoraId fedoraId) {
        try {
            return resourceFactory.getResource(fedoraId);
        } catch (final PathNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
