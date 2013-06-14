
package org.fcrepo.api;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.test.util.TestHelpers.mockSession;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.serialization.FedoraObjectSerializer;
import org.fcrepo.serialization.SerializerUtil;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

public class FedoraImportTest {

    FedoraImport testObj;

    Session mockSession;

    SerializerUtil mockSerializers;

    FedoraObjectSerializer mockSerializer;

    @Before
    public void setUp() throws RepositoryException {

        testObj = new FedoraImport();

        mockSession = mockSession(testObj);
        mockSerializers = mock(SerializerUtil.class);
        mockSerializer = mock(FedoraObjectSerializer.class);
        when(mockSerializers.getSerializer("fake-format")).thenReturn(
                mockSerializer);
        testObj.setSerializers(mockSerializers);
        testObj.setSession(mockSession);
        testObj.setUriInfo(TestHelpers.getUriInfoImpl());
    }

    @Test
    public void testImportObject() throws Exception {
        final InputStream mockInputStream = mock(InputStream.class);

        Node mockNode = mock(Node.class);
        when(mockNode.getPath()).thenReturn("/test/object");
        when(mockSession.getNode("/test/object")).thenReturn(mockNode);

        testObj.importObject(createPathList("test", "object"), "fake-format",
                mockInputStream);

        verify(mockSerializer).deserialize(mockSession, "/test/object",
                mockInputStream);

    }
}
