/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.api;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.Tombstone;
import org.fcrepo.kernel.api.services.PurgeResourceService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * @author cabeer
 */
@RunWith(MockitoJUnitRunner.Silent.class)
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

    @Before
    public void setUp() throws Exception {

        testObj = spy(new FedoraTombstones(path));
        setField(testObj, "transaction", mockTransaction);
        setField(testObj, "purgeResourceService", purgeResourceService);
        setField(testObj, "resourceFactory", resourceFactory);
        setField(testObj, "uriInfo", getUriInfoImpl());
        setField(testObj, "securityContext", mockSecurityContext);
        setField(testObj, "request", mockRequest);
        setField(testObj, "context", mockServletContext);

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
