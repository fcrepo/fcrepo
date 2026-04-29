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
import static org.fcrepo.jms.DefaultMessageFactory.EVENT_TYPE_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.IDENTIFIER_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.RESOURCE_TYPE_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.TIMESTAMP_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.USER_AGENT_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.USER_HEADER_NAME;
import static org.fcrepo.jms.DefaultMessageFactory.EVENT_ID_HEADER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import org.apache.activemq.command.ActiveMQTextMessage;

import org.fcrepo.kernel.api.observer.Event;
import org.fcrepo.kernel.api.observer.EventType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies that {@link DefaultMessageFactory} populates the JMS message correctly using only the
 * standard {@link jakarta.jms.TextMessage} API. Each test runs against multiple {@link TextMessage}
 * implementations so we exercise both the ActiveMQ Classic message type and a strict
 * JMS-only in-memory implementation that mirrors Artemis's identifier-only property-name rule.
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

    static Stream<Arguments> textMessageProviders() {
        final Supplier<TextMessage> activeMqClassic = ActiveMQTextMessage::new;
        final Supplier<TextMessage> jmsStandard = InMemoryTextMessage::new;
        return Stream.of(
                Arguments.of("activemq-classic", activeMqClassic),
                Arguments.of("jms-standard (artemis-compatible)", jmsStandard));
    }

    @BeforeEach
    public void setUp() {
        testDefaultMessageFactory = new DefaultMessageFactory();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("textMessageProviders")
    public void testBuildMessage(final String label, final Supplier<TextMessage> messageFactory) throws JMSException {
        when(mockSession.createTextMessage(anyString())).thenReturn(messageFactory.get());
        final String testPath = "/path/to/resource";
        final Message msg = doTestBuildMessage("base-url", "Test UserAgent", testPath);
        assertEquals(testPath, msg.getStringProperty(IDENTIFIER_HEADER_NAME), "Got wrong identifier in message!");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("textMessageProviders")
    public void testBuildMessageNullUrl(final String label, final Supplier<TextMessage> messageFactory)
            throws JMSException {
        when(mockSession.createTextMessage(anyString())).thenReturn(messageFactory.get());
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
