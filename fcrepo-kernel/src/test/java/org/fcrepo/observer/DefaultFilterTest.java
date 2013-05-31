/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.fcrepo.utils.FedoraTypesUtils;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

import com.google.common.base.Predicate;

/**
 * @todo Add Documentation.
 * @author Benjamin Armintor
 * @date Apr 30, 2013
 */
public class DefaultFilterTest {

    private DefaultFilter testObj;

    private Session mockSession;

    /**
     * @todo Add Documentation.
     */
    @Before
    public void setUp() throws Exception {
        testObj = new DefaultFilter();
        final Field repoF = DefaultFilter.class.getDeclaredField("repository");
        repoF.setAccessible(true);
        final Repository mockRepo = mock(Repository.class);
        mockSession = mock(Session.class);
        when(mockRepo.login()).thenReturn(mockSession);
        repoF.set(testObj, mockRepo);
        testObj.acquireSession();
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void shouldApplyToObject() throws Exception {
        @SuppressWarnings("unchecked")
        final Predicate<Node> mockFuncTrue = mock(Predicate.class);
        when(mockFuncTrue.apply(any(Node.class))).thenReturn(true);
        @SuppressWarnings("unchecked")
        final Predicate<Node> mockFuncFalse = mock(Predicate.class);
        final Predicate<Node> holdDS = FedoraTypesUtils.isFedoraDatastream;
        final Predicate<Node> holdO = FedoraTypesUtils.isFedoraObject;

        try {
            FedoraTypesUtils.isFedoraDatastream = mockFuncFalse;
            FedoraTypesUtils.isFedoraObject = mockFuncTrue;
            final String testPath = "/foo/bar";
            final Event mockEvent = mock(Event.class);
            when(mockEvent.getPath()).thenReturn(testPath);
            final Node mockNode = mock(Node.class);
            when(mockSession.getNode(testPath)).thenReturn(mockNode);
            assertTrue(testObj.apply(mockEvent));
        } finally {
            FedoraTypesUtils.isFedoraDatastream = holdDS;
            FedoraTypesUtils.isFedoraObject = holdO;
        }
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void shouldApplyToDatastream() throws Exception {
        @SuppressWarnings("unchecked")
        final Predicate<Node> mockFuncTrue = mock(Predicate.class);
        when(mockFuncTrue.apply(any(Node.class))).thenReturn(true);
        @SuppressWarnings("unchecked")
        final Predicate<Node> mockFuncFalse = mock(Predicate.class);
        final Predicate<Node> holdDS = FedoraTypesUtils.isFedoraDatastream;
        final Predicate<Node> holdO = FedoraTypesUtils.isFedoraObject;

        try {
            FedoraTypesUtils.isFedoraDatastream = mockFuncFalse;
            FedoraTypesUtils.isFedoraObject = mockFuncTrue;
            final String testPath = "/foo/bar";
            final Event mockEvent = mock(Event.class);
            when(mockEvent.getPath()).thenReturn(testPath);
            final Node mockNode = mock(Node.class);
            when(mockSession.getNode(testPath)).thenReturn(mockNode);
            assertTrue(testObj.apply(mockEvent));
        } finally {
            FedoraTypesUtils.isFedoraDatastream = holdDS;
            FedoraTypesUtils.isFedoraObject = holdO;
        }
    }

    /**
     * @todo Add Documentation.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotApplyToNonExistentNodes() throws Exception {

        final String testPath = "/foo/bar";
        final Event mockEvent = mock(Event.class);
        when(mockEvent.getPath()).thenReturn(testPath);
        when(mockSession.getNode(testPath)).thenThrow(
                PathNotFoundException.class);
        assertEquals(false, testObj.apply(mockEvent));
        verify(mockSession).getNode(testPath);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void shouldNotApplyToSystemNodes() throws Exception {
        @SuppressWarnings("unchecked")
        final Predicate<Node> mockFuncFalse = mock(Predicate.class);
        final Predicate<Node> holdDS = FedoraTypesUtils.isFedoraDatastream;
        final Predicate<Node> holdO = FedoraTypesUtils.isFedoraObject;

        try {
            FedoraTypesUtils.isFedoraDatastream = mockFuncFalse;
            FedoraTypesUtils.isFedoraObject = mockFuncFalse;
            final String testPath = "/foo/bar";
            final Event mockEvent = mock(Event.class);
            when(mockEvent.getPath()).thenReturn(testPath);
            final Node mockNode = mock(Node.class);
            when(mockSession.getNode(testPath)).thenReturn(mockNode);
            assertEquals(false, testObj.apply(mockEvent));
        } finally {
            FedoraTypesUtils.isFedoraDatastream = holdDS;
            FedoraTypesUtils.isFedoraObject = holdO;
        }
    }
}
