package org.fcrepo.binary;

import org.junit.Test;
import org.modeshape.jcr.value.binary.NamedHint;
import org.modeshape.jcr.value.binary.StrategyHint;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIME_TYPE;

public class MimeTypePolicyTest {
	@Test
	public void shouldEvaluatePolicyAndReturnHint() throws Exception {
		StrategyHint hint = new NamedHint("store-id");
		Policy policy = new MimeTypePolicy("image/x-dummy", hint);

		Session mockSession = mock(Session.class);
		Node mockRootNode = mock(Node.class);
		Node mockDsNode = mock(Node.class);

		when(mockDsNode.getSession()).thenReturn(mockSession);
		Property mockProperty = mock(Property.class);
		when(mockProperty.getString()).thenReturn("image/x-dummy");
		when(mockDsNode.getProperty(JCR_MIME_TYPE)).thenReturn(mockProperty);

		StrategyHint receivedHint = policy.evaluatePolicy(mockDsNode);

		assertThat(receivedHint, is(hint));
	}

	@Test
	public void shouldEvaluatePolicyAndReturnNoHint() throws Exception {
		StrategyHint hint = new NamedHint("store-id");
		Policy policy = new MimeTypePolicy("image/x-dummy", hint);

		Session mockSession = mock(Session.class);
		Node mockDsNode = mock(Node.class);

		when(mockDsNode.getSession()).thenReturn(mockSession);
		Property mockProperty = mock(Property.class);
		when(mockProperty.getString()).thenReturn("application/x-other");
		when(mockDsNode.getProperty(JCR_MIME_TYPE)).thenReturn(mockProperty);

		StrategyHint receivedHint = policy.evaluatePolicy(mockDsNode);

		assertNull(receivedHint);
	}

	@Test
	public void shouldEvaluatePolicyAndReturnNoHintOnException() throws Exception {
		StrategyHint hint = new NamedHint("store-id");
		Policy policy = new MimeTypePolicy("image/x-dummy", hint);

		Session mockSession = mock(Session.class);
		Node mockDsNode = mock(Node.class);

		when(mockDsNode.getProperty(JCR_MIME_TYPE)).thenThrow(new RepositoryException());

		StrategyHint receivedHint = policy.evaluatePolicy(mockDsNode);

		assertNull(receivedHint);
	}
}
