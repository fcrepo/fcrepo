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
package org.fcrepo.kernel.modeshape.observer;

import org.fcrepo.kernel.api.observer.FedoraEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;

import java.util.Set;

import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.PERSIST;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_TOMBSTONE;
import static org.fcrepo.kernel.api.FedoraJcrTypes.JCR_MIXIN_TYPES;
import static org.fcrepo.kernel.api.FedoraJcrTypes.LDP_BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.JCR_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Andrew Woods
 * @author ajs6f
 *         Date: 11/22/14
 */
public class GetNamespacedPropertiesTest {

    private GetNamespacedProperties function;

    @Mock
    private Session session;

    @Mock
    private NamespaceRegistry namespaceRegistry;

    @Mock
    private Event event;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(event.getType()).thenReturn(PERSIST);

        final Workspace workspace = Mockito.mock(Workspace.class);
        when(session.getWorkspace()).thenReturn(workspace);

        when(workspace.getNamespaceRegistry()).thenReturn(namespaceRegistry);

        when(namespaceRegistry.getURI("fedora")).thenReturn(REPOSITORY_NAMESPACE);
        when(namespaceRegistry.getURI("ldp")).thenReturn(LDP_NAMESPACE);
        when(namespaceRegistry.getURI("jcr")).thenReturn(JCR_NAMESPACE);

        function = new GetNamespacedProperties(session);
    }

    @Test
    public void testApply() {
        final FedoraEvent fedoraEvent = new FedoraEvent(event);

        fedoraEvent.addProperty(FEDORA_CONTAINER);
        fedoraEvent.addProperty(FEDORA_TOMBSTONE);
        fedoraEvent.addProperty(LDP_BASIC_CONTAINER);
        fedoraEvent.addProperty(JCR_MIXIN_TYPES);

        fedoraEvent.addType(PROPERTY_ADDED);
        fedoraEvent.addType(NODE_ADDED);

        // Perform test
        final FedoraEvent result = function.apply(fedoraEvent);
        assertNotNull(result);

        // Verify types
        final Set<Integer> types = result.getTypes();
        assertEquals(3, types.size());

        assertTrue("Should contain: " + PROPERTY_ADDED + ", " + types, types.contains(PROPERTY_ADDED));
        assertTrue("Should contain: " + NODE_ADDED + ", " + types, types.contains(NODE_ADDED));
        assertTrue("Should contain: " + PERSIST + ", " + types, types.contains(PERSIST));

        // Verify properties
        final Set<String> properties = result.getProperties();
        assertEquals(4, properties.size());

        final String expected1 = FEDORA_CONTAINER.replace("fedora:", REPOSITORY_NAMESPACE);
        final String expected2 = FEDORA_TOMBSTONE.replace("fedora:", REPOSITORY_NAMESPACE);
        final String expected3 = LDP_BASIC_CONTAINER.replace("ldp:", LDP_NAMESPACE);
        final String expected4 = JCR_MIXIN_TYPES.replace("jcr:", REPOSITORY_NAMESPACE);

        assertTrue("Should contain: " + expected1 + ", " + properties, properties.contains(expected1));
        assertTrue("Should contain: " + expected2 + ", " + properties, properties.contains(expected2));
        assertTrue("Should contain: " + expected3 + ", " + properties, properties.contains(expected3));
        assertTrue("Should contain: " + expected4 + ", " + properties, properties.contains(expected4));
    }

}
