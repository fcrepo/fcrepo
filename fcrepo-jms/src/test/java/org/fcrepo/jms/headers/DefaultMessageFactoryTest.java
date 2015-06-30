/**
 * Copyright 2015 DuraSpace, Inc.
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

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.singleton;
import static javax.jcr.observation.Event.NODE_ADDED;
import static org.fcrepo.jms.headers.DefaultMessageFactory.BASE_URL_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.EVENT_TYPE_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.IDENTIFIER_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.PROPERTIES_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.TIMESTAMP_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.USER_AGENT_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.USER_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.EVENT_ID_HEADER_NAME;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

import java.util.Set;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.activemq.command.ActiveMQObjectMessage;
import org.fcrepo.kernel.observer.FedoraEvent;
import org.fcrepo.kernel.utils.EventType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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
    private FedoraEvent mockEvent;

    private DefaultMessageFactory testDefaultMessageFactory;

    @Before
    public void setUp() throws JMSException {
        when(mockSession.createMessage()).thenReturn(new ActiveMQObjectMessage());
        testDefaultMessageFactory = new DefaultMessageFactory();
    }

    @Test
    public void testBuildMessage() throws JMSException {
        final String testPath = "/path/to/resource";
        final Message msg = doTestBuildMessage("base-url", "Test UserAgent", testPath);
        assertEquals("Got wrong identifier in message!", testPath, msg.getStringProperty(IDENTIFIER_HEADER_NAME));
    }

    @Test (expected = Exception.class)
    public void testBuildMessageException() throws JMSException {
        doThrow(Exception.class).when(mockEvent).getUserData();
        testDefaultMessageFactory.getMessage(mockEvent, mockSession);
    }

    @Test
    public void testBuildMessageNullUrl() throws JMSException {
        final String testPath = "/path/to/resource";
        final Message msg = doTestBuildMessage(null, null, testPath);
        assertEquals("Got wrong identifier in message!", testPath, msg.getStringProperty(IDENTIFIER_HEADER_NAME));
    }
    @Test
    public void testBuildMessageContent() throws JMSException {
        final String testPath = "/path/to/resource";
        final Message msg = doTestBuildMessage("base-url/", "Test UserAgent", testPath + "/" + JCR_CONTENT);
        assertEquals("Got wrong identifier in message!", testPath, msg.getStringProperty(IDENTIFIER_HEADER_NAME));
    }

    private Message doTestBuildMessage(final String baseUrl, final String userAgent, final String id)
            throws JMSException {
        final Long testDate = 46647758568747L;
        when(mockEvent.getDate()).thenReturn(testDate);

        String url = null;
        if (!isNullOrEmpty(baseUrl) || !isNullOrEmpty(userAgent)) {
            url = "{\"baseURL\":\"" + baseUrl + "\",\"userAgent\":\"" + userAgent + "\"}";
        }
        when(mockEvent.getUserData()).thenReturn(url);
        final String testUser = "testUser";
        when(mockEvent.getUserID()).thenReturn(testUser);
        when(mockEvent.getPath()).thenReturn(id);
        final Set<Integer> testTypes = singleton(NODE_ADDED);
        final String testReturnType = REPOSITORY_NAMESPACE + EventType.valueOf(NODE_ADDED).toString();
        when(mockEvent.getTypes()).thenReturn(testTypes);
        final String prop = "test-property";
        when(mockEvent.getProperties()).thenReturn(singleton(prop));
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
        assertEquals("Got wrong property in message", prop, msg.getStringProperty(PROPERTIES_HEADER_NAME));
        assertEquals("Got wrong userID in message", testUser, msg.getStringProperty(USER_HEADER_NAME));
        assertEquals("Got wrong userAgent in message", userAgent, msg.getStringProperty(USER_AGENT_HEADER_NAME));
        assertEquals("Got wrong eventID in message", eventID, msg.getStringProperty(EVENT_ID_HEADER_NAME));
        return msg;
    }

}
