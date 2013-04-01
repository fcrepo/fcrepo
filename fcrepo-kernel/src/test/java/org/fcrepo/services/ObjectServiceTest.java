package org.fcrepo.services;

import static org.fcrepo.services.PathService.getObjectJcrNodePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static org.powermock.api.mockito.PowerMockito.verifyNew;

import javax.jcr.Node;
import javax.jcr.Session;

import org.fcrepo.FedoraObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ObjectService.class)
public class ObjectServiceTest {

	@Before
	public void setUp() {
	}
	
	@After
	public void tearDown() {
		
	}
	
	@Test
	public void testCreateObjectNode() throws Exception {
		final Node mockNode = mock(Node.class);
		Session mockSession = mock(Session.class);
		String testPath = getObjectJcrNodePath("foo");
		FedoraObject mockWrapper = new FedoraObject(mockNode);
		whenNew(FedoraObject.class)
		.withArguments(mockSession, testPath)
		.thenReturn(mockWrapper);
		ObjectService testObj = new ObjectService();
		Node actual = testObj.createObjectNode(mockSession, "foo");
		assertEquals(mockNode, actual);
		verifyNew(FedoraObject.class)
		.withArguments(mockSession, testPath);
	}

	@Test
	public void testCreateObject() throws Exception {
		final Node mockNode = mock(Node.class);
		Session mockSession = mock(Session.class);
		String testPath = getObjectJcrNodePath("foo");
		FedoraObject mockWrapper = new FedoraObject(mockNode);
		whenNew(FedoraObject.class)
		.withArguments(any(Session.class), any(String.class))
		.thenReturn(mockWrapper);
		ObjectService testObj = new ObjectService();
		testObj.createObject(mockSession, "foo");
		verifyNew(FedoraObject.class)
		.withArguments(mockSession, testPath);
	}
}
