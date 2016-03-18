/*
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

import static com.google.common.collect.ImmutableMap.of;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.util.Collections.singleton;
import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static org.fcrepo.kernel.modeshape.observer.FedoraEventImpl.from;
import static org.fcrepo.kernel.modeshape.observer.FedoraEventImpl.valueOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertFalse;

import java.util.Map;
import java.util.Set;

import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.Event;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.observer.FedoraEvent;
import org.fcrepo.kernel.api.observer.EventType;

/**
 * <p>FedoraEventTest class.</p>
 *
 * @author ksclarke
 */
@RunWith(MockitoJUnitRunner.class)
public class FedoraEventImplTest {

    @Mock
    private NodeType mockPrimaryNodeType;

    @Mock
    private NodeType mockMixinNodeType;

    private FedoraEvent e;

    @Before
    public void setUp() {
        when(mockPrimaryNodeType.getName()).thenReturn("mockPrimaryNodeType");
        when(mockMixinNodeType.getName()).thenReturn("mockMixinNodeType");
        e = from(new TestEvent(1, "Path/Child", "UserId", "Identifier",
                of("1", "2"), "data", 0L, mockPrimaryNodeType, new NodeType[] { mockMixinNodeType }));
    }

    @SuppressWarnings("unused")
    @Test(expected = java.lang.NullPointerException.class)
    public void testWrapNullEvent() {
        final String path = null;
        final String userID = null;
        final String userData = null;
        final Map<String, String> info = null;
        new FedoraEventImpl(valueOf(1), path, userID, userData, 0L, info);
    }

    @Test
    public void testGetEventName() {
        assertEquals("node added", valueOf(NODE_ADDED).getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEvent() {
        valueOf(9999999);
    }

    @Test(expected = java.lang.NullPointerException.class)
    public void testWrapNullFedoraEvent() {
        from((Event)null);
    }

    @Test
    public void testGetType() {
        assertEquals(singleton(valueOf(1)), e.getTypes());
    }

    @Test
    public void testGetResourceType() {
        final Set<Resource> types = ImmutableSet.of(createResource("mockPrimaryNodeType"),
                createResource("mockMixinNodeType"));
        assertEquals(types, e.getResourceTypes());
    }

    @Test
    public void testGetPath() {
        assertEquals("Path/Child", e.getPath());
    }

    @Test
    public void testGetPathWithProperties() {
        final FedoraEvent e1 = from(new TestEvent(PROPERTY_CHANGED,
                    "Path/Child", "UserId", "Identifier", of("1", "2"),
                    "data", 0L, mockPrimaryNodeType, new NodeType[]{}));
        assertEquals("Path", e1.getPath());
    }

    @Test
    public void testGetPathWithTrailingJcrContent() {
        final FedoraEvent e1 = from(new TestEvent(1, "Path/jcr:content", "UserId",
                    "Identifier", of("1", "2"), "data", 0L, mockPrimaryNodeType, new NodeType[]{}));
        assertEquals("Path", e1.getPath());
    }

    @Test
    public void testGetPathWithHashUri() {
        final FedoraEvent e1 = from(new TestEvent(1, "Path/#/child", "UserId",
                    "Identifier", of("1", "2"), "data", 0L, mockPrimaryNodeType, new NodeType[]{}));
        assertEquals("Path#child", e1.getPath());
    }

    @Test
    public void testGetUserID() {
        assertEquals("UserId", e.getUserID());
    }

    @Test
    public void testGetEventID() {
        assertNotNull(e.getEventID());
    }

    @Test
    public void testGetUserData() {
        assertEquals("data", e.getUserData());
    }

    @Test
    public void testGetDate() {
        assertEquals(0L, e.getDate());
    }

    @Test
    public void testAddType() {
        final EventType type = valueOf(PROPERTY_CHANGED);
        e.addType(type);
        assertEquals(2, e.getTypes().size());

        assertTrue("Should contain: " + type, e.getTypes().contains(type));
        assertTrue("Should contain: NODE_ADDED", e.getTypes().contains(valueOf(1)));
    }

    @Test
    public void testToString() {
        final String text = e.toString();
        assertTrue("Should contain path: " + text, text.contains(e.getPath()));

        assertTrue("Should contain types: " + text, text.contains(e.getTypes().iterator().next().getName()));
        assertTrue("Should contain date: " + text, text.contains(Long.toString(e.getDate())));

        assertFalse("Should not contain user-data: " + text, text.contains(e.getUserData()));
        assertFalse("Should not contain user-id: " + text, text.contains(e.getUserID()));
    }

    private static class TestEvent implements org.modeshape.jcr.api.observation.Event {

        private final int type;

        private final String path;

        private final String user_id;

        private final String identifier;

        private final Map<String, String> info;

        private final String userData;

        private final long date;

        private final NodeType primaryType;

        private final NodeType[] mixinTypes;

        public TestEvent(final int type, final String path,
                final String user_id, final String identifier,
                final Map<String, String> info, final String userData,
                final long date, final NodeType primaryType, final NodeType[] mixinTypes) {
            this.type = type;
            this.path = path;
            this.user_id = user_id;
            this.identifier = identifier;
            this.info = info;
            this.userData = userData;
            this.date = date;
            this.primaryType = primaryType;
            this.mixinTypes = mixinTypes;
        }


        @Override
        public int getType() {
            return type;
        }


        @Override
        public String getPath() {
            return path;
        }


        @Override
        public String getUserID() {
            return user_id;
        }


        @Override
        public String getIdentifier() {
            return identifier;
        }


        @Override
        public Map<String, String> getInfo() {
            return info;
        }


        @Override
        public String getUserData() {
            return userData;
        }


        @Override
        public long getDate() {
            return date;
        }


        @Override
        public NodeType getPrimaryNodeType() {
            return primaryType;
        }


        @Override
        public NodeType[] getMixinNodeTypes() {
            return mixinTypes;
        }
    }
}
