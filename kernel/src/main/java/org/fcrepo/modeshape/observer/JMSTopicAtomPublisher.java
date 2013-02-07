package org.fcrepo.modeshape.observer;

import static com.google.common.collect.Iterables.any;
import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;
import static org.apache.abdera.model.Text.Type.TEXT;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.LoginException;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.Event;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class JMSTopicAtomPublisher {

	@Inject
	EventBus eventBus;

	@Inject
	private Repository repo;

	@Inject
	private ActiveMQConnectionFactory connectionFactory;

	private Connection connection;
	private Session jmsSession;
	private MessageProducer producer;

	// Atom engine
	final static private Abdera abdera = new Abdera();

	// maps JCR mutations to Fedora Classic API method types
	private OperationsMappings operationsMappings;

	final private Logger logger = LoggerFactory
			.getLogger(JMSTopicAtomPublisher.class);

	@Subscribe
	public void publishJCREvent(Event jcrEvent) throws JMSException,
			RepositoryException, IOException {

		Entry entry = abdera.newEntry();

		entry.setTitle(operationsMappings.getFedoraMethodType(jcrEvent), TEXT)
				.setBaseUri("http://localhost:8080/rest");

		// assume that the PID is the last section of the node path
		String path = jcrEvent.getPath();
		String pid = path.substring(path.lastIndexOf('/') + 1, path.length());
		entry.addCategory("xsd:string", pid, "fedora-types:pid");

		StringWriter writer = new StringWriter();
		entry.writeTo(writer);
		String atomMessage = writer.toString();
		producer.send(jmsSession.createTextMessage(atomMessage));

		logger.debug("Put event: \n" + atomMessage + "\n onto JMS.");
	}

	@PostConstruct
	public void acquireConnections() throws JMSException, LoginException,
			RepositoryException {
		logger.debug("Initializing: " + this.getClass().getCanonicalName());

		operationsMappings = new OperationsMappings();

		connection = connectionFactory.createConnection();
		connection.start();
		jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		producer = jmsSession.createProducer(jmsSession.createTopic("fedora"));
		eventBus.register(this);
	}

	@PreDestroy
	public void releaseConnections() throws JMSException {
		logger.debug("Tearing down: " + this.getClass().getCanonicalName());

		operationsMappings.session.logout();
		
		producer.close();
		jmsSession.close();
		connection.close();
		eventBus.unregister(this);
	}

	// maps JCR mutations to Fedora Classic API method types
	final private class OperationsMappings {

		// this actor will never mutate the state of the repo,
		// so we keep the session live for efficiency
		private javax.jcr.Session session;

		public String getFedoraMethodType(Event jcrEvent)
				throws PathNotFoundException, RepositoryException {

			// we need to know if this is an object or a datastream
			Set<NodeType> nodeTypes = Sets.newHashSet(session.getNode(
					jcrEvent.getPath()).getMixinNodeTypes());

			// Now we can select from the combination of JCR Event type
			// and resource type to determine a Fedora Classic API method
			Integer eventType = jcrEvent.getType();

			if (any(nodeTypes, isObjectNodeType)) {
				switch (eventType) {
				case NODE_ADDED:
					return "ingest";
				case NODE_REMOVED:
					return "purgeObject";
				case PROPERTY_ADDED:
					return "modifyObject";
				case PROPERTY_CHANGED:
					return "modifyObject";
				case PROPERTY_REMOVED:
					return "modifyObject";
				}
			}
			if (any(nodeTypes, isDatastreamNodeType)) {
				switch (eventType) {
				case NODE_ADDED:
					return "addDatastream";
				case NODE_REMOVED:
					return "purgeDatastream";
				case PROPERTY_ADDED:
					return "modifyDatastream";
				case PROPERTY_CHANGED:
					return "modifyDatastream";
				case PROPERTY_REMOVED:
					return "modifyDatastream";
				}
			}
			return null;
		}

		private Predicate<NodeType> isObjectNodeType = new Predicate<NodeType>() {
			@Override
			public boolean apply(NodeType type) {
				return type.getName().equals("fedora:object");
			}
		};

		private Predicate<NodeType> isDatastreamNodeType = new Predicate<NodeType>() {
			@Override
			public boolean apply(NodeType type) {
				return type.getName().equals("fedora:datastream");
			}
		};

		OperationsMappings() throws LoginException, RepositoryException {
			session = repo.login();
		}

	}

}
