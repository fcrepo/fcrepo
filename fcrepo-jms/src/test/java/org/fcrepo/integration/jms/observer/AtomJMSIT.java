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

package org.fcrepo.integration.jms.observer;

import static com.google.common.collect.ImmutableList.copyOf;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_OBJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.util.HashSet;
import java.util.List;
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
import javax.jms.TextMessage;

import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.fcrepo.jms.legacy.LegacyMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/atom-jms.xml", "/spring-test/repo.xml",
        "/spring-test/eventing.xml"})
@DirtiesContext
public class AtomJMSIT implements MessageListener {

    @Inject
    private Repository repository;

    @Inject
    private ActiveMQConnectionFactory connectionFactory;

    private Connection connection;

    private javax.jms.Session session;

    private MessageConsumer consumer;

    private volatile Set<Entry> entries;

    final private Logger logger = LoggerFactory.getLogger(AtomJMSIT.class);

    @Before
    public void acquireConnection() throws JMSException {
        logger.debug(this.getClass().getName() + " acquiring JMS connection.");
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(false, AUTO_ACKNOWLEDGE);
        consumer = session.createConsumer(session.createTopic("fedora"));
        consumer.setMessageListener(this);
        entries = new HashSet<>();
    }

    @After
    public void releaseConnection() throws JMSException {
        logger.debug(this.getClass().getName() + " releasing JMS connection.");
        consumer.close();
        session.close();
        connection.close();
    }

    @Test
    public void testAtomStream() throws RepositoryException,
        InterruptedException {
        final Session session = repository.login();
        session.getRootNode().addNode("test1").addMixin(FEDORA_OBJECT);
        session.save();

        waitForEntry(1);
        session.logout();

        if (entries.isEmpty()) {
            fail("Waited a second, got no messages");
        }

        String title = null;
        String path = null;
        for (final Entry entry : entries) {
            final List<Category> categories = copyOf(entry.getCategories("xsd:string"));
            for (final Category cat : categories) {
                if (cat.getLabel().equals("fedora-types:pid")) {
                    logger.debug("Found Category with term: " + cat.getTerm());
                    path = cat.getTerm();
                    title = entry.getTitle();
                }
            }
        }
        assertEquals("Got wrong pid!", "test1", path);
        assertEquals("Got wrong method!", "ingest", title);
    }


    @Test
    public void testAtomStreamNodePath() throws RepositoryException,
        InterruptedException {
        final int minEntriesSize = 2;
        Session session = repository.login();
        final String testPath = "/test1/sigma";
        session.getRootNode().addNode(testPath.substring(1)).addMixin(FEDORA_OBJECT);
        session.save();

        waitForEntry(minEntriesSize);
        session.logout();

        if (entries.isEmpty()) {
            fail("Waited a second, got no messages");
        }

        String path = null;
        String title = null;
        assertEquals("Entries size not 2", entries.size(), 2);
        for (final Entry entry : entries) {
            final List<Category> categories = copyOf(entry.getCategories("xsd:string"));
            String p = null;
            for (final Category cat : categories) {
                if (cat.getLabel().equals("path")) {
                    logger.debug("Found Category with term: " + cat.getTerm());
                    p = cat.getTerm();
                }
            }
            if (testPath.equals(p)) {
                path = p;
                title = entry.getTitle();
            }
        }
        assertEquals("Got wrong path!", testPath, path);
        assertEquals("Got wrong title/method!", "ingest", title);
        entries.clear();
        path = null;
        title = null;
        session = repository.login();
        session.removeItem(testPath);
        session.save();
        waitForEntry(2);
        session.logout();
        if (entries.isEmpty()) {
            fail("Waited a second, got no messages");
        }

        // wait for both the parent update and the removal message
        assertEquals("Entries size not 2", entries.size(), 2);
        for (final Entry entry : entries) {
            final List<Category> categories = copyOf(entry.getCategories("xsd:string"));
            String p = null;
            for (final Category cat : categories) {
                if (cat.getLabel().equals("path")) {
                    logger.debug("Found Category with term: " + cat.getTerm());
                    p = cat.getTerm();
                }
            }
            if (p.equals(testPath)) {
                path = p;
                title = entry.getTitle();
            }
        }
        assertEquals("Got wrong path!", testPath, path);
        assertEquals("Got wrong title/method!", "purge", title);
    }

    @Test
    public void testDatastreamTerm() throws RepositoryException,
        InterruptedException {
        logger.trace("BEGIN: testDatastreamTerm()");
        final Session session = repository.login();
        final Node object = session.getRootNode().addNode("testDatastreamTerm");
        object.addMixin(FEDORA_OBJECT);
        session.save();
        logger.trace("testDatastreamTerm called session.save()");

        waitForEntry(1);
        if (entries.isEmpty()) {
            fail("Waited a second, got no messages");
        }

        String path = null;
        for (final Entry entry : entries) {
            final List<Category> categories = copyOf(entry.getCategories("xsd:string"));

            logger.trace("Matched {} categories with scheme xsd:string",
                         categories
                                 .size());
            for (final Category cat : categories) {
                if (cat.getLabel().equals("fedora-types:pid")) {
                    logger.debug("Found Category with term: " + cat.getTerm());
                    path = cat.getTerm();
                }
            }
        }
        entries.clear();
        assertEquals("Got wrong object PID!", "testDatastreamTerm", path);

        final Node ds = object.addNode("DATASTREAM");
        ds.addMixin(FEDORA_DATASTREAM);
        ds.addNode(JCR_CONTENT).setProperty(JCR_DATA, "fake data");
        session.save();
        logger.trace("testDatastreamTerm called session.save()");
        session.logout();
        logger.trace("testDatastreamTerm called session.logout()");

        waitForEntry(2);
        if (entries.isEmpty()) {
            fail("Waited a second, got no messages");
        }

        path = null;
        for (final Entry entry : entries) {
            final List<Category> categories = copyOf(entry.getCategories("xsd:string"));

            logger.trace("Matched {} categories with scheme xsd:string",
                         categories
                                 .size());
            for (final Category cat : categories) {
                if (cat.getLabel().equals("fedora-types:dsID")) {
                    logger.debug("Found Category with term: " + cat.getTerm());
                    path = cat.getTerm();
                }
            }
        }

        assertEquals("Got wrong datastream ID!", "DATASTREAM", path);
        logger.trace("END: testDatastreamTerm()");
    }

    @Override
    public void onMessage(final Message message) {
        logger.debug("Received JMS message: " + message.toString());

        final TextMessage tMessage = (TextMessage) message;
        try {
            if (LegacyMethod.canParse(message)) {
                final LegacyMethod legacy = new LegacyMethod(tMessage.getText());
                final Entry entry = legacy.getEntry();
                entries.add(entry);
                logger.debug("Parsed Entry: {}", entry.toString());
            } else {
                logger.warn("Could not parse message: {}", message);
            }
        } catch (final Exception e) {
            logger.error("Exception receiving message: {}", e);
            fail(e.getMessage());
        }
        synchronized (this) {
            this.notify();
        }
    }

    private void waitForEntry(final int size) throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            if (entries.size() < size) { // must not have rec'vd event yet
                synchronized (this) {
                    this.wait(1000);
                }
            } else {
                break;
            }
        }
    }

}
