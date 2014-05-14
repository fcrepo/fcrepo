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
package org.fcrepo.kernel.observer;

import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.observation.Event;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>NOOPFilterTest class.</p>
 *
 * @author awoods
 */
public class NOOPFilterTest {

    @Mock
    Event mockEvent;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void testApply() throws Exception {
        assertTrue("Failed to pass an event through a NO-OP filter!", new NOOPFilter().getFilter(null).apply(mockEvent));
    }

}
