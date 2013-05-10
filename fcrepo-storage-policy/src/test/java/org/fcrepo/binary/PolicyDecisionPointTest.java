package org.fcrepo.binary;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.value.binary.NamedHint;
import org.modeshape.jcr.value.binary.StrategyHint;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIME_TYPE;

public class PolicyDecisionPointTest {
	static PolicyDecisionPoint pt;
	static private StrategyHint dummyHint;
	static private StrategyHint tiffHint;

	@BeforeClass
	public static void setupPdp() {
		pt = new PolicyDecisionPoint();

		dummyHint = new NamedHint("dummy-store-id");
		Policy policy = new MimeTypePolicy("image/x-dummy-type", dummyHint);

		pt.addPolicy(policy);

		tiffHint = new NamedHint("tiff-store-id");
		Policy tiffPolicy = new MimeTypePolicy("image/tiff", tiffHint);

		pt.addPolicy(tiffPolicy);
	}

	@Test
	public void testDummyNode() throws Exception {

		Session mockSession = mock(Session.class);
		Node mockRootNode = mock(Node.class);
		Node mockDsNode = mock(Node.class);

		when(mockDsNode.getSession()).thenReturn(mockSession);
		Property mockProperty = mock(Property.class);
		when(mockProperty.getString()).thenReturn("image/x-dummy-type");
		Node mockContentNode = mock(Node.class);
		when(mockDsNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(mockContentNode);
		when(mockContentNode.getProperty(JCR_MIME_TYPE)).thenReturn(mockProperty);

		StrategyHint receivedHint = pt.evaluatePolicies(mockDsNode);
		assertThat(receivedHint, is(dummyHint));
	}

	@Test
	public void testTiffNode() throws Exception {

		Session mockSession = mock(Session.class);
		Node mockRootNode = mock(Node.class);
		Node mockDsNode = mock(Node.class);

		when(mockDsNode.getSession()).thenReturn(mockSession);
		Property mockProperty = mock(Property.class);
		when(mockProperty.getString()).thenReturn("image/tiff");
		Node mockContentNode = mock(Node.class);
		when(mockDsNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(mockContentNode);
		when(mockContentNode.getProperty(JCR_MIME_TYPE)).thenReturn(mockProperty);

		StrategyHint receivedHint = pt.evaluatePolicies(mockDsNode);
		assertThat(receivedHint, is(tiffHint));
	}


	@Test
	public void testOtherNode() throws Exception {

		Session mockSession = mock(Session.class);
		Node mockRootNode = mock(Node.class);
		Node mockDsNode = mock(Node.class);

		when(mockDsNode.getSession()).thenReturn(mockSession);
		Property mockProperty = mock(Property.class);
		when(mockProperty.getString()).thenReturn("image/x-other");
		Node mockContentNode = mock(Node.class);
		when(mockDsNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(mockContentNode);
		when(mockContentNode.getProperty(JCR_MIME_TYPE)).thenReturn(mockProperty);

		StrategyHint receivedHint = pt.evaluatePolicies(mockDsNode);
		assertNull(receivedHint);
	}


}
