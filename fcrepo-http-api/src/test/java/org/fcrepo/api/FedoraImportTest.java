package org.fcrepo.api;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Map;

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

    @Before
    public void setUp() throws RepositoryException {

        testObj = new FedoraImport();

        mockSession = TestHelpers.mockSession(testObj);
        mockSerializers = mock(SerializerUtil.class);
        testObj.setSerializers(mockSerializers);


        testObj.setUriInfo(TestHelpers.getUriInfoImpl());
    }

    @Test
    public void testImportObject() throws Exception {
        InputStream mockInputStream = mock(InputStream.class);
        FedoraObjectSerializer mockSerializer = mock(FedoraObjectSerializer.class);
        when(mockSerializers.getSerializer("fake-format")).thenReturn(mockSerializer);

        testObj.importObject(createPathList("test", "object"), "fake-format", mockInputStream);
        verify(mockSerializer).deserialize(mockSession, "/test/object", mockInputStream);

    }
}
