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

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.time.Instant.ofEpochMilli;
import static java.util.Collections.singleton;
import static org.fcrepo.jms.DefaultMessageFactory.BASE_URL_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.EVENT_TYPE_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.IDENTIFIER_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.RESOURCE_TYPE_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.TIMESTAMP_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.USER_AGENT_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.USER_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.EVENT_ID_HEADER_NAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Set;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.activemq.command.ActiveMQTextMessage;

import org.fcrepo.kernel.api.observer.Event;
import org.fcrepo.kernel.api.observer.EventType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * <p>DefaultMessageFactoryTest class.</p>
 *
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultMessageFactoryTest {

    @Mock
    private Session mockSession;

    @Mock
    private Event mockEvent;

    private DefaultMessageFactory testDefaultMessageFactory;

    @Before
    public void setUp() throws JMSException {
        when(mockSession.createTextMessage(anyString())).thenReturn(new ActiveMQTextMessage());
        testDefaultMessageFactory = new DefaultMessageFactory();
    }

    @Test
    public void testBuildMessage() throws JMSException {
        final String testPath = "/path/to/resource";
        final Message msg = doTestBuildMessage("base-url", "Test UserAgent", testPath);
        assertEquals("Got wrong identifier in message!", testPath, msg.getStringProperty(IDENTIFIER_HEADER_NAME));
    }

    @Test
    public void testBuildMessageNullUrl() throws JMSException {
        final String testPath = "/path/to/resource";
        final Message msg = doTestBuildMessage(null, null, testPath);
        assertEquals("Got wrong identifier in message!", testPath, msg.getStringProperty(IDENTIFIER_HEADER_NAME));
    }

    private Message doTestBuildMessage(final String baseUrl, final String userAgent, final String id)
            throws JMSException {
        final Long testDate = 46647758568747L;

        when(mockEvent.getBaseUrl()).thenReturn(baseUrl);
        when(mockEvent.getUserAgent()).thenReturn(userAgent);
        when(mockEvent.getDate()).thenReturn(ofEpochMilli(testDate));
        final String testUser = "testUser";
        when(mockEvent.getUserID()).thenReturn(testUser);
        when(mockEvent.getUserURI()).thenReturn(URI.create("http://localhost:8080/fcrepo/" + testUser));
        when(mockEvent.getPath()).thenReturn(id);
        final Set<EventType> testTypes = singleton(EventType.RESOURCE_CREATION);
        final String testReturnType = EventType.RESOURCE_CREATION.getType();
        when(mockEvent.getTypes()).thenReturn(testTypes);
        final String prop = "test-type";
        when(mockEvent.getResourceTypes()).thenReturn(singleton(prop));
        final String eventID = "abcdefg12345678";
        when(mockEvent.getEventID()).thenReturn(eventID);

        final Message msg = testDefaultMessageFactory.getMessage(mockEvent, mockSession);

        String trimmedBaseUrl = baseUrl;
        while (!isNullOrEmpty(trimmedBaseUrl) && trimmedBaseUrl.endsWith("/")) {
            trimmedBaseUrl = trimmedBaseUrl.substring(0, trimmedBaseUrl.length() - 1);
        }

        assertEquals("Got wrong date in message!", testDate, (Long) msg.getLongProperty(TIMESTAMP_HEADER_NAME));
        assertEquals("Got wrong type in message!", testReturnType, msg.getStringProperty(EVENT_TYPE_HEADER_NAME));
        assertEquals("Got wrong base-url in message", trimmedBaseUrl, msg.getStringProperty(BASE_URL_HEADER_NAME));
        assertEquals("Got wrong resource type in message", prop, msg.getStringProperty(RESOURCE_TYPE_HEADER_NAME));
        assertEquals("Got wrong userID in message", testUser, msg.getStringProperty(USER_HEADER_NAME));
        assertEquals("Got wrong userAgent in message", userAgent, msg.getStringProperty(USER_AGENT_HEADER_NAME));
        assertEquals("Got wrong eventID in message", eventID, msg.getStringProperty(EVENT_ID_HEADER_NAME));
        return msg;
    }

}
