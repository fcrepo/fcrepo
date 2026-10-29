/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.jms;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.time.Instant.ofEpochMilli;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Set;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;

import org.apache.activemq.command.ActiveMQTextMessage;

import org.fcrepo.kernel.api.observer.Event;
import org.fcrepo.kernel.api.observer.EventType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <p>DefaultMessageFactoryTest class.</p>
 *
 * @author ajs6f
 */
@ExtendWith(MockitoExtension.class)
public class DefaultMessageFactoryTest {

    @Mock
    private Session mockSession;

    @Mock
    private Event mockEvent;

    private DefaultMessageFactory testDefaultMessageFactory;

    @BeforeEach
    public void setUp() {
        testDefaultMessageFactory = new DefaultMessageFactory();
    }

    @Test
    public void testBuildMessage() throws JMSException {
        final String testPath = "/path/to/resource";
        final Message msg = doTestBuildMessage("base-url", "Test UserAgent", testPath);
        assertEquals(testPath, msg.getStringProperty(testDefaultMessageFactory.getIdentifierHeaderName()),
                "Got wrong identifier in message!");
    }

    @Test
    public void testBuildMessageNullUrl() throws JMSException {
        final String testPath = "/path/to/resource";
        final Message msg = doTestBuildMessage(null, null, testPath);
        assertEquals(testPath, msg.getStringProperty(testDefaultMessageFactory.getIdentifierHeaderName()),
                "Got wrong identifier in message!");
    }

    /**
     * Artemis enforces the JMS spec rule that property names must be valid Java identifiers; ActiveMQ Classic does
     * not. A regression that re-introduced dotted names would slip past the other unit tests (which use an
     * ActiveMQ-Classic message) and only fail in the Artemis ITs. Asserting the constants here catches it at
     * unit-test time.
     */
    @Test
    public void testHeaderNamesAreValidJavaIdentifiersForArtemis() {
        final DefaultMessageFactory artemisFactory = new DefaultMessageFactory();
        artemisFactory.setJmsProvider("artemis");
        final List<String> headers = List.of(
                artemisFactory.getTimestampHeaderName(),
                artemisFactory.getIdentifierHeaderName(),
                artemisFactory.getEventTypeHeaderName(),
                artemisFactory.getBaseUrlHeaderName(),
                artemisFactory.getResourceTypeHeaderName(),
                artemisFactory.getUserHeaderName(),
                artemisFactory.getUserAgentHeaderName(),
                artemisFactory.getEventIdHeaderName());
        for (final String header : headers) {
            assertTrue(header.matches("[A-Za-z_$][A-Za-z0-9_$]*"),
                    "JMS header name must be a valid Java identifier (Artemis requirement): " + header);
        }
    }

    /**
     * ActiveMQ Classic does not enforce the JMS spec rule on property names, and existing downstream consumers
     * rely on the historical dotted form (e.g. {@code org.fcrepo.jms.timestamp}). This test pins that namespace
     * so a regression that flipped ActiveMQ headers to underscores would be caught at unit-test time.
     */
    @Test
    public void testHeaderNamesUseDottedNamespaceForActiveMq() {
        final DefaultMessageFactory activeMqFactory = new DefaultMessageFactory();
        activeMqFactory.setJmsProvider("activemq");
        final List<String> headers = List.of(
                activeMqFactory.getTimestampHeaderName(),
                activeMqFactory.getIdentifierHeaderName(),
                activeMqFactory.getEventTypeHeaderName(),
                activeMqFactory.getBaseUrlHeaderName(),
                activeMqFactory.getResourceTypeHeaderName(),
                activeMqFactory.getUserHeaderName(),
                activeMqFactory.getUserAgentHeaderName(),
                activeMqFactory.getEventIdHeaderName());
        for (final String header : headers) {
            assertTrue(header.startsWith("org.fcrepo.jms."),
                    "ActiveMQ JMS header name must use the dotted namespace 'org.fcrepo.jms.': " + header);
        }
    }

    private Message doTestBuildMessage(final String baseUrl, final String userAgent, final String id)
            throws JMSException {
        when(mockSession.createTextMessage(anyString())).thenReturn(new ActiveMQTextMessage());
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

        assertEquals(testDate, (Long) msg.getLongProperty(testDefaultMessageFactory.getTimestampHeaderName()),
                "Got wrong date in message!");
        assertEquals(testReturnType, msg.getStringProperty(testDefaultMessageFactory.getEventTypeHeaderName()),
                "Got wrong type in message!");
        assertEquals(trimmedBaseUrl, msg.getStringProperty(testDefaultMessageFactory.getBaseUrlHeaderName()),
                "Got wrong base-url in message");
        assertEquals(prop, msg.getStringProperty(testDefaultMessageFactory.getResourceTypeHeaderName()),
                "Got wrong resource type in message");
        assertEquals(testUser, msg.getStringProperty(testDefaultMessageFactory.getUserHeaderName()),
                "Got wrong userID in message");
        assertEquals(userAgent, msg.getStringProperty(testDefaultMessageFactory.getUserAgentHeaderName()),
                "Got wrong userAgent in message");
        assertEquals(eventID, msg.getStringProperty(testDefaultMessageFactory.getEventIdHeaderName()),
                "Got wrong eventID in message");
        return msg;
    }

}
