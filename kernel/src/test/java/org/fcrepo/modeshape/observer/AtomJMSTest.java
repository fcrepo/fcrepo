package org.fcrepo.modeshape.observer;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.inject.Inject;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.ParseException;
import org.apache.abdera.parser.Parser;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.fcrepo.modeshape.AbstractTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.SystemFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({ "/spring-test/eventing.xml", "/spring-test/repo.xml" })
public class AtomJMSTest extends AbstractTest implements MessageListener {

	@Inject
	private Repository repository;

	@Inject
	private ActiveMQConnectionFactory connectionFactory;

	private Connection connection;
	private javax.jms.Session session;
	private MessageConsumer consumer;

	static Parser parser = new Abdera().getParser();
	private Entry entry;

	final private Logger logger = LoggerFactory.getLogger(AtomJMSTest.class);

	@Test
	public void testNodeCreation() throws LoginException, RepositoryException {
		Session session = repository.login();
		session.getRootNode().addNode("test1").addMixin("fedora:object");
		session.save();
		session.logout();
		while (entry == null)
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		List<Category> categories = entry.getCategories("xsd:string");
		String path = null;
		for (Category cat : categories) {
			if (cat.getLabel().equals("fedora-types:pid")) {
				logger.debug("Found Category with term: " + cat.getTerm());
				path = cat.getTerm();
			}
		}
		assertEquals("Got wrong pid!", "test1", path);
		assertEquals("Got wrong method!", "ingest", entry.getTitle());
	}

	@Override
	public void onMessage(Message message) {
		logger.debug("Received JMS message: " + message.toString());
		TextMessage tMessage = (TextMessage) message;
		try {
			entry = (Entry) parser.parse(
					new ByteArrayInputStream(tMessage.getText().getBytes(
							"UTF-8"))).getRoot();
			logger.debug("Parsed Entry: " + entry.toString());
		} catch (ParseException e) {
			throw new SystemFailureException(e);
		} catch (JMSException e) {
			throw new SystemFailureException(e);
		} catch (UnsupportedEncodingException e) {
			throw new SystemFailureException(e);
		}

	}

	@Before
	public void acquireConnection() throws JMSException {
		logger.debug(this.getClass().getName() + " acquiring JMS connection.");
		connection = connectionFactory.createConnection();
		connection.start();
		session = connection.createSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
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

}
