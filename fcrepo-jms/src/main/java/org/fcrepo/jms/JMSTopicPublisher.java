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

import javax.jms.Destination;
import javax.jms.JMSException;

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
    private JMSTopicPublisher(final String topicName) {
        this.topicName = topicName;
    }

    protected Destination createDestination() throws JMSException {
        return jmsSession.createTopic(topicName);
    }
}
