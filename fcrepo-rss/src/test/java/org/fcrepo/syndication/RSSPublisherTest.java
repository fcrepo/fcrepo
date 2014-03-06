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
package org.fcrepo.syndication;

import static org.fcrepo.kernel.utils.TestHelpers.setField;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.observer.FedoraEvent;
import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.EventBus;

public class RSSPublisherTest {

    private RSSPublisher testObj;

    @Before
    public void setUp() {
        testObj = new RSSPublisher();

    }

    @Test
    public void testGetFeed() throws Exception {
        final EventBus mockBus = mock(EventBus.class);
        setField(testObj, "eventBus", mockBus);
        final UriInfo mockUris = mock(UriInfo.class);
        final URI mockUri = new URI("http://localhost.info");
        when(mockUris.getBaseUri()).thenReturn(mockUri);
        setField(testObj, "uriInfo", mockUris);
        testObj.initialize();
        testObj.getFeed();
    }

    @Test
    public void testInitialize() throws Exception {
        final EventBus mockBus = mock(EventBus.class);
        setField(testObj, "eventBus", mockBus);
        testObj.initialize();
        verify(mockBus).register(testObj);
    }

    @Test
    public void testNewEvent() {
        final FedoraEvent mockEvent = mock(FedoraEvent.class);
        testObj.newEvent(mockEvent);
    }

}
