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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.fcrepo.jms.legacy.LegacyMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.syndication.feed.atom.Category;
import com.sun.syndication.feed.atom.Content;
import com.sun.syndication.feed.atom.Entry;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/jms.xml", "/spring-test/repo.xml",
        "/spring-test/eventing.xml"})
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
        Session session = repository.login();
        session.getRootNode().addNode("test1").addMixin(FEDORA_OBJECT);
        session.save();

        waitForEntry(1);
        session.logout();

        if (entries.isEmpty()) fail("Waited a second, got no messages");

        String title = null;
        String path = null;
        for (Entry entry : entries) {
            List<Category> categories = copyOf(entry.getCategories());
            if (categories == null) {
              logger.error("categories null");
            }
            String p = null;
            for (Category cat : categories) {
                if (cat.getLabel().equals("path")) {
                    logger.debug("Found Category with term: " + cat.getTerm());
                    path = cat.getTerm();
                    title = entry.getTitle();
                }
            }
        }
        assertEquals("Got wrong pid!", "/test1", path);
        assertEquals("Got wrong method!", "ingest", title);
    }
    

    @Test
    public void testAtomStreamNodePath() throws RepositoryException,
        InterruptedException {
        final int minEntriesSize = 2;
        Session session = repository.login();
        session.getRootNode().addNode("test1/sigma").addMixin(FEDORA_OBJECT);
        session.save();

        waitForEntry(minEntriesSize);
        session.logout();

        if (entries.isEmpty())
            fail("Waited a second, got no messages");

        String path = null;
        assertEquals("Entries size not 2", entries.size(), 2);
        for (Entry entry : entries) {
            List<Category> categories = copyOf(entry.getCategories());
            String p = null;
            for (Category cat : categories) {
                if (cat.getLabel().equals("path")) {
                    logger.debug("Found Category with term: " + cat.getTerm());
                    p = cat.getTerm();
                }
            }
            if (p.equals("/test1/sigma")) {
                path = p;
            }
        }
        assertEquals("Got wrong path!", "/test1/sigma", path);        
    }

    @Test
    public void testDatastreamTerm() throws RepositoryException,
        InterruptedException {
        logger.trace("BEGIN: testDatastreamTerm()");
        Session session = repository.login();
        final Node object = session.getRootNode().addNode("testDatastreamTerm");
        object.addMixin(FEDORA_OBJECT);
        session.save();
        logger.trace("testDatastreamTerm called session.save()");

        waitForEntry(1);
        if (entries.isEmpty()) fail("Waited a second, got no messages");

        String path = null;
        for (Entry entry : entries) {
            List<Category> categories = copyOf(entry.getCategories());

            logger.trace("Matched {} categories with scheme xsd:string",
                         categories
                                 .size());
            for (Category cat : categories) {
                if (cat.getLabel().equals("path")) {
                    logger.debug("Found Category with term: " + cat.getTerm());
                    path = cat.getTerm();
                }
            }
        }
        entries.clear();
        assertEquals("Got wrong object PID!", "/testDatastreamTerm", path);

        final Node ds = object.addNode("DATASTREAM");
        ds.addMixin(FEDORA_DATASTREAM);
        ds.addNode(JCR_CONTENT).setProperty(JCR_DATA, "fake data");
        session.save();
        logger.trace("testDatastreamTerm called session.save()");
        session.logout();
        logger.trace("testDatastreamTerm called session.logout()");

        waitForEntry(2);
        if (entries.isEmpty()) fail("Waited a second, got no messages");

        path = null;
        for (Entry entry : entries) {
            List<Category> categories = copyOf(entry.getCategories());

            logger.trace("Matched {} categories with scheme xsd:string",
                         categories
                                 .size());
            for (Category cat : categories) {
                List<Content> c = entry.getContents();
                if (!c.get(0).getValue().equals("DATASTREAM")) {
                   continue;
                }
                if (cat.getLabel().equals("path")) {
                    logger.debug("Found Category with term: " + cat.getTerm());
                    path = cat.getTerm();
                }
            }
        }

        assertEquals("Got wrong datastream ID!", "/testDatastreamTerm/DATASTREAM", path);
        logger.trace("END: testDatastreamTerm()");
    }

    @Override
    public void onMessage(Message message) {
        logger.debug("Received JMS message: " + message.toString());

        TextMessage tMessage = (TextMessage) message;
        try {
            if (LegacyMethod.canParse(message)) {
                LegacyMethod legacy = new LegacyMethod(tMessage.getText());
                Entry entry = legacy.getEntry();
                entries.add(entry);
                logger.debug("Parsed Entry: {}", entry.toString());
            } else {
                logger.warn("Could not parse message: {}", message);
            }
        } catch (Exception e) {
            logger.error("Exception receiving message: {}", e);
            fail(e.getMessage());
        }
        synchronized (this) {
            this.notify();
        }
    }

    private void waitForEntry(int size) throws InterruptedException {
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
