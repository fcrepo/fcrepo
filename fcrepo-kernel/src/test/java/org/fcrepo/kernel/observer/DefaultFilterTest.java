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

package org.fcrepo.kernel.observer;

import static org.fcrepo.kernel.utils.FedoraTypesUtils.isFedoraDatastream;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isFedoraObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.reflect.Field;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.fcrepo.kernel.observer.DefaultFilter;
import org.fcrepo.kernel.utils.FedoraTypesUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.Repository;

import com.google.common.base.Predicate;

public class DefaultFilterTest {

    private DefaultFilter testObj;

    @Mock
    private Session mockSession;

    @Mock
    private Repository mockRepo;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new DefaultFilter();
        final Field repoF = DefaultFilter.class.getDeclaredField("repository");
        repoF.setAccessible(true);
        when(mockRepo.login()).thenReturn(mockSession);
        repoF.set(testObj, mockRepo);
        testObj.acquireSession();
    }

    @Test
    public void shouldApplyToObject() throws Exception {
        @SuppressWarnings("unchecked")
        final Predicate<Node> mockFuncTrue = mock(Predicate.class);
        when(mockFuncTrue.apply(any(Node.class))).thenReturn(true);
        @SuppressWarnings("unchecked")
        final Predicate<Node> mockFuncFalse = mock(Predicate.class);
        final Predicate<Node> holdDS = isFedoraDatastream;
        final Predicate<Node> holdO = isFedoraObject;

        try {
            FedoraTypesUtils.isFedoraDatastream = mockFuncFalse;
            FedoraTypesUtils.isFedoraObject = mockFuncTrue;
            final String testPath = "/foo/bar";
            final Event mockEvent = mock(Event.class);
            when(mockEvent.getPath()).thenReturn(testPath);
            final Node mockNode = mock(Node.class);
            when(mockSession.getItem(testPath)).thenReturn(mockNode);
            assertTrue(testObj.apply(mockEvent));
        } finally {
            isFedoraDatastream = holdDS;
            isFedoraObject = holdO;
        }
    }

    @Test
    public void shouldApplyToDatastream() throws Exception {
        @SuppressWarnings("unchecked")
        final Predicate<Node> mockFuncTrue = mock(Predicate.class);
        when(mockFuncTrue.apply(any(Node.class))).thenReturn(true);
        @SuppressWarnings("unchecked")
        final Predicate<Node> mockFuncFalse = mock(Predicate.class);
        final Predicate<Node> holdDS = isFedoraDatastream;
        final Predicate<Node> holdO = isFedoraObject;

        try {
            isFedoraDatastream = mockFuncFalse;
            isFedoraObject = mockFuncTrue;
            final String testPath = "/foo/bar";
            final Event mockEvent = mock(Event.class);
            when(mockEvent.getPath()).thenReturn(testPath);
            final Node mockNode = mock(Node.class);
            when(mockSession.getItem(testPath)).thenReturn(mockNode);
            assertTrue(testObj.apply(mockEvent));
        } finally {
            isFedoraDatastream = holdDS;
            isFedoraObject = holdO;
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotApplyToNonExistentNodes() throws Exception {

        final String testPath = "/foo/bar";
        final Event mockEvent = mock(Event.class);
        when(mockEvent.getPath()).thenReturn(testPath);
        when(mockSession.getItem(testPath)).thenThrow(
                PathNotFoundException.class);
        assertEquals(false, testObj.apply(mockEvent));
        verify(mockSession).getItem(testPath);
    }

    @Test
    public void shouldNotApplyToSystemNodes() throws Exception {
        @SuppressWarnings("unchecked")
        final Predicate<Node> mockFuncFalse = mock(Predicate.class);
        final Predicate<Node> holdDS = isFedoraDatastream;
        final Predicate<Node> holdO = isFedoraObject;

        try {
            isFedoraDatastream = mockFuncFalse;
            isFedoraObject = mockFuncFalse;
            final String testPath = "/foo/bar";
            final Event mockEvent = mock(Event.class);
            when(mockEvent.getPath()).thenReturn(testPath);
            final Node mockNode = mock(Node.class);
            when(mockSession.getItem(testPath)).thenReturn(mockNode);
            assertEquals(false, testObj.apply(mockEvent));
        } finally {
            isFedoraDatastream = holdDS;
            isFedoraObject = holdO;
        }
    }
}
