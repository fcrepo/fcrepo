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

package org.fcrepo.audit;

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

    private final int jcrEventType = 1;

    private final String jcrEventUserID = "jdoe";

    private final String jcrEventPath = "/foo/bar";

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testEventAuditing() throws Exception {
        final Logger root =
                (Logger) LoggerFactory.getLogger(LogbackAuditor.class);

        when(mockAppender.getName()).thenReturn("MockAppender");

        final Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn(jcrEventType);
        when(mockEvent.getUserID()).thenReturn(jcrEventUserID);
        when(mockEvent.getPath()).thenReturn(jcrEventPath);

        root.addAppender(mockAppender);

        final EventBus eventBus = new EventBus("Test EventBus");
        final Auditor auditor = new LogbackAuditor();
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
