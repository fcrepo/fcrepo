package org.fcrepo.modeshape.modules.audit;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.observation.Event;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

import com.google.common.eventbus.EventBus;


public class LogbackAuditorTest {

    private int jcrEventType = 1;

    private String jcrEventUserID = "jdoe";

    private String jcrEventPath = "/foo/bar";

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testEventAuditing() throws Exception {
        Logger root = (Logger) LoggerFactory.getLogger(LogbackAuditor.class);

        when(mockAppender.getName()).thenReturn("MockAppender");

        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn(jcrEventType);
        when(mockEvent.getUserID()).thenReturn(jcrEventUserID);
        when(mockEvent.getPath()).thenReturn(jcrEventPath);

        root.addAppender(mockAppender);

        EventBus eventBus = new EventBus("Test EventBus");
        Auditor auditor = new LogbackAuditor();
        eventBus.register(auditor);
        eventBus.post(mockEvent);

        verify(mockAppender).doAppend(
                (ILoggingEvent) argThat(new ArgumentMatcher<Object>() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains("jdoe node added /foo/bar");
            }
        }));
    }
}
