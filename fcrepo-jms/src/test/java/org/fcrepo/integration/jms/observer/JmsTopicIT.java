/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.jms.observer;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;

/**
 * <p>
 * JmsTopicIT class.
 * </p>
 *
 * @author acoburn
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({ "/spring-test/jms-topic.xml", "/spring-test/fcrepo-config.xml",
    "/spring-test/eventing.xml" })
@DirtiesContext
public class JmsTopicIT extends AbstractJmsIT {
    protected Destination createDestination() throws JMSException {
        return jmsSession.createTopic("fedora");
    }
}
