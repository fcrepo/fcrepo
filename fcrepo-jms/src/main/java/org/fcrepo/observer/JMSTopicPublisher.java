
package org.fcrepo.observer;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.LoginException;
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
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class JMSTopicPublisher {
	
    @Inject
    EventBus eventBus;

    @Inject
    private Repository repo;

    @Inject
    private ActiveMQConnectionFactory connectionFactory;
    
    @Inject
    private JMSEventMessageFactory eventFactory;

    private Connection connection;

    private Session jmsSession;

    private MessageProducer producer;

    final private Logger logger = LoggerFactory
            .getLogger(JMSTopicPublisher.class);

    private javax.jcr.Session session;

    @Subscribe
    public void publishJCREvent(Event fedoraEvent) throws JMSException,
            RepositoryException, IOException {
        logger.debug("Received an event from the internal bus.");
        Message tm = eventFactory.getMessage(fedoraEvent, session, jmsSession);
        logger.debug("Transformed the event to a JMS message.");
        producer.send(tm);

        logger.debug("Put event: \n{}\n onto JMS.", tm.getJMSMessageID());
    }

    @PostConstruct
    public void acquireConnections() throws JMSException, LoginException,
            RepositoryException {
        logger.debug("Initializing: " + this.getClass().getCanonicalName());

        connection = connectionFactory.createConnection();
        connection.start();
        jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        producer = jmsSession.createProducer(jmsSession.createTopic("fedora"));
        eventBus.register(this);

        session = repo.login();
    }

    @PreDestroy
    public void releaseConnections() throws JMSException {
        logger.debug("Tearing down: " + this.getClass().getCanonicalName());

        producer.close();
        jmsSession.close();
        connection.close();
        eventBus.unregister(this);
        session.logout();
    }
}
