
package org.fcrepo.observer;

import static org.slf4j.LoggerFactory.getLogger;

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
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

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

    @PostConstruct
    public void acquireConnections() throws JMSException, LoginException,
            RepositoryException {
        LOGGER.debug("Initializing: " + this.getClass().getCanonicalName());

        connection = connectionFactory.createConnection();
        connection.start();
        jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        producer = jmsSession.createProducer(jmsSession.createTopic("fedora"));
        eventBus.register(this);

        session = repo.login();
    }

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
