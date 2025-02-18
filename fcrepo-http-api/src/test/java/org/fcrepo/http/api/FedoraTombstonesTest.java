/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import org.fcrepo.common.db.DbTransactionExecutor;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.Tombstone;
import org.fcrepo.kernel.api.services.PurgeResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * @author cabeer
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FedoraTombstonesTest {

    private final String path = "/test/object";

    private final FedoraId fedoraId = FedoraId.create(path);

    private FedoraTombstones testObj;

    @Mock
    private Request mockRequest;

    @Mock
    private ServletContext mockServletContext;

    @Mock
    private Tombstone mockTombstone;

    @Mock
    private FedoraResource mockDeletedObj;

    @Mock
    private Transaction mockTransaction;

    @Mock
    private SecurityContext mockSecurityContext;

    @Mock
    private PurgeResourceService purgeResourceService;

    @Mock
    private ResourceFactory resourceFactory;

    @BeforeEach
    public void setUp() throws Exception {

        testObj = spy(new FedoraTombstones(path));
        setField(testObj, "transaction", mockTransaction);
        setField(testObj, "purgeResourceService", purgeResourceService);
        setField(testObj, "resourceFactory", resourceFactory);
        setField(testObj, "uriInfo", getUriInfoImpl());
        setField(testObj, "securityContext", mockSecurityContext);
        setField(testObj, "request", mockRequest);
        setField(testObj, "context", mockServletContext);
        setField(testObj, "dbTransactionExecutor", new DbTransactionExecutor());

        when(resourceFactory.getResource((Transaction)any(), eq(fedoraId))).thenReturn(mockTombstone);
        when(mockTombstone.getDeletedObject()).thenReturn(mockDeletedObj);
    }

    @Test
    public void testDelete() {
        final Response actual = testObj.delete();
        assertEquals(NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(purgeResourceService).perform(mockTransaction, mockDeletedObj, null);
        verify(mockTransaction).commitIfShortLived();
    }

    /**
     * If a resource is not deleted, it doesn't return a TombstoneImpl so the fcr:tombstone URI should return 404.
     */
    @Test
    public void testNotYetDeleted() throws Exception {
        when(resourceFactory.getResource((Transaction)any(), eq(fedoraId))).thenReturn(mockDeletedObj);
        final Response actual = testObj.delete();
        assertEquals(NOT_FOUND.getStatusCode(), actual.getStatus());
    }
}
