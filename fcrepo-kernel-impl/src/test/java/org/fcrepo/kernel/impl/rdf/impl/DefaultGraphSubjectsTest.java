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
package org.fcrepo.kernel.impl.rdf.impl;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * <p>DefaultGraphSubjectsTest class.</p>
 *
 * // TODO Maybe write some tests?
 *
 * @author awoods
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultGraphSubjectsTest {

    private DefaultIdentifierTranslator testObj;

    @Mock
    Node mockNode;

    @Mock
    Resource mockSubject;

    @Mock
    Session mockSession;

    @Before
    public void setUp() {
        testObj = new DefaultIdentifierTranslator(mockSession);
    }

    @Test
    public void test() {
        // TODO make test here!
    }

}
