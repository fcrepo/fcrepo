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
