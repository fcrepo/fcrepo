
package org.fcrepo.api;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.identifiers.UUIDPidMinter;
import org.fcrepo.utils.FedoraJcrTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

public class FedoraUnnamedObjectsTest {

    FedoraUnnamedObjects testObj;

    FedoraNodes mockObjects;

    Repository mockRepo;

    Session mockSession;

    SecurityContext mockSecurityContext;

    HttpServletRequest mockServletRequest;

    Principal mockPrincipal;

    String mockUser = "testuser";

    @Before
    public void setUp() throws LoginException, RepositoryException {
        mockObjects = mock(FedoraNodes.class);
        testObj = new FedoraUnnamedObjects();
        testObj.objectsResource = mockObjects;
    }

    @After
    public void tearDown() {

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIngestAndMint() throws RepositoryException, IOException,
            InvalidChecksumException {
        final UUIDPidMinter mockMint = mock(UUIDPidMinter.class);
        testObj.setPidMinter(mockMint);
        testObj.ingestAndMint(createPathList("objects", "fcr:new"));
        verify(mockMint).mintPid();
        verify(mockObjects).createObject(any(List.class),
                eq(FedoraJcrTypes.FEDORA_OBJECT), isNull(String.class),
                isNull(String.class), isNull(MediaType.class),
                isNull(InputStream.class));
    }

}
