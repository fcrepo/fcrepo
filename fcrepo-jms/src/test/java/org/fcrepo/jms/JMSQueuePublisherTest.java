/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.jms;

/**
 * <p>JMSQueuePublisherTest class.</p>
 *
 * @author acoburn
 */
public class JMSQueuePublisherTest extends AbstractJMSPublisherTest {
    protected AbstractJMSPublisher getPublisher() {
        return new JMSQueuePublisher();
    }
}
