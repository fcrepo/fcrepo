package org.fcrepo.webhooks;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.observer.FedoraEvent;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;

import com.google.common.eventbus.EventBus;

public class FedoraWebhooksTest {
    
    private FedoraWebhooks testObj;
    
    private Session mockSession;
    
    private Node mockRoot;
    
    @Before
    public void setUp() throws Exception {
        testObj = new FedoraWebhooks();
        mockSession = TestHelpers.mockSession(testObj);
        mockRoot = mock(Node.class);
        when(mockSession.getRootNode()).thenReturn(mockRoot);
        Repository mockRepo = mock(Repository.class);
        when(mockSession.getRepository()).thenReturn(mockRepo);
        when(mockRepo.login()).thenReturn(mockSession);
        Workspace mockWS = mock(Workspace.class);
        NodeTypeManager mockNT = mock(NodeTypeManager.class);
        when(mockWS.getNodeTypeManager()).thenReturn(mockNT);
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        testObj.setSession(mockSession);
        testObj.setReadOnlySessionSession(mockSession);
    }
    
    @Test
    public void testInitialize() throws Exception {
        EventBus mockBus = mock(EventBus.class);
        testObj.setEventBus(mockBus);
        testObj.initialize();
        verify(mockBus).register(testObj);
    }
    
    @Test
    public void testShowWebhooks() throws Exception {

        NodeIterator mockNodes = mock(NodeIterator.class);

        Node mockHook = mock(Node.class);
        Property mockProp = mock(Property.class);
        when(mockHook.getProperty(FedoraWebhooks.WEBHOOK_CALLBACK_PROPERTY)).thenReturn(mockProp);

        when(mockNodes.hasNext()).thenReturn(true, false);
        when(mockNodes.nextNode()).thenReturn(mockHook).thenThrow(IndexOutOfBoundsException.class);
        when(mockRoot.getNodes(FedoraWebhooks.WEBHOOK_SEARCH)).thenReturn(mockNodes);

        testObj.showWebhooks();
    }
    
    @Test
    public void testRegisterWebhook() throws Exception {
        String mockPath = "/webhook:foo";
        Node mockNode = mock(Node.class);
        NodeType mockType = mock(NodeType.class);
        when(mockType.getName()).thenReturn(FedoraWebhooks.WEBHOOK_JCR_TYPE);
        NodeType [] mockTypes = new NodeType[]{mockType};
        when(mockNode.getMixinNodeTypes()).thenReturn(mockTypes);
        when(mockSession.getNode(mockPath)).thenReturn(mockNode);
        when(mockRoot.getNode(mockPath.substring(1))).thenReturn(mockNode);
        when(mockNode.getSession()).thenReturn(mockSession);
        testObj.getSession();
        testObj.registerWebhook("foo");
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testOnEvent() throws Exception {
        FedoraEvent mockEvent = mock(FedoraEvent.class);
        String mockPath = "/mock/path/to/node";
        when(mockEvent.getPath()).thenReturn(mockPath);
        Node mockNode = mock(Node.class);
        NodeType mockType = mock(NodeType.class);
        when(mockType.getName()).thenReturn(FedoraWebhooks.WEBHOOK_JCR_TYPE);
        NodeType [] mockTypes = new NodeType[]{mockType};
        when(mockNode.getMixinNodeTypes()).thenReturn(mockTypes);
        when(mockSession.getNode(mockPath)).thenReturn(mockNode);
        when(mockRoot.getNode(mockPath.substring(1))).thenReturn(mockNode);
        when(mockNode.getSession()).thenReturn(mockSession);
        NodeIterator mockNodes = mock(NodeIterator.class);
        // let's say we have one webhook node
        Node mockHook = mock(Node.class);
        Property mockProp = mock(Property.class);
        when(mockHook.getProperty(FedoraWebhooks.WEBHOOK_CALLBACK_PROPERTY)).thenReturn(mockProp);
        when(mockProp.getString()).thenReturn("http://fedora.gov/secrets/plans/takeovers/global");
        when(mockNodes.hasNext()).thenReturn(true, false);
        when(mockNodes.nextNode()).thenReturn(mockHook).thenThrow(IndexOutOfBoundsException.class);
        when(mockRoot.getNodes(FedoraWebhooks.WEBHOOK_SEARCH)).thenReturn(mockNodes);
        testObj.getSession();
        testObj.onEvent(mockEvent);
    }
    
    @Test
    public void testSessions() {
        testObj.getSession();
        testObj.logoutSession();
        verify(mockSession).logout();
    }

}
