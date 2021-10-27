/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.jms;

/**
 * <p>JMSTopicPublisherTest class.</p>
 *
 * @author awoods
 */
public class JMSTopicPublisherTest extends AbstractJMSPublisherTest {
    protected AbstractJMSPublisher getPublisher() {
        return new JMSTopicPublisher();
    }
}
