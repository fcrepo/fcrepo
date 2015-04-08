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

import static java.util.Collections.singleton;
import static javax.jcr.observation.Event.NODE_ADDED;
import static org.fcrepo.jms.headers.DefaultMessageFactory.BASE_URL_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.CONTENT_DIGEST_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.CONTENT_SIZE_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.EVENT_TYPE_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.FIXITY_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.IDENTIFIER_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.PROPERTIES_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.TIMESTAMP_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.USER_AGENT_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.USER_HEADER_NAME;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.activemq.command.ActiveMQObjectMessage;
import org.apache.commons.lang.StringUtils;
import org.fcrepo.kernel.observer.FedoraEvent;
import org.fcrepo.kernel.observer.FixityEvent;
import org.fcrepo.kernel.utils.EventType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>DefaultMessageFactoryTest class.</p>
 *
 * @author ajs6f
 */
public class DefaultMessageFactoryTest {

    @Mock
    private Session mockSession;

    @Mock
    private FedoraEvent mockEvent;

    @Mock
    private FixityEvent mockFixityEvent;

    private DefaultMessageFactory testDefaultMessageFactory;

    @Before
    public void setUp() throws JMSException {
        initMocks(this);
        when(mockSession.createMessage()).thenReturn(
                new ActiveMQObjectMessage());
        testDefaultMessageFactory = new DefaultMessageFactory();
    }

    @Test
    public void testBuildMessage() throws RepositoryException, JMSException {
        final String testPath = "/path/to/resource";
        final String userAgent = "Test UserAgent (Like Mozilla)";
        final Message msg = doTestBuildMessage("base-url", "Test UserAgent", testPath);
        assertEquals("Got wrong identifier in message!", testPath, msg.getStringProperty(IDENTIFIER_HEADER_NAME));
    }

    @Test (expected = Exception.class)
    public void testBuildMessageException() throws RepositoryException, JMSException {
        doThrow(Exception.class).when(mockEvent).getUserData();
        testDefaultMessageFactory.getMessage(mockEvent, mockSession);
    }

    @Test
    public void testBuildMessageNullUrl() throws RepositoryException, JMSException {
        final String testPath = "/path/to/resource";
        final Message msg = doTestBuildMessage(null, null, testPath);
        assertEquals("Got wrong identifier in message!", testPath, msg.getStringProperty(IDENTIFIER_HEADER_NAME));
    }
    @Test
    public void testBuildMessageContent() throws RepositoryException, JMSException {
        final String testPath = "/path/to/resource";
        final Message msg = doTestBuildMessage("base-url/", "Test UserAgent", testPath + "/" + JCR_CONTENT);
        assertEquals("Got wrong identifier in message!", testPath, msg.getStringProperty(IDENTIFIER_HEADER_NAME));
    }

    @Test
    public void testBuildFixityMessageContent() throws RepositoryException, JMSException {
        when(mockFixityEvent.getDate()).thenReturn(1428335457057L);
        when(mockFixityEvent.getType()).thenReturn(4096);
        when(mockFixityEvent.getPath()).thenReturn("/83/0b/57/17/830b5717-1434-4653-af9c-a00d6d020426");
        when(mockFixityEvent.getIdentifier()).thenReturn("/83/0b/57/17/830b5717-1434-4653-af9c-a00d6d020426");
        when(mockFixityEvent.getBaseURL()).thenReturn("http://localhost:8080/rest/");
        when(mockFixityEvent.getUserID()).thenReturn("bypassAdmin");
        when(mockFixityEvent.getUserData())
                .thenReturn("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0");
        when(mockFixityEvent.getFixity()).thenReturn("SUCCESS");
        when(mockFixityEvent.getContentDigest()).thenReturn("urn:sha1:ca063d541865ab93d062f35800feb5011183abe6");
        when(mockFixityEvent.getContentSize()).thenReturn("56114^^http://www.w3.org/2001/XMLSchema#long");

        final Message message = mockSession.createMessage();
        message.setLongProperty(TIMESTAMP_HEADER_NAME, mockFixityEvent.getDate());
        message.setStringProperty(IDENTIFIER_HEADER_NAME, mockFixityEvent.getPath());
        message.setStringProperty(EVENT_TYPE_HEADER_NAME, "http://fedora.info/definitions/v4/repository#FIXITY");
        message.setStringProperty(BASE_URL_HEADER_NAME, mockFixityEvent.getBaseURL());
        message.setStringProperty(USER_HEADER_NAME, mockFixityEvent.getUserID());
        message.setStringProperty(USER_AGENT_HEADER_NAME, mockFixityEvent.getUserData());
        message.setStringProperty(FIXITY_HEADER_NAME, mockFixityEvent.getFixity());
        message.setStringProperty(CONTENT_DIGEST_HEADER_NAME, mockFixityEvent.getContentDigest());
        message.setStringProperty(CONTENT_SIZE_HEADER_NAME, mockFixityEvent.getContentSize());
        assertEquals(testDefaultMessageFactory.getFixityMessage(mockFixityEvent,mockSession),message);
    }

    private Message doTestBuildMessage(final String baseUrl, final String userAgent, final String id)
            throws RepositoryException, JMSException {
        final Long testDate = 46647758568747L;
        when(mockEvent.getDate()).thenReturn(testDate);

        String url = null;
        if (!StringUtils.isBlank(baseUrl) || !StringUtils.isBlank(userAgent)) {
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

        final Message msg = testDefaultMessageFactory.getMessage(mockEvent, mockSession);

        String trimmedBaseUrl = baseUrl;
        while (!StringUtils.isBlank(trimmedBaseUrl) && trimmedBaseUrl.endsWith("/")) {
            trimmedBaseUrl = trimmedBaseUrl.substring(0, trimmedBaseUrl.length() - 1);
        }

        assertEquals("Got wrong date in message!", testDate, (Long) msg.getLongProperty(TIMESTAMP_HEADER_NAME));
        assertEquals("Got wrong type in message!", testReturnType, msg.getStringProperty(EVENT_TYPE_HEADER_NAME));
        assertEquals("Got wrong base-url in message", trimmedBaseUrl, msg.getStringProperty(BASE_URL_HEADER_NAME));
        assertEquals("Got wrong property in message", prop, msg.getStringProperty(PROPERTIES_HEADER_NAME));
        assertEquals("Got wrong userID in message", testUser, msg.getStringProperty(USER_HEADER_NAME));
        assertEquals("Got wrong userAgent in message", userAgent, msg.getStringProperty(USER_AGENT_HEADER_NAME));
        return msg;
    }

}
