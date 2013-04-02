package org.fcrepo;

import static org.fcrepo.services.PathService.getObjectJcrNodePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.services.ObjectService;
import org.fcrepo.services.ServiceHelpers;
import org.fcrepo.utils.FedoraJcrTypes;
import org.fcrepo.utils.FedoraTypesUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.api.Repository;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Predicate;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { ServiceHelpers.class })
public class FedoraObjectTest implements FedoraJcrTypes {

	String testPid = "testObj";

	String testDsId = "testDs";
	
	String mockUser = "mockUser";

	Repository mockRepo;
	
	ObjectService mockObjservice;
	
	Session mockSession;
	
	Node mockRootNode;
	
	Node mockObjNode;
	
	FedoraObject testFedoraObject;
	
	NodeType[] mockNodetypes;
	
	Predicate<Node> isOwned;
	
	
	@Before
	public void setUp() throws LoginException, RepositoryException {
		this.isOwned = FedoraTypesUtils.isOwned;
		String relPath = getObjectJcrNodePath(testPid).substring(1);
		
		mockSession = mock(Session.class);
		mockRootNode = mock(Node.class);
		mockObjNode = mock(Node.class);
		Predicate<Node> mockPredicate = mock(Predicate.class);

		try{
			
			when(mockObjNode.getName()).thenReturn(testPid);
			when(mockObjNode.getSession()).thenReturn(mockSession);
		    when(mockSession.getRootNode()).thenReturn(mockRootNode);
		    when(mockRootNode.getNode(relPath)).thenReturn(mockObjNode);
		    when(mockSession.getUserID()).thenReturn(mockUser);
			testFedoraObject = new FedoraObject(mockSession, relPath);

			verify(mockRootNode).getNode(relPath);
			
			mockNodetypes = new NodeType[2];
			mockNodetypes[0] =  mock(NodeType.class);
			mockNodetypes[1] = mock(NodeType.class);
			
			when(mockObjNode.getMixinNodeTypes()).thenReturn(mockNodetypes);
			
			when(mockPredicate.apply(mockObjNode)).thenReturn(true);
			FedoraTypesUtils.isOwned = mockPredicate;
			
		} catch(RepositoryException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
				
	}
	
	@After
	public void tearDown() {
		mockSession = null;
		mockRootNode = null;
		mockObjNode = null;
		if (this.isOwned != null){
			FedoraTypesUtils.isOwned = this.isOwned;
		}
	}
	
	@Test
	public void testGetName() throws RepositoryException {
		assertEquals(testFedoraObject.getName(), testPid);
	}
	
	@Test
	public void testGetNode() {
		assertEquals(testFedoraObject.getNode(), mockObjNode);
	}
	
	@Test
	public void testSetOwnerId() throws RepositoryException {
		Property mockProp = mock(Property.class);
		when(mockObjNode.getProperty(FEDORA_OWNERID)).thenReturn(mockProp);
		String expected = "resuKcom";
		testFedoraObject.setOwnerId(expected);
		verify(mockObjNode).setProperty(FEDORA_OWNERID, expected);
	}
	
	@Test
	public void testGetOwnerId() throws RepositoryException {
		Property mockProp = mock(Property.class);
		when(mockObjNode.getProperty(FEDORA_OWNERID)).thenReturn(mockProp);
		when(mockProp.getString()).thenReturn(mockUser);
		String actual = testFedoraObject.getOwnerId();
		assertEquals(mockUser, actual);
		verify(mockObjNode).getProperty(FEDORA_OWNERID);
	}
		
	@Test
	public void testGetLabel() throws RepositoryException {
		Property mockProp = mock(Property.class);
		when(mockObjNode.hasProperty(DC_TITLE)).thenReturn(true);
		when(mockObjNode.getProperty(DC_TITLE)).thenReturn(mockProp);
		when(mockProp.getString()).thenReturn("mockTitle");
		testFedoraObject.getLabel();
		verify(mockObjNode).getProperty(DC_TITLE);
	}
	
	@Test
	public void testGetCreated() throws RepositoryException {
		Property mockProp = mock(Property.class);
		when(mockProp.getString()).thenReturn("mockDate");
		when(mockObjNode.getProperty(JCR_CREATED)).thenReturn(mockProp);
		testFedoraObject.getCreated();
		verify(mockObjNode).getProperty(JCR_CREATED);
	}
	
	@Test
	public void testGetLastModified() throws RepositoryException {
		Property mockProp = mock(Property.class);
		when(mockObjNode.getProperty(JCR_LASTMODIFIED)).thenReturn(mockProp);
		when(mockProp.getString()).thenReturn("mockDate");
		testFedoraObject.getLastModified();
		verify(mockObjNode).getProperty(JCR_LASTMODIFIED);
	}
	
	@Test
	public void testGetSize() throws RepositoryException {
		PowerMockito.mockStatic(ServiceHelpers.class);
        when(ServiceHelpers.getObjectSize(mockObjNode)).thenReturn(-8L); // obviously not a real value
		long actual = testFedoraObject.getSize();
		assertEquals(-8, actual);
	}
	
	@Test
	public void testGetModels() throws RepositoryException {
		Collection<String> actual = testFedoraObject.getModels();
		assertNotNull(actual);
	}
	
}
