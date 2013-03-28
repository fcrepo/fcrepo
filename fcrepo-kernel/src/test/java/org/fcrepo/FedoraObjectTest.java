package org.fcrepo;

import static org.fcrepo.services.PathService.getDatastreamJcrNodePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.fcrepo.utils.FedoraTypesUtils.isOwned;

import static org.fcrepo.TestHelpers.*;

import java.util.Calendar;
import java.util.Date;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import com.google.common.base.Predicate;

import org.fcrepo.services.ObjectService;
import org.fcrepo.utils.FedoraJcrTypes;
import org.fcrepo.utils.FedoraTypesUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

public class FedoraObjectTest implements FedoraJcrTypes {

	String testPid = "testObj";

	String testDsId = "testDs";
	
	String mockUser = "mockUser";

	Repository mockRepo;
	
	ObjectService mockObjservice;
	
	Session mockSession;
	
	Node mockRootNode;
	
	Node mockDsNode;
	
	FedoraObject testFedoraObject;
	
	NodeType[] mockNodetypes;
	
	@Before
	public void setUp() throws LoginException, RepositoryException {
		String relPath = getDatastreamJcrNodePath(testPid, testDsId).substring(1);
		
		mockSession = mock(Session.class);
		mockRootNode = mock(Node.class);
		mockDsNode = mock(Node.class);
		FedoraTypesUtils mockFedTypeUtils = mock(FedoraTypesUtils.class);
		Predicate<Node> mockPredicate = mock(Predicate.class);
		
		
		try{
			
			when(mockRootNode.getName()).thenReturn(testPid);
			when(mockDsNode.getSession()).thenReturn(mockSession);
		    when(mockSession.getRootNode()).thenReturn(mockRootNode);
		    when(mockRootNode.getNode(relPath)).thenReturn(mockDsNode);
		    when(mockSession.getUserID()).thenReturn(mockUser);
			testFedoraObject = new FedoraObject(mockSession, relPath);

			verify(mockRootNode).getNode(relPath);
			
			mockNodetypes = new NodeType[2];
			mockNodetypes[0] =  mock(NodeType.class);
			mockNodetypes[1] = mock(NodeType.class);
			
			when(mockDsNode.getMixinNodeTypes()).thenReturn(mockNodetypes);
			testFedoraObject.setNode(mockDsNode);
			testFedoraObject.setIsOwned(mockPredicate);
			when(mockPredicate.apply(mockDsNode)).thenReturn(true);
			
			
		} catch(RepositoryException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
				
	}
	
	@After
	public void tearDown() {
		mockSession = null;
		mockRootNode = null;
		mockDsNode = null;
	}
	
	@Test
	public void testGetOwnerId() throws RepositoryException {
		String expected = "asdf";
		Property mockProp = mock(Property.class);
		Node mockContent = getContentNodeMock(expected);
		when(mockDsNode.getNode("jcr:content")).thenReturn(mockContent);
		when(mockDsNode.getProperty(FEDORA_OWNERID)).thenReturn(mockProp);
		when(mockProp.toString()).thenReturn("mockUser");
		String actual = testFedoraObject.getOwnerId();
		assertEquals(mockUser,actual);
	}
	
	@Test
	public void testGetCreated() throws RepositoryException {
		Date expected = new Date();
		String expectedString = Long.toString(expected.getTime());
		Property mockProp = mock(Property.class);
		when(mockProp.getString()).thenReturn(expectedString);
		when(mockDsNode.getProperty(JCR_CREATED)).thenReturn(mockProp);
		String actual = testFedoraObject.getCreated();
		assertEquals(expectedString,actual);
	}
	
	@Test
	public void testGetLabel() throws RepositoryException {
		String actual = testFedoraObject.getLabel();
		verify(mockDsNode).hasProperty(DC_TITLE);
	}
	
}
