package org.fcrepo.messaging.legacy;

import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jms.JMSException;
import javax.jms.Message;

import org.fcrepo.utils.FedoraTypesUtils;
import org.junit.Before;
import org.junit.Test;

public class LegacyMethodStaticTest {
        
    @Before
    public void setUp() throws RepositoryException {
    }
    
    @Test
    public void testCanParse() throws JMSException {
        // Should the static tests be broken out into a separate test class, so we can use PowerMock with better scope?
    	boolean result;
        Message mockYes = mock(Message.class);
        when(mockYes.getJMSType()).thenReturn(EntryFactory.FORMAT);
        when(mockYes.getStringProperty("methodName")).thenReturn("ingest");
        result = LegacyMethod.canParse(mockYes);
        assertEquals(true, result);
        Message mockNoFormat = mock(Message.class);
        when(mockNoFormat.getJMSType()).thenReturn("crazyType");
        when(mockNoFormat.getStringProperty("methodName")).thenReturn("ingest");
        result = LegacyMethod.canParse(mockNoFormat);
        assertEquals(false, result);
        Message mockNoMessage = mock(Message.class);
        when(mockNoMessage.getJMSType()).thenReturn(EntryFactory.FORMAT);
        when(mockNoMessage.getStringProperty("methodName")).thenReturn("destroyEverything");
        result = LegacyMethod.canParse(mockNoMessage);
        assertEquals(false, result);
    }
    
    @Test
    public void testObjectToString() {
    	String test = null;
    	String testType = null;
    	assertEquals("null", LegacyMethod.objectToString(test, testType));
    	testType = "fedora-types:ArrayOfString";
    	test = "fo\"o";
    	assertEquals("[UNSUPPORTED" + testType + "]", LegacyMethod.objectToString(test, testType));
    	testType = "fedora-types:RelationshipTuple";
    	assertEquals("[UNSUPPORTED" + testType + "]", LegacyMethod.objectToString(test, testType));
    	testType = "xsd:boolean";
    	assertEquals(test, LegacyMethod.objectToString(test, testType));
    	testType = "xsd:nonNegativeInteger";
    	assertEquals(test, LegacyMethod.objectToString(test, testType));
    	testType = "xsd:string";
    	assertEquals("fo'o", LegacyMethod.objectToString(test, testType));
    }
    
    @Test
    public void testGetReturnValue() throws RepositoryException {
    	String testName = "test:nodeName";
    	long testDate = new Date().getTime();
    	String testXSDDate = FedoraTypesUtils.convertDateToXSDString(testDate);
    	Node mockNode = mock(Node.class);
    	Event mockEvent = mock(Event.class);
    	when(mockEvent.getDate()).thenReturn(testDate);
    	when(mockEvent.getType()).thenReturn(NODE_ADDED);
    	when(mockNode.getName()).thenReturn(testName);
    	assertEquals(testName, LegacyMethod.getReturnValue(mockEvent, mockNode));
    	when(mockEvent.getType()).thenReturn(NODE_REMOVED);
    	assertEquals(testXSDDate, LegacyMethod.getReturnValue(mockEvent, mockNode));
    	when(mockEvent.getType()).thenReturn(PROPERTY_ADDED);
    	assertEquals(testXSDDate, LegacyMethod.getReturnValue(mockEvent, mockNode));
    	when(mockEvent.getType()).thenReturn(PROPERTY_CHANGED);
    	assertEquals(testXSDDate, LegacyMethod.getReturnValue(mockEvent, mockNode));
    	when(mockEvent.getType()).thenReturn(PROPERTY_REMOVED);
    	assertEquals(testXSDDate, LegacyMethod.getReturnValue(mockEvent, mockNode));
    	when(mockEvent.getType()).thenReturn(Integer.MAX_VALUE);
    	assertEquals(null, LegacyMethod.getReturnValue(mockEvent, mockNode));
    }
}
