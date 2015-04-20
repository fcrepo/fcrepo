/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.jms.observer;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jms.JMSException;
import javax.jms.Message;

import org.fcrepo.kernel.observer.FedoraEvent;
import org.fcrepo.kernel.observer.FixityEvent;

/**
 * Produce a JMS Message from a JCR Event
 *
 * @author awoods
 */
public interface JMSEventMessageFactory {

    /**
     * Produce a JMS message from a JCR event with the
     * given session
     *
     * @param jcrEvent the jcr event
     * @param jmsSession the jms session
     * @return JMS message created from a JCR event
     * @throws RepositoryException if RepositoryException occurred
     * @throws IOException if IO exception occurred
     * @throws JMSException if JMS exception occurred
     */
    Message getMessage(final FedoraEvent jcrEvent,
            final javax.jms.Session jmsSession) throws RepositoryException,
        IOException, JMSException;

    /**
     *
     * @param fixityEvent
     * @param jmsSession
     * @return
     * @throws RepositoryException
     * @throws IOException
     * @throws JMSException
     */
    Message getFixityMessage(final FixityEvent fixityEvent,
                       final javax.jms.Session jmsSession) throws RepositoryException,
            IOException, JMSException;
}
