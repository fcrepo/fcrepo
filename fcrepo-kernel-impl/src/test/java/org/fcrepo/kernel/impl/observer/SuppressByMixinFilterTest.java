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
package org.fcrepo.kernel.impl.observer;

import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_CONTAINER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * @author escowles
 * @since 2015-04-15
 */
public class SuppressByMixinFilterTest {

    private DefaultFilter testObj;

    @Mock
    private Session mockSession;

    @Mock
    private org.modeshape.jcr.api.observation.Event mockEvent;

    @Mock
    private NodeType fedoraContainer;

    @Mock
    private NodeType internalEvent;

    @Before
    public void setUp() {
        initMocks(this);
        final Set<String> suppressedMixins = new HashSet<>();
        suppressedMixins.add("audit:InternalEvent");
        testObj = new SuppressByMixinFilter(suppressedMixins);
        when(fedoraContainer.toString()).thenReturn(FEDORA_CONTAINER);
        when(internalEvent.toString()).thenReturn("audit:InternalEvent");
    }

    @Test
    public void shouldSuppressMixin() throws Exception {
        when(mockEvent.getMixinNodeTypes()).thenReturn(new NodeType[] {fedoraContainer, internalEvent});
        assertFalse(testObj.getFilter(mockSession).apply(mockEvent));
    }

    @Test
    public void shouldAllowOthers() throws Exception {
        when(mockEvent.getMixinNodeTypes()).thenReturn(new NodeType[] {fedoraContainer});
        assertTrue(testObj.getFilter(mockSession).apply(mockEvent));
    }
}
