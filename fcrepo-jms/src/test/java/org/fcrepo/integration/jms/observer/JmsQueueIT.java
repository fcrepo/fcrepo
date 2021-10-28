/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.jms.observer;

import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.jms.Destination;
import javax.jms.JMSException;

/**
 * <p>
 * JmsQueueIT class.
 * </p>
 *
 * @author acoburn
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/spring-test/jms-queue.xml", "/spring-test/fcrepo-config.xml",
    "/spring-test/eventing.xml" })
@DirtiesContext
public class JmsQueueIT extends AbstractJmsIT {

    protected Destination createDestination() throws JMSException {
        return jmsSession.createQueue("fcrepo-queue");
    }
}
