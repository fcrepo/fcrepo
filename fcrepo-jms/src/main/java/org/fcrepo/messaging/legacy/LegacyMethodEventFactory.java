
package org.fcrepo.messaging.legacy;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.StringWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.fcrepo.observer.JMSEventMessageFactory;
import org.slf4j.Logger;

public class LegacyMethodEventFactory implements JMSEventMessageFactory {

    private final Logger LOGGER = getLogger(LegacyMethodEventFactory.class);

    public LegacyMethodEventFactory() {
    }

    @Override
    public Message getMessage(final Event jcrEvent,
            final javax.jcr.Session jcrSession,
            final javax.jms.Session jmsSession) throws RepositoryException,
            IOException, JMSException {
        LOGGER.trace("Received an event to transform.");
        final String path = jcrEvent.getPath();
        LOGGER.trace("Retrieved path from event.");
        final Node resource = jcrSession.getNode(path);
        LOGGER.trace("Retrieved node from event.");
        final LegacyMethod legacy = new LegacyMethod(jcrEvent, resource);
        final StringWriter writer = new StringWriter();
        legacy.writeTo(writer);
        final String atomMessage = writer.toString();
        LOGGER.debug("Constructed serialized Atom message from event.");
        final TextMessage tm = jmsSession.createTextMessage(atomMessage);
        final String pid = legacy.getPid();
        if (pid != null) {
            tm.setStringProperty("pid", pid);
        }
        tm.setStringProperty("methodName", legacy.getMethodName());
        tm.setJMSType(EntryFactory.FORMAT);
        tm.setStringProperty("fcrepo.server.version",
        		EntryFactory.SERVER_VERSION);
        LOGGER.trace("Successfully created JMS message from event.");
        return tm;
    }

}
