package org.fcrepo.messaging.legacy;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.Event;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Person;
import org.apache.abdera.model.Text;
import org.fcrepo.utils.FedoraJcrTypes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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
}
