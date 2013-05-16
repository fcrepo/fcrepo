package org.fcrepo.api.repository;

import org.fcrepo.api.FedoraImport;
import org.fcrepo.serialization.FedoraObjectSerializer;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.InputStream;
import java.util.Map;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FedoraRepositoryImportTest {

    FedoraRepositoryImport testObj;

    Session mockSession;
    Map<String, FedoraObjectSerializer> mockSerializers;

    @Before
    public void setUp() throws RepositoryException {

        testObj = new FedoraRepositoryImport();

        mockSession = TestHelpers.mockSession(testObj);
        mockSerializers = mock(Map.class);
        testObj.setSerializers(mockSerializers);


        testObj.setUriInfo(TestHelpers.getUriInfoImpl());
    }

    @Test
    public void testImportObject() throws Exception {
        InputStream mockInputStream = mock(InputStream.class);
        FedoraObjectSerializer mockSerializer = mock(FedoraObjectSerializer.class);
        when(mockSerializers.get("fake-format")).thenReturn(mockSerializer);

        testObj.importObject("fake-format", mockInputStream);
        verify(mockSerializer).deserialize(mockSession, "/", mockInputStream);

    }
}
