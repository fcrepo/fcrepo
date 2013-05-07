package org.fcrepo.serialization.jcrxml;

import org.fcrepo.FedoraObject;
import org.junit.Test;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Session;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JcrXmlSerializerTest {
	@Test
	public void testSerialize() throws Exception {
		final Session mockSession = mock(Session.class);
		final Node mockNode = mock(Node.class);
		final FedoraObject mockObject = mock(FedoraObject.class);
		when(mockObject.getNode()).thenReturn(mockNode);
		when(mockNode.getSession()).thenReturn(mockSession);
		when(mockNode.getPath()).thenReturn("/path/to/node");

		final OutputStream os = new ByteArrayOutputStream();

		new JcrXmlSerializer().serialize(mockObject, os);

		verify(mockSession).exportSystemView("/path/to/node", os, false, false);
	}

	@Test
	public void testDeserialize() throws Exception {
		final Session mockSession = mock(Session.class);
		final InputStream mockIS = mock(InputStream.class);

		new JcrXmlSerializer().deserialize(mockSession, "/objects", mockIS);
		verify(mockSession).importXML("/objects", mockIS, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

	}
}
