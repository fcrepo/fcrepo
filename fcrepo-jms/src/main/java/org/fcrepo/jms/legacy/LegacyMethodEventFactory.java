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

package org.fcrepo.jms.legacy;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.StringWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.fcrepo.jms.observer.JMSEventMessageFactory;
import org.slf4j.Logger;

/**
 * Create Fedora 3.x-like ATOM payloads for events
 */
public class LegacyMethodEventFactory implements JMSEventMessageFactory {

    private final Logger LOGGER = getLogger(LegacyMethodEventFactory.class);

    /**
     * Default event factory settings
     */
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
