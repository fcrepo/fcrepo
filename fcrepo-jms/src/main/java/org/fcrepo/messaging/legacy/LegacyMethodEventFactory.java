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

    @Override
	public Message getMessage(Event jcrEvent, javax.jcr.Session jcrSession, javax.jms.Session jmsSession)
			throws RepositoryException, IOException, JMSException {
        String path = jcrEvent.getPath();
        Node resource = jcrSession.getNode(path);
		LegacyMethod legacy = new LegacyMethod(jcrEvent, resource);
        StringWriter writer = new StringWriter();
        legacy.writeTo(writer);
        String atomMessage = writer.toString();
        TextMessage tm = jmsSession.createTextMessage(atomMessage);
        String pid = legacy.getPid();
        if (pid != null) tm.setStringProperty("pid", pid);
        tm.setStringProperty("methodName", legacy.getMethodName());
        tm.setJMSType(LegacyMethod.FORMAT);
        tm.setStringProperty("fcrepo.server.version", LegacyMethod.SERVER_VERSION);
        logger.debug("Put event: \n{}\n onto JMS.", atomMessage);
        return tm;
	}

}
