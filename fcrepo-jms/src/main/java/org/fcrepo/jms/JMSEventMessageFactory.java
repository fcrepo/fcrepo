/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.jms;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

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
            final jakarta.jms.Session jmsSession) throws JMSException;
}
