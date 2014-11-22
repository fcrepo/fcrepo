/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.impl.observer;

import static org.fcrepo.kernel.impl.observer.SimpleObserver.EVENT_TYPES;
import static org.fcrepo.kernel.impl.utils.TestHelpers.setField;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.Mockito.mock;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;

import org.fcrepo.kernel.impl.observer.eventmappings.OneToOne;
import org.fcrepo.kernel.observer.EventFilter;
import org.fcrepo.kernel.observer.FedoraEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.modeshape.jcr.api.Repository;

import com.google.common.base.Predicate;
import com.google.common.eventbus.EventBus;

/**
 * <p>SimpleObserverTest class.</p>
 *
 * @author awoods
 */
public class SimpleObserverTest {

    private SimpleObserver testObserver;

    @Mock
    private ObservationManager mockOM;

    @Mock
    private Repository mockRepository;

    @Mock
    private Session mockSession;

    @Mock
    private Workspace mockWS;

    @Mock
    private NamespaceRegistry mockNS;

    @Mock
    private EventBus mockBus;

    @Mock
    private Event mockEvent;

    @Mock
    private EventIterator mockEvents;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        mockSession = mock(Session.class, Mockito.withSettings().extraInterfaces(org.modeshape.jcr.api.Session.class));
        when(mockRepository.login()).thenReturn((org.modeshape.jcr.api.Session) mockSession);
        when(mockEvents.hasNext()).thenReturn(true, false);
        when(mockEvents.next()).thenReturn(mockEvent);
        testObserver = new SimpleObserver();
        setField(testObserver, "repository", mockRepository);
        setField(testObserver, "eventMapper", new OneToOne());
        setField(testObserver, "eventFilter", new NOOPFilter());
        setField(testObserver, "eventBus", mockBus);
        setField(testObserver, "session", mockSession);
    }

    @Test
    public void testBuildListener() throws Exception {
        when(mockWS.getObservationManager()).thenReturn(mockOM);
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        testObserver.buildListener();
        verify(mockOM).addEventListener(testObserver, EVENT_TYPES, "/", true, null, null, false);
    }

    @Test
    public void testOnEvent() throws Exception {
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        when(mockWS.getNamespaceRegistry()).thenReturn(mockNS);
        testObserver.onEvent(mockEvents);
        verify(mockBus).post(any(FedoraEvent.class));
    }

    @Test
    public void testOnEventAllFiltered() {
        setField(testObserver, "eventFilter", new NoPassFilter());
        testObserver.onEvent(mockEvents);
        verify(mockBus, never()).post(any(FedoraEvent.class));
    }

    private class NoPassFilter implements EventFilter {

        @Override
        public boolean apply(final Event input) {
            return false;
        }

        @Override
        public Predicate<Event> getFilter(final Session session) {
            return this;
        }}

}
