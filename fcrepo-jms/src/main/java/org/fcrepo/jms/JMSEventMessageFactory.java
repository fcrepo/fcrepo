/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.jms;

import javax.jms.JMSException;
import javax.jms.Message;

import org.fcrepo.kernel.api.observer.Event;

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
     * @throws JMSException if JMS exception occurred
     */
    Message getMessage(final Event jcrEvent,
            final javax.jms.Session jmsSession) throws JMSException;
}
