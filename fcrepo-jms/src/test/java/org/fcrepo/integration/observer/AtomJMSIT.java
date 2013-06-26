
package org.fcrepo.integration.observer;

import static com.google.common.collect.ImmutableList.copyOf;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_OBJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.util.List;

import javax.inject.Inject;
import javax.jcr.ItemExistsException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.fcrepo.messaging.legacy.LegacyMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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

    static Parser parser = new Abdera().getParser();

    private volatile Entry entry;

    final private Logger logger = LoggerFactory.getLogger(AtomJMSIT.class);

    @Before
    public void acquireConnection() throws JMSException {
        logger.debug(this.getClass().getName() + " acquiring JMS connection.");
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(false, AUTO_ACKNOWLEDGE);
        consumer = session.createConsumer(session.createTopic("fedora"));
        consumer.setMessageListener(this);
    }

    @After
    public void releaseConnection() throws JMSException {
        logger.debug(this.getClass().getName() + " releasing JMS connection.");
        consumer.close();
        session.close();
        connection.close();
    }

    @Test
    public void testAtomStream() throws LoginException, RepositoryException,
        InterruptedException {
        Session session = repository.login();
        session.getRootNode().addNode("test1").addMixin(FEDORA_OBJECT);
        session.save();

        waitForEntry();

        session.logout();

        if (entry == null) fail("Waited a second, got no messages");
        List<Category> categories = copyOf(entry.getCategories("xsd:string"));
        final String title = entry.getTitle();
        entry = null;
        String path = null;
        for (Category cat : categories) {
            if (cat.getLabel().equals("fedora-types:pid")) {
                logger.debug("Found Category with term: " + cat.getTerm());
                path = cat.getTerm();
            }
        }
        assertEquals("Got wrong pid!", "test1", path);
        assertEquals("Got wrong method!", "ingest", title);
    }

    @Test
    public void testDatastreamTerm() throws NoSuchNodeTypeException,
        VersionException, ConstraintViolationException, LockException,
        ItemExistsException, PathNotFoundException, RepositoryException,
        InterruptedException {
        logger.trace("BEGIN: testDatastreamTerm()");
        Session session = repository.login();
        final Node object = session.getRootNode().addNode("testDatastreamTerm");
        object.addMixin(FEDORA_OBJECT);
        session.save();
        logger.trace("testDatastreamTerm called session.save()");

        waitForEntry();
        if (entry == null) fail("Waited a second, got no messages");
        List<Category> categories = copyOf(entry.getCategories("xsd:string"));
        entry = null;

        String path = null;

        logger.trace("Matched {} categories with scheme xsd:string", categories
                .size());
        for (Category cat : categories) {
            if (cat.getLabel().equals("fedora-types:pid")) {
                logger.debug("Found Category with term: " + cat.getTerm());
                path = cat.getTerm();
            }
        }
        assertEquals("Got wrong object PID!", "testDatastreamTerm", path);

        final Node ds = object.addNode("DATASTREAM");
        ds.addMixin(FEDORA_DATASTREAM);
        ds.addNode(JCR_CONTENT).setProperty(JCR_DATA, "fake data");
        session.save();
        logger.trace("testDatastreamTerm called session.save()");
        session.logout();
        logger.trace("testDatastreamTerm called session.logout()");

        waitForEntry();
        if (entry == null) fail("Waited a second, got no messages");
        categories = copyOf(entry.getCategories("xsd:string"));
        entry = null;

        logger.trace("Matched {} categories with scheme xsd:string", categories
                .size());
        for (Category cat : categories) {
            if (cat.getLabel().equals("fedora-types:dsID")) {
                logger.debug("Found Category with term: " + cat.getTerm());
                path = cat.getTerm();
            }
        }

        assertEquals("Got wrong datastream ID!", "DATASTREAM", path);
        logger.trace("END: testDatastreamTerm()");
    }

    @Override
    public void onMessage(Message message) {
        logger.debug("Received JMS message: " + message.toString());

        TextMessage tMessage = (TextMessage) message;
        try {
            if (LegacyMethod.canParse(message)) {
                LegacyMethod legacy = new LegacyMethod(tMessage.getText());
                entry = legacy.getEntry();
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

    private void waitForEntry() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            if (entry == null) { // must not have rec'vd event yet
                synchronized (this) {
                    this.wait(1000);
                }
            } else {
                break;
            }
        }
    }

}
