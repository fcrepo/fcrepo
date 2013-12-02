
package org.fcrepo.jms.headers;

import static javax.jcr.observation.Event.NODE_ADDED;
import static org.fcrepo.jms.headers.DefaultMessageFactory.EVENT_TYPE_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.IDENTIFIER_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.TIMESTAMP_HEADER_NAME;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.activemq.command.ActiveMQObjectMessage;
import org.fcrepo.kernel.utils.EventType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DefaultMessageFactoryTest {

    @Mock
    private Session mockSession;

    @Mock
    private Event mockEvent;

    private DefaultMessageFactory testDefaultMessageFactory;

    @Before
    public void setUp() throws JMSException {
        initMocks(this);
        when(mockSession.createMessage()).thenReturn(
                new ActiveMQObjectMessage());
        testDefaultMessageFactory = new DefaultMessageFactory();
    }

    @Test
    public void testBuildMessage() throws RepositoryException, IOException,
                                  JMSException {
        final Long testDate = 46647758568747L;
        when(mockEvent.getDate()).thenReturn(testDate);
        final String testPath = "super/calli/fragi/listic";
        when(mockEvent.getPath()).thenReturn(testPath);
        final Integer testType = NODE_ADDED;
        final String testReturnType =
            REPOSITORY_NAMESPACE + EventType.valueOf(NODE_ADDED).toString();
        when(mockEvent.getType()).thenReturn(testType);
        final Message testMessage =
            testDefaultMessageFactory.getMessage(mockEvent, null, mockSession);
        assertEquals("Got wrong date in message!", testDate, (Long) testMessage
                .getLongProperty(TIMESTAMP_HEADER_NAME));
        assertEquals("Got wrong identifier in message!", testPath, testMessage
                .getStringProperty(IDENTIFIER_HEADER_NAME));
        assertEquals("Got wrong type in message!", testReturnType, testMessage
                .getStringProperty(EVENT_TYPE_HEADER_NAME));
    }

}
