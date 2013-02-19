
package org.fcrepo.messaging.legacy;

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
import org.slf4j.LoggerFactory;

public class LegacyMethodEventFactory implements JMSEventMessageFactory {

    final private Logger logger = LoggerFactory
            .getLogger(LegacyMethodEventFactory.class);

    public LegacyMethodEventFactory() {
    }

    @Override
    public Message getMessage(Event jcrEvent, javax.jcr.Session jcrSession,
            javax.jms.Session jmsSession) throws RepositoryException,
            IOException, JMSException {
        logger.trace("Received an event to transform.");
        String path = jcrEvent.getPath();
        logger.trace("Retrieved path from event.");
        Node resource = jcrSession.getNode(path);
        logger.trace("Retrieved node from event.");
        LegacyMethod legacy = new LegacyMethod(jcrEvent, resource);
        StringWriter writer = new StringWriter();
        legacy.writeTo(writer);
        String atomMessage = writer.toString();
        logger.debug("Constructed serialized Atom message from event.");
        TextMessage tm = jmsSession.createTextMessage(atomMessage);
        String pid = legacy.getPid();
        if (pid != null) tm.setStringProperty("pid", pid);
        tm.setStringProperty("methodName", legacy.getMethodName());
        tm.setJMSType(LegacyMethod.FORMAT);
        tm.setStringProperty("fcrepo.server.version",
                LegacyMethod.SERVER_VERSION);
        logger.trace("Successfully created JMS message from event.");
        return tm;
    }

}
