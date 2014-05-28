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

import static com.google.common.base.Predicates.alwaysFalse;
import static com.google.common.base.Predicates.alwaysTrue;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isFedoraDatastream;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isFedoraObject;
import static java.util.UUID.randomUUID;
import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.Repository;

import com.google.common.base.Predicate;

/**
 * @author ajs6f
 * @since 2013
 */
public class DefaultFilterTest {

    private DefaultFilter testObj;

    @Mock
    private Session mockSession;

    @Mock
    private Repository mockRepo;

    @Mock
    private Event mockEvent;

    @Mock
    private Node mockNode;

    @Mock
    private Property mockProperty;

    private final static String testId = randomUUID().toString();

    private final static String testPath = "/foo/bar";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(mockEvent.getPath()).thenReturn(testPath);
        when(mockEvent.getIdentifier()).thenReturn(testId);
        when(mockEvent.getType()).thenReturn(NODE_ADDED);
        when(mockNode.isNode()).thenReturn(true);
        testObj = new DefaultFilter();
        when(mockRepo.login()).thenReturn(mockSession);
    }

    @Test
    public void shouldApplyToObjectOrDatastream() throws Exception {
        when(mockSession.getItem(testPath)).thenReturn(mockNode);
        final Predicate<Node> holdO = isFedoraObject;
        try {
            isFedoraObject = alwaysTrue();
            assertTrue(testObj.getFilter(mockSession).apply(mockEvent));
        } finally {
            isFedoraObject = holdO;
        }
    }

    @Test
    public void shouldNotApplyToNonExistentNodes() throws Exception {
        when(mockSession.getNodeByIdentifier(testId)).thenThrow(
                new PathNotFoundException("Expected."));
        assertFalse(testObj.getFilter(mockSession).apply(mockEvent));
    }

    @Test
    public void shouldNotApplyToNonFedoraNodes() throws Exception {
        when(mockSession.getNode(testPath)).thenReturn(mockNode);
        final Predicate<Node> holdO = isFedoraObject;
        final Predicate<Node> hold1 = isFedoraDatastream;
        try {
            isFedoraObject = alwaysFalse();
            isFedoraDatastream = alwaysFalse();
            assertFalse(testObj.getFilter(mockSession).apply(mockEvent));
        } finally {
            isFedoraObject = holdO;
            isFedoraDatastream = hold1;
        }
    }

    @Test(expected = RuntimeException.class)
    public void testBadItem() throws RepositoryException {
        when(mockEvent.getType()).thenReturn(PROPERTY_ADDED);
        when(mockSession.getNode("/foo")).thenThrow(
                new RepositoryException("Expected."));
        when(mockProperty.isNode()).thenReturn(false);
        testObj.getFilter(mockSession).apply(mockEvent);
    }

    @Test
    public void testProperty() throws RepositoryException {
        when(mockProperty.isNode()).thenReturn(false);
        when(mockProperty.getParent()).thenReturn(mockNode);
        when(mockSession.getItem(testPath)).thenReturn(mockProperty);
        final Predicate<Node> holdO = isFedoraObject;
        try {
            isFedoraObject = alwaysTrue();
            assertNotNull(testObj.getFilter(mockSession).apply(mockEvent));
        } finally {
            isFedoraObject = holdO;
        }
    }
}
