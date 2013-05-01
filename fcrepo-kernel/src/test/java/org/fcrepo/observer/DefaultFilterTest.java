
package org.fcrepo.observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.*;

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

public class DefaultFilterTest {
    
    private DefaultFilter testObj;
    
    private Session mockSession;
    @Before
    public void setUp() throws Exception {
        testObj = new DefaultFilter();
        Field repoF = DefaultFilter.class.getDeclaredField("repository");
        repoF.setAccessible(true);
        Repository mockRepo = mock(Repository.class);
        mockSession = mock(Session.class);
        when(mockRepo.login()).thenReturn(mockSession);
        repoF.set(testObj, mockRepo);
        testObj.acquireSession();
    }

    @Test
    public void shouldApplyToObject() throws Exception {
        Predicate<Node> mockFuncTrue = mock(Predicate.class);
        when(mockFuncTrue.apply(any(Node.class))).thenReturn(true);
        Predicate<Node> mockFuncFalse = mock(Predicate.class);
        Predicate<Node> holdDS = FedoraTypesUtils.isFedoraDatastream;
        Predicate<Node> holdO = FedoraTypesUtils.isFedoraObject;
        
        try {
            FedoraTypesUtils.isFedoraDatastream = mockFuncFalse;
            FedoraTypesUtils.isFedoraObject = mockFuncTrue;
            String testPath = "/foo/bar";
            Event mockEvent = mock(Event.class);
            when(mockEvent.getPath()).thenReturn(testPath);
            Node mockNode = mock(Node.class);
            when(mockSession.getNode(testPath)).thenReturn(mockNode);
            assertTrue(testObj.apply(mockEvent));
        } finally {
            FedoraTypesUtils.isFedoraDatastream = holdDS;
            FedoraTypesUtils.isFedoraObject = holdO;
        }
    }

    @Test
    public void shouldApplyToDatastream() throws Exception {
        Predicate<Node> mockFuncTrue = mock(Predicate.class);
        when(mockFuncTrue.apply(any(Node.class))).thenReturn(true);
        Predicate<Node> mockFuncFalse = mock(Predicate.class);
        Predicate<Node> holdDS = FedoraTypesUtils.isFedoraDatastream;
        Predicate<Node> holdO = FedoraTypesUtils.isFedoraObject;
        
        try {
            FedoraTypesUtils.isFedoraDatastream = mockFuncFalse;
            FedoraTypesUtils.isFedoraObject = mockFuncTrue;
            String testPath = "/foo/bar";
            Event mockEvent = mock(Event.class);
            when(mockEvent.getPath()).thenReturn(testPath);
            Node mockNode = mock(Node.class);
            when(mockSession.getNode(testPath)).thenReturn(mockNode);
            assertTrue(testObj.apply(mockEvent));
        } finally {
            FedoraTypesUtils.isFedoraDatastream = holdDS;
            FedoraTypesUtils.isFedoraObject = holdO;
        }
    }
    
    @Test
    public void shouldNotApplyToNonExistentNodes() throws Exception {
        
        String testPath = "/foo/bar";
        Event mockEvent = mock(Event.class);
        when(mockEvent.getPath()).thenReturn(testPath);
        Node mockNode = mock(Node.class);
        when(mockSession.getNode(testPath)).thenThrow(PathNotFoundException.class);
        assertEquals(false, testObj.apply(mockEvent));
        verify(mockSession).getNode(testPath);
    }
    
    @Test
    public void shouldNotApplyToSystemNodes() throws Exception {
        Predicate<Node> mockFuncFalse = mock(Predicate.class);
        Predicate<Node> holdDS = FedoraTypesUtils.isFedoraDatastream;
        Predicate<Node> holdO = FedoraTypesUtils.isFedoraObject;
        
        try {
            FedoraTypesUtils.isFedoraDatastream = mockFuncFalse;
            FedoraTypesUtils.isFedoraObject = mockFuncFalse;
            String testPath = "/foo/bar";
            Event mockEvent = mock(Event.class);
            when(mockEvent.getPath()).thenReturn(testPath);
            Node mockNode = mock(Node.class);
            when(mockSession.getNode(testPath)).thenReturn(mockNode);
            assertEquals(false, testObj.apply(mockEvent));
        } finally {
            FedoraTypesUtils.isFedoraDatastream = holdDS;
            FedoraTypesUtils.isFedoraObject = holdO;
        }
    }
}
