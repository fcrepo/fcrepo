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

package org.fcrepo.jms.legacy;

import static javax.jcr.observation.Event.NODE_ADDED;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.jcr.observation.Event;
import javax.jms.TextMessage;

import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.jms.legacy.LegacyMethodEventFactory;
import org.fcrepo.kernel.observer.FedoraEvent;
import org.junit.Before;
import org.junit.Test;

public class LegacyMethodEventFactoryTest {

    private LegacyMethodEventFactory testObj;

    @Before
    public void setUp() {
        testObj = new LegacyMethodEventFactory();
    }

    @Test
    public void testGetMessage() throws Exception {

        String testPath = "/foo/bar";
        javax.jms.Session mockJMS = mock(javax.jms.Session.class);
        TextMessage mockText = mock(TextMessage.class);
        when(mockJMS.createTextMessage(anyString())).thenReturn(mockText);
        Event mockEvent = mock(Event.class);
        when(mockEvent.getPath()).thenReturn(testPath);
        when(mockEvent.getType()).thenReturn(NODE_ADDED);
        when(mockEvent.getInfo()).thenReturn(
                Collections.singletonMap(
                        FedoraEvent.NODE_TYPE_KEY,
                        FedoraJcrTypes.FEDORA_OBJECT));
        testObj.getMessage(mockEvent, mockJMS);
        verify(mockText).setStringProperty("methodName", "ingest");
    }
}
