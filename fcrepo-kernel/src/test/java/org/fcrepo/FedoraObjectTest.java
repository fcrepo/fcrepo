package org.fcrepo;

import static org.fcrepo.TestHelpers.getContentNodeMock;
import static org.fcrepo.services.PathService.getDatastreamJcrNodePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Date;

import javax.jcr.Binary;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.services.ObjectService;
import org.fcrepo.utils.FedoraJcrTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

import com.google.common.base.Predicate;

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
		Predicate<Node> mockPredicate = mock(Predicate.class);

		try{
			
			when(mockDsNode.getName()).thenReturn(testDsId);
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
	public void testGetName() throws RepositoryException {
		assertEquals(testFedoraObject.getName(), testDsId);
	}
	
	@Test
	public void testGetNode() {
		assertEquals(testFedoraObject.getNode(), mockDsNode);
	}
	
	@Test
	public void testSetOwnerId() throws RepositoryException {
		Property mockProp = mock(Property.class);
		when(mockDsNode.getProperty(FEDORA_OWNERID)).thenReturn(mockProp);
		when(mockProp.getString()).thenReturn(mockUser);
		testFedoraObject.setOwnerId(mockUser);
		String expected = mockDsNode.getProperty(FEDORA_OWNERID).getString();
		assertEquals(mockUser, expected);
	}
	
	@Test
	public void testGetOwnerId() throws RepositoryException {
		String expected = "asdf";
		Property mockProp = mock(Property.class);
		Node mockContent = getContentNodeMock(expected);
		when(mockDsNode.getNode("jcr:content")).thenReturn(mockContent);
		when(mockDsNode.getProperty(FEDORA_OWNERID)).thenReturn(mockProp);
		when(mockProp.getString()).thenReturn("mockUser");
		String actual = testFedoraObject.getOwnerId();
		assertEquals(mockUser, actual);
	}
		
	@Test
	public void testGetLabel() throws RepositoryException {
		Property mockProp = mock(Property.class);
		when(mockDsNode.hasProperty(DC_TITLE)).thenReturn(true);
		when(mockDsNode.getProperty(DC_TITLE)).thenReturn(mockProp);
		when(mockProp.getString()).thenReturn("mockTitle");
		String actual = testFedoraObject.getLabel();
		assertEquals("mockTitle", actual);
	}
	
	@Test
	public void testGetCreated() throws RepositoryException {
		Date expected = new Date();
		String expectedString = Long.toString(expected.getTime());
		Property mockProp = mock(Property.class);
		when(mockProp.getString()).thenReturn(expectedString);
		when(mockDsNode.getProperty(JCR_CREATED)).thenReturn(mockProp);
		String actual = testFedoraObject.getCreated();
		assertEquals(expectedString, actual);
	}
	
	@Test
	public void testGetLastModified() throws RepositoryException {
		Property mockProp = mock(Property.class);
		when(mockDsNode.getProperty("jcr:lastModified")).thenReturn(mockProp);
		when(mockProp.getString()).thenReturn("mockDate");
		String actual = testFedoraObject.getLastModified();
		assertEquals("mockDate", actual);
	}
	
	@Test
	public void testGetSize() throws RepositoryException {
		String expected = "asdf";
		Binary mockBinary = mock(Binary.class);
		PropertyIterator mockPI = mock(PropertyIterator.class);
		NodeIterator mockNI = mock(NodeIterator.class);
		Node mockContent = getContentNodeMock(expected);
		Property mockProp = mock(Property.class);
		when(mockDsNode.getNode("jcr:content")).thenReturn(mockContent);
		when(mockDsNode.getProperties()).thenReturn(mockPI);
		when(mockPI.hasNext()).thenReturn(true,false);
		when(mockPI.nextProperty()).thenReturn(mockProp);
		when(mockProp.getBinary()).thenReturn(mockBinary);
		when(mockBinary.getSize()).thenReturn(0L);
		when(mockDsNode.getNodes()).thenReturn(mockNI);
		when(mockNI.hasNext()).thenReturn(true,false);
		when(mockNI.nextNode()).thenReturn(mockDsNode);
		long actual = testFedoraObject.getSize();
		assertEquals(4, actual);
	}
	
	@Test
	public void testGetModels() throws RepositoryException {
		Collection<String> actual = testFedoraObject.getModels();
		assertNotNull(actual);
	}
	
}
