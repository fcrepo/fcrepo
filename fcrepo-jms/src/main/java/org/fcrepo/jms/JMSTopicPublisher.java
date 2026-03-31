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
public class JMSTopicPublisher extends AbstractJMSPublisher {

    private final String topicName;

    /**
     * Create a JMS Topic with the default name of "fedora"
     */
    public JMSTopicPublisher() {
        this("fedora");
    }

    /**
     * Create a JMS Topic with a configurable name
     *
     * @param topicName the name of the topic
     */
    public JMSTopicPublisher(final String topicName) {
        this.topicName = topicName;
    }

    protected Destination createDestination() throws JMSException {
        return jmsSession.createTopic(topicName);
    }
}
