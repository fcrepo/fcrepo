/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.jms.headers;

import static java.util.Collections.singleton;
import static javax.jcr.observation.Event.NODE_ADDED;
import static org.fcrepo.jms.headers.DefaultMessageFactory.EVENT_TYPE_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.IDENTIFIER_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.TIMESTAMP_HEADER_NAME;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.activemq.command.ActiveMQObjectMessage;
import org.fcrepo.kernel.observer.FedoraEvent;
import org.fcrepo.kernel.utils.EventType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DefaultMessageFactoryTest {

    @Mock
    private Session mockSession;

    @Mock
    private FedoraEvent mockEvent;

    private DefaultMessageFactory testDefaultMessageFactory;

    @Before
    public void setUp() throws JMSException {
        initMocks(this);
        when(mockSession.createMessage()).thenReturn(
                new ActiveMQObjectMessage());
        testDefaultMessageFactory = new DefaultMessageFactory();
    }

    @Test
    public void testBuildMessage() throws RepositoryException, IOException,
                                  JMSException {
        final Long testDate = 46647758568747L;
        when(mockEvent.getDate()).thenReturn(testDate);
        final String testPath = "super/calli/fragi/listic";
        when(mockEvent.getPath()).thenReturn(testPath);
        final Set<Integer> testTypes = singleton(NODE_ADDED);
        final String testReturnType =
            REPOSITORY_NAMESPACE + EventType.valueOf(NODE_ADDED).toString();
        when(mockEvent.getTypes()).thenReturn(testTypes);
        final Message testMessage =
            testDefaultMessageFactory.getMessage(mockEvent, mockSession);
        assertEquals("Got wrong date in message!", testDate, (Long) testMessage
                .getLongProperty(TIMESTAMP_HEADER_NAME));
        assertEquals("Got wrong identifier in message!", testPath, testMessage
                .getStringProperty(IDENTIFIER_HEADER_NAME));
        assertEquals("Got wrong type in message!", testReturnType, testMessage
                .getStringProperty(EVENT_TYPE_HEADER_NAME));
    }

}
