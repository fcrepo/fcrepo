/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.jms;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.time.Instant.ofEpochMilli;
import static java.util.Collections.singleton;
import static org.fcrepo.jms.DefaultMessageFactory.BASE_URL_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.EVENT_ID_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.EVENT_TYPE_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.IDENTIFIER_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.RESOURCE_TYPE_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.TIMESTAMP_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.USER_AGENT_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.USER_HEADER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.fcrepo.kernel.api.observer.Event;
import org.fcrepo.kernel.api.observer.EventType;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import java.net.URI;
import java.util.Set;

/**
 * <p>DefaultMessageFactoryTest class.</p>
 *
 * @author ajs6f
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class DefaultMessageFactoryTest {

    @Mock
    private Session mockSession;

    @Mock
    private Event mockEvent;

    private DefaultMessageFactory testDefaultMessageFactory;

    @BeforeEach
    public void setUp() throws JMSException {
        when(mockSession.createTextMessage(anyString())).thenReturn(new ActiveMQTextMessage());
        testDefaultMessageFactory = new DefaultMessageFactory();
    }

    @Test
    public void testBuildMessage() throws JMSException {
        final String testPath = "/path/to/resource";
        final Message msg = doTestBuildMessage("base-url", "Test UserAgent", testPath);
        assertEquals(testPath, msg.getStringProperty(IDENTIFIER_HEADER_NAME), "Got wrong identifier in message!");
    }

    @Test
    public void testBuildMessageNullUrl() throws JMSException {
        final String testPath = "/path/to/resource";
        final Message msg = doTestBuildMessage(null, null, testPath);
        assertEquals(testPath, msg.getStringProperty(IDENTIFIER_HEADER_NAME), "Got wrong identifier in message!");
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

        assertEquals(testDate, (Long) msg.getLongProperty(TIMESTAMP_HEADER_NAME), "Got wrong date in message!");
        assertEquals(testReturnType, msg.getStringProperty(EVENT_TYPE_HEADER_NAME), "Got wrong type in message!");
        assertEquals(trimmedBaseUrl, msg.getStringProperty(BASE_URL_HEADER_NAME), "Got wrong base-url in message");
        assertEquals(prop, msg.getStringProperty(RESOURCE_TYPE_HEADER_NAME), "Got wrong resource type in message");
        assertEquals(testUser, msg.getStringProperty(USER_HEADER_NAME), "Got wrong userID in message");
        assertEquals(userAgent, msg.getStringProperty(USER_AGENT_HEADER_NAME), "Got wrong userAgent in message");
        assertEquals(eventID, msg.getStringProperty(EVENT_ID_HEADER_NAME), "Got wrong eventID in message");
        return msg;
    }

}
