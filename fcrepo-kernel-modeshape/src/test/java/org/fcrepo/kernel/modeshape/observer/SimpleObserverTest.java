/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.observer;

import static org.fcrepo.kernel.modeshape.observer.SimpleObserver.EVENT_TYPES;
import static org.fcrepo.kernel.modeshape.utils.TestHelpers.setField;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;

import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.observer.FedoraEvent;
import org.fcrepo.kernel.modeshape.FedoraRepositoryImpl;
import org.fcrepo.kernel.modeshape.observer.eventmappings.OneToOne;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.modeshape.jcr.api.Repository;
import org.modeshape.jcr.api.observation.Event;

import com.google.common.eventbus.EventBus;

/**
 * <p>SimpleObserverTest class.</p>
 *
 * @author awoods
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleObserverTest {

    private SimpleObserver testObserver;

    private FedoraRepository testRepository;

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

    @Mock
    private NodeType mockNodeType, fedoraContainer;

    @Before
    public void setUp() throws RepositoryException {
        mockSession = mock(Session.class, Mockito.withSettings().extraInterfaces(org.modeshape.jcr.api.Session.class));
        when(mockRepository.login()).thenReturn((org.modeshape.jcr.api.Session) mockSession);
        when(mockEvents.hasNext()).thenReturn(true, false);
        when(mockEvents.next()).thenReturn(mockEvent);
        when(mockEvent.getType()).thenReturn(1);
        when(mockEvent.getPath()).thenReturn("/foo");
        when(mockEvent.getUserID()).thenReturn("userId");
        when(mockEvent.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getName()).thenReturn("nt:folder");
        when(mockEvent.getMixinNodeTypes()).thenReturn(new NodeType[] {fedoraContainer});
        when(fedoraContainer.getName()).thenReturn("fedora:Container");
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        when(mockWS.getNamespaceRegistry()).thenReturn(mockNS);
        testObserver = new SimpleObserver();
        setField(testObserver, "repository", new FedoraRepositoryImpl(mockRepository));
        setField(testObserver, "eventMapper", new OneToOne());
        setField(testObserver, "eventFilter", (EventFilter) x -> true);
        setField(testObserver, "eventBus", mockBus);
        setField(testObserver, "session", mockSession);
    }

    @Test
    public void testBuildListener() throws RepositoryException {
        when(mockWS.getObservationManager()).thenReturn(mockOM);
        testObserver.buildListener();
        verify(mockOM).addEventListener(testObserver, EVENT_TYPES, "/", true, null, null, false);
    }

    @Test
    public void testOnEvent() throws RepositoryException {
        testObserver.onEvent(mockEvents);
        verify(mockBus).post(any(FedoraEvent.class));
    }

    @Test
    public void testOnEventAllFiltered() {
        setField(testObserver, "eventFilter", (EventFilter) e -> false);
        testObserver.onEvent(mockEvents);
        verify(mockBus, never()).post(any(FedoraEvent.class));
    }
}
