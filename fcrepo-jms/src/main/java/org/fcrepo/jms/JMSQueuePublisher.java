/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.jms;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;

/**
 * Machinery to publish JMS messages when an EventBus
 * message is received.
 *
 * @author barmintor
 * @author awoods
 */
public class JMSQueuePublisher extends AbstractJMSPublisher {

    private final String queueName;

    /**
     * Create a JMS Topic with the default name of "fedora"
     */
    public JMSQueuePublisher() {
        this("fedora");
    }

    /**
     * Create a JMS Topic with a configurable name
     *
     * @param queueName the name of the queue
     */
    public JMSQueuePublisher(final String queueName) {
        this.queueName = queueName;
    }

    protected Destination createDestination() throws JMSException {
        return jmsSession.createQueue(queueName);
    }
}
