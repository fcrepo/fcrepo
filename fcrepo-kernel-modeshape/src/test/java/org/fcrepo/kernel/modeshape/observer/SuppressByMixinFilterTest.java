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

import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_CONTAINER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.nodetype.NodeType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author escowles
 * @author ajs6f
 * @since 2015-04-15
 */
@RunWith(MockitoJUnitRunner.class)
public class SuppressByMixinFilterTest {

    private DefaultFilter testObj;

    @Mock
    private org.modeshape.jcr.api.observation.Event mockEvent;

    @Mock
    private NodeType fedoraContainer;

    @Mock
    private NodeType internalEvent;

    @Before
    public void setUp() {
        final Set<String> suppressedMixins = new HashSet<>();
        suppressedMixins.add("audit:InternalEvent");
        testObj = new SuppressByMixinFilter(suppressedMixins);
        when(fedoraContainer.toString()).thenReturn(FEDORA_CONTAINER);
        when(internalEvent.toString()).thenReturn("audit:InternalEvent");
    }

    @Test
    public void shouldSuppressMixin() throws Exception {
        when(mockEvent.getMixinNodeTypes()).thenReturn(new NodeType[] {fedoraContainer, internalEvent});
        assertFalse(testObj.test(mockEvent));
    }

    @Test
    public void shouldAllowOthers() throws Exception {
        when(mockEvent.getMixinNodeTypes()).thenReturn(new NodeType[] {fedoraContainer});
        assertTrue(testObj.test(mockEvent));
    }
}
